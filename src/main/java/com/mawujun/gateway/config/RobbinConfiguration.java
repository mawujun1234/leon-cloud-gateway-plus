package com.mawujun.gateway.config;

import com.mawujun.gateway.filter.AILoadBalancerClientFilter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.DispatcherHandler;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ LoadBalancerClient.class, RibbonAutoConfiguration.class,
        DispatcherHandler.class })
@AutoConfigureAfter(RibbonAutoConfiguration.class)
@EnableConfigurationProperties(LoadBalancerProperties.class)
public class RobbinConfiguration {

    @Bean
    @Profile({"dev","test"})
    public AILoadBalancerClientFilter aiLoadBalancerClientFilter(LoadBalancerClient client, LoadBalancerProperties properties){
        return new AILoadBalancerClientFilter(client,properties);
    }
}
