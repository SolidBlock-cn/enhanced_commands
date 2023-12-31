package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.Collection;
import java.util.Collections;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.command.argument.EntityArgumentType.getPlayers;
import static net.minecraft.command.argument.EntityArgumentType.players;
import static net.minecraft.command.argument.ItemSlotArgumentType.getItemSlot;
import static net.minecraft.command.argument.ItemSlotArgumentType.itemSlot;
import static net.minecraft.command.argument.ItemStackArgumentType.getItemStackArgument;
import static net.minecraft.command.argument.ItemStackArgumentType.itemStack;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.ConcentrationTypeArgumentType.concentrationType;
import static pers.solid.ecmd.argument.ConcentrationTypeArgumentType.getConcentrationType;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum FoodCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literalR2("food")
        .executes(context -> executeGetAll(context, Collections.singleton(context.getSource().getPlayerOrThrow()), null))
        .then(literal("get")
            .executes(context -> executeGetAll(context, Collections.singleton(context.getSource().getPlayerOrThrow()), null))
            .then(argument("players", players())
                .executes(context -> executeGetAll(context, getPlayers(context, "players"), ConcentrationType.AVERAGE))
                .then(argument("concentration_type", concentrationType())
                    .executes(context -> executeGetAll(context, getPlayers(context, "players"), getConcentrationType(context, "concentration_type"))))))
        .then(literal("setfood")
            .then(argument("players", players())
                .then(argument("food", integer())
                    .executes(context -> executeSetFood(context, getPlayers(context, "players"), getInteger(context, "food")))
                    .then(argument("saturation", floatArg())
                        .executes(context -> executeSetFoodAndSaturation(context, getPlayers(context, "players"), getInteger(context, "food"), getFloat(context, "saturation")))))))
        .then(literal("setsaturation")
            .then(argument("players", players())
                .then(argument("saturation", floatArg())
                    .executes(context -> executeSetSaturation(context, getPlayers(context, "players"), getFloat(context, "saturation"))))))
        .then(literal("setexhaustion")
            .then(argument("players", players())
                .then(argument("exhaustion", floatArg())
                    .executes(context -> executeSetExhaustion(context, getPlayers(context, "players"), getFloat(context, "exhaustion"))))))
        .then(literal("add")
            .executes(context -> executeAddToMax(context, Collections.singleton(context.getSource().getPlayerOrThrow())))
            .then(argument("players", players())
                .executes(context -> executeAddToMax(context, getPlayers(context, "players")))
                .then(argument("food", integer())
                    .executes(context -> executeAdd(context, getPlayers(context, "players"), getInteger(context, "food"), 0))
                    .then(argument("saturation_modifier", floatArg())
                        .executes(context -> executeAdd(context, getPlayers(context, "players"), getInteger(context, "food"), getFloat(context, "saturation_modifier")))))
                .then(literal("item")
                    .executes(context -> executeAddFromSlot(context, getPlayers(context, "players"), -1))
                    .then(argument("item", itemStack(registryAccess))
                        .executes(context -> executeAddFromFood(context, getPlayers(context, "players"), getItemStackArgument(context, "item").createStack(1, false)))))
                .then(literal("from")
                    .executes(context -> executeAddFromSlot(context, getPlayers(context, "players"), -1))
                    .then(argument("slot", itemSlot())
                        .executes(context -> executeAddFromSlot(context, getPlayers(context, "players"), getItemSlot(context, "slot")))))))
        .then(literal("tick")
            .executes(context -> executeTick(context, Collections.singleton(context.getSource().getPlayerOrThrow()), 1))
            .then(argument("players", players())
                .executes(context -> executeTick(context, getPlayers(context, "players"), 1))
                .then(argument("times", integer(0, 32767))
                    .executes(context -> executeTick(context, getPlayers(context, "players"), getInteger(context, "times")))))));
  }

  public static int executeGetAll(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players, ConcentrationType concentrationType) {
    final int size = players.size();
    if (size == 1) {
      final PlayerEntity player = players.iterator().next();
      final HungerManager hungerManager = player.getHungerManager();
      final int foodLevel = hungerManager.getFoodLevel();
      final float saturationLevel = hungerManager.getSaturationLevel();
      final float exhaustion = hungerManager.getExhaustion();
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.get.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET), TextUtil.literal(foodLevel).styled(Styles.RESULT), TextUtil.literal(saturationLevel).styled(Styles.RESULT), TextUtil.literal(exhaustion).styled(Styles.RESULT)), false);
    } else {
      final IntList foodLevels = new IntArrayList(size);
      final FloatList saturationLevels = new FloatArrayList(size);
      final FloatList exhaustionLevels = new FloatArrayList(size);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.get.multiple",
          TextUtil.literal(size).styled(Styles.TARGET),
          concentrationType.getDisplayName(),
          Text.literal(concentrationType.longToString(concentrationType.concentrateInt(foodLevels))).styled(Styles.RESULT),
          Text.literal(concentrationType.floatToString(concentrationType.concentrateFloat(saturationLevels))).styled(Styles.RESULT),
          Text.literal(concentrationType.floatToString(concentrationType.concentrateFloat(exhaustionLevels))).styled(Styles.RESULT)
      ), false);
    }
    return size;
  }

  public static int executeSetFood(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players, int value) {
    final int size = players.size();
    for (PlayerEntity player : players) {
      player.getHungerManager().setFoodLevel(value);
    }
    if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.set_food.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT)), true);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.set_food.multiple", TextUtil.literal(size).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeSetFoodAndSaturation(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players, int food, float saturation) {
    final int size = players.size();
    for (PlayerEntity player : players) {
      final HungerManager hungerManager = player.getHungerManager();
      hungerManager.setFoodLevel(food);
      hungerManager.setSaturationLevel(food);
    }
    if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.set_food_and_saturation.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(food).styled(Styles.RESULT), TextUtil.literal(saturation).styled(Styles.RESULT)), true);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.set_food_and_saturation.multiple", TextUtil.literal(size).styled(Styles.TARGET), TextUtil.literal(food).styled(Styles.RESULT), TextUtil.literal(saturation).styled(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeSetSaturation(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players, float value) {
    final int size = players.size();
    for (PlayerEntity player : players) {
      player.getHungerManager().setSaturationLevel(value);
    }
    if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.set_saturation.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT)), true);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.set_saturation.multiple", TextUtil.literal(size).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeSetExhaustion(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players, float value) {
    final int size = players.size();
    for (PlayerEntity player : players) {
      player.getHungerManager().setExhaustion(value);
    }
    if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.set_exhaustion.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT)), true);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.set_exhaustion.multiple", TextUtil.literal(size).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeAddToMax(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players) {
    final int size = players.size();
    for (PlayerEntity player : players) {
      final HungerManager hungerManager = player.getHungerManager();
      hungerManager.setFoodLevel(20);
      hungerManager.setSaturationLevel(20);
      hungerManager.setExhaustion(0);
    }
    if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add_to_max.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET)), true);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add_to_max.multiple", TextUtil.literal(size).styled(Styles.TARGET)), true);
    }
    return size;
  }

  public static int executeAdd(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players, int food, float saturationModifier) {
    final int size = players.size();
    for (PlayerEntity player : players) {
      player.getHungerManager().add(food, saturationModifier);
    }
    if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(food).styled(Styles.RESULT), TextUtil.literal(saturationModifier).styled(Styles.RESULT)), true);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add.multiple", TextUtil.literal(size).styled(Styles.TARGET), TextUtil.literal(food).styled(Styles.RESULT), TextUtil.literal(saturationModifier).styled(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeAddFromFood(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players, ItemStack stack) {
    final int size = players.size();
    for (PlayerEntity player : players) {
      player.getHungerManager().eat(stack.getItem(), stack);
    }
    if (!stack.isFood()) {
      throw new CommandException(Text.translatable("enhanced_commands.commands.food.add_from.not_food", stack.getName()));
    }
    if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add_food.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.styled(stack.getName(), Styles.RESULT)), true);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add_food.multiple", TextUtil.literal(size).styled(Styles.TARGET), TextUtil.styled(stack.getName(), Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeAddFromSlot(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players, int slot) {
    final int size = players.size();
    if (size == 1) {
      final PlayerEntity player = players.iterator().next();
      final ItemStack stack = slot == -1 ? player.getMainHandStack() : player.getInventory().getStack(slot);
      player.getHungerManager().eat(stack.getItem(), stack);
      if (!stack.isFood()) {
        if (slot == -1) {
          throw new CommandException(Text.translatable("enhanced_commands.commands.food.add_from_hand.not_food", player, stack.getName()));
        } else {
          throw new CommandException(Text.translatable("enhanced_commands.commands.food.add_from.not_food", stack.getName()));
        }
      }
      if (slot == -1) {
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add_from_hand.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET), TextUtil.styled(stack.getName(), Styles.RESULT)), true);
      } else {
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add_from.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET), TextUtil.styled(stack.getName(), Styles.RESULT)), true);
      }
      return 1;
    } else {
      int successes = 0;
      for (PlayerEntity player : players) {
        final ItemStack stack = slot == -1 ? player.getMainHandStack() : player.getInventory().getStack(slot);
        player.getHungerManager().eat(stack.getItem(), stack);
        if (stack.isFood()) successes++;
      }
      if (successes == 0) {
        if (slot == -1) {
          throw new CommandException(TextUtil.enhancedTranslatable("enhanced_commands.commands.food.add_from_hand.none_food", players.size()));
        } else {
          throw new CommandException(TextUtil.enhancedTranslatable("enhanced_commands.commands.food.add_from.none_food", players.size()));
        }
      }
      int finalSuccesses = successes;
      if (slot == -1) {
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add_from_hand.multiple", TextUtil.literal(finalSuccesses).styled(Styles.TARGET)), true);
      } else {
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.food.add_from.multiple", TextUtil.literal(finalSuccesses).styled(Styles.TARGET)), true);
      }
      return successes;
    }
  }

  private int executeTick(CommandContext<ServerCommandSource> context, Collection<? extends PlayerEntity> players, int times) {
    int updated = 0;
    for (PlayerEntity player : players) {
      for (int i = 0; i < times; i++) {
        player.getHungerManager().update(player);
        updated++;
      }
    }
    CommandBridge.sendFeedback(context, () -> {
      if (players.size() == 1) {
        return TextUtil.enhancedTranslatable("enhanced_commands.commands.food.tick.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), times);
      } else {
        return TextUtil.enhancedTranslatable("enhanced_commands.commands.food.tick.single", TextUtil.literal(players.size()).styled(Styles.TARGET), times);
      }
    }, true);
    return updated;
  }
}
