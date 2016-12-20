package me.chanjar.simple.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static me.chanjar.simple.SpringCallbackLogger.LOGGER;

/**
 * 在Application扫描路径范围内的Auto-Configuration。
 * 用来说明AutoConfigureBefore和ConditionalOnMissingBean是失效的。
 * Created by qianjia on 2016/10/4.
 */
@Configuration
@AutoConfigureBefore(InsideAutoConfiguration.class)
@ConditionalOnMissingBean(name="insideAutoConfigurationBean")
public class InsideAutoConfiguration2 {

  public InsideAutoConfiguration2() {
    LOGGER.info("*** {}", getClass().getSimpleName());
  }

  @Bean
  public String insideAutoConfigurationBean2() {
    return "insideAutoConfigurationBean2";
  }

}
