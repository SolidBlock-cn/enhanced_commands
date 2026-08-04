package pers.solid.ecmd.util.pack;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.pack.problems.ValidationProblem;

/**
 * @see net.minecraft.core.RegistrySetBuilder.LazyHolder
 */
public class LazyReference<T> extends Holder.Reference<T> {
  private @Nullable ValidationProblem problem;

  protected LazyReference(HolderOwner<T> owner, @Nullable ResourceKey<T> key, @Nullable T value) {
    super(Type.STAND_ALONE, owner, key, value);
  }

  public LazyReference(ResourceKey<T> key) {
    this(RegistryHelper.safeHolderOwner(), key, null);
  }

  @Override
  public void bindValue(T value) {
    super.bindValue(value);
  }

  public void setProblem(ValidationProblem problem) {
    this.problem = problem;
  }

  @Override
  public T value() {
    if (problem != null) {
      throw new CommandRuntimeException(Component.translatable("enhanced_commands.registry.invalid_entry", key().location().toString(), problem.message()));
    }
    return super.value();
  }
}
