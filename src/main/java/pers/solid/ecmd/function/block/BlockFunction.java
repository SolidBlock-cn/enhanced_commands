package pers.solid.ecmd.function.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.StringRepresentableFunction;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.NbtConvertible;

import java.util.List;

/**
 * 方块函数，用于定义如何在世界的某个地方设置方块。它类似于原版中的 {@link BlockStateArgument} 以及 WorldEdit 中的方块蒙版（block mask）。方块函数不止定义方块，有可能是对方块本身进行修改，也有可能对方块实体进行修改。由于它是在已有方块的基础上进行修改的，故称为方块函数。
 */
public interface BlockFunction extends StringRepresentableFunction, NbtConvertible {
  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_function.cannotParse"));


  static @NotNull BlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final BlockFunction parseUnit = parseUnit(commandRegistryAccess, parser, suggestionsOnly);
    if (parseUnit instanceof NbtBlockFunction) {
      return parseUnit;
    }
    List<PropertyNameFunction> propertyNameFunctions = null;
    if (!(parseUnit instanceof PropertyNamesBlockFunction) && parser.reader.canRead(0) && parser.reader.peek(-1) != ']') {
      // 当前面以“]”结尾时，说明已经在其他解析器中读取了属性，此时在这里不再读取任何属性
      // 尝试读取属性
      parser.suggestions.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest("[", SimpleBlockSuggestedParser.START_OF_PROPERTIES);
        }
      });
      if (parser.reader.canRead() && parser.reader.peek() == '[') {
        final SimpleBlockFunctionSuggestedParser suggestedParser = new SimpleBlockFunctionSuggestedParser(commandRegistryAccess, parser);
        suggestedParser.parsePropertyNames();
        propertyNameFunctions = suggestedParser.propertyNameFunctions;
      }
    }
    CompoundNbtFunction nbtFunction = null;
    parser.suggestions.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("{", NbtPredicateSuggestedParser.START_OF_COMPOUND);
      }
    });
    if (parser.reader.canRead() && parser.reader.peek() == '{') {
      // 尝试读取 NBT
      nbtFunction = new NbtFunctionSuggestedParser(parser.reader, parser.suggestions).parseCompound(false);
    }
    if (propertyNameFunctions != null || nbtFunction != null) {
      return new PropertiesNbtCombinationBlockFunction(parseUnit, propertyNameFunctions == null ? null : new PropertyNamesBlockFunction(propertyNameFunctions), nbtFunction == null ? null : new NbtBlockFunction(nbtFunction));
    }
    return parseUnit;
  }

  @NotNull
  static BlockFunction parseUnit(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    CommandSyntaxException exception = null;
    final int cursorOnStart = parser.reader.getCursor();
    int cursorOnEnd = cursorOnStart;
    for (BlockFunctionType<?> type : BlockFunctionType.REGISTRY) {
      try {
        parser.reader.setCursor(cursorOnStart);
        final BlockFunction parse = type.parse(commandRegistryAccess, parser, suggestionsOnly);
        if (parse != null) {
          // keep the current position of the cursor
          return parse;
        }

      } catch (
          CommandSyntaxException exception1) {
        cursorOnEnd = parser.reader.getCursor();
        exception = exception1;
      }
    }
    parser.reader.setCursor(cursorOnEnd);
    if (exception != null) {
      throw exception;
    }
    throw CANNOT_PARSE.createWithContext(parser.reader);
  }

  default boolean setBlock(World world, BlockPos pos, int flags) {
    final BlockState origState = world.getBlockState(pos);
    MutableObject<NbtCompound> blockEntityData = new MutableObject<>(null);
    boolean result = world.setBlockState(pos, getModifiedState(origState, origState, world, pos, flags, blockEntityData), flags);
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity != null) {
      final NbtCompound modifiedData = blockEntityData.getValue();
      if (modifiedData != null) {
        blockEntity.readNbt(modifiedData);
        result = true;
      }
    }
    return result;
  }

  /**
   * 对已有的方块状态进行修改。如果此方块函数不修改方块状态，应该返回 blockState 参数。
   *
   * @param blockState      当前的一系列修改过程中所使用的方块状态。当不同的多个方块函数依次使用时，方块函数的返回值会用于这个参数。
   * @param origState       在整个修改过程之前，所使用的方块状态。当不同的多个方块函数依次使用时，此参数均不改变。
   * @param world           当前所在的世界。
   * @param pos             正在修改的方块所在的坐标。
   * @param flags           正在修改的方块修改时的 flags。
   * @param blockEntityData 此参数用于在修改方块的过程中一并修改方块实体。在完成对方块状态的修改后，才会将这个数据并入到方块实体中。
   * @return 修改后的方块状态。
   */
  @Contract(mutates = "param6")
  BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData);

  void writeNbt(@NotNull NbtCompound nbtCompound);

  @NotNull BlockFunctionType<?> getType();

  @Override
  default NbtCompound createNbt() {
    NbtCompound nbtCompound = new NbtCompound();
    final BlockFunctionType<?> type = getType();
    final Identifier id = BlockFunctionType.REGISTRY.getId(type);
    nbtCompound.putString("type", Preconditions.checkNotNull(id, "Unknown block function type: %s", type).toString());
    writeNbt(nbtCompound);
    return nbtCompound;
  }

  static BlockFunction fromNbt(NbtCompound nbtCompound) {
    final BlockFunctionType<?> type = BlockFunctionType.REGISTRY.get(new Identifier(nbtCompound.getString("type")));
    Preconditions.checkNotNull(type, "Unknown block function type: %s", type);
    return type.fromNbt(nbtCompound);
  }
}
