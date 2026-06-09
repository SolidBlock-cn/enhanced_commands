package pers.solid.ecmd.config.annotations;

import pers.solid.ecmd.config.ConfigEntryScopeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 决定一个配置项的作用范围，或一个配置类的各项的默认作用范围。
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigEntryScope {
  ConfigEntryScopeType value();
}
