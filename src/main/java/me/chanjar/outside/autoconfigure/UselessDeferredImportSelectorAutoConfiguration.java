package me.chanjar.outside.autoconfigure;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotationMetadata;

import static me.chanjar.simple.SpringCallbackLogger.LOGGER;

/**
 * Created by qianjia on 2016/10/4.
 */
@Configuration
@Import(UselessDeferredImportSelectorAutoConfiguration.UselessDeferredImportSelector.class)
public class UselessDeferredImportSelectorAutoConfiguration {

  public UselessDeferredImportSelectorAutoConfiguration() {
    LOGGER.info("*** {}", getClass().getSimpleName());
  }

  /**
   * 在Auto Configuration里使用DeferredImportSelector效果和ImportSelector是一样的
   */
  public static class UselessDeferredImportSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      LOGGER.info("*** {}", getClass().getSimpleName());
      return new String[0];
    }
  }
}
