package pers.solid.ecmd.function.block;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.SerializableFunction;
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
      stringBuilder.append(propertyFunctions.stream().map(SerializableFunction::asString).collect(Collectors.joining(", ")));
      stringBuilder.append(']');
    }
    return stringBuilder.toString();
  }

  @Override
  public boolean setBlock(World world, BlockPos pos, int flags) {
    final BlockState origState = world.getBlockState(pos);
    BlockState stateToPlace = block.getDefaultState();
    for (PropertyFunction<?> propertyFunction : propertyFunctions) {
      stateToPlace = propertyFunction.getModifiedState(stateToPlace, origState);
    }
    return world.setBlockState(pos, stateToPlace, flags);
  }

  @Override
  public void writeNbt(NbtCompound nbtCompound) {
    nbtCompound.putString("block", Registries.BLOCK.getId(block).toString());
    if (!propertyFunctions.isEmpty()) {
      final NbtList nbtList = new NbtList();
      nbtCompound.put("properties", nbtList);
      nbtList.addAll(Collections2.transform(propertyFunctions, PropertyFunction::createNbt));
    }
  }

  @Override
  public BlockFunctionType<?> getType() {
    return null;
  }

  public enum Type implements BlockFunctionType<SimpleBlockFunction> {
    SIMPLE_TYPE;

    @Override
    public SimpleBlockFunction fromNbt(NbtCompound nbtCompound) {
      final Block block = Registries.BLOCK.get(new Identifier(nbtCompound.getString("block")));
      final Collection<PropertyFunction<?>> propertyFunctions;
      final NbtElement propertiesElement = nbtCompound.get("properties");
      if (propertiesElement instanceof NbtList nbtList && nbtList.getType() == NbtElement.COMPOUND_TYPE) {
        propertyFunctions = nbtList.stream().map(nbtElement -> PropertyFunction.fromNbt((NbtCompound) nbtElement, block)).collect(Collectors.toUnmodifiableList());
      } else {
        propertyFunctions = Collections.emptyList();
      }
      return new SimpleBlockFunction(block, propertyFunctions);
    }

    @Override
    public @Nullable SimpleBlockFunction parse(SuggestedParser parser0, boolean suggestionsOnly) throws CommandSyntaxException {
      SimpleBlockFunctionSuggestedParser parser = new SimpleBlockFunctionSuggestedParser(parser0);
      parser.suggestions.add((context, suggestionsBuilder) -> CommandSource.forEachMatching(parser.registryWrapper.streamKeys().map(RegistryKey::getValue)::iterator, suggestionsBuilder.getRemaining().toLowerCase(), id -> id, id -> suggestionsBuilder.suggest(id.toString())));
      if (parser.reader.canRead() && Identifier.isCharValid(parser.reader.peek())) {
        parser.parseBlockId();
        parser.parseProperties();
        return new SimpleBlockFunction(parser.block, parser.propertyFunctions);
      }
      return null;
    }
  }
}
