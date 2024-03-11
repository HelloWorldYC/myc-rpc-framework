package github.myc.annotation;

import java.lang.annotation.*;

/**
 * RPC 引用注解，用于自动连接（/注入）服务实现类，有点类似于 @Autowired 自动依赖注入
 * Retention(RetentionPolicy.RUNTIME)：该注解在运行时可见，可以通过反射机制来访问和处理注解信息
 * Target({ElementType.FIELD})：该注解只能应用于类的字段（成员变量）
 * Inherited：应用了该注解的类，其子类也会继承该注解，但是，这仅适用于类级别的注解，对于方法或字段级别的注解不起作用。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface RpcReference {

    /**
     * 服务版本，默认是空字符串
     */
    String version() default "";

    /**
     * 服务的 group，默认是空字符串
     */
    String group() default "";

}
