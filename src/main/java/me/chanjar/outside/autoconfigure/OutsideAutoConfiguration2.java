package me.chanjar.outside.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import static me.chanjar.simple.SpringCallbackLogger.LOGGER;

/**
 * Created by qianjia on 2016/10/4.
 */
@Configuration
@AutoConfigureBefore(OutsideAutoConfiguration.class)
@ConditionalOnMissingBean(name="outsideAutoConfigurationBean")
public class OutsideAutoConfiguration2 {

  public OutsideAutoConfiguration2() {
    LOGGER.info("*** {}", getClass().getSimpleName());
  }

  @Bean
  public String outsideAutoConfigurationBean2() {
    return "outsideAutoConfigurationBean2";
  }

}
