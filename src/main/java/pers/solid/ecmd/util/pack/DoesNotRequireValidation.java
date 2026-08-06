package pers.solid.ecmd.util.pack;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * 如果一个类是实现了 {@link RequiresValidation} 的类的子类，并且没有需要验证的字段，则可以让该类实现此方法，这样免去了手动实现 {@link #membersToValidate()} 并返回空集合的麻烦。
 *
 * @implNote 实现了该接口并不意味着一定没有需要验证的字段，实现该接口的类的子类仍可以覆盖 {@link #membersToValidate()} 并使之返回非空列表。
 */
public interface DoesNotRequireValidation extends RequiresValidation {
  @Override
  default Iterable<? extends @Nullable Object> membersToValidate() {
    return Collections.emptyList();
  }
}
