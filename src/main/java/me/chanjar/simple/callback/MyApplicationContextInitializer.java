package me.chanjar.simple.callback;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static me.chanjar.simple.callback.SpringCallbackLogger.LOGGER;

/**
 * Created by qianjia on 2016/10/2.
 */
public class MyApplicationContextInitializer implements ApplicationContextInitializer {


  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    LOGGER.info("*** {}", getClass().getSimpleName());
  }

}
