package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;

import java.util.*;

public record EntitiesNbtData(EntitySelector entitySelector) implements NbtTarget<Entity> {
  // todo: consider using an entity selector as a record component, instead of the actual collection of entities

  @Override
  public Collection<Entity> values(ServerCommandSource source) throws CommandSyntaxException {
    final List<? extends Entity> entities = entitySelector.getEntities(source);
    return Collections.unmodifiableList(entities);
  }

  @Override
  public NbtCompound getNbtFor(ServerCommandSource commandSource, Entity source) {
    return NbtPredicate.entityToNbt(source);
  }

  @Override
  public int executeQuery(ServerCommandSource source, NbtPathArgumentType.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, Random random) throws CommandSyntaxException {
    final Collection<Entity> entities = values(source);
    final Map<Entity, NbtElement> nbts = getNbtsInPath(source, path);
    if (nbts.size() == 1 && nbtConcentrationType != NbtConcentrationType.LIST) {
      final var soleEntry = nbts.entrySet().iterator().next();
      final Entity entity = soleEntry.getKey();
      final NbtElement nbt = soleEntry.getValue();
      if (path == null) {
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.entity.query", entity.getDisplayName(), NbtHelper.toPrettyPrintedText(nbt)), false);
        return NbtSource.toInt(nbt);
      }
      if (scale == 1) {
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.entity.query_path", entity.getDisplayName(), path.toString(), NbtHelper.toPrettyPrintedText(nbt)), false);
        return NbtSource.toInt(nbt);
      } else {
        final double scaledValue = NbtSource.scaleNbt(nbt, scale, path);
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.entity.query_scale", entity.getDisplayName(), path.toString(), scale, NbtHelper.toPrettyPrintedText(nbt)), false);
        return MathHelper.floor(scaledValue);
      }
    }
    final Object2DoubleMap<Entity> scaledNbts;
    if (scale != 1 && path != null) {
      scaledNbts = new Object2DoubleOpenHashMap<>();
      for (Map.Entry<Entity, NbtElement> entry : nbts.entrySet()) {
        scaledNbts.put(entry.getKey(), NbtSource.scaleNbt(entry.getValue(), scale, path));
      }
    } else {
      scaledNbts = null;
    }

    if (nbtConcentrationType == NbtConcentrationType.ALL) {
      source.sendFeedback$ecBridge(() -> {
        List<Text> texts = new ArrayList<>();
        texts.add(Text.translatable("enhanced_commands.commands.nbt.entities.query.header", Math.min(nbts.size(), QUERY_LIMIT)).enhanced$$().formatted(Formatting.AQUA));
        for (var entry : Iterables.limit(nbts.entrySet(), QUERY_LIMIT)) {
          final Entity entity = entry.getKey();
          if (path == null) {
            texts.add(Text.literal(" - ").append(Text.translatable("enhanced_commands.commands.nbt.entity.query", entity.getDisplayName(), NbtHelper.toPrettyPrintedText(entry.getValue()))));
          } else if (scale == 1) {
            texts.add(Text.literal(" - ").append(Text.translatable("enhanced_commands.commands.nbt.entity.query_path", entity.getDisplayName(), path.toString(), NbtHelper.toPrettyPrintedText(entry.getValue()))));
          } else {
            texts.add(Text.literal(" - ").append(Text.translatable("enhanced_commands.commands.nbt.entity.query_scale", entity.getDisplayName(), path.toString(), scale, scaledNbts.getOrDefault(entity, 0))));
          }
        }
        if (nbts.size() > QUERY_LIMIT) {
          texts.add(Text.translatable("enhanced_commands.commands.nbt.query_limit_notice", QUERY_LIMIT).formatted(Formatting.YELLOW));
        }
        return ScreenTexts.joinLines(texts);
      }, false);
      return nbts.size();
    }

    final NbtElement concentratedNbts = nbtConcentrationType.concentrate(nbts.values(), random);
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.entities.query", entities.size(), NbtHelper.toPrettyPrintedText(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.entities.query_path", entities.size(), path.toString(), NbtHelper.toPrettyPrintedText(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else {
      final double scaledConcentratedNbt = NbtSource.scaleNbt(concentratedNbts, scale, path);
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.entities.query_scale", entities.size(), path.toString(), scale, scaledConcentratedNbt).enhanced$$(), false);
      return MathHelper.floor(scaledConcentratedNbt);
    }
  }

  @Override
  public void setNbtFor(ServerCommandSource commandSource, Entity target, NbtCompound nbt) throws CommandSyntaxException {
    UUID uuid = target.getUuid();
    target.readNbt(nbt);
    target.setUuid(uuid);
  }

  @Override
  public Text feedbackModify(Collection<Entity> values) {
    if (values.size() == 1) {
      return Text.translatable("commands.data.entity.modified", values.iterator().next().getDisplayName());
    } else {
      return Text.translatable("enhanced_commands.commands.nbt.entities.modify", values.size()).enhanced$$();
    }
  }
}
