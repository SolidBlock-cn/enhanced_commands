package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TextUtil;

public record BlockNbtData(BlockEntity blockEntity) implements NbtTarget.Single<BlockEntity> {
  @Override
  public BlockEntity value() {
    return blockEntity;
  }

  @Override
  public int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale) throws CommandSyntaxException {
    final BlockPos pos = blockEntity.getPos();

    final NbtCompound nbt = getNbt(source.getRegistryManager());
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.block.query", blockEntity.getCachedState().getBlock().getName(), TextUtil.wrapVector(pos), NbtHelper.toPrettyPrintedText(nbt)), false);
      return NbtSource.toInt(nbt);
    }
    final NbtElement nbtAtPath = Iterables.getOnlyElement(path.get(nbt));
    if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.block.query_path", blockEntity.getCachedState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), NbtHelper.toPrettyPrintedText(nbtAtPath)), false);
      return NbtSource.toInt(nbtAtPath);
    } else {
      final double scaledValue = NbtSource.scaleNbt(nbtAtPath, scale, path);
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.block.query_scale", blockEntity.getCachedState().getBlock().getName(), TextUtil.wrapVector(pos), path.toString(), scale, scaledValue), false);
      return MathHelper.floor(scaledValue);
    }
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

