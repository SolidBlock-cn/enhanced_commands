package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.NbtFunctionParser;
import pers.solid.ecmd.argument.NbtPredicateParser;
import pers.solid.ecmd.argument.SimpleBlockFunctionParser;
import pers.solid.ecmd.argument.SimpleBlockParser;
import pers.solid.ecmd.command.FillReplaceCommand;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.mixin.MixinShared;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collections;
import java.util.List;

/**
 * 方块函数，用于定义如何在世界的某个地方设置方块。它类似于原版中的 {@link BlockStateArgument} 以及 WorldEdit 中的方块蒙版（block mask）。方块函数不止定义方块，有可能是对方块本身进行修改，也有可能对方块实体进行修改。由于它是在已有方块的基础上进行修改的，故称为方块函数。
 */
public interface BlockFunction extends ExpressionConvertible {
  RegistryKey<Registry<BlockFunction>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("block_function"));
  MapCodec<BlockFunction> MAP_CODEC = BlockFunctionType.REGISTRY.getCodec().dispatchMap(BlockFunction::getType, BlockFunctionType::getCodec);
  Codec<BlockFunction> CODEC = CodecUtil.combined(Registries.BLOCK.getCodec().xmap(block -> new SimpleBlockFunction(block, ImmutableList.of()), SimpleBlockFunction::block), MAP_CODEC.codec(), blockFunction -> blockFunction instanceof SimpleBlockFunction s && s.properties().isEmpty() ? s : null);
  Codec<RegistryEntry<BlockFunction>> ENTRY_CODEC = RegistryElementCodec.of(REGISTRY_KEY, CODEC);

  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.block_function.cannot_parse"));
  Text OVERLAY_TOOLTIP = Text.translatable("enhanced_commands.block_function.overlay.symbol_tooltip");
  Text PICK_TOOLTIP = Text.translatable("enhanced_commands.block_function.pick.symbol_tooltip");

  static @NotNull BlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return parsePick(parseContext);
  }

  static @NotNull BlockFunction parsePick(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseOverlay(parseContext), functions -> {
      ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
      for (BlockFunction function : functions) {
        builder.add(function);
      }
      return new PickBlockFunction(new WeightedList.Uniform<>(builder.build()));
    }, "|", PICK_TOOLTIP, parseContext);
  }

  static @NotNull BlockFunction parseOverlay(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseCombination(parseContext), functions -> {
      ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
      for (BlockFunction blockFunction : functions) {
        builder.add(blockFunction);
      }
      return new OverlayBlockFunction(builder.build());
    }, "*", OVERLAY_TOOLTIP, parseContext);
  }

  static @NotNull <S> BlockFunction parseCombination(ParseContext<S> parseContext) throws CommandSyntaxException {
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
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder.suggest("{", NbtPredicateParser.START_OF_COMPOUND).buildFuture());
    if (reader.canRead() && reader.peek() == '{') {
      // 尝试读取 NBT
      nbtFunction = new NbtFunctionParser<>(parseContext).parseCompound(false);
    } else {
      nbtFunction = null;
    }
    if (propertyNameFunctions != null || nbtFunction != null) {
      return new PropertiesNbtCombinationBlockFunction(parseUnit, propertyNameFunctions == null ? null : new PropertyNamesBlockFunction(propertyNameFunctions), nbtFunction == null ? null : new NbtBlockFunction(nbtFunction));
    }
    return parseUnit;
  }

  @NotNull
  static BlockFunction parseUnit(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorOnStart = reader.getCursor();

    // 强制将 simple 调整到最后再去使用
    for (Parser<BlockFunction> argumentParser : Iterables.concat(BlockFunctionTypes.PARSERS, Collections.singleton(SimpleBlockFunction.Type.SIMPLE_TYPE))) {
      reader.setCursor(cursorOnStart);
      final BlockFunction parse = argumentParser.parse(parseContext);
      if (parse != null) {
        return parse;
      }
    }
    reader.setCursor(cursorOnStart);
    throw CANNOT_PARSE.createWithContext(reader);
  }

  default boolean setBlock(World world, BlockPos pos, BlockFunctionContext context) {
    return setBlock(world, pos, context, null);
  }

  default boolean setBlock(World world, BlockPos pos, BlockFunctionContext context, @Nullable BlockPlacementHistory history) {
    final BlockState origState = world.getBlockState(pos);
    MutableObject<NbtCompound> blockEntityData = new MutableObject<>(null);
    BlockState modifiedState = getModifiedState(origState, origState, world, pos, blockEntityData, context);
    final int modFlags = context.modFlags;
    if ((modFlags & FillReplaceCommand.POST_PROCESS_FLAG) != 0) {
      modifiedState = Block.postProcessState(modifiedState, world, pos);
    }

    if (history != null) {
      history.recordBlockAndEntity(world, pos, modifiedState);
    }
    boolean result = MixinShared.setBlockStateWithModFlags(world, pos, modifiedState, context.flags, modFlags);
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity != null) {
      final NbtCompound modifiedData = blockEntityData.getValue();
      if (modifiedData != null) {
        blockEntity.read(modifiedData, world.getRegistryManager());
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
   * @param blockEntityData 此参数用于在修改方块的过程中一并修改方块实体。在完成对方块状态的修改后，才会将这个数据并入到方块实体中。
   * @param context         正在修改的方块修改时的 flags。
   * @return 修改后的方块状态。
   */
  @NotNull
  BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context);

  @NotNull
  BlockFunctionType<?> getType();

  default boolean isEmpty() {
    return this == EmptyBlockFunction.INSTANCE;
  }

  /**
   * 取消该对象的缓存状态。例如，对于 {@link NoiseBlockFunction} 而言，调用一次该方法将会使其重新生成采样器，其种子可能随机生成。
   */
  default BlockFunction getRefreshed(Random random) {
    return this;
  }
}
