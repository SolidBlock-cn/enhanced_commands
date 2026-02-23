package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.ConstantBlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TextUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public record BlockNbtData(RegionArgument<?> region, BlockPredicate blockPredicate) implements NbtTarget<BlockEntity> {
  public static final MapCodec<BlockNbtData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      RegionArgument.CODEC.fieldOf("region").forGetter(BlockNbtData::region),
      BlockPredicate.CODEC.fieldOf("block").forGetter(BlockNbtData::blockPredicate)
  ).apply(i, BlockNbtData::new));

  public static BlockNbtData handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parseContext.reader());
    final RegionArgument<?> regionArgument = RegionArgument.parse(parseContext);
    parseContext.clearSuggestion();
    return new BlockNbtData(regionArgument, ConstantBlockPredicate.ALWAYS_TRUE);
  }

  @Override
  public Collection<BlockEntity> values(CommandSourceStack source) throws CommandSyntaxException {
    final Region region = this.region.toAbsoluteRegion(source);
    final ServerLevel world = source.getLevel();
    final ImmutableList<BlockEntity> blockEntities;
    final BoundingBox blockBox = region.minContainingBlockBox();
    if (region.numberOfBlocksAffected() < 32L || blockBox == null) {
      // 区域较小的情况下，可以直接迭代区域中的所有方块坐标，并筛选实体。
      final Stream<@NotNull BlockEntity> stream;
      if (blockPredicate != null) {
        final ExecutionContext context = new ExecutionContext(world.getRandom(), source, null);
        stream = region
            .stream()
            .mapMulti((@NotNull BlockPos blockPos, Consumer<BlockEntity> consumer) -> {
              // 在有方块谓词的情况下，在已有实体的情况下对谓词进行测试。
              final BlockEntity blockEntity = world.getBlockEntity(blockPos);
              if (blockEntity != null) {
                final BlockInWorld cachedBlockPosition = new BlockInWorld(world, blockPos, false);
                if (blockPredicate.test(cachedBlockPosition, context)) {
                  consumer.accept(blockEntity);
                }
              }
            });
      } else {
        stream = region.stream().map(world::getBlockEntity).filter(Objects::nonNull);
      }
      blockEntities = stream.collect(ImmutableList.toImmutableList());
    } else {
      // 区域较大的情况下，迭代所涉区块内的所有实体，并对实体测试区域范围。
      Set<LevelChunk> affectedChunks = new HashSet<>();
      for (BlockPos shrunkPos : BlockPos.betweenClosed(Mth.floorDiv(blockBox.minX(), 16), 0, Mth.floorDiv(blockBox.minZ(), 16), Mth.floorDiv(blockBox.maxX(), 16), 0, Mth.floorDiv(blockBox.maxZ(), 16))) {
        final LevelChunk worldChunk = world.getChunkSource().getChunkNow(shrunkPos.getX(), shrunkPos.getZ());
        if (worldChunk != null) affectedChunks.add(worldChunk);
      }
      Stream<Map.Entry<BlockPos, BlockEntity>> stream = affectedChunks
          .stream()
          .flatMap(worldChunk -> worldChunk.getBlockEntities().entrySet().stream())
          .filter(entry -> region.contains(entry.getKey()));
      if (blockPredicate != null) {
        // 在有方块谓词的情况下，对已有的方块实体坐标测试方块谓词。
        final ExecutionContext context = new ExecutionContext(world.getRandom(), source, null);
        stream = stream.filter(entry -> blockPredicate.test(new BlockInWorld(world, entry.getKey(), false), context));
      }
      blockEntities = stream.map(Map.Entry::getValue).collect(ImmutableList.toImmutableList());
    }
    return blockEntities;
  }

  @Override
  public CompoundTag getNbtFor(CommandSourceStack commandSource, BlockEntity source) {
    return source.saveWithFullMetadata(commandSource.registryAccess());
  }

  @Override
  public Type getType() {
    return Type.BLOCK;
  }

  @Override
  public int executeQuery(CommandSourceStack source, NbtPathArgument.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, RandomSource random) throws CommandSyntaxException {
    final Map<BlockEntity, Tag> nbts = getNbtsInPath(source, path);
    if (nbts.size() == 1 && nbtConcentrationType != NbtConcentrationType.LIST) {
      final var soleEntry = nbts.entrySet().iterator().next();
      final BlockEntity value = soleEntry.getKey();
      final BlockPos pos = value.getBlockPos();

      final Tag nbt = soleEntry.getValue();
      if (path == null) {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.block.query", value.getBlockState().getBlock().getName(), TextUtil.wrapVector(pos), NbtUtils.toPrettyComponent(nbt)), false);
        return NbtSource.toInt(nbt);
      }
      if (scale == 1) {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.block.query_path", value.getBlockState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), NbtUtils.toPrettyComponent(nbt)), false);
        return NbtSource.toInt(nbt);
      } else {
        final double scaledValue = NbtSource.scaleNbt(nbt, scale, path);
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.block.query_scale", value.getBlockState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), scale, scaledValue), false);
        return Mth.floor(scaledValue);
      }
    }
    final Object2DoubleMap<BlockEntity> scaledNbts;
    if (scale != 1 && path != null) {
      scaledNbts = new Object2DoubleOpenHashMap<>();
      for (Map.Entry<BlockEntity, Tag> entry : nbts.entrySet()) {
        scaledNbts.put(entry.getKey(), NbtSource.scaleNbt(entry.getValue(), scale, path));
      }
    } else {
      scaledNbts = null;
    }

    if (nbtConcentrationType == NbtConcentrationType.ALL) {
      source.sendFeedback$ecBridge(() -> {
        List<Component> texts = new ArrayList<>();
        texts.add(Component.translatable("enhanced_commands.commands.nbt.blocks.query.header", Math.min(nbts.size(), QUERY_LIMIT)).enhanced$$().withStyle(ChatFormatting.AQUA));
        for (var entry : Iterables.limit(nbts.entrySet(), QUERY_LIMIT)) {
          final BlockEntity blockEntity = entry.getKey();
          final BlockPos pos = blockEntity.getBlockPos();
          if (path == null) {
            texts.add(Component.literal(" - ").append(Component.translatable("enhanced_commands.commands.nbt.block.query", blockEntity.getBlockState().getBlock().getName(), TextUtil.wrapVector(pos), NbtUtils.toPrettyComponent(entry.getValue()))));
          } else if (scale == 1) {
            texts.add(Component.literal(" - ").append(Component.translatable("enhanced_commands.commands.nbt.block.query_path", blockEntity.getBlockState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), NbtUtils.toPrettyComponent(entry.getValue()))));
          } else {
            texts.add(Component.literal(" - ").append(Component.translatable("enhanced_commands.commands.nbt.block.query_scale", blockEntity.getBlockState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), scale, scaledNbts.getOrDefault(blockEntity, 0))));
          }
        }
        if (nbts.size() > QUERY_LIMIT) {
          texts.add(Component.translatable("enhanced_commands.commands.nbt.query_limit_notice", QUERY_LIMIT).withStyle(ChatFormatting.YELLOW));
        }
        return CommonComponents.joinLines(texts);
      }, false);
      return nbts.size();
    }

    final Tag concentratedNbts = nbtConcentrationType.concentrate(nbts.values(), random);
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.blocks.query", nbts.size(), NbtUtils.toPrettyComponent(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.blocks.query_path", nbts.size(), path.toString(), NbtUtils.toPrettyComponent(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else {
      final double scaledConcentratedNbt = NbtSource.scaleNbt(concentratedNbts, scale, path);
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.blocks.query_scale", nbts.size(), path.toString(), scale, scaledConcentratedNbt).enhanced$$(), false);
      return Mth.floor(scaledConcentratedNbt);
    }
  }

  @Override
  public void setNbtFor(CommandSourceStack commandSource, BlockEntity target, CompoundTag nbt) throws CommandSyntaxException {
    target.loadWithComponents(nbt, commandSource.registryAccess());
    target.setChanged();
    final Level world = target.getLevel();
    if (world != null) {
      world.sendBlockUpdated(target.getBlockPos(), target.getBlockState(), target.getBlockState(), Block.UPDATE_ALL);
    }
  }

  @Override
  public Component feedbackModify(Collection<BlockEntity> values) {
    if (values.size() == 1) {
      final BlockEntity blockEntity = values.iterator().next();
      final BlockPos pos = blockEntity.getBlockPos();
      return Component.translatable("commands.data.block.modified", pos.getX(), pos.getY(), pos.getZ());
    } else {
      return Component.translatable("enhanced_commands.commands.nbt.blocks.modify", values.size()).enhanced$$();
    }
  }

  @Override
  public @NotNull String asString() {
    return "block " + region.asString();
  }
}
