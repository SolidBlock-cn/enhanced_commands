package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableFunction;

import java.util.UUID;

public record EntityNbtData(Entity entity) implements NbtSource.Single, NbtTarget {
  @Override
  public Text feedbackQuery(NbtElement nbtElement) {
    return Text.translatable("commands.data.entity.query", this.entity.getDisplayName(), NbtHelper.toPrettyPrintedText(nbtElement));
  }

  @Override
  public NbtCompound getNbt() {
    return NbtPredicate.entityToNbt(this.entity);
  }

  @Override
  public void setNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    UUID uUID = this.entity.getUuid();
    this.entity.readNbt(nbt);
    this.entity.setUuid(uUID);
  }

  @Override
  public void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    UUID uUID = this.entity.getUuid();
    this.entity.readNbt(getNbt());
    this.entity.setUuid(uUID);
  }

  @Override
  public Text feedbackModify() {
    return Text.translatable("commands.data.entity.modified", this.entity.getDisplayName());
  }
}
