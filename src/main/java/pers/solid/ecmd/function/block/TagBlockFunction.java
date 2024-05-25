package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.Parser;

import java.util.List;
import java.util.stream.Collectors;

public final class TagBlockFunction implements BlockFunction {
  private final @NotNull TagKey<Block> tag;
  private final @NotNull List<PropertyNameFunction> properties;
  private transient Block[] blocks;
  private transient World world;

  public TagBlockFunction(@NotNull TagKey<Block> tag, @NotNull List<PropertyNameFunction> properties) {
    this.tag = tag;
    this.properties = properties;
  }

  public Block[] getBlocks(@NotNull World world) {
    if (!world.equals(this.world)) {
      blocks = world.createCommandRegistryWrapper(RegistryKeys.BLOCK).streamEntries().filter(blockReference -> blockReference.isIn(tag)).map(RegistryEntry.Reference::value).toArray(Block[]::new);
      this.world = world;
    }
    return blocks;
  }

  @Override
  public @NotNull String asString() {
    if (properties.isEmpty()) {
      return "#" + tag.id().toString();
    } else {
      return "#" + tag.id().toString() + "[" + properties.stream().map(PropertyNameFunction::asString).collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final Block[] blocks = getBlocks(world);
    if (blocks.length == 0) {
      return blockState;
    }
    BlockState state = blocks[world.getRandom().nextInt(blocks.length)].getDefaultState();
    for (PropertyNameFunction propertyNameFunction : properties) {
      state = propertyNameFunction.getModifiedState(origState, state, world.getRandom());
    }
    return state;
  }

  @Override
  public @NotNull BlockFunctionType<TagBlockFunction> getType() {
    return BlockFunctionTypes.TAG;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof TagBlockFunction that))
      return false;

    if (!tag.equals(that.tag))
      return false;
    return properties.equals(that.properties);
  }

  @Override
  public int hashCode() {
    int result = tag.hashCode();
    result = 31 * result + properties.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "TagBlockFunction{" +
        "tag=" + tag +
        ", properties=" + properties +
        '}';
  }

  public static final Codec<TagBlockFunction> CODEC = RecordCodecBuilder.create(i -> i.apply2(TagBlockFunction::new, TagKey.unprefixedCodec(RegistryKeys.BLOCK).fieldOf("tag").forGetter(f -> f.tag), PropertyNameFunction.CODEC.listOf().optionalFieldOf("properties", ImmutableList.of()).forGetter(f -> f.properties)));

  public enum Type implements BlockFunctionType<TagBlockFunction>, Parser<BlockFunctionArgument> {
    TAG_TYPE;

    @Override
    public @NotNull Codec<TagBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable TagBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser0, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      SimpleBlockFunctionSuggestedParser parser = new SimpleBlockFunctionSuggestedParser(commandRegistryAccess, parser0);
      parser.parseBlockTagIdAndProperties();
      if (parser.tagId != null) {
        return new TagBlockFunction(parser.tagId.getTag(), parser.propertyNameFunctions);
      } else {
        return null;
      }
    }
  }
}
