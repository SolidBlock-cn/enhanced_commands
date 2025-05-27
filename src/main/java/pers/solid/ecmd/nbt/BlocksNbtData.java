package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.TextUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public record BlocksNbtData(Collection<BlockEntity> blockEntities) implements NbtTarget<BlockEntity> {

  @Override
  public Collection<BlockEntity> values() {
    return blockEntities;
  }

  @Override
  public NbtCompound getNbtFor(BlockEntity source, @NotNull RegistryWrapper.WrapperLookup registryLookup) {
    return source.createNbtWithIdentifyingData(registryLookup);
  }

  @Override
  public int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException {
    if (blockEntities.size() == 1 && nbtConcentrationType != NbtConcentrationType.LIST) {
      return new BlockNbtData(blockEntities.iterator().next()).executeQuery(source, path, scale, nbtConcentrationType, random);
    }
    final Map<BlockEntity, NbtElement> nbts = getNbtsInPath(path, source.getRegistryManager());
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
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.blocks.query", blockEntities.size(), NbtHelper.toPrettyPrintedText(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.blocks.query_path", blockEntities.size(), path.toString(), NbtHelper.toPrettyPrintedText(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else {
      final double scaledConcentratedNbt = NbtSource.scaleNbt(concentratedNbts, scale, path);
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.blocks.query_scale", blockEntities.size(), path.toString(), scale, scaledConcentratedNbt).enhanced$$(), false);
      return MathHelper.floor(scaledConcentratedNbt);
    }
  }

  @Override
  public void setNbtFor(BlockEntity target, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    target.read(nbt, registryLookup);
    target.markDirty();
    final World world = target.getWorld();
    if (world != null) {
      world.updateListeners(target.getPos(), target.getCachedState(), target.getCachedState(), Block.NOTIFY_ALL);
    }
  }

  @Override
  public Text feedbackModify() {
    if (blockEntities.size() == 1) {
      final BlockEntity blockEntity = blockEntities.iterator().next();
      final BlockPos pos = blockEntity.getPos();
      return Text.translatable("commands.data.block.modified", pos.getX(), pos.getY(), pos.getZ());
    } else {
      return Text.translatable("enhanced_commands.commands.nbt.blocks.modify", blockEntities.size()).enhanced$$();
    }
  }
}
