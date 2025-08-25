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
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.ReturnValueConsumer;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.nbt.*;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.BossBarCommand;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.LightType;
import pers.solid.ecmd.argument.DirectionArgumentType;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.RegionArgumentType;
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

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.command.ModCommands.literalR2;

/**
 * @see net.minecraft.server.command.ExecuteCommand
 */
public final class SeparatedExecuteCommand {
  private static final SimpleCommandExceptionType CONDITIONAL_FAIL_EXCEPTION = ExecuteCommandAccessor.getCONDITIONAL_FAIL_EXCEPTION();
  public static final BinaryOperator<ReturnValueConsumer> BINARY_RESULT_CONSUMER = (consumer, consumer2) -> (successful, returnValue) -> {
    consumer.onResult(successful, returnValue);
    consumer2.onResult(successful, returnValue);
  };

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
    RootCommandNode<ServerCommandSource> literalCommandNode = dispatcher.getRoot();
    dispatcher.register(literal("execute")
        .redirect(dispatcher.getRoot()));
    dispatcher.register(literalR2("run")
        .redirect(dispatcher.getRoot()));

    dispatcher.register(addConditionArguments(literalCommandNode, literalR2("if"), true, registryAccess));
    dispatcher.register(addConditionArguments(literalCommandNode, literalR2("unless"), false, registryAccess));
    dispatcher.register(literalR2("as")
        .then(argument("targets", EntityArgumentType.entities()).fork(literalCommandNode, context -> {
          List<ServerCommandSource> list = Lists.newArrayList();

          for (Entity entity : EntityArgumentType.getOptionalEntities(context, "targets")) {
            list.add(context.getSource().withEntity(entity));
          }

          return list;
        })));
    dispatcher.register(literalR2("at")
        .then(argument("targets", EntityArgumentType.entities()).fork(literalCommandNode, context -> {
          List<ServerCommandSource> list = Lists.newArrayList();

          for (Entity entity : EntityArgumentType.getOptionalEntities(context, "targets")) {
            list.add(context.getSource().withWorld((ServerWorld) entity.getWorld()).withPosition(entity.getPos()).withRotation(entity.getRotationClient()));
          }

          return list;
        })));
    dispatcher.register(literalR2("for")
        .then(argument("targets", EntityArgumentType.entities()).fork(literalCommandNode, context -> {
          final ServerCommandSource source = context.getSource();
          List<ServerCommandSource> list = new ArrayList<>();
          for (Entity entity : EntityArgumentType.getOptionalEntities(context, "targets")) {
            ServerCommandSource serverCommandSource = source.withEntity(entity).withWorld((ServerWorld) entity.getWorld()).withPosition(entity.getPos()).withRotation(entity.getRotationClient());
            list.add(serverCommandSource);
          }
          return list;
        })));
    dispatcher.register(literalR2("inregion")
        .then(argument("region", RegionArgumentType.region(registryAccess))
            .fork(literalCommandNode, context -> {
              final Region region = RegionArgumentType.getRegion(context, "region");
              final ServerCommandSource source = context.getSource();
              List<ServerCommandSource> list = new ArrayList<>();
              for (BlockPos pos : region) {
                ServerCommandSource serverCommandSource = source.withPosition(Vec3d.ofBottomCenter(pos));
                list.add(serverCommandSource);
              }
              return list;
            })));
    dispatcher.register(literalR2("silenced").redirect(literalCommandNode, context -> context.getSource().withSilent()));
    dispatcher.register(literalR2("store")
        .then(addStoreArguments(literalCommandNode, literal("result"), true))
        .then(addStoreArguments(literalCommandNode, literal("success"), false)));
    dispatcher.register(literalR2("positioned")
        .then(argument("pos", Vec3ArgumentType.vec3()).redirect(literalCommandNode, context -> context.getSource().withPosition(Vec3ArgumentType.getVec3(context, "pos")).withEntityAnchor(EntityAnchorArgumentType.EntityAnchor.FEET)))
        .then(literal("as")
            .then(argument("targets", EntityArgumentType.entities()).fork(literalCommandNode, context -> {
              List<ServerCommandSource> list = Lists.newArrayList();

              for (Entity entity : EntityArgumentType.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withPosition(entity.getPos()));
              }

              return list;
            })))
        .then(literal("over")
            .then(argument("heightmap", HeightmapArgumentType.heightmap()).redirect(literalCommandNode, context -> {
              Vec3d vec3d = context.getSource().getPosition();
              ServerWorld serverWorld = context.getSource().getWorld();
              double d = vec3d.getX();
              double e = vec3d.getZ();
              if (!serverWorld.isChunkLoaded(ChunkSectionPos.getSectionCoordFloored(d), ChunkSectionPos.getSectionCoordFloored(e))) {
                throw BlockPosArgumentType.UNLOADED_EXCEPTION.create();
              } else {
                int i = serverWorld.getTopY(HeightmapArgumentType.getHeightmap(context, "heightmap"), MathHelper.floor(d), MathHelper.floor(e));
                return context.getSource().withPosition(new Vec3d(d, i, e));
              }
            }))));
    dispatcher.register(literalR2("rotated")
        .then(argument("rot", RotationArgumentType.rotation()).redirect(literalCommandNode, context -> context.getSource().withRotation(RotationArgumentType.getRotation(context, "rot").getRotation(context.getSource()))))
        .then(literal("as")
            .then(argument("targets", EntityArgumentType.entities()).fork(literalCommandNode, context -> {
              List<ServerCommandSource> list = Lists.newArrayList();

              for (Entity entity : EntityArgumentType.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withRotation(entity.getRotationClient()));
              }

              return list;
            }))));
    dispatcher.register(literalR2("facing")
        .then(literal("entity")
            .then(argument("targets", EntityArgumentType.entities())
                .then(argument("anchor", EntityAnchorArgumentType.entityAnchor()).fork(literalCommandNode, context -> {
                  List<ServerCommandSource> list = Lists.newArrayList();
                  EntityAnchorArgumentType.EntityAnchor entityAnchor = EntityAnchorArgumentType.getEntityAnchor(context, "anchor");

                  for (Entity entity : EntityArgumentType.getOptionalEntities(context, "targets")) {
                    list.add(context.getSource().withLookingAt(entity, entityAnchor));
                  }

                  return list;
                }))))
        .then(argument("pos", Vec3ArgumentType.vec3()).redirect(literalCommandNode, context -> context.getSource().withLookingAt(Vec3ArgumentType.getVec3(context, "pos")))));
    dispatcher.register(literalR2("align")
        .then(argument("axes", SwizzleArgumentType.swizzle()).redirect(literalCommandNode, context -> context.getSource().withPosition(context.getSource().getPosition().floorAlongAxes(SwizzleArgumentType.getSwizzle(context, "axes"))))));
    dispatcher.register(literalR2("anchored")
        .then(argument("anchor", EntityAnchorArgumentType.entityAnchor()).redirect(literalCommandNode, context -> context.getSource().withEntityAnchor(EntityAnchorArgumentType.getEntityAnchor(context, "anchor")))));
    dispatcher.register(literalR2("in")
        .then(argument("dimension", DimensionArgumentType.dimension()).redirect(literalCommandNode, context -> context.getSource().withWorld(DimensionArgumentType.getDimensionArgument(context, "dimension")))));
    dispatcher.register(addOnArguments(literalCommandNode, literalR2("on")));
  }

  private static ArgumentBuilder<ServerCommandSource, ?> addStoreArguments(RootCommandNode<ServerCommandSource> node, LiteralArgumentBuilder<ServerCommandSource> builder, boolean requestResult) {
    builder.then(literal("score")
        .then(argument("targets", ScoreHolderArgumentType.scoreHolders()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
            .then(argument("objective", ScoreboardObjectiveArgumentType.scoreboardObjective()).redirect(node, context -> executeStoreScore(context.getSource(), ScoreHolderArgumentType.getScoreboardScoreHolders(context, "targets"), ScoreboardObjectiveArgumentType.getObjective(context, "objective"), requestResult)))));
    builder.then(literal("bossbar")
        .then(argument("id", IdentifierArgumentType.identifier()).suggests(BossBarCommand.SUGGESTION_PROVIDER)
            .then(literal("probability").redirect(node, context -> executeStoreBossbar(context.getSource(), BossBarCommand.getBossBar(context), true, requestResult)))
            .then(literal("max").redirect(node, context -> executeStoreBossbar(context.getSource(), BossBarCommand.getBossBar(context), false, requestResult)))));

    for (DataCommand.ObjectType objectType : DataCommand.TARGET_OBJECT_TYPES) {
      objectType.addArgumentsToBuilder(builder, builderx -> builderx
          .then(argument("path", NbtPathArgumentType.nbtPath())
              .then(literal("int")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"), result -> NbtInt.of((int) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))
              .then(literal("float")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"), result -> NbtFloat.of((float) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))
              .then(literal("short")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"), result -> NbtShort.of((short) (int) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))
              .then(literal("long")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"), result -> NbtLong.of((long) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))
              .then(literal("double")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"), result -> NbtDouble.of((double) result * DoubleArgumentType.getDouble(context, "scale")), requestResult))))
              .then(literal("byte")
                  .then(argument("scale", DoubleArgumentType.doubleArg()).redirect(node, context -> executeStoreData(context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"), result -> NbtByte.of((byte) (int) ((double) result * DoubleArgumentType.getDouble(context, "scale"))), requestResult))))));
    }

    return builder;
  }

  private static ServerCommandSource executeStoreScore(ServerCommandSource source, Collection<ScoreHolder> targets, ScoreboardObjective objective, boolean requestResult) {
    return ExecuteCommandAccessor.callExecuteStoreScore(source, targets, objective, requestResult);
  }

  private static ServerCommandSource executeStoreBossbar(ServerCommandSource source, CommandBossBar bossBar, boolean storeInValue, boolean requestResult) {
    return ExecuteCommandAccessor.callExecuteStoreBossbar(source, bossBar, storeInValue, requestResult);
  }

  private static ServerCommandSource executeStoreData(ServerCommandSource source, DataCommandObject object, NbtPathArgumentType.NbtPath path, IntFunction<NbtElement> nbtSetter, boolean requestResult) {
    return ExecuteCommandAccessor.callExecuteStoreData(source, object, path, nbtSetter, requestResult);
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addConditionArguments(CommandNode<ServerCommandSource> root, LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, boolean positive, CommandRegistryAccess registryAccess) {
    ExecuteCommandAccessor.callAddConditionArguments(root, argumentBuilder, positive, registryAccess);
    return addExtraConditionArguments(root, argumentBuilder, positive, registryAccess);
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addBlockInfoArguments(CommandNode<ServerCommandSource> root, T argumentBuilder, boolean positive) {
    return argumentBuilder
        .then(addBlockFloatInfoConditionalLogic(root, literal("hardness"), AbstractBlock.AbstractBlockState::getHardness, positive))
        .then(addBlockIntInfoConditionalLogic(root, literal("luminance"), (state, serverWorld, pos) -> state.getLuminance(), positive))
        .then(addBlockIntInfoConditionalLogicWithDirection(root, literal("strong_redstone_power"), AbstractBlock.AbstractBlockState::getStrongRedstonePower, positive))
        .then(addBlockIntInfoConditionalLogicWithDirection(root, literal("weak_redstone_power"), AbstractBlock.AbstractBlockState::getWeakRedstonePower, positive))
        .then(addBlockIntInfoConditionalLogic(root, literal("light"), (state, serverWorld, pos) -> serverWorld.getLightLevel(pos), positive))
        .then(addBlockIntInfoConditionalLogic(root, literal("block_light"), (state, serverWorld, pos) -> serverWorld.getLightLevel(LightType.BLOCK, pos), positive))
        .then(addBlockIntInfoConditionalLogic(root, literal("sky_light"), (state, serverWorld, pos) -> serverWorld.getLightLevel(LightType.SKY, pos), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("emits_redstone_power"), (state, serverWorld, pos) -> state.emitsRedstonePower(), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("opaque"), (state, serverWorld, pos) -> state.isOpaque(), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("model_offset"), (state, serverWorld, pos) -> !state.getModelOffset(pos).equals(Vec3d.ZERO), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("suffocate"), AbstractBlock.AbstractBlockState::shouldSuffocate, positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("block_vision"), AbstractBlock.AbstractBlockState::shouldBlockVision, positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("replaceable"), (state, serverWorld, pos) -> state.isReplaceable(), positive))
        .then(addBlockBooleanInfoConditionalLogicWith(root, literal("random_ticks"), (state, serverWorld, pos) -> state.hasRandomTicks(), positive));
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addBlockFloatInfoConditionalLogic(CommandNode<ServerCommandSource> root, T node, ToFloatTriFunction<BlockState, ServerWorld, BlockPos> function, boolean positive) {
    return node.then(addConditionLogic(root, argument("range", NumberRangeArgumentType.floatRange()), positive, context -> {
      final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
      final ServerWorld world = context.getSource().getWorld();
      return NumberRangeArgumentType.FloatRangeArgumentType.getRangeArgument(context, "range").test(function.applyAsFloat(world.getBlockState(pos), world, pos));
    }));
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addBlockIntInfoConditionalLogic(CommandNode<ServerCommandSource> root, T node, ToIntTriFunction<BlockState, ServerWorld, BlockPos> function, boolean positive) {
    return node.then(addConditionLogic(root, argument("range", NumberRangeArgumentType.intRange()), positive, context -> {
      final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
      final ServerWorld world = context.getSource().getWorld();
      return NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(context, "range").test(function.applyAsInt(world.getBlockState(pos), world, pos));
    }));
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addBlockIntInfoConditionalLogicWithDirection(CommandNode<ServerCommandSource> root, T node, ToIntQuadFunction<BlockState, ServerWorld, BlockPos, Direction> function, boolean positive) {
    return node.then(argument("direction", DirectionArgumentType.direction())
        .then(addConditionLogic(root, argument("range", NumberRangeArgumentType.intRange()), positive, context -> {
          final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
          final ServerWorld world = context.getSource().getWorld();
          final Direction direction = DirectionArgumentType.getDirection(context, "direction");
          return NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(context, "range").test(function.applyAsInt(world.getBlockState(pos), world, pos, direction));
        })));
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addBlockBooleanInfoConditionalLogicWith(CommandNode<ServerCommandSource> root, T node, TriPredicate<BlockState, ServerWorld, BlockPos> function, boolean positive) {
    return addConditionLogic(root, node, positive, context -> {
      final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
      final ServerWorld world = context.getSource().getWorld();
      return function.test(world.getBlockState(pos), world, pos);
    });
  }


  public static <T extends ArgumentBuilder<ServerCommandSource, T>> T addExtraConditionArguments(CommandNode<ServerCommandSource> root, T argumentBuilder, boolean positive, CommandRegistryAccess registryAccess) {
    return argumentBuilder
        .then(literal("blockinfo")
            .then(addBlockInfoArguments(root, argument("pos", EnhancedPosArgumentType.blockPos()), positive)))
        .then(literal("rand")
            .then(addConditionLogic(root, argument("probability", FloatArgumentType.floatArg(0, 1)), positive, context -> context.getSource().getWorld().random.nextFloat() < FloatArgumentType.getFloat(context, "probability"))));
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addConditionLogic(CommandNode<ServerCommandSource> root, T builder, boolean positive, Condition condition) {
    return builder.fork(root, context -> ExecuteCommandAccessor.callGetSourceOrEmptyForConditionFork(context, positive, condition.test(context))).executes(context -> {
      if (positive == condition.test(context)) {
        context.getSource().sendFeedback$ecBridge(() -> Text.translatable("commands.execute.conditional.pass"), false);
        return 1;
      } else {
        throw CONDITIONAL_FAIL_EXCEPTION.create();
      }
    });
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addOnArguments(CommandNode<ServerCommandSource> node, LiteralArgumentBuilder<ServerCommandSource> builder) {
    return ExecuteCommandAccessor.callAddOnArguments(node, builder);
  }

  @FunctionalInterface
  interface Condition {
    boolean test(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
  }

}
