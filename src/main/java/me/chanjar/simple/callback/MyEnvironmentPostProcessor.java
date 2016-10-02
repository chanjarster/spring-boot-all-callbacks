package me.chanjar.simple.callback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Created by qianjia on 2016/10/2.
 */
public class MyEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    System.out.println("me.chanjar.simple.callback.SpringCallbackLogger - *** " + getClass().getSimpleName());
  }

}
