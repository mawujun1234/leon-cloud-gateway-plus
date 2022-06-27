package com.mawujun.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SelectedInstanceCallback;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.List;

//通过自定义负载均衡器来达到均衡的目的
public class LoadBalancer implements ReactorServiceInstanceLoadBalancer {
    Logger logger= LoggerFactory.getLogger(ReactiveLoadBalancerClientFilter.class);

    private String serviceId;
    ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    public LoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId=serviceId;
    }

    public Mono<Response<ServiceInstance>> choose(Request request) {
        RequestDataContext context=(RequestDataContext)request.getContext();
        context.getClientRequest();

        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                .getIfAvailable(NoopServiceInstanceListSupplier::new);
        ServerHttpRequest aaaa= ServerHttpRequestUtils.get();
        return supplier.get(request).next()
                .map(serviceInstances -> processInstanceResponse(supplier, serviceInstances,aaaa));
    }

    private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier,
                                                              List<ServiceInstance> serviceInstances,ServerHttpRequest request) {
        //ServerHttpRequest aaaa=ThreadLocalUtils.get();
        Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances,request);
        if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
            ((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
        }
        return serviceInstanceResponse;
    }
    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances,ServerHttpRequest request) {
        if (instances.isEmpty()) {
            return new EmptyResponse();
        }
        String clientIp=getIpStrict(request);//getIpAddress(request);
        if(clientIp!=null && !"".equals(clientIp.trim())){
            //判断是否有微服务的地址是和开发者机器上的地址一样，如果有就使用这个微服务
            for(ServiceInstance serviceInstance:instances){
                if(serviceInstance.getHost().equals(clientIp)){
                    return new DefaultResponse(serviceInstance);
                }
            }
            //没找到匹配的，就返回一个空的
            return new EmptyResponse();
        } else {//如果没有指定，那就走自动判断的路线
            //clientIp=getIpAddress(request);
            clientIp=IpUtils.getIpAddr(request);
            //自动判断
            // 如果能获取到ip，就使用指定机器上的服务进行
            if(clientIp!=null  && clientIp.length() != 0
                    && !"unknown".equalsIgnoreCase(clientIp)
                    && !"127.0.0.1".equalsIgnoreCase(clientIp)){
                for(ServiceInstance serviceInstance:instances){
                    if(serviceInstance.getHost().equals(clientIp)){
                        return new DefaultResponse(serviceInstance);
                    }
                }
            }
            //如果在指定ip的机器上没有找到匹配的服务，就自动找一个微服务提供服务
            int pos = 1;
            ServiceInstance instance = instances.get(pos % instances.size());
            return new DefaultResponse(instance);
        }

    }

    /**
     * 判断当前微服务是否使用指定ip模式，如果是就必须有这个ip的微服务
     * 如果返回null，就表示不使用指定模式
     * @param request
     * @return
     */
    public String getIpStrict(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String strict = headers.getFirst("y-target-host");
        if(strict!=null){
            //指定的微服务使用严格模式
            String[] array=strict.split(",");
            for(String a:array){
                if(a.contains(this.serviceId)){
                    //返回指定的ip地址
                    return a.split(":")[1];
                }
            }
            return null;
        } else {
            return null;
        }
    }
    public String getIpAddress(ServerHttpRequest request) {
        //ServerHttpRequest request=ThreadLocalUtils.get();
        HttpHeaders headers = request.getHeaders();
        String ip = headers.getFirst("x-forwarded-for");
        logger.info("x-forwarded-for：{}",ip);
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];

            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("Proxy-Client-IP");
            logger.info("Proxy-Client-IP：{}",ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("WL-Proxy-Client-IP");
            logger.info("WL-Proxy-Client-IP：{}",ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("HTTP_CLIENT_IP");
            logger.info("HTTP_CLIENT_IP：{}",ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("HTTP_X_FORWARDED_FOR");
            logger.info("HTTP_X_FORWARDED_FOR：{}",ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("X-Real-IP");
            logger.info("X-Real-IP：{}",ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
            logger.info(" request.getRemoteAddress().getAddress().getHostAddress()：{}",ip);
        }
        //如果获取到的是本地，就代表没有获取到，就按照正常的负载均衡逻辑
        if("127.0.0.1".equals(ip)){
            ip= null;
        }
        return ip;
    }

}
