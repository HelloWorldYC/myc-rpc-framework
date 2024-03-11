package github.myc.annotation;

import java.lang.annotation.*;

/**
 * RPC 服务注释，标记在服务实现类上
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface RpcService {

    /**
     * 服务版本，默认是空字符串
     */
    String version() default "";

    /**
     * 服务的 group，默认是空字符串
     */
    String group() default "";

}
