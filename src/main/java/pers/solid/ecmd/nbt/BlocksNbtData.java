package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Collection;

public record BlocksNbtData(Collection<BlockEntity> blockEntities, NbtConcentrationType nbtConcentrationType, Random random) implements NbtSource, NbtTarget {
  @Override
  public <T> Collection<T> getNbts(FailableFunction<NbtCompound, T, CommandSyntaxException> mappingFunction, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException {
    return IterateUtils.transformFailableImmutableList(blockEntities, blockEntity -> mappingFunction.apply(blockEntity.createNbtWithIdentifyingData(registryLookup)));
  }

  @Override
  public NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) throws CommandSyntaxException {
    return nbtConcentrationType.concentrate(nbtElements, random);
  }

  @Override
  public Text feedbackQuery(NbtElement nbtElement) {
    if (blockEntities.size() == 1) {
      final BlockPos pos = blockEntities.iterator().next().getPos();
      return Text.translatable("commands.data.block.query", pos.getX(), pos.getY(), pos.getZ(), NbtHelper.toPrettyPrintedText(nbtElement));
    } else {
      return Text.translatable("enhanced_commands.nbt.blocks.query", blockEntities.size(), NbtHelper.toPrettyPrintedText(nbtElement)).enhanced$$();
    }
  }

  @Override
  public void setNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    for (BlockEntity blockEntity : blockEntities) {
      blockEntity.read(nbt, registryLookup);
      blockEntity.markDirty();
      final World world = blockEntity.getWorld();
      if (world != null) {
        world.updateListeners(blockEntity.getPos(), blockEntity.getCachedState(), blockEntity.getCachedState(), Block.NOTIFY_ALL);
      }
    }
  }

  @Override
  public void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    for (BlockEntity blockEntity : blockEntities) {
      blockEntity.read(operator.apply(blockEntity.createNbtWithIdentifyingData(registryLookup)), registryLookup);
      blockEntity.markDirty();
      final World world = blockEntity.getWorld();
      if (world != null) {
        world.updateListeners(blockEntity.getPos(), blockEntity.getCachedState(), blockEntity.getCachedState(), Block.NOTIFY_ALL);
      }
    }
  }

  @Override
  public Text feedbackModify() {
    if (blockEntities.size() == 1) {
      final BlockEntity blockEntity = blockEntities.iterator().next();
      final BlockPos pos = blockEntity.getPos();
      return Text.translatable("commands.data.block.modified", pos.getX(), pos.getY(), pos.getZ());
    } else {
      return Text.translatable("enhanced_commands.nbt.blocks.modify", blockEntities.size()).enhanced$$();
    }
  }
}
