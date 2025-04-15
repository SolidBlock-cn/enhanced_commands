package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.DataCommandStorage;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record StorageNbtData(DataCommandStorage storage, Identifier value) implements NbtTarget.Single<Identifier> {
  @Override
  public int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale) throws CommandSyntaxException {
    final NbtCompound nbt = getNbt(source.getRegistryManager());
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.storage.query", value, NbtHelper.toPrettyPrintedText(nbt)), false);
      return NbtSource.toInt(nbt);
    }
    final NbtElement nbtAtPath = Iterables.getOnlyElement(path.get(nbt));
    if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.storage.query_path", value, path.toString(), NbtHelper.toPrettyPrintedText(nbtAtPath)), false);
      return NbtSource.toInt(nbtAtPath);
    } else {
      final double scaledValue = NbtSource.scaleNbt(nbtAtPath, scale, path);
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.storage.query_scale", value, path.toString(), scale, NbtHelper.toPrettyPrintedText(nbtAtPath)), false);
      return MathHelper.floor(scaledValue);
    }
  }

  @Override
  public NbtCompound getNbtFor(Identifier source, @NotNull RegistryWrapper.WrapperLookup registryLookup) {
    return this.storage.get(source);
  }

  @Override
  public void setNbtFor(Identifier target, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    storage.set(target, nbt);
  }

  @Override
  public Text feedbackModify() {
    return Text.translatable("commands.data.storage.modified", this.value);
  }
}
