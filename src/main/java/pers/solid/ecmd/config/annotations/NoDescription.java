package pers.solid.ecmd.config.annotations;

import java.lang.annotation.*;

/**
 * 标有此注解的类或字段，在反射时不会自动生成描述，但仍会正常自动生成显示名称。
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoDescription {}
