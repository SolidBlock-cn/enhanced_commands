package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.predicate.entity.EntitySelectorCodec;
import pers.solid.ecmd.predicate.entity.EntitySelectors;

import java.util.*;

public record EntityNbtData(EntitySelector entitySelector) implements NbtTarget<Entity> {
  public static final MapCodec<EntityNbtData> CODEC = EntitySelectorCodec.INSTANCE.fieldOf("selector").xmap(EntityNbtData::new, EntityNbtData::entitySelector);

  public static EntityNbtData handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    ParsingUtil.expectAndSkipWhitespace(reader);
    final EntitySelector selector = parseContext.parseAndSuggestArgument(EntityArgument.entities());
    if (reader.canRead()) {
      parseContext.clearSuggestion();
    }
    return new EntityNbtData(selector);
  }

  @Override
  public Collection<Entity> values(CommandSourceStack source) throws CommandSyntaxException {
    final List<? extends Entity> entities = entitySelector.findEntities(source);
    return Collections.unmodifiableList(entities);
  }

  @Override
  public CompoundTag getNbtFor(CommandSourceStack commandSource, Entity source) {
    return NbtPredicate.getEntityTagToCompare(source);
  }

  @Override
  public Type getType() {
    return Type.ENTITY;
  }

  @Override
  public int executeQuery(CommandSourceStack source, NbtPathArgument.@Nullable NbtPath path, double scale, NbtConcentrationType nbtConcentrationType, RandomSource random) throws CommandSyntaxException {
    final Collection<Entity> entities = values(source);
    final Map<Entity, Tag> nbts = getNbtsInPath(source, path);
    if (nbts.size() == 1 && nbtConcentrationType != NbtConcentrationType.LIST) {
      final var soleEntry = nbts.entrySet().iterator().next();
      final Entity entity = soleEntry.getKey();
      final Tag nbt = soleEntry.getValue();
      if (path == null) {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.entity.query", entity.getDisplayName(), NbtUtils.toPrettyComponent(nbt)), false);
        return NbtSource.toInt(nbt);
      }
      if (scale == 1) {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.entity.query_path", entity.getDisplayName(), path.toString(), NbtUtils.toPrettyComponent(nbt)), false);
        return NbtSource.toInt(nbt);
      } else {
        final double scaledValue = NbtSource.scaleNbt(nbt, scale, path);
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.entity.query_scale", entity.getDisplayName(), path.toString(), scale, NbtUtils.toPrettyComponent(nbt)), false);
        return Mth.floor(scaledValue);
      }
    }
    final Object2DoubleMap<Entity> scaledNbts;
    if (scale != 1 && path != null) {
      scaledNbts = new Object2DoubleOpenHashMap<>();
      for (Map.Entry<Entity, Tag> entry : nbts.entrySet()) {
        scaledNbts.put(entry.getKey(), NbtSource.scaleNbt(entry.getValue(), scale, path));
      }
    } else {
      scaledNbts = null;
    }

    if (nbtConcentrationType == NbtConcentrationType.ALL) {
      source.sendFeedback$ecBridge(() -> {
        List<Component> texts = new ArrayList<>();
        texts.add(Component.translatable("enhanced_commands.commands.nbt.entities.query.header", Math.min(nbts.size(), QUERY_LIMIT)).enhanced$$().withStyle(ChatFormatting.AQUA));
        for (var entry : Iterables.limit(nbts.entrySet(), QUERY_LIMIT)) {
          final Entity entity = entry.getKey();
          if (path == null) {
            texts.add(Component.literal(" - ").append(Component.translatable("enhanced_commands.commands.nbt.entity.query", entity.getDisplayName(), NbtUtils.toPrettyComponent(entry.getValue()))));
          } else if (scale == 1) {
            texts.add(Component.literal(" - ").append(Component.translatable("enhanced_commands.commands.nbt.entity.query_path", entity.getDisplayName(), path.toString(), NbtUtils.toPrettyComponent(entry.getValue()))));
          } else {
            texts.add(Component.literal(" - ").append(Component.translatable("enhanced_commands.commands.nbt.entity.query_scale", entity.getDisplayName(), path.toString(), scale, scaledNbts.getOrDefault(entity, 0))));
          }
        }
        if (nbts.size() > QUERY_LIMIT) {
          texts.add(Component.translatable("enhanced_commands.commands.nbt.query_limit_notice", QUERY_LIMIT).withStyle(ChatFormatting.YELLOW));
        }
        return CommonComponents.joinLines(texts);
      }, false);
      return nbts.size();
    }

    final Tag concentratedNbts = nbtConcentrationType.concentrate(nbts.values(), random);
    if (path == null) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.entities.query", entities.size(), NbtUtils.toPrettyComponent(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else if (scale == 1) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.entities.query_path", entities.size(), path.toString(), NbtUtils.toPrettyComponent(concentratedNbts)).enhanced$$(), false);
      return NbtSource.toInt(concentratedNbts);
    } else {
      final double scaledConcentratedNbt = NbtSource.scaleNbt(concentratedNbts, scale, path);
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.entities.query_scale", entities.size(), path.toString(), scale, scaledConcentratedNbt).enhanced$$(), false);
      return Mth.floor(scaledConcentratedNbt);
    }
  }

  @Override
  public void setNbtFor(CommandSourceStack commandSource, Entity target, CompoundTag nbt) throws CommandSyntaxException {
    UUID uuid = target.getUUID();
    target.load(nbt);
    target.setUUID(uuid);
  }

  @Override
  public Component feedbackModify(Collection<Entity> values) {
    if (values.size() == 1) {
      return Component.translatable("commands.data.entity.modified", values.iterator().next().getDisplayName());
    } else {
      return Component.translatable("enhanced_commands.commands.nbt.entities.modify", values.size()).enhanced$$();
    }
  }

  @Override
  public @NotNull String asString() {
    return "entity " + EntitySelectors.express(entitySelector);
  }
}
