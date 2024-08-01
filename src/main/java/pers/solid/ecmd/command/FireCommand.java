package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.argument.ConcentrationTypeArgumentType;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.Collections;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum FireCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType setFireKeywords = KeywordArgsArgumentType.builder().addOptionalArg("influenced_by_enchant", BoolArgumentType.bool(), false).build();
    final KeywordArgsArgumentType extinguishKeywords = KeywordArgsArgumentType.builder().addOptionalArg("sound", BoolArgumentType.bool(), false).build();

    dispatcher.register(literalR2("fire")
        .then(literal("get")
            .executes(context -> executeGetFire(context, Collections.singleton(context.getSource().getEntityOrThrow()), null))
            .then(argument("entities", EntityArgumentType.entities())
                .executes(context -> executeGetFire(context, EntityArgumentType.getEntities(context, "entities"), ConcentrationType.AVERAGE))
                .then(argument("concentration_type", ConcentrationTypeArgumentType.concentrationType())
                    .executes(context -> executeGetFire(context, EntityArgumentType.getEntities(context, "entities"), ConcentrationTypeArgumentType.getConcentrationType(context, "concentration_type"))))))
        .then(literal("set")
            .then(argument("entities", EntityArgumentType.entities())
                .then(argument("time", TimeArgumentType.time())
                    .executes(context -> executeSetFire(context, EntityArgumentType.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "time"), false))
                    .then(argument("keyword_args", setFireKeywords)
                        .executes(context -> executeSetFire(context, EntityArgumentType.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "time"), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args").getBoolean("influenced_by_enchant")))))))
        .then(literal("extinguish")
            .executes(context -> executeExtinguishFire(context, Collections.singleton(context.getSource().getEntityOrThrow()), false))
            .then(argument("entities", EntityArgumentType.entities())
                .executes(context -> executeExtinguishFire(context, EntityArgumentType.getEntities(context, "entities"), false))
                .then(argument("keyword_args", extinguishKeywords)
                    .executes(context -> executeExtinguishFire(context, EntityArgumentType.getEntities(context, "entities"), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args").getBoolean("sound")))))));
  }

  public static int executeGetFire(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, ConcentrationType concentrationType) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      final int fireTicks = entity.getFireTicks();
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.fire.get.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(fireTicks).styled(Styles.RESULT)), false);
      return fireTicks;
    } else {
      final IntList integers = new IntArrayList();
      for (Entity entity : entities) {
        integers.add(entity.getFireTicks());
      }
      final double result = concentrationType.concentrateInt(integers);
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.fire.get.multiple", integers.size(), concentrationType.getDisplayName(), Text.literal(concentrationType.longToString(result)).styled(Styles.RESULT)).enhanced$$(), false);
      return (int) (result);
    }
  }

  public static int executeSetFire(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, int value, boolean influencedByFireResistance) {
    for (Entity entity : entities) {
      if (influencedByFireResistance && entity instanceof LivingEntity livingEntity) {
        livingEntity.setFireTicks(value);
      } else {
        entity.setFireTicks(value);
      }
    }
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.fire.set.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(entity.getFireTicks()).styled(Styles.RESULT)).enhanced$$(), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.fire.set.multiple", TextUtil.literal(entities.size()).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.TARGET)).enhanced$$(), true);
    }
    return entities.size();
  }

  public static int executeExtinguishFire(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, boolean sound) {
    for (Entity entity : entities) {
      if (sound) {
        entity.extinguishWithSound();
      } else {
        entity.extinguish();
      }
    }
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.fire.extinguish.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.fire.extinguish.multiple", TextUtil.literal(entities.size()).styled(Styles.TARGET)).enhanced$$(), true);
    }
    return entities.size();
  }
}
