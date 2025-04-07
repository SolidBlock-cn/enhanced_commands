package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public record BlockNbtData(BlockEntity blockEntity) implements NbtTarget.Single<BlockEntity> {
  @Override
  public BlockEntity value() {
    return blockEntity;
  }

  @Override
  public Text feedbackQuery(NbtElement element) {
    final BlockPos pos = blockEntity.getPos();
    return Text.translatable("commands.data.block.query", pos.getX(), pos.getY(), pos.getZ(), NbtHelper.toPrettyPrintedText(element));
  }

  @Override
  public NbtCompound getNbtFor(BlockEntity source, @NotNull RegistryWrapper.WrapperLookup registryLookup) {
    return source.createNbtWithIdentifyingData(registryLookup);
  }

  @Override
  public void setNbtFor(BlockEntity target, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    BlockState blockState = target.getCachedState();
    target.read(nbt, registryLookup);
    target.markDirty();
    final World world = target.getWorld();
    if (world != null) {
      world.updateListeners(target.getPos(), blockState, blockState, 3);
    }
  }

  @Override
  public Text feedbackModify() {
    final BlockPos pos = blockEntity.getPos();
    return Text.translatable("commands.data.block.modified", pos.getX(), pos.getY(), pos.getZ());
  }
}

