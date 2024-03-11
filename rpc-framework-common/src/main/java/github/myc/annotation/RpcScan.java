package github.myc.annotation;

import org.springframework.context.annotation.Import;
import github.myc.spring.CustomScannerRegistrar;

import java.lang.annotation.*;

/**
 * 扫描的自定义注释
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(CustomScannerRegistrar.class)
public @interface RpcScan {

    String[] basePackage();

}
