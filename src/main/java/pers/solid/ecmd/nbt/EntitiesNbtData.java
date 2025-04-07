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
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Collection;
import java.util.UUID;

public record EntitiesNbtData(Collection<? extends Entity> entities, NbtConcentrationType nbtConcentrationType, Random random) implements NbtSource, NbtTarget {
  @Override
  public <T> Collection<T> getNbts(FailableFunction<NbtCompound, T, CommandSyntaxException> mappingFunction, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException {
    return IterateUtils.transformFailableImmutableList(entities, entity -> mappingFunction.apply(NbtPredicate.entityToNbt(entity)));
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
  public void setNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    for (Entity entity : entities) {
      UUID uuid = entity.getUuid();
      entity.readNbt(nbt);
      entity.setUuid(uuid);
    }
  }

  @Override
  public void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    for (Entity entity : entities) {
      UUID uuid = entity.getUuid();
      entity.readNbt(operator.apply(NbtPredicate.entityToNbt(entity)));
      entity.setUuid(uuid);
    }
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
