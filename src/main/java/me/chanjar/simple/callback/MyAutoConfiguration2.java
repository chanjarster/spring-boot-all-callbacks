package me.chanjar.simple.callback;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static me.chanjar.simple.callback.SpringCallbackLogger.LOGGER;

/**
 * Created by qianjia on 2016/10/4.
 */
@Configuration
@AutoConfigureBefore(MyAutoConfiguration.class)
@ConditionalOnMissingBean(MyAutoConfiguration.class)
public class MyAutoConfiguration2 {

  public MyAutoConfiguration2() {
    LOGGER.info("*** {}", getClass().getSimpleName());
  }

  @Bean
  public String myAutoConfigurationBean2() {
    return "myAutoConfigurationBean2";
  }

}
