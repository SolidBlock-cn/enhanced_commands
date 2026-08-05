package pers.solid.ecmd.data.neoforge;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.data.TagGenerationBridge;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TagGenerationBridgeImpl<T> {

  TagsProvider.TagAppender<T> createTagAppenderFor(TagKey<T> tag);

  class ForBlock extends BlockTagsProvider implements TagGenerationBridgeImpl<Block> {
    private final TagGenerationBridge<Block> bridge;

    public ForBlock(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId, @Nullable ExistingFileHelper existingFileHelper, TagGenerationBridge<Block> bridge) {
      super(output, lookupProvider, modId, existingFileHelper);
      this.bridge = bridge;
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
      final TagBuilderFactoryBridgeImpl<Block> factoryBridge = new TagBuilderFactoryBridgeImpl<>(this);
      bridge.configure(factoryBridge, provider);
    }


    @Override
    public TagAppender<Block> createTagAppenderFor(TagKey<Block> tag) {
      return tag(tag);
    }
  }

  record TagBuilderFactoryBridgeImpl<T>(TagGenerationBridgeImpl<T> bridge) implements TagGenerationBridge.TagBuilderFactoryBridge<T> {
    @Override
    public TagGenerationBridge.TagBuilderBridge<T> builderBridgeOf(TagKey<T> tagKey) {
      final TagsProvider.TagAppender<T> tagAppender = bridge.createTagAppenderFor(tagKey);
      return new TagBuilderBridgeImpl<>(tagAppender);
    }
  }

  class TagBuilderBridgeImpl<T> extends TagGenerationBridge.TagBuilderBridge<T> {

    private final TagsProvider.TagAppender<T> tagAppender;

    public TagBuilderBridgeImpl(TagsProvider.TagAppender<T> tagAppender) {
      this.tagAppender = tagAppender;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> add(T element) {
      if (tagAppender instanceof IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> intrinsic) {
        intrinsic.add(element);
      } else {
        throw new UnsupportedOperationException("Not intrinsic");
      }
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> add(ResourceKey<T> key) {
      tagAppender.add(key);
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> addAll(List<ResourceKey<T>> resourceKeys) {
      tagAppender.addAll(resourceKeys);
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> addOptional(ResourceLocation location) {
      tagAppender.addOptional(location);
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> addTag(TagKey<T> tag) {
      tagAppender.addTag(tag);
      return this;
    }

    @Override
    public TagGenerationBridge.TagBuilderBridge<T> addOptionalTag(ResourceLocation location) {
      tagAppender.addOptionalTag(location);
      return this;
    }
  }
}
