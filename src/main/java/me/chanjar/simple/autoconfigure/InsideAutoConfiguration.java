package me.chanjar.simple.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static me.chanjar.simple.SpringCallbackLogger.LOGGER;

/**
 * Created by qianjia on 2016/10/4.
 */
@Configuration
public class InsideAutoConfiguration {

  public InsideAutoConfiguration() {
    LOGGER.info("*** {}", getClass().getSimpleName());
  }

  @Bean
  public String insideAutoConfigurationBean() {
    return "insideAutoConfigurationBean";
  }

}
