package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockFunctionSuggestedParser;
import pers.solid.ecmd.argument.SimpleBlockSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.Parser;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.List;
import java.util.stream.Collectors;

public record PropertyNamesBlockFunction(@NotNull List<PropertyNameFunction> functions) implements BlockFunction {
  public static final Codec<PropertyNamesBlockFunction> CODEC = RecordCodecBuilder.create(i -> i.ap(PropertyNamesBlockFunction::new, PropertyNameFunction.CODEC.listOf().fieldOf("properties").forGetter(PropertyNamesBlockFunction::functions)));

  @Override
  public @NotNull String asString() {
    return "[" + functions.stream().map(PropertyNameFunction::asString).collect(Collectors.joining(",")) + "]";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    for (PropertyNameFunction propertyNameFunction : functions) {
      blockState = propertyNameFunction.getModifiedState(origState, blockState, world.getRandom());
    }
    return blockState;
  }

  @Override
  public @NotNull BlockFunctionType<PropertyNamesBlockFunction> getType() {
    return BlockFunctionTypes.PROPERTY_NAMES;
  }

  public enum Type implements BlockFunctionType<PropertyNamesBlockFunction>, Parser<BlockFunctionArgument> {
    PROPERTY_NAMES_TYPE;

    @Override
    public @NotNull Codec<PropertyNamesBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable BlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("[", SimpleBlockSuggestedParser.START_OF_PROPERTIES, suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '[') {
        final SimpleBlockFunctionSuggestedParser suggestedParser = new SimpleBlockFunctionSuggestedParser(commandRegistryAccess, parser);
        suggestedParser.parsePropertyNames();
        return new PropertyNamesBlockFunction(suggestedParser.propertyNameFunctions);
      } else {
        return null;
      }
    }
  }
}
