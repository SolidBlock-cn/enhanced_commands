package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.NbtConcentrationType;

import java.util.Collection;
import java.util.UUID;

public record EntitiesNbtData(Collection<Entity> entities, NbtConcentrationType nbtConcentrationType, Random random) implements NbtTarget<Entity> {
  @Override
  public Collection<Entity> values() {
    return entities;
  }

  @Override
  public NbtCompound getNbtFor(Entity source, @NotNull RegistryWrapper.WrapperLookup registryLookup) {
    return NbtPredicate.entityToNbt(source);
  }

  @Override
  public NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) throws CommandSyntaxException {
    return nbtConcentrationType.concentrate(nbtElements, random);
  }

  @Override
  public Text feedbackQuery(NbtElement nbtElement) {
    if (entities.size() == 1) {
      return Text.translatable("commands.data.entity.query", entities.iterator().next().getDisplayName(), NbtHelper.toPrettyPrintedText(nbtElement));
    } else {
      return Text.translatable("enhanced_commands.nbt.entities.query", entities.size(), NbtHelper.toPrettyPrintedText(nbtElement)).enhanced$$();
    }
  }

  @Override
  public void setNbtFor(Entity target, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    UUID uuid = target.getUuid();
    target.readNbt(nbt);
    target.setUuid(uuid);
  }

  @Override
  public Text feedbackModify() {
    if (entities.size() == 1) {
      return Text.translatable("commands.data.entity.modified", entities.iterator().next().getDisplayName());
    } else {
      return Text.translatable("enhanced_commands.nbt.entities.modify", entities.size()).enhanced$$();
    }
  }
}
