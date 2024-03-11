package github.myc.spring;


import github.myc.annotation.RpcScan;
import github.myc.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.stereotype.Component;

/**
 * 扫描和过滤指定的注释
 * ImportBeanDefinitionRegistrar：可以在应用程序启动时自定义注册逻辑，实现动态的 bean 注册。
 * ResourceLoaderAware：用于向实现类提供 ResourceLoader 实例，ResourceLoader 提供了一种方便的方式来加载资源文件，如配置文件、类路径资源等。
 */
@Slf4j
public class CustomScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private static final String SPRING_BEAN_BASE_PACKAGE = "github.myc";
    private static final String BASE_PACKAGE_ATTRIBUTE_NAME = "basePackage";
    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 该方法是 ImportBeanDefinitionRegistrar 接口中的一个方法，用于注册 bean 定义到 bean 容器中。
     * @param annotationMetadata 用于访问导入了 CustomScannerRegistrar 的注解元数据。通过该参数，可以获取与导入相关的注解信息，如注解的属性值、注解所在类的信息等。
     * @param beanDefinitionRegistry 用于注册 bean 定义的 bean 容器，通过 registerBeanDefinition() 方法注册 bean 定义
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        // 获取 @RpcScan 注解的元素属性
        AnnotationAttributes rpcScanAnnotationAttributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(RpcScan.class.getName()));
        String[] rpcScanBasePackages = new String[0];
        if (rpcScanAnnotationAttributes != null) {
            // 获取 @RpcScan 注解的 basePackage 元素值
            rpcScanBasePackages = rpcScanAnnotationAttributes.getStringArray(BASE_PACKAGE_ATTRIBUTE_NAME);
        }
        if (rpcScanBasePackages.length == 0) {
            // 没有设置好的包路径，则设置注解所在包的路径为包扫描路径
            rpcScanBasePackages = new String[]{((StandardAnnotationMetadata) annotationMetadata).getIntrospectedClass().getPackage().getName()};
        }
        // 自定义包扫描器，扫描 @RpcService 注解
        CustomScanner rpcServiceScanner = new CustomScanner(beanDefinitionRegistry, RpcService.class);
        // 自定义包扫描器，扫描 @Component 注解
        CustomScanner springBeanScanner = new CustomScanner(beanDefinitionRegistry, Component.class);
        // 注意： @RpcReference 注解用于字段上，不用注册为 bean，因此不用扫描
        if (resourceLoader != null) {
            rpcServiceScanner.setResourceLoader(resourceLoader);
            springBeanScanner.setResourceLoader(resourceLoader);
        }
        int rpcServiceCount = rpcServiceScanner.scan(rpcScanBasePackages);
        log.info("rpcServiceScanner 扫描的路径：[{}]", rpcScanBasePackages);
        log.info("rpcServiceScanner 扫描的数量：[{}]", rpcServiceCount);
        int springBeanCount = springBeanScanner.scan(SPRING_BEAN_BASE_PACKAGE);
        log.info("springBeanScanner 扫描的路径：[{}]", SPRING_BEAN_BASE_PACKAGE);
        log.info("springBeanScanner 扫描的数量：[{}]", springBeanCount);
    }
}
