package com.rsrini.stickycache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.rsrini.stickycache.controller.StaticResourceHandler;

@SpringBootApplication
@ComponentScan(basePackages={"com.rsrini.stickycache.services","com.rsrini.stickycache.controller"})
@Import({ StaticResourceHandler.class })
@EnableAutoConfiguration
@Configuration
@PropertySource("classpath:sticky.properties")
public class StickyCacheApplication{

	
	public static void main(String[] args) {
		SpringApplication.run(StickyCacheApplication.class);
	}
	
	 @Bean
     public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
	      return new PropertySourcesPlaceholderConfigurer();
	}
	 
}