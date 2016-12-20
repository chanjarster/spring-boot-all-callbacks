package me.chanjar.simple.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static me.chanjar.simple.SpringCallbackLogger.LOGGER;

/**
 * 在Application扫描路径范围内的Auto-Configuration。
 * 用来说明它是被提前加载的。
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
