package me.chanjar.simple.callback;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static me.chanjar.simple.callback.SpringCallbackLogger.LOGGER;

/**
 * Created by qianjia on 2016/10/4.
 */
@Configuration
public class MyConfiguration {

  public MyConfiguration() {
    LOGGER.info("*** {}", getClass().getSimpleName());
  }

  @Bean
  public String myConfigurationBean() {
    return "myConfigurationBean";
  }
}
