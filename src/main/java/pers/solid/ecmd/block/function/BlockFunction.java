package pers.solid.ecmd.block.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.SimpleBlockParser;
import pers.solid.ecmd.command.SetReplaceBlocksCommand;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.nbt.NbtParserShared;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.nbt.function.NbtFunctionParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.property.function.PropertyNameFunction;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.Collections;
import java.util.List;

/**
 * 方块函数，用于定义如何在世界的某个地方设置方块。它类似于原版中的 {@link BlockInput} 以及 WorldEdit 中的方块蒙版（block mask）。方块函数不止定义方块，有可能是对方块本身进行修改，也有可能对方块实体进行修改。由于它是在已有方块的基础上进行修改的，故称为方块函数。
 */
public interface BlockFunction extends ExpressionConvertible {
  ResourceKey<Registry<BlockFunction>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("block_function"));
  MapCodec<BlockFunction> MAP_CODEC = BlockFunctionType.CODEC.dispatchMap(BlockFunction::getType, BlockFunctionType::codec);
  Codec<BlockFunction> CODEC = Codec.lazyInitialized(() -> CodecUtil.combined(
      CodecUtil.combinedIdAndTag(SimpleBlockFunction.STRING_BASED_CODEC, TagBlockFunction.STRING_BASED_CODEC),
      MAP_CODEC.codec(),
      blockFunction -> blockFunction instanceof SimpleBlockFunction s && s.properties().isEmpty() ? Either.left(s) : blockFunction instanceof TagBlockFunction t && t.properties().isEmpty() ? Either.right(t) : null,
      Either::unwrap));

  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.block_function.cannot_parse"));
  Component OVERLAY_TOOLTIP = Component.translatable("enhanced_commands.function.overlay.symbol_tooltip");
  Component PICK_TOOLTIP = Component.translatable("enhanced_commands.function.pick.symbol_tooltip");

  static BlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return parsePick(parseContext);
  }

  static BlockFunction parsePick(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseOverlay(parseContext), functions -> {
      ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
      for (BlockFunction function : functions) {
        builder.add(function);
      }
      return new PickBlockFunction(new WeightedList.Uniform<>(builder.build()));
    }, "|", PICK_TOOLTIP, parseContext);
  }

  static BlockFunction parseOverlay(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseCombination(parseContext), functions -> {
      ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
      for (BlockFunction blockFunction : functions) {
        builder.add(blockFunction);
      }
      return new OverlayBlockFunction(builder.build());
    }, "*", OVERLAY_TOOLTIP, parseContext);
  }

  static <S> BlockFunction parseCombination(ParseContext<S> parseContext) throws CommandSyntaxException {
    final BlockFunction parseUnit = parseUnit(parseContext);
    if (parseUnit instanceof NbtBlockFunction) {
      return parseUnit;
    }
    final StringReader reader = parseContext.reader();
    List<PropertyNameFunction> propertyNameFunctions;

    if (!(parseUnit instanceof PropertyNamesBlockFunction) && reader.canRead(0) && reader.peek(-1) != ']') {
      // 当前面以“]”结尾时，说明已经在其他解析器中读取了属性，此时在这里不再读取任何属性
      // 尝试读取属性
      parseContext.addSuggestion((context, builder) -> builder.suggest("[", SimpleBlockParser.START_OF_PROPERTIES).buildFuture());
      if (reader.canRead() && reader.peek() == '[') {
        final SimpleBlockFunctionParser<S> suggestedParser = new SimpleBlockFunctionParser<>(parseContext);
        suggestedParser.parsePropertyNames();
        propertyNameFunctions = suggestedParser.propertyNameFunctions;
      } else {
        propertyNameFunctions = null;
      }
    } else {
      propertyNameFunctions = null;
    }
    NbtFunction nbtFunction;
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder.suggest("{", NbtParserShared.START_OF_COMPOUND).buildFuture());
    if (reader.canRead() && reader.peek() == '{') {
      // 尝试读取 NBT
      nbtFunction = NbtFunctionParser.parseCompound(parseContext, false);
    } else {
      nbtFunction = null;
    }
    if (propertyNameFunctions != null || nbtFunction != null) {
      return new PropertiesNbtCombinationBlockFunction(parseUnit, propertyNameFunctions == null ? null : new PropertyNamesBlockFunction(propertyNameFunctions), nbtFunction == null ? null : new NbtBlockFunction(nbtFunction));
    }
    return parseUnit;
  }

  static BlockFunction parseUnit(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorOnStart = reader.getCursor();

    // 强制将 simple 调整到最后再去使用
    for (Parser<? extends BlockFunction> argumentParser : Iterables.concat(BlockFunctionParsing.PARSERS, Collections.singleton(SimpleBlockFunction.SimpleParser.INSTANCE))) {
      reader.setCursor(cursorOnStart);
      final BlockFunction parse = argumentParser.parse(parseContext);
      if (parse != null) {
        return parse;
      }
    }
    reader.setCursor(cursorOnStart);
    throw CANNOT_PARSE.createWithContext(reader);
  }

  default boolean setBlock(Level world, BlockPos pos, BlockFunctionContext context) throws CommandSyntaxException {
    return setBlock(world, pos, context, null, null);
  }

  default boolean setBlock(Level world, BlockPos pos, BlockFunctionContext context, @Nullable BlockState oldState, @Nullable BlockPlacementHistory history) throws CommandSyntaxException {
    final BlockState origState = world.getBlockState(pos);
    MutableObject<@Nullable CompoundTag> blockEntityData = new MutableObject<>(null);
    BlockState newState = getModifiedState(origState, origState, world, pos, blockEntityData, context);
    final int modFlags = context.modFlags;
    if ((modFlags & SetReplaceBlocksCommand.POST_PROCESS_FLAG) != 0) {
      newState = Block.updateFromNeighbourShapes(newState, world, pos);
    }

    if (history != null) {
      history.recordBlockAndEntity(world, pos, oldState == null ? world.getBlockState(pos) : oldState, newState);
    }
    final BlockEntity oldEntity = world.getBlockEntity(pos);
    if (oldEntity != null && !oldEntity.isValidBlockState(newState)) {
      world.removeBlockEntity(pos);
    }
    boolean result = MixinShared.setBlockStateWithModFlags(world, pos, newState, context.flags, modFlags);
    final BlockEntity newEntity = world.getBlockEntity(pos);
    if (newEntity != null) {
      final CompoundTag modifiedData = blockEntityData.getValue();
      if (modifiedData != null) {
        newEntity.loadWithComponents(modifiedData, world.registryAccess());
        result = true;
      }
    }
    return result;
  }

  /**
   * 对已有的方块状态进行修改。如果此方块函数不修改方块状态，应该返回 blockState 参数。
   *
   * @param blockState      当前的一系列修改过程中所使用的方块状态。当不同的多个方块函数依次使用时，方块函数的返回值会用于这个参数。
   * @param originalState   在整个修改过程之前，所使用的方块状态。当不同的多个方块函数依次使用时，此参数均不改变。
   * @param level           当前所在的世界。
   * @param pos             正在修改的方块所在的坐标。
   * @param blockEntityData 此参数用于在修改方块的过程中一并修改方块实体。在完成对方块状态的修改后，才会将这个数据并入到方块实体中。
   * @param context         正在修改的方块修改时的 flags。
   * @return 修改后的方块状态。
   */
  BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException;

  BlockFunctionType<?> getType();

  default boolean isEmpty() {
    return this == EmptyBlockFunction.INSTANCE;
  }
}
