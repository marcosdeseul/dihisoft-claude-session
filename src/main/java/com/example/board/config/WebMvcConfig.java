package com.example.board.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("forward:/index.html");
    registry
        .addViewController("/{path:^(?!api|h2-console|assets)[^.]*}")
        .setViewName("forward:/index.html");
    registry
        .addViewController("/{path:^(?!api|h2-console|assets)[^.]*}/**")
        .setViewName("forward:/index.html");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/index.html").addResourceLocations("classpath:/static/");
    registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets/");
  }
}
