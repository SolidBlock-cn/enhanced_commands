package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record EntityNbtData(Entity entity) implements NbtTarget.Single<Entity> {
  @Override
  public Entity value() {
    return entity;
  }


  @Override
  public Text feedbackQuery(NbtElement nbtElement) {
    return Text.translatable("commands.data.entity.query", this.entity.getDisplayName(), NbtHelper.toPrettyPrintedText(nbtElement));
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
