package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import pers.solid.ecmd.data.TagGenerationBridge;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TagGenerationBridgeImpl<T> {

  FabricTagProvider<T>.FabricTagBuilder createFabricTagBuilder(TagKey<T> tag);

  class Custom<T> extends FabricTagProvider<T> implements TagGenerationBridgeImpl<T> {
    private final TagGenerationBridge<T> tagGenerationBridge;

    public Custom(FabricDataOutput output, ResourceKey<? extends Registry<T>> registryKey, CompletableFuture<HolderLookup.Provider> registriesFuture, TagGenerationBridge<T> tagGenerationBridge) {
      super(output, registryKey, registriesFuture);
      this.tagGenerationBridge = tagGenerationBridge;
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
      final TagBuilderFactoryBridgeImpl<T> bridge = new TagBuilderFactoryBridgeImpl<>(this);
      tagGenerationBridge.configure(bridge, wrapperLookup);
    }

    @Override
    public FabricTagBuilder createFabricTagBuilder(TagKey<T> tag) {
      return getOrCreateTagBuilder(tag);
    }
  }

  class ForBlock extends FabricTagProvider.BlockTagProvider implements TagGenerationBridgeImpl<Block> {
    private final TagGenerationBridge<Block> tagGenerationBridge;

    public ForBlock(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture, TagGenerationBridge<Block> tagGenerationBridge) {
      super(output, registriesFuture);
      this.tagGenerationBridge = tagGenerationBridge;
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
      final TagBuilderFactoryBridgeImpl<Block> bridge = new TagBuilderFactoryBridgeImpl<>(this);
      tagGenerationBridge.configure(bridge, wrapperLookup);
    }

    @Override
    public FabricTagProvider<Block>.FabricTagBuilder createFabricTagBuilder(TagKey<Block> tag) {
      return getOrCreateTagBuilder(tag);
    }
  }

  record TagBuilderFactoryBridgeImpl<T>(TagGenerationBridgeImpl<T> tagProvider) implements TagGenerationBridge.TagBuilderFactoryBridge<T> {

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> builderBridgeOf(TagKey<T> tagKey) {
      final FabricTagProvider<T>.FabricTagBuilder fabricTagBuilder = tagProvider.createFabricTagBuilder(tagKey);
      return new TagBuilderBridgeImpl<>(fabricTagBuilder);
    }
  }

  class TagBuilderBridgeImpl<T> extends TagGenerationBridge.TagBuilderBridge<T> {
    private final FabricTagProvider<T>.FabricTagBuilder builder;

    public TagBuilderBridgeImpl(FabricTagProvider<T>.FabricTagBuilder builder) {
      this.builder = builder;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> add(T element) {
      builder.add(element);
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> add(ResourceKey<T> key) {
      builder.add(key);
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> addAll(List<ResourceKey<T>> resourceKeys) {
      builder.addAll(resourceKeys);
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> addOptional(ResourceLocation location) {
      builder.addOptional(location);
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> addTag(TagKey<T> tag) {
      builder.forceAddTag(tag);
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> addOptionalTag(ResourceLocation location) {
      builder.addOptionalTag(location);
      return this;
    }
  }
}
