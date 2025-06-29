package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.ConstantBlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.*;
import java.util.stream.Stream;

public record BlocksNbtData(RegionArgument<?> region, BlockPredicate blockPredicate) implements NbtTarget<BlockEntity> {
  public static final MapCodec<BlocksNbtData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      RegionArgument.CODEC.fieldOf("region").forGetter(BlocksNbtData::region),
      BlockPredicate.CODEC.fieldOf("block_predicate").forGetter(BlocksNbtData::blockPredicate)
  ).apply(i, BlocksNbtData::new));

  public static BlocksNbtData handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parseContext.reader());
    final RegionArgument<?> regionArgument = RegionArgument.parse(parseContext);
    parseContext.clearSuggestion();
    return new BlocksNbtData(regionArgument, ConstantBlockPredicate.ALWAYS_TRUE);
  }

  @Override
  public Collection<BlockEntity> values(ServerCommandSource source) throws CommandSyntaxException {
    final Region region = this.region.toAbsoluteRegion((PositionProvider) source);
    final ServerWorld world = source.getWorld();
    final ImmutableList<BlockEntity> blockEntities;
    final BlockBox blockBox = region.minContainingBlockBox();
    if (region.numberOfBlocksAffected() < 32L || blockBox == null) {
      blockEntities = region.stream().map(world::getBlockEntity).filter(Objects::nonNull).collect(ImmutableList.toImmutableList());
    } else {
      Set<WorldChunk> affectedChunks = new HashSet<>();
      for (BlockPos shrunkPos : BlockPos.iterate(MathHelper.floorDiv(blockBox.getMinX(), 16), 0, MathHelper.floorDiv(blockBox.getMinZ(), 16), MathHelper.floorDiv(blockBox.getMaxX(), 16), 0, MathHelper.floorDiv(blockBox.getMaxZ(), 16))) {
        final WorldChunk worldChunk = world.getChunkManager().getWorldChunk(shrunkPos.getX(), shrunkPos.getZ());
        if (worldChunk != null) affectedChunks.add(worldChunk);
      }
      Stream<Map.Entry<BlockPos, BlockEntity>> stream = affectedChunks.stream().flatMap(worldChunk -> worldChunk.getBlockEntities().entrySet().stream()).filter(entry -> region.contains(entry.getKey()));
      if (blockPredicate != null) {
        final ExecutionContext context = new ExecutionContext(world.getRandom(), source, null);
        stream = stream.filter(entry -> blockPredicate.test(new CachedBlockPosition(world, entry.getKey(), false), context));
      }
      blockEntities = stream.map(Map.Entry::getValue).collect(ImmutableList.toImmutableList());
    }
    return blockEntities;
  }

  @Override
  public NbtCompound getNbtFor(ServerCommandSource commandSource, BlockEntity source) {
    return source.createNbtWithIdentifyingData(commandSource.getRegistryManager());
  }

  @Override
  public int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException {
    final Map<BlockEntity, NbtElement> nbts = getNbtsInPath(source, path);
    if (nbts.size() == 1 && nbtConcentrationType != NbtConcentrationType.LIST) {
      final var soleEntry = nbts.entrySet().iterator().next();
      final BlockEntity value = soleEntry.getKey();
      final BlockPos pos = value.getPos();

      final NbtElement nbt = soleEntry.getValue();
      if (path == null) {
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.block.query", value.getCachedState().getBlock().getName(), TextUtil.wrapVector(pos), NbtHelper.toPrettyPrintedText(nbt)), false);
        return NbtSource.toInt(nbt);
      }
      if (scale == 1) {
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.block.query_path", value.getCachedState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), NbtHelper.toPrettyPrintedText(nbt)), false);
        return NbtSource.toInt(nbt);
      } else {
        final double scaledValue = NbtSource.scaleNbt(nbt, scale, path);
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.block.query_scale", value.getCachedState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), scale, scaledValue), false);
        return MathHelper.floor(scaledValue);
      }
    }
    final Object2DoubleMap<BlockEntity> scaledNbts;
    if (scale != 1 && path != null) {
      scaledNbts = new Object2DoubleOpenHashMap<>();
      for (Map.Entry<BlockEntity, NbtElement> entry : nbts.entrySet()) {
        scaledNbts.put(entry.getKey(), NbtSource.scaleNbt(entry.getValue(), scale, path));
      }
    } else {
      scaledNbts = null;
    }

    if (nbtConcentrationType == NbtConcentrationType.ALL) {
      source.sendFeedback$ecBridge(() -> {
        List<Text> texts = new ArrayList<>();
        texts.add(Text.translatable("enhanced_commands.commands.nbt.blocks.query.header", Math.min(nbts.size(), QUERY_LIMIT)).enhanced$$().formatted(Formatting.AQUA));
        for (var entry : Iterables.limit(nbts.entrySet(), QUERY_LIMIT)) {
          final BlockEntity blockEntity = entry.getKey();
          final BlockPos pos = blockEntity.getPos();
          if (path == null) {
            texts.add(Text.literal(" - ").append(Text.translatable("enhanced_commands.commands.nbt.block.query", blockEntity.getCachedState().getBlock().getName(), TextUtil.wrapVector(pos), NbtHelper.toPrettyPrintedText(entry.getValue()))));
          } else if (scale == 1) {
            texts.add(Text.literal(" - ").append(Text.translatable("enhanced_commands.commands.nbt.block.query_path", blockEntity.getCachedState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), NbtHelper.toPrettyPrintedText(entry.getValue()))));
          } else {
            texts.add(Text.literal(" - ").append(Text.translatable("enhanced_commands.commands.nbt.block.query_scale", blockEntity.getCachedState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), scale, scaledNbts.getOrDefault(blockEntity, 0))));
          }
        }
        if (nbts.size() > QUERY_LIMIT) {
          texts.add(Text.translatable("enhanced_commands.commands.nbt.query_limit_notice", QUERY_LIMIT).formatted(Formatting.YELLOW));
        }
        return ScreenTexts.joinLines(texts);
      }, false);
      return nbts.size();
    }

    final NbtElement concentratedNbts = nbtConcentrationType.concentrate(nbts.values(), random);
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.blocks.query", nbts.size(), NbtHelper.toPrettyPrintedText(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.blocks.query_path", nbts.size(), path.toString(), NbtHelper.toPrettyPrintedText(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else {
      final double scaledConcentratedNbt = NbtSource.scaleNbt(concentratedNbts, scale, path);
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.blocks.query_scale", nbts.size(), path.toString(), scale, scaledConcentratedNbt).enhanced$$(), false);
      return MathHelper.floor(scaledConcentratedNbt);
    }
  }

  @Override
  public void setNbtFor(ServerCommandSource commandSource, BlockEntity target, NbtCompound nbt) throws CommandSyntaxException {
    target.read(nbt, commandSource.getRegistryManager());
    target.markDirty();
    final World world = target.getWorld();
    if (world != null) {
      world.updateListeners(target.getPos(), target.getCachedState(), target.getCachedState(), Block.NOTIFY_ALL);
    }
  }

  @Override
  public Text feedbackModify(Collection<BlockEntity> values) {
    if (values.size() == 1) {
      final BlockEntity blockEntity = values.iterator().next();
      final BlockPos pos = blockEntity.getPos();
      return Text.translatable("commands.data.block.modified", pos.getX(), pos.getY(), pos.getZ());
    } else {
      return Text.translatable("enhanced_commands.commands.nbt.blocks.modify", values.size()).enhanced$$();
    }
  }
}
