package github.myc.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

/**
 * 自定义包扫描器，进行组件扫描时，只有带有指定注解的组件会被扫描和注册到 Spring 的 context 中。
 */
public class CustomScanner extends ClassPathBeanDefinitionScanner {

    public CustomScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annoType) {
        // 将 BeanDefinitionRegistry 对象传递给父类 ClassPathBeanDefinitionScanner，以进行后续的组件扫描和注册操作。
        super(registry);
        // 该过滤器用于只包含指定注解类型的组件，在扫描时只包含符合指定注解类型的组件。
        super.addIncludeFilter(new AnnotationTypeFilter(annoType));
    }

    @Override
    public int scan(String... basePackages) {
        return super.scan(basePackages);
    }
}
