package pers.solid.ecmd.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import pers.solid.ecmd.argument.DirectionArgument;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.RegionArgument;
import pers.solid.ecmd.mixins.accessor.ExecuteCommandAccessor;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.lambda.ToFloatTriFunction;
import pers.solid.ecmd.util.lambda.ToIntQuadFunction;
import pers.solid.ecmd.util.lambda.ToIntTriFunction;
import pers.solid.ecmd.util.lambda.TriPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.command.ModCommands.literalR2;

/**
 * @see net.minecraft.server.commands.ExecuteCommand
 */
public final class SeparatedExecuteCommand {
  private static final SimpleCommandExceptionType CONDITIONAL_FAIL_EXCEPTION = ExecuteCommandAccessor.getERROR_CONDITIONAL_FAILED();
  public static final BinaryOperator<CommandResultCallback> BINARY_RESULT_CONSUMER = (consumer, consumer2) -> (successful, returnValue) -> {
    consumer.onResult(successful, returnValue);
    consumer2.onResult(successful, returnValue);
  };

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
    RootCommandNode<CommandSourceStack> literalCommandNode = dispatcher.getRoot();
    dispatcher.register(literal("execute")
        .redirect(dispatcher.getRoot()));
    dispatcher.register(literalR2("run")
        .redirect(dispatcher.getRoot()));

    dispatcher.register(addConditionArguments(literalCommandNode, literalR2("if"), true, commandBuildContext));
    dispatcher.register(addConditionArguments(literalCommandNode, literalR2("unless"), false, commandBuildContext));
    dispatcher.register(literalR2("as")
        .then(argument("targets", EntityArgument.entities()).fork(literalCommandNode, context -> {
          List<CommandSourceStack> list = Lists.newArrayList();

          for (Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
            list.add(context.getSource().withEntity(entity));
          }

          return list;
        })));
    dispatcher.register(literalR2("at")
        .then(argument("targets", EntityArgument.entities()).fork(literalCommandNode, context -> {
          List<CommandSourceStack> list = Lists.newArrayList();

          for (Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
            list.add(context.getSource().withLevel((ServerLevel) entity.level()).withPosition(entity.position()).withRotation(entity.getRotationVector()));
          }

          return list;
        })));
    dispatcher.register(literalR2("for")
        .then(argument("targets", EntityArgument.entities()).fork(literalCommandNode, context -> {
          final CommandSourceStack source = context.getSource();
          List<CommandSourceStack> list = new ArrayList<>();
          for (Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
            CommandSourceStack serverCommandSource = source.withEntity(entity).withLevel((ServerLevel) entity.level()).withPosition(entity.position()).withRotation(entity.getRotationVector());
            list.add(serverCommandSource);
          }
          return list;
        })));
    dispatcher.register(literalR2("inregion")
        .then(argument("region", RegionArgument.region(commandBuildContext))
            .fork(literalCommandNode, context -> {
              final Region region = RegionArgument.getRegion(context, "region");
              final CommandSourceStack source = context.getSource();
              List<CommandSourceStack> list = new ArrayList<>();
              for (BlockPos pos : region) {
                CommandSourceStack serverCommandSource = source.withPosition(Vec3.atBottomCenterOf(pos));
                list.add(serverCommandSource);
              }
              return list;
            })));
    dispatcher.register(literalR2("silenced").redirect(literalCommandNode, context -> context.getSource().withSuppressedOutput()));
    dispatcher.register(literalR2("store")
        .then(addStoreArguments(literalCommandNode, literal("result"), true))
        .then(addStoreArguments(literalCommandNode, literal("success"), false)));
    dispatcher.register(literalR2("positioned")
        .then(argument("pos", Vec3Argument.vec3()).redirect(literalCommandNode, context -> context.getSource().withPosition(Vec3Argument.getVec3(context, "pos")).withAnchor(EntityAnchorArgument.Anchor.FEET)))
        .then(literal("as")
            .then(argument("targets", EntityArgument.entities()).fork(literalCommandNode, context -> {
              List<CommandSourceStack> list = Lists.newArrayList();

              for (Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withPosition(entity.position()));
              }

              return list;
            })))
        .then(literal("over")
            .then(argument("heightmap", HeightmapTypeArgument.heightmap()).redirect(literalCommandNode, context -> {
              Vec3 vec3d = context.getSource().getPosition();
              ServerLevel serverWorld = context.getSource().getLevel();
              double d = vec3d.x();
              double e = vec3d.z();
              if (!serverWorld.hasChunk(SectionPos.blockToSectionCoord(d), SectionPos.blockToSectionCoord(e))) {
                throw BlockPosArgument.ERROR_NOT_LOADED.create();
              } else {
                int i = serverWorld.getHeight(HeightmapTypeArgument.getHeightmap(context, "heightmap"), Mth.floor(d), Mth.floor(e));
                return context.getSource().withPosition(new Vec3(d, i, e));
              }
            }))));
    dispatcher.register(literalR2("rotated")
        .then(argument("rot", RotationArgument.rotation()).redirect(literalCommandNode, context -> context.getSource().withRotation(RotationArgument.getRotation(context, "rot").getRotation(context.getSource()))))
        .then(literal("as")
            .then(argument("targets", EntityArgument.entities()).fork(literalCommandNode, context -> {
              List<CommandSourceStack> list = Lists.newArrayList();

              for (Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withRotation(entity.getRotationVector()));
              }

              return list;
            }))));
    dispatcher.register(literalR2("facing")
        .then(literal("entity")
            .then(argument("targets", EntityArgument.entities())
                .then(argument("anchor", EntityAnchorArgument.anchor()).fork(literalCommandNode, context -> {
                  List<CommandSourceStack> list = Lists.newArrayList();
                  EntityAnchorArgument.Anchor entityAnchor = EntityAnchorArgument.getAnchor(context, "anchor");

                  for (Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
                    list.add(context.getSource().facing(entity, entityAnchor));
                  }

                  return list;
                }))))
        .then(argument("pos", Vec3Argument.vec3()).redirect(literalCommandNode, context -> context.getSource().facing(Vec3Argument.getVec3(context, "pos")))));
    dispatcher.register(literalR2("align")
        .then(argument("axes", SwizzleArgument.swizzle()).redirect(literalCommandNode, context -> context.getSource().withPosition(context.getSource().getPosition().align(SwizzleArgument.getSwizzle(context, "axes"))))));
    dispatcher.register(literalR2("anchored")
        .then(argument("anchor", EntityAnchorArgument.anchor()).redirect(literalCommandNode, context -> context.getSource().withAnchor(EntityAnchorArgument.getAnchor(context, "anchor")))));
    dispatcher.register(literalR2("in")
        .then(argument("dimension", DimensionArgument.dimension()).redirect(literalCommandNode, context -> context.getSource().withLevel(DimensionArgument.getDimension(context, "dimension")))));
    dispatcher.register(addOnArguments(literalCommandNode, literalR2("on")));
  }

  private static ArgumentBuilder<CommandSourceStack, ?> addStoreArguments(RootCommandNode<CommandSourceStack> node, LiteralArgumentBuilder<CommandSourceStack> builder, boolean requestResult) {
    builder.then(literal("score")
        .then(argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
            .then(argument("objective", ObjectiveArgument.objective()).redirect(node, context -> executeStoreScore(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"), ObjectiveArgument.getObjective(context, "objective"), requestResult)))));
    builder.then(literal("bossbar")
        .then(argument("id", ResourceLocationArgument.id()).suggests(BossBarCommands.SUGGEST_BOSS_BAR)
            .then(literal("probability").redirect(node, context -> executeStoreBossbar(context.getSource(), BossBarCommands.getBossBar(context), true, requestResult)))
            .then(literal("max").redirect(node, context -> executeStoreBossbar(context.getSource(), BossBarCommands.getBossBar(context), false, requestResult)))));

    for (DataCommands.DataProvider objectType : DataCommands.TARGET_PROVIDERS) {
      objectType.wrap(builder, builderx -> builderx
          .then(argument("path", NbtPathArgument.nbtPath())
              .then(literal("int")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.access(context), NbtPathArgument.getPath(context, "path"), result -> IntTag.valueOf((int) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))
              .then(literal("float")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.access(context), NbtPathArgument.getPath(context, "path"), result -> FloatTag.valueOf((float) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))
              .then(literal("short")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.access(context), NbtPathArgument.getPath(context, "path"), result -> ShortTag.valueOf((short) (int) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))
              .then(literal("long")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.access(context), NbtPathArgument.getPath(context, "path"), result -> LongTag.valueOf((long) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))
              .then(literal("double")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.access(context), NbtPathArgument.getPath(context, "path"), result -> DoubleTag.valueOf((double) result * DoubleArgumentType.getDouble(context, "scale")), requestResult))))
              .then(literal("byte")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.access(context), NbtPathArgument.getPath(context, "path"), result -> ByteTag.valueOf((byte) (int) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))));
    }

    return builder;
  }

  private static CommandSourceStack executeStoreScore(CommandSourceStack source, Collection<ScoreHolder> targets, Objective objective, boolean requestResult) {
    return ExecuteCommandAccessor.callStoreValue(source, targets, objective, requestResult);
  }

  private static CommandSourceStack executeStoreBossbar(CommandSourceStack source, CustomBossEvent bossBar, boolean storeInValue, boolean requestResult) {
    return ExecuteCommandAccessor.callStoreValue(source, bossBar, storeInValue, requestResult);
  }

  private static CommandSourceStack executeStoreData(CommandSourceStack source, DataAccessor object, NbtPathArgument.NbtPath path, IntFunction<Tag> nbtSetter, boolean requestResult) {
    return ExecuteCommandAccessor.callStoreData(source, object, path, nbtSetter, requestResult);
  }

  private static LiteralArgumentBuilder<CommandSourceStack> addConditionArguments(CommandNode<CommandSourceStack> root, LiteralArgumentBuilder<CommandSourceStack> argumentBuilder, boolean positive, CommandBuildContext commandBuildContext) {
    ExecuteCommandAccessor.callAddConditionals(root, argumentBuilder, positive, commandBuildContext);
    return addExtraConditionArguments(root, argumentBuilder, positive, commandBuildContext);
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBlockInfoArguments(CommandNode<CommandSourceStack> root, T argumentBuilder, boolean positive) {
    return argumentBuilder
        .then(addBlockFloatInfoConditionalLogic(root, literal("hardness"), BlockBehaviour.BlockStateBase::getDestroySpeed, positive))
        .then(addBlockIntInfoConditionalLogic(root, literal("luminance"), (state, serverWorld, pos) -> state.getLightEmission(), positive))
        .then(addBlockIntInfoConditionalLogicWithDirection(root, literal("strong_redstone_power"), BlockBehaviour.BlockStateBase::getDirectSignal, positive))
        .then(addBlockIntInfoConditionalLogicWithDirection(root, literal("weak_redstone_power"), BlockBehaviour.BlockStateBase::getSignal, positive))
        .then(addBlockIntInfoConditionalLogic(root, literal("light"), (state, serverWorld, pos) -> serverWorld.getMaxLocalRawBrightness(pos), positive))
        .then(addBlockIntInfoConditionalLogic(root, literal("block_light"), (state, serverWorld, pos) -> serverWorld.getBrightness(LightLayer.BLOCK, pos), positive))
        .then(addBlockIntInfoConditionalLogic(root, literal("sky_light"), (state, serverWorld, pos) -> serverWorld.getBrightness(LightLayer.SKY, pos), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("emits_redstone_power"), (state, serverWorld, pos) -> state.isSignalSource(), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("opaque"), (state, serverWorld, pos) -> state.canOcclude(), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("model_offset"), (state, serverWorld, pos) -> !state.getOffset(pos).equals(Vec3.ZERO), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("suffocate"), BlockBehaviour.BlockStateBase::isSuffocating, positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("block_vision"), BlockBehaviour.BlockStateBase::isViewBlocking, positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("replaceable"), (state, serverWorld, pos) -> state.canBeReplaced(), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("random_ticks"), (state, serverWorld, pos) -> state.isRandomlyTicking(), positive));
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBlockFloatInfoConditionalLogic(CommandNode<CommandSourceStack> root, T node, ToFloatTriFunction<BlockState, ServerLevel, BlockPos> function, boolean positive) {
    return node.then(addConditionLogic(root, argument("range", RangeArgument.floatRange()), positive, context -> {
      final BlockPos pos = EnhancedPosArgument.getLoadedBlockPos(context, "pos");
      final ServerLevel world = context.getSource().getLevel();
      return RangeArgument.Floats.getRange(context, "range").matches(function.applyAsFloat(world.getBlockState(pos), world, pos));
    }));
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBlockIntInfoConditionalLogic(CommandNode<CommandSourceStack> root, T node, ToIntTriFunction<BlockState, ServerLevel, BlockPos> function, boolean positive) {
    return node.then(addConditionLogic(root, argument("range", RangeArgument.intRange()), positive, context -> {
      final BlockPos pos = EnhancedPosArgument.getLoadedBlockPos(context, "pos");
      final ServerLevel world = context.getSource().getLevel();
      return RangeArgument.Ints.getRange(context, "range").matches(function.applyAsInt(world.getBlockState(pos), world, pos));
    }));
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBlockIntInfoConditionalLogicWithDirection(CommandNode<CommandSourceStack> root, T node, ToIntQuadFunction<BlockState, ServerLevel, BlockPos, Direction> function, boolean positive) {
    return node.then(argument("direction", DirectionArgument.direction())
        .then(addConditionLogic(root, argument("range", RangeArgument.intRange()), positive, context -> {
          final BlockPos pos = EnhancedPosArgument.getLoadedBlockPos(context, "pos");
          final ServerLevel world = context.getSource().getLevel();
          final Direction direction = DirectionArgument.getDirection(context, "direction");
          return RangeArgument.Ints.getRange(context, "range").matches(function.applyAsInt(world.getBlockState(pos), world, pos, direction));
        })));
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBlockBooleanInfoConditionalLogicWith(CommandNode<CommandSourceStack> root, T node, TriPredicate<BlockState, ServerLevel, BlockPos> function, boolean positive) {
    return addConditionLogic(root, node, positive, context -> {
      final BlockPos pos = EnhancedPosArgument.getLoadedBlockPos(context, "pos");
      final ServerLevel world = context.getSource().getLevel();
      return function.test(world.getBlockState(pos), world, pos);
    });
  }


  public static <T extends ArgumentBuilder<CommandSourceStack, T>> T addExtraConditionArguments(CommandNode<CommandSourceStack> root, T argumentBuilder, boolean positive, CommandBuildContext commandBuildContext) {
    return argumentBuilder
        .then(literal("blockinfo")
            .then(addBlockInfoArguments(root, argument("pos", EnhancedPosArgument.blockPos()), positive)))
        .then(literal("rand")
            .then(addConditionLogic(root, argument("probability", FloatArgumentType.floatArg(0, 1)), positive, context -> context.getSource().getLevel().random.nextFloat() < FloatArgumentType.getFloat(context, "probability"))));
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addConditionLogic(CommandNode<CommandSourceStack> root, T builder, boolean positive, Condition condition) {
    return builder.fork(root, context -> ExecuteCommandAccessor.callExpect(context, positive, condition.test(context))).executes(context -> {
      if (positive == condition.test(context)) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("commands.execute.conditional.pass"), false);
        return 1;
      } else {
        throw CONDITIONAL_FAIL_EXCEPTION.create();
      }
    });
  }

  private static LiteralArgumentBuilder<CommandSourceStack> addOnArguments(CommandNode<CommandSourceStack> node, LiteralArgumentBuilder<CommandSourceStack> builder) {
    return ExecuteCommandAccessor.callCreateRelationOperations(node, builder);
  }

  @FunctionalInterface
  interface Condition {
    boolean test(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
  }

}
