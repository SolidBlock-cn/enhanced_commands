package pers.solid.ecmd.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.List;

public interface TagGenerationBridge<T> {
  void configure(TagBuilderFactoryBridge<T> bridge, HolderLookup.Provider registries);

  interface TagBuilderFactoryBridge<T> {
    TagBuilderBridge<T> builderBridgeOf(TagKey<T> tagKey);
  }

  abstract class TagBuilderBridge<T> {
    public abstract TagBuilderBridge<T> add(T element);

    @SafeVarargs
    public final TagBuilderBridge<T> add(T... elements) {
      for (T element : elements) {
        add(element);
      }
      return this;
    }

    public abstract TagBuilderBridge<T> add(ResourceKey<T> key);

    @SafeVarargs
    public final TagBuilderBridge<T> add(ResourceKey<T>... keys) {
      for (ResourceKey<T> key : keys) {
        add(key);
      }
      return this;
    }

    public abstract TagBuilderBridge<T> addAll(List<ResourceKey<T>> keys);

    public abstract TagBuilderBridge<T> addOptional(ResourceLocation location);

    public abstract TagBuilderBridge<T> addTag(TagKey<T> tag);

    public abstract TagBuilderBridge<T> addOptionalTag(ResourceLocation location);
  }
}
