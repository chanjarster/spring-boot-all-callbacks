# spring-boot-all-callbacks

注：本文基于spring-boot 1.4.0.RELEASE, spring 4.3.2.RELEASE撰写。

## 启动顺序

Spring boot的启动代码一般是这样的：

```java
@SpringBootApplication
public class SampleApplication {
  public static void main(String[] args) throws Exception {
    SpringApplication.run(SampleApplication.class, args);
  }
}
```

### 初始化SpringApplication

1. ``SpringApplication#run(Object source, String... args)``[#L1173][SpringApplicationL1173]
1. [SpringApplication#L1185][SpringApplicationL1185] call ``SpringApplication(sources)``[#L236][SpringApplicationL236]
  1. ``SpringApplication#initialize(Object[] sources)``[#L256][SpringApplicationL256] [javadoc][SpringApplication]
    1. [SpringApplication#L257][SpringApplicationL257] 添加source（复数），``SpringApplication``使用source来构建Bean。一般来说在``run``的时候都会把``@SpringBootApplication``标记的类放到``sources``参数里，然后由这个类出发找到Bean的定义。
    2. [SpringApplication#L261][SpringApplicationL261] 初始化``ApplicationContextInitializer``列表（见附录）
    3. [SpringApplication#L263][SpringApplicationL263] 初始化``ApplicationListener``列表（见附录）
1. [SpringApplication#L1185][SpringApplicationL1185] call ``SpringApplication#run(args)``[#L297][SpringApplicationL297]，进入运行阶段

所以在spring boot应用的初始化阶段只需要``ApplicationContextInitializer``和``ApplicationListener``。由此可以推断，诸如``@Configuration``的工作、Bean定义加载、Bean创建等工作等都是在后续阶段进行的。


### 推送ApplicationStartedEvent

``SpringApplication#run(args)``[#L297][SpringApplicationL297]

1. [SpringApplication#L302][SpringApplicationL302] 初始化``SpringApplicationRunListeners``（见附录）。它内部只包含``org.springframework.boot.context.event.EventPublishingRunListener``。
1. [SpringApplication#L303][SpringApplicationL303] 推送``ApplicationStartedEvent``给所有的``SpringApplication#listeners``。 下面是关心此事件的listener：
    1. LiquibaseServiceLocatorApplicationListener
    1. [LoggingApplicationListener][LoggingApplicationListener]（见附录）

### 准备Environment

``SpringApplication#run(args)``[#L297][SpringApplicationL297]，[#L307][SpringApplicationL307]，[#L329][SpringApplicationL329]准备[ConfigurableEnvironment][ConfigurableEnvironment]。

1. [SpringApplication#L333][SpringApplicationL333] 创建[StandardEnvironment][StandardEnvironment]（见附录）。
1. [SpringApplication#L334][SpringApplicationL334] 配置[StandardEnvironment][StandardEnvironment]，将命令行和默认参数整吧整吧，添加到[MutablePropertySources][MutablePropertySources]。
1. [SpringApplication#L335][SpringApplicationL335] 推送``ApplicationEnvironmentPreparedEvent``给所有的``SpringApplication#listeners``。下面是关心此事件的listener:
  1. BackgroundPreinitializer
  1. FileEncodingApplicationListener
  1. AnsiOutputApplicationListener
  1. [ConfigFileApplicationListener][ConfigFileApplicationListener]（见附录）
  1. DelegatingApplicationListener
  1. ClasspathLoggingApplicationListener
  1. [LoggingApplicationListener][LoggingApplicationListener]
  1. ApplicationPidFileWriter

可以参考[官方文档][boot-features-external-config]了解[StandardEnvironment][StandardEnvironment]构建好之后，其[MutablePropertySources][MutablePropertySources]内部到底有些啥东东。

### 准备ApplicationContext

``SpringApplication#run(args)``[#L297][SpringApplicationL297]，[#L310][SpringApplicationL310]创建ApplicationContext（实际上创建的是[AnnotationConfigApplicationContext][AnnotationConfigApplicationContext]或[AnnotationConfigEmbeddedWebApplicationContext][AnnotationConfigEmbeddedWebApplicationContext]）。

然后[SpringApplication#L311][SpringApplicationL311]，[#L342][SpringApplicationL342]准备ApplicationContext

TODO

### 刷新ApplicationContext

``SpringApplication#run(args)``[#L297][SpringApplicationL297]

TODO

### 推送ApplicationReadyEvent or ApplicationFailedEvent

``SpringApplication#run(args)``[#L297][SpringApplicationL297]

TODO


## 回调接口

### ApplicationContextInitializer

[javadoc][ApplicationContextInitializer] [相关文档][howto-customize-the-environment-or-application-context]

加载方式：读取classpath*:META-INF/spring.factories中key等于org.springframework.context.ApplicationContextInitializer的property列出的类

排序方式：[AnnotationAwareOrderComparator][AnnotationAwareOrderComparator]

已知清单1：spring-boot-1.4.0.RELEASE.jar!/META-INF/spring.factories

1. org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer
1. org.springframework.boot.context.ContextIdApplicationContextInitializer
1. org.springframework.boot.context.config.DelegatingApplicationContextInitializer
1. org.springframework.boot.context.web.ServerPortInfoApplicationContextInitializer

已知清单2：spring-boot-autoconfigure-1.4.0.RELEASE.jar!/META-INF/spring.factories

1. org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer
1. org.springframework.boot.autoconfigure.logging.AutoConfigurationReportLoggingInitializer


### ApplicationListener

[javadoc][ApplicationListener] [相关文档][howto-customize-the-environment-or-application-context]

加载方式：读取classpath*:META-INF/spring.factories中key等于org.springframework.context.ApplicationListener的property列出的类

排序方式：[AnnotationAwareOrderComparator][AnnotationAwareOrderComparator]

已知清单1：spring-boot-1.4.0.RELEASE.jar!/META-INF/spring.factories中定义的

1. org.springframework.boot.ClearCachesApplicationListener
1. org.springframework.boot.builder.ParentContextCloserApplicationListener
1. org.springframework.boot.context.FileEncodingApplicationListener
1. org.springframework.boot.context.config.AnsiOutputApplicationListener
1. org.springframework.boot.context.config.ConfigFileApplicationListener
1. org.springframework.boot.context.config.DelegatingApplicationListener
1. org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener
1. org.springframework.boot.logging.ClasspathLoggingApplicationListener
1. org.springframework.boot.logging.LoggingApplicationListener


已知清单2：spring-boot-autoconfigure-1.4.0.RELEASE.jar!/META-INF/spring.factories中定义的

1. org.springframework.boot.autoconfigure.BackgroundPreinitializer

### SpringApplicationRunListener

[javadoc][SpringApplicationRunListener] 

加载方式：读取classpath*:META-INF/spring.factories中key等于org.springframework.boot.SpringApplicationRunListener的property列出的类

排序方式：[AnnotationAwareOrderComparator][AnnotationAwareOrderComparator]

已知清单：spring-boot-1.4.0.RELEASE.jar!/META-INF/spring.factories定义的

1. org.springframework.boot.context.event.EventPublishingRunListener

### EnvironmentPostProcessor

[EnvironmentPostProcessor][EnvironmentPostProcessor]可以用来自定义[StandardEnvironment][StandardEnvironment]（[相关文档][howto-customize-the-environment-or-application-context]）。

加载方式：读取classpath*:META-INF/spring.factories中key等于org.springframework.boot.env.EnvironmentPostProcessor的property列出的类

排序方式：[AnnotationAwareOrderComparator][AnnotationAwareOrderComparator]

已知清单：spring-boot-1.4.0.RELEASE.jar!/META-INF/spring.factories定义的

1. org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor
1. org.springframework.boot.env.SpringApplicationJsonEnvironmentPostProcessor


## 内置类说明

### LoggingApplicationListener

[LoggingApplicationListener][LoggingApplicationListener]用来配置日志系统的，比如logback、log4j。Spring boot对于日志有[详细解释][boot-features-logging]，如果你想[自定义日志配置][boot-features-custom-log-configuration]，那么也请参考本文中对于[LoggingApplicationListener][LoggingApplicationListener]的被调用时机的说明以获得更深入的了解。

### StandardEnvironment

[StandardEnvironment][StandardEnvironment]有一个[MutablePropertySources][MutablePropertySources]，它里面有多个[PropertySource][PropertySource]，[PropertySource][PropertySource]负责提供property（即property的提供源），目前已知的[PropertySource][PropertySource]实现有：MapPropertySource、SystemEnvironmentPropertySource、CommandLinePropertySource等。当[StandardEnvironment][StandardEnvironment]查找property值的时候，是从[MutablePropertySources][MutablePropertySources]里依次查找的，而且一旦查找到就不再查找，也就是说如果要覆盖property的值，那么就得提供顺序在前的[PropertySource][PropertySource]。

### ConfigFileApplicationListener

[ConfigFileApplicationListener][ConfigFileApplicationListener]用来将``application.properties``加载到[StandardEnvironment][StandardEnvironment]中。

[ConfigFileApplicationListener][ConfigFileApplicationListener]内部使用了[EnvironmentPostProcessor][EnvironmentPostProcessor]（见附录）自定义[StandardEnvironment][StandardEnvironment]



  [AnnotationAwareOrderComparator]: http://docs.spring.io/spring/docs/4.3.2.RELEASE/javadoc-api/org/springframework/core/annotation/AnnotationAwareOrderComparator.html
  [SpringApplicationL1173]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L1173
  [SpringApplicationL1185]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L1185
  [SpringApplicationL236]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L236
  [SpringApplicationL256]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L256
  [SpringApplicationL257]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L257
  [SpringApplicationL261]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L261
  [SpringApplicationL263]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L263
  [SpringApplicationL297]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L297
  [SpringApplicationL302]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L302
  [SpringApplicationL303]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L303
  [SpringApplicationL307]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L307
  [SpringApplicationL333]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L333
  [SpringApplicationL334]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L334
  [SpringApplicationL335]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L335
  [SpringApplicationL310]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L310
  [SpringApplicationL311]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L311
  [SpringApplicationL329]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L329
  [SpringApplicationL342]: https://github.com/spring-projects/spring-boot/blob/v1.4.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L342
  [ConfigurableEnvironment]: http://docs.spring.io/spring/docs/4.3.2.RELEASE/javadoc-api/org/springframework/core/env/ConfigurableEnvironment.html
  [SpringApplication]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/api/org/springframework/boot/SpringApplication.html
  [StandardEnvironment]: http://docs.spring.io/spring/docs/4.3.2.RELEASE/javadoc-api/org/springframework/core/env/StandardEnvironment.html
  [MutablePropertySources]: http://docs.spring.io/spring/docs/4.3.2.RELEASE/javadoc-api/org/springframework/core/env/MutablePropertySources.html
  [PropertySource]: http://docs.spring.io/spring/docs/4.3.2.RELEASE/javadoc-api/org/springframework/core/env/PropertySource.html
  [ConfigFileApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/api/org/springframework/boot/context/config/ConfigFileApplicationListener.html
  [boot-features-external-config]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/reference/htmlsingle/#boot-features-external-config
  [ApplicationContextInitializer]: http://docs.spring.io/spring/docs/4.3.2.RELEASE/javadoc-api/org/springframework/context/ApplicationContextInitializer.html
  [ApplicationListener]: http://docs.spring.io/spring/docs/4.3.2.RELEASE/javadoc-api/org/springframework/context/ApplicationListener.html
  [LoggingApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/api/org/springframework/boot/logging/LoggingApplicationListener.html
  [SpringApplicationRunListener]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/api/org/springframework/boot/SpringApplicationRunListener.html
  [EnvironmentPostProcessor]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/api/org/springframework/boot/env/EnvironmentPostProcessor.html
  [boot-features-logging]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/reference/htmlsingle/#boot-features-logging
  [boot-features-custom-log-configuration]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/reference/htmlsingle/#boot-features-custom-log-configuration
  [howto-customize-the-environment-or-application-context]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/reference/htmlsingle/#howto-customize-the-environment-or-application-context
  [AnnotationConfigApplicationContext]: http://docs.spring.io/spring/docs/4.3.2.RELEASE/javadoc-api/org/springframework/context/annotation/AnnotationConfigApplicationContext.html
  [AnnotationConfigEmbeddedWebApplicationContext]: http://docs.spring.io/spring-boot/docs/1.4.0.RELEASE/api/org/springframework/boot/context/embedded/AnnotationConfigEmbeddedWebApplicationContext.html
