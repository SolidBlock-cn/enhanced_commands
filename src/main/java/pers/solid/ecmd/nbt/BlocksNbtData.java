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
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.NbtConcentrationType;

import java.util.Collection;

public record BlocksNbtData(Collection<BlockEntity> blockEntities, NbtConcentrationType nbtConcentrationType, Random random) implements NbtTarget<BlockEntity> {

  @Override
  public Collection<BlockEntity> values() {
    return blockEntities;
  }

  @Override
  public NbtCompound getNbtFor(BlockEntity source, @NotNull RegistryWrapper.WrapperLookup registryLookup) {
    return source.createNbtWithIdentifyingData(registryLookup);
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
      return Text.translatable("enhanced_commands.nbt.blocks.modify", blockEntities.size()).enhanced$$();
    }
  }
}
