package pers.solid.ecmd.function.block;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.property.GeneralPropertyFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.NbtConvertible;
import pers.solid.ecmd.util.Parser;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class TagBlockFunction implements BlockFunction {
  private final @NotNull TagKey<Block> blockTag;
  private final @NotNull Collection<PropertyNameFunction> propertyNameFunctions;
  private transient Block[] blocks;
  private transient World world;

  public TagBlockFunction(@NotNull TagKey<Block> blockTag, @NotNull Collection<PropertyNameFunction> propertyNameFunctions) {
    this.blockTag = blockTag;
    this.propertyNameFunctions = propertyNameFunctions;
  }

  public Block[] getBlocks(@NotNull World world) {
    if (!world.equals(this.world)) {
      blocks = world.createCommandRegistryWrapper(RegistryKeys.BLOCK).streamEntries().filter(blockReference -> blockReference.isIn(blockTag)).map(RegistryEntry.Reference::value).toArray(Block[]::new);
      this.world = world;
    }
    return blocks;
  }

  @Override
  public @NotNull String asString() {
    if (propertyNameFunctions.isEmpty()) {
      return "#" + blockTag.id().toString();
    } else {
      return "#" + blockTag.id().toString() + "[" + propertyNameFunctions.stream().map(PropertyNameFunction::asString).collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final Block[] blocks = getBlocks(world);
    if (blocks.length == 0) {
      return blockState;
    }
    BlockState state = blocks[world.getRandom().nextInt(blocks.length)].getDefaultState();
    for (PropertyNameFunction propertyNameFunction : propertyNameFunctions) {
      state = propertyNameFunction.getModifiedState(origState, state, world.getRandom());
    }
    return state;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("tag", blockTag.id().toString());
    if (!propertyNameFunctions.isEmpty()) {
      final NbtList nbtList = new NbtList();
      nbtCompound.put("properties", nbtList);
      nbtList.addAll(Collections2.transform(propertyNameFunctions, NbtConvertible::createNbt));
    }
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

    if (!blockTag.equals(that.blockTag))
      return false;
    return propertyNameFunctions.equals(that.propertyNameFunctions);
  }

  @Override
  public int hashCode() {
    int result = blockTag.hashCode();
    result = 31 * result + propertyNameFunctions.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "TagBlockFunction{" +
        "blockTag=" + blockTag +
        ", propertyNameFunctions=" + propertyNameFunctions +
        '}';
  }

  public enum Type implements BlockFunctionType<TagBlockFunction>, Parser<BlockFunctionArgument> {
    TAG_TYPE;

    @Override
    public @NotNull TagBlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, new Identifier(nbtCompound.getString("tag")));
      final List<PropertyNameFunction> functions = nbtCompound.getList("properties", NbtElement.COMPOUND_TYPE)
          .stream()
          .map(nbtElement -> PropertyNameFunction.fromNbt((NbtCompound) nbtElement))
          .toList();
      GeneralPropertyFunction.OfName.updateExcepts(functions);
      return new TagBlockFunction(tag, functions);
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
