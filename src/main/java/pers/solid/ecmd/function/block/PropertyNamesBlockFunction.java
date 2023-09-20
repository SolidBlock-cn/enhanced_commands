package pers.solid.ecmd.function.block;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockFunctionSuggestedParser;
import pers.solid.ecmd.argument.SimpleBlockSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.StringRepresentableFunction;
import pers.solid.ecmd.function.property.GeneralPropertyFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.NbtConvertible;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public record PropertyNamesBlockFunction(@NotNull Collection<PropertyNameFunction> propertyNameFunctions) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    return "[" + propertyNameFunctions.stream().map(StringRepresentableFunction::asString).collect(Collectors.joining(",")) + "]";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    for (PropertyNameFunction propertyNameFunction : propertyNameFunctions) {
      blockState = propertyNameFunction.getModifiedState(blockState, origState);
    }
    return blockState;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    final NbtList nbtList = new NbtList();
    nbtCompound.put("functions", nbtList);
    nbtList.addAll(Collections2.transform(propertyNameFunctions, NbtConvertible::createNbt));
  }

  @Override
  public @NotNull BlockFunctionType<PropertyNamesBlockFunction> getType() {
    return BlockFunctionTypes.PROPERTY_NAMES;
  }

  public enum Type implements BlockFunctionType<PropertyNamesBlockFunction> {
    PROPERTY_NAMES_TYPE;

    @Override
    public PropertyNamesBlockFunction fromNbt(@NotNull NbtCompound nbtCompound) {
      final List<PropertyNameFunction> functions = nbtCompound.getList("functions", NbtElement.COMPOUND_TYPE)
          .stream()
          .map(nbtElement -> PropertyNameFunction.fromNbt((NbtCompound) nbtElement))
          .toList();
      GeneralPropertyFunction.OfName.updateExcepts(functions);
      return new PropertyNamesBlockFunction(functions);
    }

    @Override
    public @Nullable BlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("[", SimpleBlockSuggestedParser.START_OF_PROPERTIES, suggestionsBuilder));
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
