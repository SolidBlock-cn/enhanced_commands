package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.function.FailableFunction;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.function.Function;

public record BlocksNbtData(Collection<BlockEntity> blockEntities, NbtConcentrationType nbtConcentrationType, Random random) implements NbtSource, NbtTarget {
  @Override
  public <T> Collection<T> getNbts(Function<NbtCompound, T> mappingFunction) throws CommandSyntaxException {
    return blockEntities.stream().map(BlockEntity::createNbtWithIdentifyingData).map(mappingFunction).collect(ImmutableList.toImmutableList());
  }

  @Override
  public NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) throws CommandSyntaxException {
    return nbtConcentrationType.concentrate(nbtElements, random);
  }

  @Override
  public Text feedbackQuery(NbtElement nbtElement, NbtConcentrationType nbtConcentrationType) {
    return TextUtil.enhancedTranslatable("enhanced_commands.nbt.blocks.query", blockEntities.size(), nbtElement);
  }

  @Override
  public void setNbt(NbtCompound nbt) {
    for (BlockEntity blockEntity : blockEntities) {
      blockEntity.readNbt(nbt);
      blockEntity.markDirty();
      blockEntity.getWorld().updateListeners(blockEntity.getPos(), blockEntity.getCachedState(), blockEntity.getCachedState(), Block.NOTIFY_ALL);
    }
  }

  @Override
  public void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator) throws CommandSyntaxException {
    for (BlockEntity blockEntity : blockEntities) {
      blockEntity.readNbt(operator.apply(blockEntity.createNbtWithIdentifyingData()));
      blockEntity.markDirty();
      blockEntity.getWorld().updateListeners(blockEntity.getPos(), blockEntity.getCachedState(), blockEntity.getCachedState(), Block.NOTIFY_ALL);
    }
  }

  @Override
  public Text feedbackModify() {
    return TextUtil.enhancedTranslatable("enhanced_commands.nbt.blocks.modify", blockEntities.size());
  }
}
