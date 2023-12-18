package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.function.FailableFunction;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

public record EntitiesNbtData(Collection<? extends Entity> entities, NbtConcentrationType nbtConcentrationType, Random random) implements NbtSource, NbtTarget {
  @Override
  public <T> Collection<T> getNbts(Function<NbtCompound, T> mappingFunction) throws CommandSyntaxException {
    return entities.stream().map(NbtPredicate::entityToNbt).map(mappingFunction).collect(ImmutableList.toImmutableList());
  }

  @Override
  public NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) throws CommandSyntaxException {
    return nbtConcentrationType.concentrate(nbtElements, random);
  }

  @Override
  public Text feedbackQuery(NbtElement nbtElement, NbtConcentrationType nbtConcentrationType) {
    return TextUtil.enhancedTranslatable("enhanced_commands.nbt.entities.query", entities.size(), NbtHelper.toPrettyPrintedText(nbtElement));
  }

  @Override
  public void setNbt(NbtCompound nbt) throws CommandSyntaxException {
    for (Entity entity : entities) {
      UUID uuid = entity.getUuid();
      entity.readNbt(nbt);
      entity.setUuid(uuid);
    }
  }

  @Override
  public void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator) throws CommandSyntaxException {
    for (Entity entity : entities) {
      UUID uuid = entity.getUuid();
      entity.readNbt(operator.apply(NbtPredicate.entityToNbt(entity)));
      entity.setUuid(uuid);
    }
  }

  @Override
  public Text feedbackModify() {
    return TextUtil.enhancedTranslatable("enhanced_commands.nbt.entities.modify", entities.size());
  }
}
