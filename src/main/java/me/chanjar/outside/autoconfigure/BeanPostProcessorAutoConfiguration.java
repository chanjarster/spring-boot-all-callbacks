package me.chanjar.outside.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static me.chanjar.simple.SpringCallbackLogger.LOGGER;

/**
 * 提供了BeanPostProcessor的Auto-Configuration。
 * 用来说明当Auto-Configuration提供BeanPostProcessor时，会被提前加载。
 * Created by qianjia on 2016/12/20.
 */
@Configuration
public class BeanPostProcessorAutoConfiguration {

  public BeanPostProcessorAutoConfiguration() {
    LOGGER.info("*** {}", getClass().getSimpleName());
  }

  @Bean
  public BeanPostProcessor oneFakeBeanPostProcessor() {

    return new BeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
      }

      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
      }
    };

  }
}
