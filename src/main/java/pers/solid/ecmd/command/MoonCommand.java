package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import pers.solid.ecmd.argument.SimpleEnumArgumentTypes;
import pers.solid.ecmd.util.MoonPhase;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum MoonCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final LiteralCommandNode<ServerCommandSource> moonNode = dispatcher.register(literal("moon")
        .executes(MoonCommand::executeGetPhase)
        .then(literal("get")
            .executes(MoonCommand::executeGetPhase))
        .then(literal("set")
            .then(argument("moon_phase", new SimpleEnumArgumentTypes.MoonPhaseArgumentType())
                .executes(MoonCommand::executeSetPhase)))
        .then(literal("rotate")
            .executes(context -> executeRotatePhase(context, 1))
            .then(argument("steps", IntegerArgumentType.integer())
                .executes(context -> executeRotatePhase(context, IntegerArgumentType.getInteger(context, "steps")))))/*
        .then(literal("if")
            .then(literal("phase"))
            .then(literal("completeness")))
        .then(literal("unless")
            .then(literal("phase"))
            .then(literal("completeness")))*/);

    dispatcher.register(literal("jadeplate").redirect(moonNode));
  }

  private static int executeGetPhase(CommandContext<ServerCommandSource> context) {
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    final int moonPhase = world.getMoonPhase();
    final MoonPhase moonPhaseValue = MoonPhase.byNumericId(moonPhase);
    source.sendFeedback$ecBridge(() -> {
      final long dayTime = world.getTimeOfDay() % 24000L;
      final Random random = world.getRandom();
      final String type = switch (moonPhaseValue) {
        case FULL_MOON -> "full";
        case NEW_MOON -> "new";
        case WANING_GIBBOUS, WAXING_GIBBOUS -> "gibbous";
        case WANING_CRESCENT, WAXING_CRESCENT -> "crescent";
        case FIRST_QUARTER, THIRD_QUARTER -> "quarter";
      };
      if (13800L < dayTime && dayTime < 22200L) {
        // 夜间
        if (moonPhaseValue == MoonPhase.FULL_MOON) {
          final ServerPlayerEntity player = source.getPlayer();
          final int specialRand = random.nextInt(15);
          // 满月期间的特殊返回
          if (!world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE) && specialRand == 0) {
            return pattern(random, moonPhaseValue, "enhanced_commands.commands.moon.phase.get.full.special.fixed.", 5);
          } else if (player != null && player.getHungerManager().getFoodLevel() < 8 && specialRand == 1) {
            return pattern(random, moonPhaseValue, "enhanced_commands.commands.moon.phase.get.full.special.hungry.", 5);
          } else if (player != null && player.hasStatusEffect(StatusEffects.JUMP_BOOST) && specialRand == 2) {
            return pattern(random, moonPhaseValue, "enhanced_commands.commands.moon.phase.get.full.special.jump_boost.", 5);
          } else if (player != null && player.getVehicle() instanceof AbstractMinecartEntity && specialRand == 3) {
            return pattern(random, moonPhaseValue, "enhanced_commands.commands.moon.phase.get.full.special.minecart.", 5);
          } else {
            // 非特殊返回时，满月也有 20 个可能的返回结果。
            return pattern(random, moonPhaseValue, "enhanced_commands.commands.moon.phase.get." + type + ".", 20);
          }
        } else {
          return pattern(random, moonPhaseValue, "enhanced_commands.commands.moon.phase.get." + type + ".", 10);
        }
      } else {
        // 白天
        return Text.translatable("enhanced_commands.commands.moon.phase.get.0", TextUtil.styled(moonPhaseValue.displayName, Styles.RESULT));
      }
    }, false);
    return moonPhase;
  }

  private static Text pattern(Random random, MoonPhase moonPhase, String specialKeyPrefix, int randomRange) {
    return Text.translatable("enhanced_commands.commands.moon.phase.get.pattern", Text.translatable("enhanced_commands.commands.moon.phase.get." + random.nextInt(10), TextUtil.styled(moonPhase.displayName, Styles.RESULT)), Text.translatable(specialKeyPrefix + random.nextInt(randomRange)));
  }

  private static int executeSetPhase(CommandContext<ServerCommandSource> context) {
    final MoonPhase moonPhase = context.getArgument("moon_phase", MoonPhase.class);
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    final long timeOfDay = world.getTimeOfDay();
    final int actualMoonPhase = world.getMoonPhase();
    world.setTimeOfDay(timeOfDay + (moonPhase.ordinal() - actualMoonPhase) * 24000L);
    source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.moon.phase.set.success", TextUtil.styled(moonPhase.displayName, Styles.RESULT)), true);
    return 1;
  }

  private static int executeRotatePhase(CommandContext<ServerCommandSource> context, int steps) {
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    final long timeOfDay = world.getTimeOfDay();
    world.setTimeOfDay(timeOfDay + steps * 24000L);
    final MoonPhase moonPhase = MoonPhase.byNumericId(world.getMoonPhase());
    source.sendFeedback$ecBridge(() -> steps >= 0 ? Text.translatable("enhanced_commands.commands.moon.phase.rotate.next", TextUtil.literal(steps).styled(Styles.TARGET), TextUtil.styled(moonPhase.displayName, Styles.RESULT)) : Text.translatable("enhanced_commands.commands.moon.phase.rotate.previous", TextUtil.literal(-steps).styled(Styles.TARGET), TextUtil.styled(moonPhase.displayName, Styles.RESULT)), true);
    return 1;
  }
}
