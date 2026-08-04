package pers.solid.ecmd.util.pack;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.pack.problems.EntryAbsentValidationProblem;
import pers.solid.ecmd.util.pack.problems.RegistryAbsentValidationProblem;
import pers.solid.ecmd.util.pack.problems.ValidationProblem;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @see net.minecraft.core.RegistrySetBuilder.LazyHolder
 */
public class LazyReference<T> extends Holder.Reference<T> {
  /**
   * 存储 LazyReference 的缓存。每次重新加载数据包时，缓存都重置一次，然后读取文件（会从中读取缓存）。读取完成后，会将缓存中的各个 LazyReference 绑定值。
   *
   * @see #resetCache()
   * @see #bindCachedValues(HolderLookup.Provider)
   */
  protected static final LoadingCache<ResourceKey<?>, LazyReference<?>> cache = CacheBuilder.newBuilder()
      .weakValues()
      .build(CacheLoader.from(LazyReference::new));
  private @Nullable ValidationProblem problem;
  private @Nullable Holder.Reference<T> boundHolderReference;

  protected LazyReference(HolderOwner<T> owner, @Nullable ResourceKey<T> key, @Nullable T value) {
    super(Type.STAND_ALONE, owner, key, value);
  }

  private LazyReference(ResourceKey<T> key) {
    this(RegistryHelper.safeHolderOwner(), key, null);
  }

  @SuppressWarnings("unchecked")
  public static <T> LazyReference<T> of(ResourceKey<T> key) {
    return (LazyReference<T>) cache.getUnchecked(key);
  }

  @ApiStatus.Internal
  public static void resetCache() {
    EnhancedCommands.LOGGER.info("Enhanced Commands: Resetting cache for lazy reference. It should be triggered when loading (including reloading) data.");
    cache.invalidateAll();
  }

  @ApiStatus.Internal
  public static void bindCachedValues(HolderLookup.Provider provider) {
    for (LazyReference<?> lazyReference : cache.asMap().values()) {
      lazyReference.bindValueFromProvider(provider);
    }
    EnhancedCommands.LOGGER.info("Binding cached values for lazy references. Amount bound (including those failed): {}", cache.size());
  }

  @Override
  public void bindValue(T value) {
    super.bindValue(value);
  }

  private void bindValueFromProvider(HolderLookup.Provider provider) {
    final Optional<? extends HolderLookup.RegistryLookup<T>> or = provider.lookup(key().registryKey());
    if (or.isEmpty()) {
      setProblem(new RegistryAbsentValidationProblem<>(key().registryKey()));
      return;
    }
    final Optional<Reference<T>> o = or.get().get(key());
    if (o.isPresent()) {
      bindValue(o.get().value());
      boundHolderReference = o.get();
    } else {
      setProblem(new EntryAbsentValidationProblem<>(key()));
    }
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

  @Override
  public boolean is(TagKey<T> tagKey) {
    if (this.boundHolderReference != null) {
      return this.boundHolderReference.is(tagKey);
    }
    return super.is(tagKey);
  }

  @Override
  public Stream<TagKey<T>> tags() {
    if (this.boundHolderReference != null) {
      return this.boundHolderReference.tags();
    }
    return super.tags();
  }

  @Override
  public boolean canSerializeIn(HolderOwner<T> owner) {
    if (this.boundHolderReference != null) {
      return this.boundHolderReference.canSerializeIn(owner);
    }
    return super.canSerializeIn(owner);
  }
}
