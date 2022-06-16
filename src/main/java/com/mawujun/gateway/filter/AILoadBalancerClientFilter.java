package com.mawujun.gateway.filter;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.List;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

public class AILoadBalancerClientFilter extends LoadBalancerClientFilter {
    Logger logger= LoggerFactory.getLogger(AILoadBalancerClientFilter.class);

    @Autowired
    SpringClientFactory clientFactory;

    public AILoadBalancerClientFilter(LoadBalancerClient loadBalancer, LoadBalancerProperties properties) {
        super(loadBalancer, properties);
        logger.info("初始化");
    }

    @Override
    public int getOrder() {
        return LOAD_BALANCER_CLIENT_FILTER_ORDER-1;
    }

    protected ServiceInstance choose(ServerWebExchange exchange) {
        logger.info("请求地址:",exchange.getRequest().getURI());
        String serviceId=((URI) exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR)).getHost();
        //参考RoundRobinRule
        ILoadBalancer lb=clientFactory.getLoadBalancer(serviceId);
        List<Server> reachableServers = lb.getReachableServers();
        Server server =null;

        if(reachableServers!=null && reachableServers.size()>0){
            String requestIP=getIpAddress(exchange.getRequest());
            logger.info("请求节点ip：{}，size={}",requestIP,reachableServers.size());
            for(Server serverTmp:reachableServers){
                logger.info("实例ip：{}",serverTmp.getHost());
                if(serverTmp.getHost().equals(requestIP)){
                    server=serverTmp;
                    logger.info("选择了节点：{}",server.getHost());
                    break;
                }
            }
            if (server != null) {
                return new RibbonLoadBalancerClient.RibbonServer(serviceId, server, false,
                        serverIntrospector(serviceId).getMetadata(server));
            }
        }
        logger.info("使用负载均衡");
        return loadBalancer.choose(serviceId);
    }

    private ServerIntrospector serverIntrospector(String serviceId) {
        ServerIntrospector serverIntrospector = this.clientFactory.getInstance(serviceId,
                ServerIntrospector.class);
        if (serverIntrospector == null) {
            serverIntrospector = new DefaultServerIntrospector();
        }
        return serverIntrospector;
    }

    public String getIpAddress(ServerHttpRequest request) {
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
        return ip;
    }

}
