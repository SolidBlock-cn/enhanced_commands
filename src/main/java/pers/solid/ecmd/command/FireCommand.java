package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import pers.solid.ecmd.argument.KeywordArgsArgument;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.Collections;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.argument.SimpleEnumArgument.concentrationType;
import static pers.solid.ecmd.argument.SimpleEnumArgument.getConcentrationType;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum FireCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final KeywordArgsArgument setFireKeywords = KeywordArgsArgument.builder().addOptionalArg("influenced_by_enchant", BoolArgumentType.bool(), false).build();
    final KeywordArgsArgument extinguishKeywords = KeywordArgsArgument.builder().addOptionalArg("sound", BoolArgumentType.bool(), false).build();

    dispatcher.register(literalR2("fire")
        .then(literal("get")
            .executes(context -> executeGetFire(context, Collections.singleton(context.getSource().getEntityOrException()), null))
            .then(argument("entities", EntityArgument.entities())
                .executes(context -> executeGetFire(context, EntityArgument.getEntities(context, "entities"), ConcentrationType.AVERAGE))
                .then(argument("concentration_type", concentrationType())
                    .executes(context -> executeGetFire(context, EntityArgument.getEntities(context, "entities"), getConcentrationType(context, "concentration_type"))))))
        .then(literal("set")
            .then(argument("entities", EntityArgument.entities())
                .then(argument("time", TimeArgument.time())
                    .executes(context -> executeSetFire(context, EntityArgument.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "time"), false))
                    .then(argument("keyword_args", setFireKeywords)
                        .executes(context -> executeSetFire(context, EntityArgument.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "time"), KeywordArgsArgument.getKeywordArgs(context, "keyword_args").getBoolean("influenced_by_enchant")))))))
        .then(literal("extinguish")
            .executes(context -> executeExtinguishFire(context, Collections.singleton(context.getSource().getEntityOrException()), false))
            .then(argument("entities", EntityArgument.entities())
                .executes(context -> executeExtinguishFire(context, EntityArgument.getEntities(context, "entities"), false))
                .then(argument("keyword_args", extinguishKeywords)
                    .executes(context -> executeExtinguishFire(context, EntityArgument.getEntities(context, "entities"), KeywordArgsArgument.getKeywordArgs(context, "keyword_args").getBoolean("sound")))))));
  }

  public static int executeGetFire(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, ConcentrationType concentrationType) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      final int fireTicks = entity.getRemainingFireTicks();
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.fire.get.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(fireTicks).withStyle(Styles.RESULT)), false);
      return fireTicks;
    } else {
      final IntList integers = new IntArrayList();
      for (Entity entity : entities) {
        integers.add(entity.getRemainingFireTicks());
      }
      final double result = concentrationType.concentrateInt(integers);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.fire.get.multiple", integers.size(), concentrationType.getDisplayName(), Component.literal(concentrationType.longToString(result)).withStyle(Styles.RESULT)).enhanced$$(), false);
      return (int) (result);
    }
  }

  public static int executeSetFire(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, int value, boolean influencedByFireResistance) {
    for (Entity entity : entities) {
      if (influencedByFireResistance && entity instanceof LivingEntity livingEntity) {
        livingEntity.setRemainingFireTicks(value);
      } else {
        entity.setRemainingFireTicks(value);
      }
    }
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.fire.set.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(entity.getRemainingFireTicks()).withStyle(Styles.RESULT)).enhanced$$(), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.fire.set.multiple", TextUtil.literal(entities.size()).withStyle(Styles.TARGET), TextUtil.literal(value).withStyle(Styles.TARGET)).enhanced$$(), true);
    }
    return entities.size();
  }

  public static int executeExtinguishFire(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, boolean sound) {
    for (Entity entity : entities) {
      if (sound) {
        entity.extinguishFire();
      } else {
        entity.clearFire();
      }
    }
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.fire.extinguish.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.fire.extinguish.multiple", TextUtil.literal(entities.size()).withStyle(Styles.TARGET)).enhanced$$(), true);
    }
    return entities.size();
  }
}
