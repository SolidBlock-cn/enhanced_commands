package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SimpleBlockFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.property.PropertyFunction;
import pers.solid.ecmd.util.Parser;

import java.util.List;
import java.util.stream.Collectors;

public record SimpleBlockFunction(Block block, List<PropertyFunction<?>> properties) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    final StringBuilder stringBuilder = new StringBuilder(Registries.BLOCK.getId(block).toString());
    if (!properties.isEmpty()) {
      stringBuilder.append('[');
      stringBuilder.append(properties.stream().map(PropertyFunction::asString).collect(Collectors.joining(", ")));
      stringBuilder.append(']');
    }
    return stringBuilder.toString();
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    BlockState stateToPlace = block.getDefaultState();
    final Random random = world.getRandom();
    for (PropertyFunction<?> propertyFunction : properties) {
      stateToPlace = propertyFunction.getModifiedState(stateToPlace, origState, random);
    }
    return stateToPlace;
  }

  public static final Codec<SimpleBlockFunction> CODEC = Registries.BLOCK.getCodec().dispatch("block", SimpleBlockFunction::block, block -> RecordCodecBuilder.create(i -> i.ap(properties -> new SimpleBlockFunction(block, properties), PropertyFunction.getCodec(block).listOf().optionalFieldOf("properties", ImmutableList.of()).forGetter(SimpleBlockFunction::properties))));

  @Override
  public @NotNull BlockFunctionType<SimpleBlockFunction> getType() {
    return BlockFunctionTypes.SIMPLE;
  }

  public enum Type implements BlockFunctionType<SimpleBlockFunction>, Parser<BlockFunctionArgument> {
    SIMPLE_TYPE;

    @Override
    public @NotNull Codec<SimpleBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull SimpleBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser0, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      SimpleBlockFunctionSuggestedParser parser = new SimpleBlockFunctionSuggestedParser(commandRegistryAccess, parser0);
      parser.parseBlockId();
      parser.parseProperties();
      return new SimpleBlockFunction(parser.block, parser.propertyFunctions);
    }
  }
}
