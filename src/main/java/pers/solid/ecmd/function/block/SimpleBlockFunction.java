package pers.solid.ecmd.function.block;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SimpleBlockFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.StringRepresentableFunction;
import pers.solid.ecmd.function.property.GeneralPropertyFunction;
import pers.solid.ecmd.function.property.PropertyFunction;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public record SimpleBlockFunction(Block block, Collection<PropertyFunction<?>> propertyFunctions) implements BlockFunction {
  @Override
  public @NotNull String asString() {
    final StringBuilder stringBuilder = new StringBuilder(Registries.BLOCK.getId(block).toString());
    if (!propertyFunctions.isEmpty()) {
      stringBuilder.append('[');
      stringBuilder.append(propertyFunctions.stream().map(StringRepresentableFunction::asString).collect(Collectors.joining(", ")));
      stringBuilder.append(']');
    }
    return stringBuilder.toString();
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    BlockState stateToPlace = block.getDefaultState();
    for (PropertyFunction<?> propertyFunction : propertyFunctions) {
      stateToPlace = propertyFunction.getModifiedState(stateToPlace, origState);
    }
    return stateToPlace;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("block", Registries.BLOCK.getId(block).toString());
    if (!propertyFunctions.isEmpty()) {
      final NbtList nbtList = new NbtList();
      nbtCompound.put("properties", nbtList);
      nbtList.addAll(Collections2.transform(propertyFunctions, PropertyFunction::createNbt));
    }
  }

  @Override
  public @NotNull BlockFunctionType<SimpleBlockFunction> getType() {
    return BlockFunctionTypes.SIMPLE;
  }

  public enum Type implements BlockFunctionType<SimpleBlockFunction> {
    SIMPLE_TYPE;

    @Override
    public SimpleBlockFunction fromNbt(NbtCompound nbtCompound) {
      final Block block = Registries.BLOCK.get(new Identifier(nbtCompound.getString("block")));
      final Collection<PropertyFunction<?>> propertyFunctions;
      final NbtElement propertiesElement = nbtCompound.get("properties");
      if (propertiesElement instanceof NbtList nbtList && nbtList.getHeldType() == NbtElement.COMPOUND_TYPE) {
        propertyFunctions = nbtList.stream().map(nbtElement -> PropertyFunction.fromNbt((NbtCompound) nbtElement, block)).collect(Collectors.toUnmodifiableList());
        GeneralPropertyFunction.updateExcepts(propertyFunctions);
      } else {
        propertyFunctions = Collections.emptyList();
      }
      return new SimpleBlockFunction(block, propertyFunctions);
    }

    @Override
    public @NotNull SimpleBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser0, boolean suggestionsOnly) throws CommandSyntaxException {
      SimpleBlockFunctionSuggestedParser parser = new SimpleBlockFunctionSuggestedParser(commandRegistryAccess, parser0);
      parser.parseBlockId();
      parser.parseProperties();
      return new SimpleBlockFunction(parser.block, parser.propertyFunctions);
    }
  }
}
