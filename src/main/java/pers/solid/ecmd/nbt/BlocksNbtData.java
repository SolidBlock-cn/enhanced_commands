package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.NbtConcentrationType;

import java.util.Collection;
import java.util.function.Function;

public record BlocksNbtData(Collection<BlockEntity> blockEntities, NbtConcentrationType nbtConcentrationType, Random random) implements NbtSource, NbtTarget {
  @Override
  public <T> Collection<T> getNbts(Function<NbtCompound, T> mappingFunction, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException {
    return blockEntities.stream().map(blockEntity -> blockEntity.createNbtWithIdentifyingData(registryLookup)).map(mappingFunction).collect(ImmutableList.toImmutableList());
  }

  @Override
  public NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) throws CommandSyntaxException {
    return nbtConcentrationType.concentrate(nbtElements, random);
  }

  @Override
  public Text feedbackQuery(NbtElement nbtElement, NbtConcentrationType nbtConcentrationType) {
    return Text.translatable("enhanced_commands.nbt.blocks.query", blockEntities.size(), nbtElement).enhanced$$();
  }

  @Override
  public void setNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    for (BlockEntity blockEntity : blockEntities) {
      blockEntity.read(nbt, registryLookup);
      blockEntity.markDirty();
      blockEntity.getWorld().updateListeners(blockEntity.getPos(), blockEntity.getCachedState(), blockEntity.getCachedState(), Block.NOTIFY_ALL);
    }
  }

  @Override
  public void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    for (BlockEntity blockEntity : blockEntities) {
      blockEntity.read(operator.apply(blockEntity.createNbtWithIdentifyingData(registryLookup)), registryLookup);
      blockEntity.markDirty();
      blockEntity.getWorld().updateListeners(blockEntity.getPos(), blockEntity.getCachedState(), blockEntity.getCachedState(), Block.NOTIFY_ALL);
    }
  }

  @Override
  public Text feedbackModify() {
    return Text.translatable("enhanced_commands.nbt.blocks.modify", blockEntities.size()).enhanced$$();
  }
}
