package me.chanjar.simple.callback;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import static me.chanjar.simple.callback.SpringCallbackLogger.LOGGER;
/**
 * Created by qianjia on 2016/10/2.
 */
public class MyApplicationListener implements ApplicationListener {
  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    LOGGER.info("*** {}, event: {}", getClass().getSimpleName(), event.toString());
  }
}
