package pers.solid.ecmd.util.pack;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.nbt.function.PositionalListEntry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 用于模组中需要验证的可数据驱动对象。模组的验证主要针对在建立好了注册表的情况下，检查有无无效引用、循环引用等问题。
 */
public interface RequiresValidation {
  /**
   * 对 {@link #membersToValidate()} 返回的各值进行验证。如果遇到 null 或非 {@link RequiresValidation} 的类型，不会报错，但是如果是基础类型、Optional、集合等禁止的类型，则仍会报错。
   */
  default void validate(ValidationContext context) {
    for (@Nullable Object member : membersToValidate()) {
      if (member instanceof RequiresValidation r) {
        r.validate(context);
      } else if (member instanceof Holder.Reference<?> ref) {
        ReferenceEntry.of(ref).validate(context);
      } else if (member instanceof Collection<?>
          || member instanceof String
          || member instanceof Number
          || member instanceof Boolean
          || member instanceof Enum<?>
          || member instanceof Map<?, ?>
          || member instanceof Optional<?>
          || member instanceof Pair<?, ?>
          || member instanceof PositionalListEntry<?>) {
        throw new IllegalStateException(String.format("Cannot validate %s! Invalid type of member to validate: %s", context.getName().asString(), member.getClass().getName()));
      }
    }
  }

  /**
   * <p>返回此对象中需要验证的字段。注意，如果该字段是 Optional 或者集合，需要在返回的结果中展开。例如：
   * <p>❌错误：
   * <pre>{@code
   * public Iterable<...> membersToValidate() {
   *   return List.of(blockFunctionList, optionalBlockPredicate);
   * }
   * }</pre>
   * <p>✅正确：
   * <pre>{@code
   * public Iterable<...> membersToValidate() {
   *   return Iterables.concat(blockFunctionList, Collections.singletonList(optionalBlockPredicate.orElse(null));
   * }
   * }</pre>
   * <p>如果需要验证的字段只有一个列表，可以直接返回这个列表。
   * <p>返回的列表可以包含 null，也可以包括不属于 {@link RequiresValidation} 的对象，这种情况下在验证时会被忽略，不会报错。但是如果遇到 Optional、集合等禁止的类型，则仍会报错。
   *
   * @implNote 如果该类比较简单，没有需要验证的字段，可以直接让类实现 {@link DoesNotRequireValidation}。
   * @see #validate(ValidationContext)
   */
  Iterable<? extends @Nullable Object> membersToValidate();
}
