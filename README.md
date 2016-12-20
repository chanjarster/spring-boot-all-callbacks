# Spring Boot启动过程及回调接口汇总

相关代码在： [https://github.com/chanjarster/spring-boot-all-callbacks](https://github.com/chanjarster/spring-boot-all-callbacks)

注：本文基于[spring-boot 1.4.1.RELEASE][code-spring-boot-1.4.1.RELEASE], [spring 4.3.3.RELEASE][code-spring-4.3.3.RELEASE]撰写。

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

1. ``SpringApplication#run(Object source, String... args)``[#L1174][code-SpringApplicationL1174]
1. [SpringApplication#L1186][code-SpringApplicationL1186] -> ``SpringApplication(sources)``[#L236][code-SpringApplicationL236]
  1. ``SpringApplication#initialize(Object[] sources)``[#L256][code-SpringApplicationL256] [javadoc][boot-SpringApplication]
    1. [SpringApplication#L257][code-SpringApplicationL257] 添加source（复数），``SpringApplication``使用source来构建Bean。一般来说在``run``的时候都会把[@SpringBootApplication][boot-SpringBootApplication]标记的类(本例中是SampleApplication)放到``sources``参数里，然后由这个类出发找到Bean的定义。
    2. [SpringApplication#L261][code-SpringApplicationL261] 初始化[ApplicationContextInitializer][core-ApplicationContextInitializer]列表（见附录）
    3. [SpringApplication#L263][code-SpringApplicationL263] 初始化[ApplicationListener][core-ApplicationListener]列表（见附录）
1. [SpringApplication#L1186][code-SpringApplicationL1186] -> ``SpringApplication#run(args)``[#L297][code-SpringApplicationL297]，进入运行阶段


### 推送ApplicationStartedEvent

``SpringApplication#run(args)``[#L297][code-SpringApplicationL297]

1. [SpringApplication#L303][code-SpringApplicationL303] 初始化[SpringApplicationRunListeners][code-SpringApplicationRunListeners] ([SpringApplicationRunListener][boot-SpringApplicationRunListener]的集合)。它内部只包含[EventPublishingRunListener][boot-EventPublishingRunListener]。
1. [SpringApplication#L304][code-SpringApplicationL304] 推送[ApplicationStartedEvent][boot-ApplicationStartedEvent]给所有的[ApplicationListener][core-ApplicationListener]（见附录）。 下面是关心此事件的listener：
    1. [LiquibaseServiceLocatorApplicationListener][boot-LiquibaseServiceLocatorApplicationListener]
    1. [LoggingApplicationListener][boot-LoggingApplicationListener]（见附录）

### 准备Environment

``SpringApplication#run(args)``[#L297][code-SpringApplicationL297]->[#L308][code-SpringApplicationL308]->``prepareEnvironment(...)``[#L331][code-SpringApplicationL331]准备[ConfigurableEnvironment][core-ConfigurableEnvironment]。

1. [SpringApplication#L335][code-SpringApplicationL335] 创建[StandardEnvironment][core-StandardEnvironment]（见附录）。
1. [SpringApplication#L336][code-SpringApplicationL336] 配置[StandardEnvironment][core-StandardEnvironment]，将命令行和默认参数整吧整吧，添加到[MutablePropertySources][core-MutablePropertySources]。
1. [SpringApplication#L337][code-SpringApplicationL337] 推送[ApplicationEnvironmentPreparedEvent][boot-ApplicationEnvironmentPreparedEvent]给所有的[ApplicationListener][core-ApplicationListener]（见附录）。下面是关心此事件的listener:
  1. [BackgroundPreinitializer][boot-BackgroundPreinitializer]
  1. [FileEncodingApplicationListener][boot-FileEncodingApplicationListener]
  1. [AnsiOutputApplicationListener][boot-AnsiOutputApplicationListener]
  1. [ConfigFileApplicationListener][boot-ConfigFileApplicationListener]（见附录）
  1. [DelegatingApplicationListener][boot-DelegatingApplicationListener]
  1. [ClasspathLoggingApplicationListener][boot-ClasspathLoggingApplicationListener]
  1. [LoggingApplicationListener][boot-LoggingApplicationListener]
  1. [ApplicationPidFileWriter][boot-ApplicationPidFileWriter]

可以参考[官方文档][ref-boot-features-external-config]了解[StandardEnvironment][core-StandardEnvironment]构建好之后，其[MutablePropertySources][core-MutablePropertySources]内部到底有些啥东东。

### 创建及准备ApplicationContext

``SpringApplication#run(args)``[#L297][code-SpringApplicationL297]

1. [SpringApplication#L311][code-SpringApplicationL311]->``SpringApplication#createApplicationContext()``[#L583][code-SpringApplicationL583]创建[ApplicationContext][core-ApplicationContext]。可以看到实际上创建的是[AnnotationConfigApplicationContext][core-AnnotationConfigApplicationContext]或[AnnotationConfigEmbeddedWebApplicationContext][boot-AnnotationConfigEmbeddedWebApplicationContext]。
  1. 在构造[AnnotationConfigApplicationContext][core-AnnotationConfigApplicationContext]的时候，间接注册了一个[BeanDefinitionRegistryPostProcessor][core-BeanDefinitionRegistryPostProcessor]的Bean：[ConfigurationClassPostProcessor][core-ConfigurationClassPostProcessor]。经由[AnnotatedBeanDefinitionReader][core-AnnotatedBeanDefinitionReader][构造函数][code-AnnotatedBeanDefinitionReader#L83]->[AnnotationConfigUtils.registerAnnotationConfigProcessors][code-AnnotationConfigUtils#L160]。
1. [SpringApplication#L313][code-SpringApplicationL313]->``SpringApplication#prepareContext(...)``[#L344][code-SpringApplicationL344]准备[ApplicationContext][core-ApplicationContext]
  1. [SpringApplication#L347][code-SpringApplicationL347]->``context.setEnvironment(environment)``，把之前准备好的[Environment][core-Environment]塞给[ApplicationContext][core-ApplicationContext]
  1. [SpringApplication#L348][code-SpringApplicationL348]->``postProcessApplicationContext(context)``[#L605][code-SpringApplication#L605]，给[ApplicationContext][core-ApplicationContext]设置了一些其他东西
  1. [SpringApplication#L349][code-SpringApplicationL349]->``applyInitializers(context)``[#L630][code-SpringApplication#L630]，调用之前准备好的[ApplicationContextInitializer][core-ApplicationContextInitializer]
  1. [SpringApplication#L350][code-SpringApplicationL350]->``listeners.contextPrepared(context)``->[EventPublishingRunListener.contextPrepared][code-EventPublishingRunListener#L73]，但实际上啥都没做。
  1. [SpringApplication#L366][code-SpringApplicationL366]->``load``[#L687][code-SpringApplication#L687]，负责将source(复数)里所定义的Bean加载到[ApplicationContext][core-ApplicationContext]里，在本例中就是SampleApplication，这些source是在**初始化SpringApplication**阶段获得的。
  1. [SpringApplication#L367][code-SpringApplicationL367]->``listeners.contextLoaded(context)``->[EventPublishingRunListener.contextLoaded][code-EventPublishingRunListener#L78]。
    1. 将[SpringApplication][boot-SpringApplication]自己拥有的[ApplicationListener][core-ApplicationListener]加入到[ApplicationContext][core-ApplicationContext]
    1. 发送[ApplicationPreparedEvent][boot-ApplicationPreparedEvent]。目前已知关心这个事件的有[ConfigFileApplicationListener][boot-ConfigFileApplicationListener]、[LoggingApplicationListener][boot-LoggingApplicationListener]、[ApplicationPidFileWriter][boot-ApplicationPidFileWriter]
  
要注意的是在这个阶段，[ApplicationContext][core-ApplicationContext]里只有SampleApplication，SampleApplication是Bean的加载工作的起点。

### 刷新ApplicationContext

根据前面所讲，这里的[ApplicationContext][core-ApplicationContext]实际上是[GenericApplicationContext][core-GenericApplicationContext]
->[AnnotationConfigApplicationContext][core-AnnotationConfigApplicationContext]或者[AnnotationConfigEmbeddedWebApplicationContext][boot-AnnotationConfigEmbeddedWebApplicationContext]

``SpringApplication#run(args)``[#L297][code-SpringApplicationL297]
->[#L315][code-SpringApplicationL315]->``SpringApplication#refreshContext(context)``[#L370][code-SpringApplicationL370]
->[#L371][code-SpringApplicationL371]->``SpringApplication#refresh(context)``[#L759][code-SpringApplicationL759]
->[#L761][code-SpringApplicationL761]->``AbstractApplicationContext#refresh``[AbstractApplicationContext#L507][code-AbstractApplicationContext#L507]

1. [AbstractApplicationContext#L510][code-AbstractApplicationContext#L510]->``AbstractApplicationContext#prepareRefresh()``[#L575][code-AbstractApplicationContext#L575]，做了一些初始化工作，比如设置了当前Context的状态，初始化propertySource（其实啥都没干），检查required的property是否都已在Environment中（其实并没有required的property可供检查）等。
1. [AbstractApplicationContext#L513][code-AbstractApplicationContext#L513]->``obtainFreshBeanFactory()``[#L611][code-AbstractApplicationContext#L611]，获得[BeanFactory][core-BeanFactory]，实际上这里获得的是[DefaultListableBeanFactory][core-DefaultListableBeanFactory]
1. [AbstractApplicationContext#L516][code-AbstractApplicationContext#L516]->``prepareBeanFactory(beanFactory)``[#L625][code-AbstractApplicationContext#L625]准备[BeanFactory][core-BeanFactory]
  1. 给beanFactory设置了ClassLoader
  1. 给beanFactory设置了[SpEL解析器][core-StandardBeanExpressionResolver]
  1. 给beanFactory设置了[PropertyEditorRegistrar][core-PropertyEditorRegistrar]
  1. 给beanFactory添加了[ApplicationContextAwareProcessor][code-ApplicationContextAwareProcessor]（[BeanPostProcessor][core-BeanPostProcessor]的实现类），需要注意的是它是第一个被添加到[BeanFactory][core-BeanFactory]的[BeanPostProcessor][core-BeanPostProcessor]
  1. 给beanFactory设置忽略解析以下类的依赖：[ResourceLoaderAware][core-ResourceLoaderAware]、[ApplicationEventPublisherAware][core-ApplicationEventPublisherAware]、[MessageSourceAware][core-MessageSourceAware]、[ApplicationContextAware][core-ApplicationContextAware]、[EnvironmentAware][core-EnvironmentAware]。原因是注入这些回调接口本身没有什么意义。
  1. 给beanFactory添加了以下类的依赖解析：[BeanFactory][core-BeanFactory]、[ResourceLoader][core-ResourceLoader]、[ApplicationEventPublisher][core-ApplicationEventPublisher]、[ApplicationContext][core-ApplicationContext]
  1. 给beanFactory添加[LoadTimeWeaverAwareProcessor][core-LoadTimeWeaverAwareProcessor]用来处理[LoadTimeWeaverAware][core-LoadTimeWeaverAware]的回调，在和AspectJ集成的时候会用到
  1. 把``getEnvironment()``作为Bean添加到beanFactory中，Bean Name: ``environment``
  1. 把``getEnvironment().getSystemProperties()``作为Bean添加到beanFactory中，Bean Name: ``systemProperties``
  1. 把``getEnvironment().getSystemEnvironment()``作为Bean添加到beanFactory中，Bean Name: ``systemEnvironment``
1. [AbstractApplicationContext#L520][code-AbstractApplicationContext#L520]->``postProcessBeanFactory(beanFactory)``，后置处理[BeanFactory][core-BeanFactory]，实际啥都没做
1. [AbstractApplicationContext#L523][code-AbstractApplicationContext#L523]->``invokeBeanFactoryPostProcessors(beanFactory)``，利用[BeanFactoryPostProcessor][core-BeanFactoryPostProcessor]，对beanFactory做后置处理。调用此方法时有四个[BeanFactoryPostProcessor][core-BeanFactoryPostProcessor]：
  1. [SharedMetadataReaderFactoryContextInitializer][code-SharedMetadataReaderFactoryContextInitializer]的内部类[CachingMetadataReaderFactoryPostProcessor][code-CachingMetadataReaderFactoryPostProcessor]，是在 **创建及准备ApplicationContext 2.3** 时添加的：[#L57][code-SharedMetadataReaderFactoryContextInitializer#L57]
  1. [ConfigurationWarningsApplicationContextInitializer][boot-ConfigurationWarningsApplicationContextInitializer]的内部类[ConfigurationWarningsPostProcessor][code-ConfigurationWarningsApplicationContextInitializer#L75]，是在 **创建及准备ApplicationContext 2.3** 时添加的：[#L60][code-ConfigurationWarningsApplicationContextInitializer#L60]
  1. [ConfigFileApplicationListener][boot-ConfigFileApplicationListener]的内部类[PropertySourceOrderingPostProcessor][code-PropertySourceOrderingPostProcessor]，是在 **创建及准备ApplicationContext 2.6** 时添加的：[#L158][code-ConfigFileApplicationListener#L158]->[#L199][code-ConfigFileApplicationListener#L199]->[#L244][code-ConfigFileApplicationListener#L244]
  1. [ConfigurationClassPostProcessor][core-ConfigurationClassPostProcessor]，负责读取[BeanDefinition][core-BeanDefinition]是在 **创建及准备ApplicationContext 1.1** 时添加的
1. [AbstractApplicationContext#L526][code-AbstractApplicationContext#L526]->``registerBeanPostProcessors(beanFactory)``，注册[BeanPostProcessor][core-BeanPostProcessor]
1. [AbstractApplicationContext#L529][code-AbstractApplicationContext#L529]->``initMessageSource()``[#L704][code-AbstractApplicationContext#L704]，初始化[MessageSource][core-MessageSource]，不过其实此时的[MessageSource][core-MessageSource]是个Noop对象。
1. [AbstractApplicationContext#L532][code-AbstractApplicationContext#L532]->``initApplicationEventMulticaster()``[#L739][code-AbstractApplicationContext#L739]，初始化[ApplicationEventMulticaster][core-ApplicationEventMulticaster]。
1. [AbstractApplicationContext#L535][code-AbstractApplicationContext#L535]->``onRefresh()``[#L793][code-AbstractApplicationContext#L793]，这个方法啥都没做
1. [AbstractApplicationContext#L538][code-AbstractApplicationContext#L538]->``registerListeners()``[#L801][code-AbstractApplicationContext#L801]，把自己的[ApplicationListener][core-ApplicationListener]注册到[ApplicationEventMulticaster][core-ApplicationEventMulticaster]里，并且将之前因为没有[ApplicationEventMulticaster][core-ApplicationEventMulticaster]而无法发出的[ApplicationEvent][core-ApplicationEvent]发送出去。
1. [AbstractApplicationContext#L541][code-AbstractApplicationContext#L541]->``finishBeanFactoryInitialization``[#L828][code-AbstractApplicationContext#L828]。注意[#L861][code-AbstractApplicationContext#L861]，在这一步的时候才会实例化所有non-lazy-init bean，这里说的实例化不只是new而已，注入、[BeanPostProcessor][core-BeanPostProcessor]都会执行。
1. [AbstractApplicationContext#L544][code-AbstractApplicationContext#L544]->``finishRefresh()``[#L869][code-AbstractApplicationContext#L869]。
  1. 在[#L877][code-AbstractApplicationContext#L877]发送了[ContextRefreshedEvent][core-ContextRefreshedEvent]

### 调用 ApplicationRunner 和 CommandLineRunner

``SpringApplication#run(args)``[#L297][code-SpringApplicationL297]
->``afterRefresh(context, applicationArguments)``[#L316][code-SpringApplicationL316]
->``callRunners(context, args)``[#L771][code-SpringApplicationL771]
->[#L774][code-SpringApplicationL774] 先后调用了当前[ApplicationContext][core-ApplicationContext]中的[ApplicationRunner][boot-ApplicationRunner]和[CommandLineRunner][boot-CommandLineRunner]。关于它们的相关文档可以看[这里][ref-boot-features-command-line-runner]。

需要注意的是，此时的[ApplicationContext][core-ApplicationContext]已经刷新完毕了，该有的Bean都已经有了。

### 推送ApplicationReadyEvent or ApplicationFailedEvent

``SpringApplication#run(args)``[#L297][code-SpringApplicationL297]->``listeners.finished(context, null)``[#L317][code-SpringApplicationL317]
间接地调用了``EventPublishingRunListener#getFinishedEvent``[EventPublishingRunListener#L96][code-EventPublishingRunListener#L96]，发送了[ApplicationReadyEvent][boot-ApplicationReadyEvent]或[ApplicationFailedEvent][boot-ApplicationFailedEvent]



## 回调接口

### ApplicationContextInitializer

[javadoc][core-ApplicationContextInitializer] [相关文档][ref-boot-howto-customize-the-environment-or-application-context]

加载方式：读取``classpath*:META-INF/spring.factories``中key等于``org.springframework.context.ApplicationContextInitializer``的property列出的类

排序方式：[AnnotationAwareOrderComparator][core-AnnotationAwareOrderComparator]

已知清单1：spring-boot-1.4.1.RELEASE.jar!/META-INF/spring.factories

1. [ConfigurationWarningsApplicationContextInitializer][boot-ConfigurationWarningsApplicationContextInitializer]（优先级：0）
1. [ContextIdApplicationContextInitializer][boot-ContextIdApplicationContextInitializer]（优先级：Ordered.LOWEST_PRECEDENCE - 10）
1. [DelegatingApplicationContextInitializer][boot-DelegatingApplicationContextInitializer]（优先级：无=Ordered.LOWEST_PRECEDENCE）
1. [ServerPortInfoApplicationContextInitializer][boot-ServerPortInfoApplicationContextInitializer]（优先级：无=Ordered.LOWEST_PRECEDENCE）

已知清单2：spring-boot-autoconfigure-1.4.1.RELEASE.jar!/META-INF/spring.factories

1. [SharedMetadataReaderFactoryContextInitializer][code-SharedMetadataReaderFactoryContextInitializer]（优先级：无=Ordered.LOWEST_PRECEDENCE）
1. [AutoConfigurationReportLoggingInitializer][boot-AutoConfigurationReportLoggingInitializer]（优先级：无=Ordered.LOWEST_PRECEDENCE）

### ApplicationListener

[javadoc][core-ApplicationListener] [相关文档][ref-boot-howto-customize-the-environment-or-application-context]

加载方式：读取``classpath*:META-INF/spring.factories``中key等于``org.springframework.context.ApplicationListener``的property列出的类

排序方式：[AnnotationAwareOrderComparator][core-AnnotationAwareOrderComparator]

已知清单1：spring-boot-1.4.1.RELEASE.jar!/META-INF/spring.factories中定义的

1. [ClearCachesApplicationListener][boot-ClearCachesApplicationListener]（优先级：无=Ordered.LOWEST_PRECEDENCE）
1. [ParentContextCloserApplicationListener][boot-ParentContextCloserApplicationListener]（优先级：Ordered.LOWEST_PRECEDENCE - 10）
1. [FileEncodingApplicationListener][boot-FileEncodingApplicationListener]（优先级：Ordered.LOWEST_PRECEDENCE）
1. [AnsiOutputApplicationListener][boot-AnsiOutputApplicationListener]（优先级：ConfigFileApplicationListener.DEFAULT_ORDER + 1）
1. [ConfigFileApplicationListener][boot-ConfigFileApplicationListener]（优先级：Ordered.HIGHEST_PRECEDENCE + 10）
1. [DelegatingApplicationListener][boot-DelegatingApplicationListener]（优先级：0)
1. [LiquibaseServiceLocatorApplicationListener][boot-LiquibaseServiceLocatorApplicationListener]（优先级：无=Ordered.LOWEST_PRECEDENCE）
1. [ClasspathLoggingApplicationListener][boot-ClasspathLoggingApplicationListener]（优先级：LoggingApplicationListener的优先级 + 1）
1. [LoggingApplicationListener][boot-LoggingApplicationListener]（优先级：Ordered.HIGHEST_PRECEDENCE + 20）

已知清单2：spring-boot-autoconfigure-1.4.1.RELEASE.jar!/META-INF/spring.factories中定义的

1. [BackgroundPreinitializer][boot-BackgroundPreinitializer]

### SpringApplicationRunListener

[javadoc][boot-SpringApplicationRunListener] 

加载方式：读取``classpath*:META-INF/spring.factories``中key等于``org.springframework.boot.SpringApplicationRunListener``的property列出的类

排序方式：[AnnotationAwareOrderComparator][core-AnnotationAwareOrderComparator]

已知清单：spring-boot-1.4.1.RELEASE.jar!/META-INF/spring.factories定义的

1. org.springframework.boot.context.event.EventPublishingRunListener（优先级：0）

### EnvironmentPostProcessor

[EnvironmentPostProcessor][boot-EnvironmentPostProcessor]可以用来自定义[StandardEnvironment][core-StandardEnvironment]（[相关文档][ref-boot-howto-customize-the-environment-or-application-context]）。

加载方式：读取``classpath*:META-INF/spring.factories``中key等于``org.springframework.boot.env.EnvironmentPostProcessor``的property列出的类

排序方式：[AnnotationAwareOrderComparator][core-AnnotationAwareOrderComparator]

已知清单：spring-boot-1.4.1.RELEASE.jar!/META-INF/spring.factories定义的

1. [CloudFoundryVcapEnvironmentPostProcessor][core-CloudFoundryVcapEnvironmentPostProcessor]（优先级：ConfigFileApplicationListener.DEFAULT_ORDER - 1）
1. [SpringApplicationJsonEnvironmentPostProcessor][boot-SpringApplicationJsonEnvironmentPostProcessor]（优先级：Ordered.HIGHEST_PRECEDENCE + 5）

### BeanPostProcessor

[javadoc][core-BeanPostProcessor] [相关文档][ref-beans-factory-extension-bpp]

用来对Bean**实例**进行修改的勾子，根据Javadoc ApplicationContext会自动侦测到BeanPostProcessor Bean，然后将它们应用到后续创建的所有Bean上。

### BeanFactoryPostProcessor和BeanDefinitionRegistryPostProcessor

[相关文档][ref-beans-factory-extension-factory-postprocessors]

[PostProcessorRegistrationDelegate][code-PostProcessorRegistrationDelegate#L57]负责调用[BeanDefinitionRegistryPostProcessor][core-BeanDefinitionRegistryPostProcessor]和[BeanFactoryPostProcessor][core-BeanFactoryPostProcessor]。
[BeanDefinitionRegistryPostProcessor][core-BeanDefinitionRegistryPostProcessor]在[BeanFactoryPostProcessor][core-BeanFactoryPostProcessor]之前被调用。


### *Aware

*Aware是一类可以用来获得Spring对象的interface，这些interface都继承了[Aware][core-Aware]，已知的有：

* [ApplicationEventPublisherAware][core-ApplicationEventPublisherAware]
* [NotificationPublisherAware][core-NotificationPublisherAware]
* [MessageSourceAware][core-MessageSourceAware]
* [EnvironmentAware][core-EnvironmentAware]
* [BeanFactoryAware][core-BeanFactoryAware]
* [EmbeddedValueResolverAware][core-EmbeddedValueResolverAware]
* [ResourceLoaderAware][core-ResourceLoaderAware]
* [ImportAware][core-ImportAware]
* [LoadTimeWeaverAware][core-LoadTimeWeaverAware]
* [BeanNameAware][core-BeanNameAware]
* [BeanClassLoaderAware][core-BeanClassLoaderAware]
* [ApplicationContextAware][core-ApplicationContextAware]

## @Configuration 和 Auto-configuration

[@Configuration][core-Configuration]替代xml来定义[BeanDefinition][core-BeanDefinition]的一种手段。[Auto-configuration][ref-using-boot-auto-configuration]也是定义[BeanDefinition][core-BeanDefinition]的一种手段。

这两者的相同之处有：

1. 都是使用[@Configuration][core-Configuration]注解的类，这些类里都可以定义[@Bean][core-Bean]、[@Import][core-Import]、[@ImportResource][core-ImportResource]。
1. 都可以使用[@Condition*][ref-boot-features-condition-annotations]来根据情况选择是否加载

而不同之处有：

1. 加载方式不同：
    * 普通[@Configuration][core-Configuration]则是通过扫描package path加载的 
    * [Auto-configuration][ref-using-boot-auto-configuration]的是通过读取``classpath*:META-INF/spring.factories``中key等于``org.springframework.boot.autoconfigure.EnableAutoConfiguration``的property列出的[@Configuration][core-Configuration]加载的
1. 加载顺序不同：普通[@Configuration][core-Configuration]的加载在[Auto-configuration][ref-using-boot-auto-configuration]之前，但是有例外情况，看下面。
1. 内部加载顺序可控上的不同：
    * 普通[@Configuration][core-Configuration]则无法控制加载顺序
    * [Auto-configuration][ref-using-boot-auto-configuration]可以使用[@AutoConfigureOrder][boot-AutoConfigureOrder]、[@AutoConfigureBefore][boot-AutoConfigureBefore]、[@AutoConfigureAfter][boot-AutoConfigureAfter]

以下情况下[Auto-configuration][ref-using-boot-auto-configuration]会在普通[@Configuration][core-Configuration]前加载：

1. [Auto-configuration][ref-using-boot-auto-configuration]如果出现在最初的扫描路径里（@ComponentScan），就会被提前加载到，然后被当作普通的[@Configuration][core-Configuration]处理，这样[@AutoConfigureBefore][boot-AutoConfigureBefore]和[@AutoConfigureAfter][boot-AutoConfigureAfter]就没用了。参看例子代码里的InsideAutoConfiguration和InsideAutoConfiguration2。
1. [Auto-configuration][ref-using-boot-auto-configuration]如果提供[BeanPostProcessor][core-BeanPostProcessor]，那么它会被提前加载。参见例子代码里的BeanPostProcessorAutoConfiguration。
1. [Auto-configuration][ref-using-boot-auto-configuration]如果使用了[ImportBeanDefinitionRegistrar][core-ImportBeanDefinitionRegistrar]，那么[ImportBeanDefinitionRegistrar][core-ImportBeanDefinitionRegistrar]会被提前加载。参见例子代码里的ImportBeanDefinitionRegistrarAutoConfiguration。
1. [Auto-configuration][ref-using-boot-auto-configuration]如果使用了[ImportSelector][core-ImportSelector]，那么[ImportSelector][core-ImportSelector]会被提前加载。参见例子代码里的UselessDeferredImportSelectorAutoConfiguration。

参考[EnableAutoConfiguration][boot-EnableAutoConfiguration]和附录[EnableAutoConfigurationImportSelector][boot-EnableAutoConfigurationImportSelector]了解Spring boot内部处理机制。


### AnnotatedBeanDefinitionReader

这个类用来读取[@Configuration][core-Configuration]和[@Component][core-Component]，并将[BeanDefinition][core-BeanDefinition]注册到[ApplicationContext][core-ApplicationContext]里。

### ConfigurationClassPostProcessor

[ConfigurationClassPostProcessor][core-ConfigurationClassPostProcessor]是一个[BeanDefinitionRegistryPostProcessor][core-BeanDefinitionRegistryPostProcessor]，负责处理[@Configuration][core-Configuration]。

需要注意一个烟雾弹：看[#L296][code-ConfigurationClassPostProcessor#L296]->[ConfigurationClassUtils#L209][code-ConfigurationClassUtils#L209]。而order的值则是在[ConfigurationClassUtils#L122][code-ConfigurationClassUtils#L122]从注解中提取的。
这段代码似乎告诉我们它会对[@Configuration][core-Configuration]进行排序，然后按次序加载。
实际上不是的，[@Configuration][core-Configuration]是一个递归加载的过程。在本例中，是先从SampleApplication开始加载的，而事实上在这个时候，也就只有SampleApplication它自己可以提供排序。
而之后则直接使用了[ConfigurationClassParser][code-ConfigurationClassParser]，它里面并没有排序的逻辑。

关于排序的方式简单来说是这样的：[@Configuration][core-Configuration]的排序根据且只根据[@Order][core-Order]排序，如果没有[@Order][core-Order]则优先级最低。

### ConfigurationClassParser

前面讲了[ConfigurationClassPostProcessor][core-ConfigurationClassPostProcessor]使用[ConfigurationClassParser][code-ConfigurationClassParser]，实际上加载[@Configuration][core-Configuration]的工作是在这里做的。

下面讲以下加载的顺序：

1. 以SampleApplication为起点开始扫描
1. 扫描得到所有的[@Configuration][core-Configuration]和[@Component][core-Component]，从中读取[BeanDefinition][core-BeanDefinition]并导入：
  1. 如果[@Configuration][core-Configuration]注解了[@Import][core-Import]
    1. 如果使用的是[ImportSelector][core-ImportSelector]，则递归导入
    1. 如果使用的是[ImportBeanDefinitionRegistrar][core-ImportBeanDefinitionRegistrar]，则递归导入
    1. 如果使用的是[DeferredImportSelector][core-DeferredImportSelector]，则仅收集不导入
  1. 如果[@Configuration][core-Configuration]注解了[@ImportResource][core-ImportResource]，则递归导入
1. 迭代之前收集的[DeferredImportSelector][core-DeferredImportSelector]，递归导入

那[Auto-configuration][ref-using-boot-auto-configuration]在哪里呢？
实际上是在第3步里，[@SpringBootApplication][boot-SpringBootApplication]存在注解[@EnableAutoConfiguration][boot-EnableAutoConfiguration]，它使用了[EnableAutoConfigurationImportSelector][boot-EnableAutoConfigurationImportSelector]，
[EnableAutoConfigurationImportSelector][boot-EnableAutoConfigurationImportSelector]是一个[DeferredImportSelector][core-DeferredImportSelector]，所以也就是说，[Auto-configuration][ref-using-boot-auto-configuration]是在普通[@Configuration][core-Configuration]之后再加载的。

顺带一提，如果[Auto-configuration][ref-using-boot-auto-configuration]里再使用[DeferredImportSelector][core-DeferredImportSelector]，那么效果和使用[ImportSelector][core-ImportSelector]效果是一样的，不会再被延后处理。参见例子代码里的UselessDeferredImportSelectorAutoConfiguration。

### EnableAutoConfigurationImportSelector

[EnableAutoConfigurationImportSelector][boot-EnableAutoConfigurationImportSelector]负责导入[Auto-configuration][ref-using-boot-auto-configuration]。

它利用[AutoConfigurationSorter][code-AutoConfigurationSorter]对[Auto-configuration][ref-using-boot-auto-configuration]进行排序。逻辑算法是：

1. 先根据类名排序
1. 再根据[@AutoConfigureOrder][boot-AutoConfigureOrder]排序，如果没有[@AutoConfigureOrder][boot-AutoConfigureOrder]则优先级最低
1. 再根据[@AutoConfigureBefore][boot-AutoConfigureBefore]，[@AutoConfigureAfter][boot-AutoConfigureAfter]排序


## 内置类说明

### LoggingApplicationListener

[LoggingApplicationListener][boot-LoggingApplicationListener]用来配置日志系统的，比如logback、log4j。Spring boot对于日志有[详细解释][ref-boot-features-logging]，如果你想[自定义日志配置][ref-boot-features-custom-log-configuration]，那么也请参考本文中对于[LoggingApplicationListener][boot-LoggingApplicationListener]的被调用时机的说明以获得更深入的了解。

### StandardEnvironment

[StandardEnvironment][core-StandardEnvironment]有一个[MutablePropertySources][core-MutablePropertySources]，它里面有多个[PropertySource][core-PropertySource]，[PropertySource][core-PropertySource]负责提供property（即property的提供源），目前已知的[PropertySource][core-PropertySource]实现有：[MapPropertySource][core-MapPropertySource]、[SystemEnvironmentPropertySource][core-SystemEnvironmentPropertySource]、[CommandLinePropertySource][core-CommandLinePropertySource]等。当[StandardEnvironment][core-StandardEnvironment]查找property值的时候，是从[MutablePropertySources][core-MutablePropertySources]里依次查找的，而且一旦查找到就不再查找，也就是说如果要覆盖property的值，那么就得提供顺序在前的[PropertySource][core-PropertySource]。

### ConfigFileApplicationListener

[ConfigFileApplicationListener][boot-ConfigFileApplicationListener]用来将``application.properties``加载到[StandardEnvironment][core-StandardEnvironment]中。

[ConfigFileApplicationListener][boot-ConfigFileApplicationListener]内部使用了[EnvironmentPostProcessor][boot-EnvironmentPostProcessor]（见附录）自定义[StandardEnvironment][core-StandardEnvironment]

### ApplicationContextAwareProcessor

[ApplicationContextAwareProcessor][code-ApplicationContextAwareProcessor]实现了[BeanPostProcessor][core-BeanPostProcessor]接口，根据javadoc这个类用来调用以下接口的回调方法：

1. [EnvironmentAware][core-EnvironmentAware]
1. [EmbeddedValueResolverAware][core-EmbeddedValueResolverAware]
1. [ResourceLoaderAware][core-ResourceLoaderAware]
1. [ApplicationEventPublisherAware][core-ApplicationEventPublisherAware]
1. [MessageSourceAware][core-MessageSourceAware]
1. [ApplicationContextAware][core-ApplicationContextAware]

### AnnotationConfigApplicationContext

根据[javadoc][core-AnnotationConfigApplicationContext]，这个类用来将[@Configuration][core-Configuration]和[@Component][core-Component]作为输入来注册BeanDefinition。

特别需要注意的是，在[javadoc][core-AnnotationConfigApplicationContext]中讲到其支持@Bean的覆盖：

> In case of multiple @Configuration classes, @Bean methods defined in later classes will override those defined in earlier classes. This can be leveraged to deliberately override certain bean definitions via an extra @Configuration class.

它使用[AnnotatedBeanDefinitionReader][core-AnnotatedBeanDefinitionReader]来读取[@Configuration][core-Configuration]和[@Component][core-Component]。

### AnnotatedBeanDefinitionReader

[AnnotatedBeanDefinitionReader][core-AnnotatedBeanDefinitionReader]在其构造函数内部间接([AnnotationConfigUtils#L145][code-AnnotationConfigUtils#L145])的给[BeanFactory][core-BeanFactory]注册了几个与[BeanDefinition][core-BeanDefinition]相关注解的处理器。

1. [ConfigurationClassPostProcessor][core-ConfigurationClassPostProcessor]
1. [AutowiredAnnotationBeanPostProcessor][core-AutowiredAnnotationBeanPostProcessor]
1. [RequiredAnnotationBeanPostProcessor][core-RequiredAnnotationBeanPostProcessor]
1. [CommonAnnotationBeanPostProcessor][core-CommonAnnotationBeanPostProcessor]
1. [EventListenerMethodProcessor][core-EventListenerMethodProcessor]
1. [PersistenceAnnotationBeanPostProcessor][core-PersistenceAnnotationBeanPostProcessor]


  [boot-ApplicationReadyEvent]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/event/ApplicationReadyEvent.html
  [boot-ApplicationFailedEvent]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/event/ApplicationFailedEvent.html
  [boot-AnnotationConfigEmbeddedWebApplicationContext]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/embedded/AnnotationConfigEmbeddedWebApplicationContext.html
  [boot-AnsiOutputApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/config/AnsiOutputApplicationListener.html
  [boot-ApplicationEnvironmentPreparedEvent]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/event/ApplicationEnvironmentPreparedEvent.html
  [boot-ApplicationPidFileWriter]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/system/ApplicationPidFileWriter.html
  [boot-ApplicationPreparedEvent]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/event/ApplicationPreparedEvent.html
  [boot-ApplicationStartedEvent]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/event/ApplicationStartedEvent.html
  [boot-AutoConfigurationReportLoggingInitializer]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/autoconfigure/logging/AutoConfigurationReportLoggingInitializer.html
  [boot-AutoConfigureAfter]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/autoconfigure/AutoConfigureAfter.html
  [boot-AutoConfigureBefore]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/autoconfigure/AutoConfigureBefore.html
  [boot-AutoConfigureOrder]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/autoconfigure/AutoConfigureOrder.html
  [boot-BackgroundPreinitializer]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/autoconfigure/BackgroundPreinitializer.html
  [boot-ClasspathLoggingApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/logging/ClasspathLoggingApplicationListener.html
  [boot-ClearCachesApplicationListener]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/ClearCachesApplicationListener.java
  [boot-ConfigFileApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/config/ConfigFileApplicationListener.html
  [boot-ConfigurationWarningsApplicationContextInitializer]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/ConfigurationWarningsApplicationContextInitializer.html
  [boot-ContextIdApplicationContextInitializer]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/ContextIdApplicationContextInitializer.html
  [boot-DelegatingApplicationContextInitializer]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/config/DelegatingApplicationContextInitializer.html
  [boot-DelegatingApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/config/DelegatingApplicationListener.html
  [boot-EnableAutoConfigurationImportSelector]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/autoconfigure/EnableAutoConfigurationImportSelector.html
  [boot-EnableAutoConfiguration]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/autoconfigure/EnableAutoConfiguration.html
  [boot-EnvironmentPostProcessor]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/env/EnvironmentPostProcessor.html
  [boot-EventPublishingRunListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/event/EventPublishingRunListener.html
  [boot-FileEncodingApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/FileEncodingApplicationListener.html
  [boot-LiquibaseServiceLocatorApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/liquibase/LiquibaseServiceLocatorApplicationListener.html
  [boot-LoggingApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/logging/LoggingApplicationListener.html
  [boot-ParentContextCloserApplicationListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/builder/ParentContextCloserApplicationListener.html
  [boot-ServerPortInfoApplicationContextInitializer]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/context/web/ServerPortInfoApplicationContextInitializer.html
  [boot-SpringApplicationJsonEnvironmentPostProcessor]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/env/SpringApplicationJsonEnvironmentPostProcessor.html
  [boot-SpringApplicationRunListener]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/SpringApplicationRunListener.html
  [boot-SpringApplication]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/SpringApplication.html
  [boot-SpringBootApplication]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/autoconfigure/SpringBootApplication.html
  [code-AnnotationConfigUtils#L145]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/annotation/AnnotationConfigUtils.java#L145
  [boot-ApplicationRunner]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/ApplicationRunner.html
  [boot-CommandLineRunner]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/CommandLineRunner.html
  [code-AbstractApplicationContext#L507]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L507
  [code-AbstractApplicationContext#L510]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L510
  [code-AbstractApplicationContext#L513]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L513
  [code-AbstractApplicationContext#L516]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L516
  [code-AbstractApplicationContext#L520]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L520
  [code-AbstractApplicationContext#L523]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L523
  [code-AbstractApplicationContext#L526]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L526
  [code-AbstractApplicationContext#L529]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L529
  [code-AbstractApplicationContext#L575]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L575
  [code-AbstractApplicationContext#L611]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L611
  [code-AbstractApplicationContext#L625]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L625
  [code-AbstractApplicationContext#L526]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L526
  [code-AbstractApplicationContext#L532]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L532
  [code-AbstractApplicationContext#L535]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L535
  [code-AbstractApplicationContext#L538]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L538
  [code-AbstractApplicationContext#L541]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L541
  [code-AbstractApplicationContext#L544]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L544
  [code-AbstractApplicationContext#L704]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L704
  [code-AbstractApplicationContext#L739]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L739
  [code-AbstractApplicationContext#L793]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L793
  [code-AbstractApplicationContext#L801]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L801
  [code-AbstractApplicationContext#L828]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L828
  [code-AbstractApplicationContext#L861]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L861
  [code-AbstractApplicationContext#L869]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L869
  [code-AbstractApplicationContext#L877]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L877
  [code-AnnotatedBeanDefinitionReader#L83]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/annotation/AnnotatedBeanDefinitionReader.java#L83
  [code-AnnotationConfigApplicationContext#L51]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/annotation/AnnotationConfigApplicationContext.java#L51
  [code-AnnotationConfigUtils#L145]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/annotation/AnnotationConfigUtils.java#L145
  [code-AnnotationConfigUtils#L160]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/annotation/AnnotationConfigUtils.java#L160
  [code-ApplicationContextAwareProcessor]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/ApplicationContextAwareProcessor.java
  [code-AutoConfigurationSorter]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/AutoConfigurationSorter.java
  [code-CachingMetadataReaderFactoryPostProcessor]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/SharedMetadataReaderFactoryContextInitializer.java#L66
  [code-ConfigFileApplicationListener#L158]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/config/ConfigFileApplicationListener.java#L158
  [code-ConfigFileApplicationListener#L199]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/config/ConfigFileApplicationListener.java#L199
  [code-ConfigFileApplicationListener#L244]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/config/ConfigFileApplicationListener.java#L244
  [code-ConfigurationClassParser]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/annotation/ConfigurationClassParser.java
  [code-ConfigurationClassPostProcessor#L296]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/annotation/ConfigurationClassPostProcessor.java#L296
  [code-ConfigurationClassUtils#L122]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/annotation/ConfigurationClassUtils.java#L122
  [code-ConfigurationClassUtils#L209]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/annotation/ConfigurationClassUtils.java#L209
  [code-ConfigurationWarningsApplicationContextInitializer#L60]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/ConfigurationWarningsApplicationContextInitializer.java#L60
  [code-ConfigurationWarningsApplicationContextInitializer#L75]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/ConfigurationWarningsApplicationContextInitializer.java#L75
  [code-EventPublishingRunListener#L73]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/event/EventPublishingRunListener.java#L73
  [code-EventPublishingRunListener#L78]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/event/EventPublishingRunListener.java#L78
  [code-EventPublishingRunListener#L96]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/event/EventPublishingRunListener.java#L96
  [code-PropertySourceOrderingPostProcessor]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/config/ConfigFileApplicationListener.java#L285
  [code-PostProcessorRegistrationDelegate#L57]: https://github.com/spring-projects/spring-framework/blob/v4.3.3.RELEASE/spring-context/src/main/java/org/springframework/context/support/PostProcessorRegistrationDelegate.java#L57
  [code-SharedMetadataReaderFactoryContextInitializer#L57]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/SharedMetadataReaderFactoryContextInitializer.java#L57
  [code-SharedMetadataReaderFactoryContextInitializer]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/SharedMetadataReaderFactoryContextInitializer.java
  [code-SpringApplication#L605]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L605
  [code-SpringApplication#L630]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L630
  [code-SpringApplication#L687]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L687
  [code-SpringApplicationL1174]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L1174
  [code-SpringApplicationL1186]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L1186
  [code-SpringApplicationL236]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L236
  [code-SpringApplicationL256]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L256
  [code-SpringApplicationL257]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L257
  [code-SpringApplicationL261]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L261
  [code-SpringApplicationL263]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L263
  [code-SpringApplicationL297]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L297
  [code-SpringApplicationL303]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L303
  [code-SpringApplicationL304]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L304
  [code-SpringApplicationL308]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L308
  [code-SpringApplicationL311]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L311
  [code-SpringApplicationL313]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L313
  [code-SpringApplicationL315]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L315
  [code-SpringApplicationL317]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L317
  [code-SpringApplicationL331]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L331
  [code-SpringApplicationL335]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L335
  [code-SpringApplicationL336]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L336
  [code-SpringApplicationL337]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L337
  [code-SpringApplicationL344]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L344
  [code-SpringApplicationL347]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L347
  [code-SpringApplicationL348]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L348
  [code-SpringApplicationL349]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L349
  [code-SpringApplicationL350]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L350
  [code-SpringApplicationL366]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L366
  [code-SpringApplicationL367]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L367
  [code-SpringApplicationL370]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L370
  [code-SpringApplicationL371]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L371
  [code-SpringApplicationL583]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L583
  [code-SpringApplicationL603]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L603
  [code-SpringApplicationL628]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L628
  [code-SpringApplicationL685]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L685
  [code-SpringApplicationL759]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L759
  [code-SpringApplicationL761]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L761
  [code-SpringApplicationL316]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L316
  [code-SpringApplicationL771]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L771
  [code-SpringApplicationL774]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java#L774
  [code-SpringApplicationRunListeners]: https://github.com/spring-projects/spring-boot/blob/v1.4.1.RELEASE/spring-boot/src/main/java/org/springframework/boot/SpringApplicationRunListeners.java
  [code-spring-4.3.3.RELEASE]: https://github.com/spring-projects/spring-framework/tree/v4.3.3.RELEASE
  [code-spring-boot-1.4.1.RELEASE]: https://github.com/spring-projects/spring-boot/tree/v1.4.1.RELEASE
  [core-MessageSource]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/MessageSource.html
  [core-ApplicationEventMulticaster]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/event/SimpleApplicationEventMulticaster.html
  [core-ContextRefreshedEvent]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/event/ContextRefreshedEvent.html
  [core-AnnotatedBeanDefinitionReader]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/AnnotatedBeanDefinitionReader.html
  [core-AnnotationAwareOrderComparator]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/annotation/AnnotationAwareOrderComparator.html
  [core-AnnotationConfigApplicationContext]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/AnnotationConfigApplicationContext.html
  [core-ApplicationContextAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/ApplicationContextAware.html
  [core-ApplicationContextInitializer]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/ApplicationContextInitializer.html
  [core-ApplicationContext]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/ApplicationContext.html
  [core-ApplicationEventPublisherAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/ApplicationEventPublisherAware.html
  [core-ApplicationEventPublisher]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/ApplicationEventPublisher.html
  [core-ApplicationEvent]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/ApplicationEvent.html
  [core-ApplicationListener]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/ApplicationListener.html
  [core-Aware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/Aware.html
  [core-BeanClassLoaderAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/BeanClassLoaderAware.html
  [core-BeanDefinitionRegistryPostProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/support/BeanDefinitionRegistryPostProcessor.html
  [core-BeanDefinition]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/config/BeanDefinition.html
  [core-BeanFactoryAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/BeanFactoryAware.html
  [core-BeanFactoryPostProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/config/BeanFactoryPostProcessor.html
  [core-BeanFactory]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/BeanFactory.html
  [core-BeanNameAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/BeanNameAware.html
  [core-BeanPostProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/config/BeanPostProcessor.html
  [core-Bean]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/Bean.html
  [core-CloudFoundryVcapEnvironmentPostProcessor]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/api/org/springframework/boot/cloud/CloudFoundryVcapEnvironmentPostProcessor.html
  [core-Component]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/stereotype/Component.html
  [core-ConfigurableEnvironment]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/env/ConfigurableEnvironment.html
  [core-ConfigurationClassPostProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/ConfigurationClassPostProcessor.html
  [core-Configuration]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/Configuration.html
  [core-DefaultListableBeanFactory]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/support/DefaultListableBeanFactory.html
  [core-DeferredImportSelector]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/DeferredImportSelector.html
  [core-EmbeddedValueResolverAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/EmbeddedValueResolverAware.html
  [core-EnvironmentAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/EnvironmentAware.html
  [core-Environment]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/env/Environment.html
  [core-GenericApplicationContext]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/support/GenericApplicationContext.html
  [core-ImportAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/ImportAware.html
  [core-ImportBeanDefinitionRegistrar]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/ImportBeanDefinitionRegistrar.html
  [core-ImportResource]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/ImportResource.html
  [core-ImportSelector]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/ImportSelector.html
  [core-Import]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/Import.html
  [core-LoadTimeWeaverAwareProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/weaving/LoadTimeWeaverAwareProcessor.html
  [core-LoadTimeWeaverAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/weaving/LoadTimeWeaverAware.html
  [core-MessageSourceAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/MessageSourceAware.html
  [core-MutablePropertySources]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/env/MutablePropertySources.html
  [core-NotificationPublisherAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/jmx/export/notification/NotificationPublisherAware.html
  [core-Order]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/annotation/Order.html
  [core-PropertyEditorRegistrar]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/PropertyEditorRegistrar.html
  [core-PropertySource]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/env/PropertySource.html
  [core-ResourceLoaderAware]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/ResourceLoaderAware.html
  [core-ResourceLoader]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/io/ResourceLoader.html
  [core-StandardBeanExpressionResolver]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/expression/StandardBeanExpressionResolver.html
  [core-StandardEnvironment]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/env/StandardEnvironment.html
  [core-AutowiredAnnotationBeanPostProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/annotation/AutowiredAnnotationBeanPostProcessor.html
  [core-RequiredAnnotationBeanPostProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/beans/factory/annotation/RequiredAnnotationBeanPostProcessor.html
  [core-CommonAnnotationBeanPostProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/annotation/CommonAnnotationBeanPostProcessor.html
  [core-EventListenerMethodProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/context/event/EventListenerMethodProcessor.html
  [core-PersistenceAnnotationBeanPostProcessor]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/orm/jpa/support/PersistenceAnnotationBeanPostProcessor.html
  [core-MapPropertySource]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/env/MapPropertySource.html
  [core-SystemEnvironmentPropertySource]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/env/SystemEnvironmentPropertySource.html
  [core-CommandLinePropertySource]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/javadoc-api/org/springframework/core/env/CommandLinePropertySource.html
  [ref-boot-features-condition-annotations]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/reference/htmlsingle/#boot-features-condition-annotations
  [ref-boot-features-custom-log-configuration]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/reference/htmlsingle/#boot-features-custom-log-configuration
  [ref-boot-features-external-config]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/reference/htmlsingle/#boot-features-external-config
  [ref-boot-features-logging]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/reference/htmlsingle/#boot-features-logging
  [ref-boot-howto-customize-the-environment-or-application-context]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/reference/htmlsingle/#howto-customize-the-environment-or-application-context
  [ref-using-boot-auto-configuration]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/reference/htmlsingle/#using-boot-auto-configuration
  [ref-beans-factory-extension-bpp]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/spring-framework-reference/htmlsingle/#beans-factory-extension-bpp
  [ref-beans-factory-extension-factory-postprocessors]: http://docs.spring.io/spring/docs/4.3.3.RELEASE/spring-framework-reference/htmlsingle/#beans-factory-extension-factory-postprocessors
  [ref-boot-features-command-line-runner]: http://docs.spring.io/spring-boot/docs/1.4.1.RELEASE/reference/htmlsingle/#boot-features-command-line-runner
