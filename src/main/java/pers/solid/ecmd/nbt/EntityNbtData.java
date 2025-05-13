package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record EntityNbtData(Entity entity) implements NbtTarget.Single<Entity> {
  @Override
  public Entity value() {
    return entity;
  }


  @Override
  public int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale) throws CommandSyntaxException {
    final NbtElement nbt = getNbt(path, source.getRegistryManager());
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.entity.query", this.entity.getDisplayName(), NbtHelper.toPrettyPrintedText(nbt)), false);
      return NbtSource.toInt(nbt);
    }
    if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.entity.query_path", this.entity.getDisplayName(), path.toString(), NbtHelper.toPrettyPrintedText(nbt)), false);
      return NbtSource.toInt(nbt);
    } else {
      final double scaledValue = NbtSource.scaleNbt(nbt, scale, path);
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.nbt.entity.query_scale", this.entity.getDisplayName(), path.toString(), scale, NbtHelper.toPrettyPrintedText(nbt)), false);
      return MathHelper.floor(scaledValue);
    }
  }

  @Override
  public NbtCompound getNbtFor(Entity source, @NotNull RegistryWrapper.WrapperLookup registryLookup) {
    return NbtPredicate.entityToNbt(source);
  }

  @Override
  public void setNbtFor(Entity target, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    UUID uUID = target.getUuid();
    target.readNbt(nbt);
    target.setUuid(uUID);
  }


  @Override
  public Text feedbackModify() {
    return Text.translatable("commands.data.entity.modified", this.entity.getDisplayName());
  }
}
