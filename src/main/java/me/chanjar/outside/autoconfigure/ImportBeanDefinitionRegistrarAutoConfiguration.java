package me.chanjar.outside.autoconfigure;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import static me.chanjar.simple.SpringCallbackLogger.LOGGER;

/**
 * 导入ImportBeanDefinitionRegistrar的Auto-Configuration。
 * 用来说明当使用ImportBeanDefinitionRegistrar时，ImportBeanDefinitionRegistrar会被提前调用。
 * Created by qianjia on 2016/12/20.
 */
@Import(ImportBeanDefinitionRegistrarAutoConfiguration.FakeImportBeanDefinitionRegistrar.class)
@Configuration
public class ImportBeanDefinitionRegistrarAutoConfiguration {

  public ImportBeanDefinitionRegistrarAutoConfiguration() {
    LOGGER.info("*** {}", getClass().getSimpleName());
  }

  public static class FakeImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

      LOGGER.info("*** {}", getClass().getSimpleName());

    }
  }
}
