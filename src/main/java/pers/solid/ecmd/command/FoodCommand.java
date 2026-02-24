package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.Collections;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;
import static net.minecraft.commands.arguments.SlotArgument.getSlot;
import static net.minecraft.commands.arguments.SlotArgument.slot;
import static net.minecraft.commands.arguments.item.ItemArgument.getItem;
import static net.minecraft.commands.arguments.item.ItemArgument.item;
import static pers.solid.ecmd.argument.SimpleEnumArgument.concentrationType;
import static pers.solid.ecmd.argument.SimpleEnumArgument.getConcentrationType;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum FoodCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final DynamicCommandExceptionType ADD_FROM_NOT_FOOD = new DynamicCommandExceptionType(stackName -> Component.translatable("enhanced_commands.commands.food.add_from.not_food", stackName));
  public static final Dynamic2CommandExceptionType ADD_FROM_HAND_NOT_FOOD = new Dynamic2CommandExceptionType((playerName, stackName) -> Component.translatable("enhanced_commands.commands.food.add_from_hand.not_food", playerName, stackName));
  public static final DynamicCommandExceptionType ADD_FROM_HAND_NONE_FOOD = new DynamicCommandExceptionType(playersSize -> Component.translatable(("enhanced_commands.commands.food.add_from_hand.none_food"), playersSize).enhanced$$());
  public static final DynamicCommandExceptionType ADD_FROM_NONE_FOOD = new DynamicCommandExceptionType(playersSize -> Component.translatable(("enhanced_commands.commands.food.add_from.none_food"), playersSize).enhanced$$());

  public static int executeGetAll(CommandContext<CommandSourceStack> context, Collection<? extends Player> players, ConcentrationType concentrationType) throws CommandSyntaxException {
    final int size = players.size();
    if (size == 1) {
      final Player player = players.iterator().next();
      final FoodData hungerManager = player.getFoodData();
      final int foodLevel = hungerManager.getFoodLevel();
      final float saturationLevel = hungerManager.getSaturationLevel();
      final float exhaustion = hungerManager.getExhaustionLevel();
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.get.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET), TextUtil.literal(foodLevel).withStyle(Styles.RESULT), TextUtil.literal(saturationLevel).withStyle(Styles.RESULT), TextUtil.literal(exhaustion).withStyle(Styles.RESULT)), false);
    } else {
      final IntList foodLevels = new IntArrayList(size);
      final FloatList saturationLevels = new FloatArrayList(size);
      final FloatList exhaustionLevels = new FloatArrayList(size);
      final double food = concentrationType.concentrateInt(foodLevels);
      final double saturation = concentrationType.concentrateFloat(saturationLevels);
      final double exhaustion = concentrationType.concentrateFloat(exhaustionLevels);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.get.multiple",
          TextUtil.literal(size).withStyle(Styles.TARGET),
          concentrationType.getDisplayName(),
          Component.literal(concentrationType.longToString(food)).withStyle(Styles.RESULT),
          Component.literal(concentrationType.floatToString(saturation)).withStyle(Styles.RESULT),
          Component.literal(concentrationType.floatToString(exhaustion)).withStyle(Styles.RESULT)
      ), false);
    }
    return size;
  }

  public static int executeSetFood(CommandContext<CommandSourceStack> context, Collection<? extends Player> players, int value) {
    final int size = players.size();
    for (Player player : players) {
      player.getFoodData().setFoodLevel(value);
    }
    if (size == 1) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.set_food.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT)), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.set_food.multiple", TextUtil.literal(size).withStyle(Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeSetFoodAndSaturation(CommandContext<CommandSourceStack> context, Collection<? extends Player> players, int food, float saturation) {
    final int size = players.size();
    for (Player player : players) {
      final FoodData hungerManager = player.getFoodData();
      hungerManager.setFoodLevel(food);
      hungerManager.setSaturation(food);
    }
    if (size == 1) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.set_food_and_saturation.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(food).withStyle(Styles.RESULT), TextUtil.literal(saturation).withStyle(Styles.RESULT)), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.set_food_and_saturation.multiple", TextUtil.literal(size).withStyle(Styles.TARGET), TextUtil.literal(food).withStyle(Styles.RESULT), TextUtil.literal(saturation).withStyle(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeSetSaturation(CommandContext<CommandSourceStack> context, Collection<? extends Player> players, float value) {
    final int size = players.size();
    for (Player player : players) {
      player.getFoodData().setSaturation(value);
    }
    if (size == 1) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.set_saturation.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT)), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.set_saturation.multiple", TextUtil.literal(size).withStyle(Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeSetExhaustion(CommandContext<CommandSourceStack> context, Collection<? extends Player> players, float value) {
    final int size = players.size();
    for (Player player : players) {
      player.getFoodData().setExhaustion(value);
    }
    if (size == 1) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.set_exhaustion.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT)), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.set_exhaustion.multiple", TextUtil.literal(size).withStyle(Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeAddToMax(CommandContext<CommandSourceStack> context, Collection<? extends Player> players) {
    final int size = players.size();
    for (Player player : players) {
      final FoodData hungerManager = player.getFoodData();
      hungerManager.setFoodLevel(20);
      hungerManager.setSaturation(20);
      hungerManager.setExhaustion(0);
    }
    if (size == 1) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add_to_max.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET)), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add_to_max.multiple", TextUtil.literal(size).withStyle(Styles.TARGET)), true);
    }
    return size;
  }

  public static int executeAdd(CommandContext<CommandSourceStack> context, Collection<? extends Player> players, int food, float saturationModifier) {
    final int size = players.size();
    for (Player player : players) {
      player.getFoodData().eat(food, saturationModifier);
    }
    if (size == 1) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.literal(food).withStyle(Styles.RESULT), TextUtil.literal(saturationModifier).withStyle(Styles.RESULT)), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add.multiple", TextUtil.literal(size).withStyle(Styles.TARGET), TextUtil.literal(food).withStyle(Styles.RESULT), TextUtil.literal(saturationModifier).withStyle(Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeAddFromFood(CommandContext<CommandSourceStack> context, Collection<? extends Player> players, ItemStack stack) throws CommandSyntaxException {
    final int size = players.size();
    for (Player player : players) {
      player.getFoodData().eat(stack.get(DataComponents.FOOD));
    }
    if (!stack.has(DataComponents.FOOD)) {
      throw ADD_FROM_NOT_FOOD.create(stack.getHoverName());
    }
    if (size == 1) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add_food.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), TextUtil.styled(stack.getHoverName(), Styles.RESULT)), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add_food.multiple", TextUtil.literal(size).withStyle(Styles.TARGET), TextUtil.styled(stack.getHoverName(), Styles.RESULT)), true);
    }
    return size;
  }

  public static int executeAddFromSlot(CommandContext<CommandSourceStack> context, Collection<? extends Player> players, int slot) throws CommandSyntaxException {
    final int size = players.size();
    if (size == 1) {
      final Player player = players.iterator().next();
      final ItemStack stack = slot == -1 ? player.getMainHandItem() : player.getInventory().getItem(slot);
      player.getFoodData().eat(stack.get(DataComponents.FOOD));
      if (!stack.has(DataComponents.FOOD)) {
        if (slot == -1) {
          throw ADD_FROM_HAND_NOT_FOOD.create(player.getDisplayName(), stack.getHoverName());
        } else {
          throw ADD_FROM_NOT_FOOD.create(stack.getHoverName());
        }
      }
      if (slot == -1) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add_from_hand.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET), TextUtil.styled(stack.getHoverName(), Styles.RESULT)), true);
      } else {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add_from.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET), TextUtil.styled(stack.getHoverName(), Styles.RESULT)), true);
      }
      return 1;
    } else {
      int successes = 0;
      for (Player player : players) {
        final ItemStack stack = slot == -1 ? player.getMainHandItem() : player.getInventory().getItem(slot);
        player.getFoodData().eat(stack.get(DataComponents.FOOD));
        if (stack.has(DataComponents.FOOD)) successes++;
      }
      if (successes == 0) {
        if (slot == -1) {
          throw ADD_FROM_HAND_NONE_FOOD.create(players.size());
        } else {
          throw ADD_FROM_NONE_FOOD.create(players.size());
        }
      }
      int finalSuccesses = successes;
      if (slot == -1) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add_from_hand.multiple", TextUtil.literal(finalSuccesses).withStyle(Styles.TARGET)), true);
      } else {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.food.add_from.multiple", TextUtil.literal(finalSuccesses).withStyle(Styles.TARGET)), true);
      }
      return successes;
    }
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(literalR2("food")
        .executes(context -> executeGetAll(context, Collections.singleton(context.getSource().getPlayerOrException()), null))
        .then(literal("get")
            .executes(context -> executeGetAll(context, Collections.singleton(context.getSource().getPlayerOrException()), null))
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
            .executes(context -> executeAddToMax(context, Collections.singleton(context.getSource().getPlayerOrException())))
            .then(argument("players", players())
                .executes(context -> executeAddToMax(context, getPlayers(context, "players")))
                .then(argument("food", integer())
                    .executes(context -> executeAdd(context, getPlayers(context, "players"), getInteger(context, "food"), 0))
                    .then(argument("saturation_modifier", floatArg())
                        .executes(context -> executeAdd(context, getPlayers(context, "players"), getInteger(context, "food"), getFloat(context, "saturation_modifier")))))
                .then(literal("item")
                    .executes(context -> executeAddFromSlot(context, getPlayers(context, "players"), -1))
                    .then(argument("item", item(commandBuildContext))
                        .executes(context -> executeAddFromFood(context, getPlayers(context, "players"), getItem(context, "item").createItemStack(1, false)))))
                .then(literal("from")
                    .executes(context -> executeAddFromSlot(context, getPlayers(context, "players"), -1))
                    .then(argument("slot", slot())
                        .executes(context -> executeAddFromSlot(context, getPlayers(context, "players"), getSlot(context, "slot")))))))
        .then(literal("tick")
            .executes(context -> executeTick(context, Collections.singleton(context.getSource().getPlayerOrException()), 1))
            .then(argument("players", players())
                .executes(context -> executeTick(context, getPlayers(context, "players"), 1))
                .then(argument("times", integer(0, 32767))
                    .executes(context -> executeTick(context, getPlayers(context, "players"), getInteger(context, "times")))))));
  }

  private int executeTick(CommandContext<CommandSourceStack> context, Collection<? extends ServerPlayer> players, int times) {
    int updated = 0;
    for (ServerPlayer player : players) {
      for (int i = 0; i < times; i++) {
        player.getFoodData().tick(player);
        updated++;
      }
    }
    context.getSource().sendFeedback$ecBridge(() -> {
      if (players.size() == 1) {
        return Component.translatable("enhanced_commands.commands.food.tick.single", TextUtil.styled(players.iterator().next().getDisplayName(), Styles.TARGET), times).enhanced$$();
      } else {
        return Component.translatable("enhanced_commands.commands.food.tick.single", TextUtil.literal(players.size()).withStyle(Styles.TARGET), times).enhanced$$();
      }
    }, true);
    return updated;
  }
}
