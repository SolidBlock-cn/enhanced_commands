package pers.solid.ecmd.config.annotations;

import pers.solid.ecmd.config.ConfigReflectionHelper;

import java.lang.annotation.*;

/**
 * 注解于本模组中的配置分类或配置项中，以修改该其描述。请注意，注解的方式修改描述存在一定的限制，如果需要进行复杂的修改，可以在 {@link ConfigReflectionHelper#createCategoryFromReflection} 中设置 {@code entryModifiers} 参数。仅适用于 {@link ConfigReflectionHelper} 创建的配置分类或配置项。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface OverrideDescription {
  /**
   * 需要使用的描述。
   */
  TextInfo value();
}
