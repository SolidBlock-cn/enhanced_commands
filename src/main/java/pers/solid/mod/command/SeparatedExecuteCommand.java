package pers.solid.mod.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.argument.*;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionManager;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.*;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.command.*;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.RandomUtils;
import pers.solid.mod.argument.BlockPredicateArgumentType;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.mod.command.ModCommands.REQUIRES_PERMISSION_2;

/**
 * @see net.minecraft.server.command.ExecuteCommand
 */
public final class SeparatedExecuteCommand {
  private static final Dynamic2CommandExceptionType BLOCKS_TOOBIG_EXCEPTION = new Dynamic2CommandExceptionType((maxCount, count) -> Text.translatable("commands.execute.blocks.toobig", maxCount, count));
  private static final SimpleCommandExceptionType CONDITIONAL_FAIL_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.execute.conditional.fail"));
  private static final DynamicCommandExceptionType CONDITIONAL_FAIL_COUNT_EXCEPTION = new DynamicCommandExceptionType(count -> Text.translatable("commands.execute.conditional.fail_count", count));
  private static final BinaryOperator<ResultConsumer<ServerCommandSource>> BINARY_RESULT_CONSUMER = (consumer, consumer2) -> (context, success, result) -> {
    consumer.onCommandComplete(context, success, result);
    consumer2.onCommandComplete(context, success, result);
  };
  private static final SuggestionProvider<ServerCommandSource> LOOT_CONDITIONS = (context, builder) -> {
    LootConditionManager lootConditionManager = context.getSource().getServer().getPredicateManager();
    return CommandSource.suggestIdentifiers(lootConditionManager.getIds(), builder);
  };

  private static LiteralArgumentBuilder<ServerCommandSource> literalR2(String literal) {
    return literal(literal).requires(REQUIRES_PERMISSION_2);
  }

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
    RootCommandNode<ServerCommandSource> literalCommandNode = dispatcher.getRoot();
    dispatcher.register(literal("execute")
        .redirect(dispatcher.getRoot()));
    dispatcher.register(literalR2("run")
        .redirect(dispatcher.getRoot()));

    dispatcher.register(addConditionArguments(literalCommandNode, literalR2("if"), true, commandRegistryAccess));
    dispatcher.register(addConditionArguments(literalCommandNode, literalR2("unless"), false, commandRegistryAccess));
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
            list.add(context.getSource().withWorld((ServerWorld) entity.world).withPosition(entity.getPos()).withRotation(entity.getRotationClient()));
          }

          return list;
        })));
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
        .then(argument("rot", RotationArgumentType.rotation()).redirect(literalCommandNode, context -> context.getSource().withRotation(RotationArgumentType.getRotation(context, "rot").toAbsoluteRotation(context.getSource()))))
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
    dispatcher.register(literalR2("summon")
        .then(argument("entity", RegistryEntryArgumentType.registryEntry(commandRegistryAccess, RegistryKeys.ENTITY_TYPE)).suggests(SuggestionProviders.SUMMONABLE_ENTITIES).redirect(literalCommandNode, context -> summon(context.getSource(), RegistryEntryArgumentType.getSummonableEntityType(context, "entity")))));
    dispatcher.register(addOnArguments(literalCommandNode, literalR2("on")));
  }

  private static ArgumentBuilder<ServerCommandSource, ?> addStoreArguments(RootCommandNode<ServerCommandSource> node, LiteralArgumentBuilder<ServerCommandSource> builder, boolean requestResult) {
    builder.then(literal("score")
        .then(argument("targets", ScoreHolderArgumentType.scoreHolders()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
            .then(argument("objective", ScoreboardObjectiveArgumentType.scoreboardObjective()).redirect(node, context -> executeStoreScore(context.getSource(), ScoreHolderArgumentType.getScoreboardScoreHolders(context, "targets"), ScoreboardObjectiveArgumentType.getObjective(context, "objective"), requestResult)))));
    builder.then(literal("bossbar")
        .then(argument("id", IdentifierArgumentType.identifier()).suggests(BossBarCommand.SUGGESTION_PROVIDER)
            .then(literal("value").redirect(node, context -> executeStoreBossbar(context.getSource(), BossBarCommand.getBossBar(context), true, requestResult)))
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

  private static ServerCommandSource executeStoreScore(ServerCommandSource source, Collection<String> targets, ScoreboardObjective objective, boolean requestResult) {
    Scoreboard scoreboard = source.getServer().getScoreboard();
    return source.mergeConsumers((context, success, result) -> {
      for (String string : targets) {
        ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(string, objective);
        int i = requestResult ? result : success ? 1 : 0;
        scoreboardPlayerScore.setScore(i);
      }
    }, BINARY_RESULT_CONSUMER);
  }

  private static ServerCommandSource executeStoreBossbar(ServerCommandSource source, CommandBossBar bossBar, boolean storeInValue, boolean requestResult) {
    return source.mergeConsumers((context, success, result) -> {
      int i = requestResult ? result : success ? 1 : 0;
      if (storeInValue) {
        bossBar.setValue(i);
      } else {
        bossBar.setMaxValue(i);
      }
    }, BINARY_RESULT_CONSUMER);
  }

  private static ServerCommandSource executeStoreData(ServerCommandSource source, DataCommandObject object, NbtPathArgumentType.NbtPath path, IntFunction<NbtElement> nbtSetter, boolean requestResult) {
    return source.mergeConsumers((context, success, result) -> {
      try {
        NbtCompound nbtCompound = object.getNbt();
        int i = requestResult ? result : success ? 1 : 0;
        path.put(nbtCompound, nbtSetter.apply(i));
        object.setNbt(nbtCompound);
      } catch (CommandSyntaxException var9) {
      }
    }, BINARY_RESULT_CONSUMER);
  }

  private static boolean isLoaded(ServerWorld world, BlockPos pos) {
    int i = ChunkSectionPos.getSectionCoord(pos.getX());
    int j = ChunkSectionPos.getSectionCoord(pos.getZ());
    WorldChunk worldChunk = world.getChunkManager().getWorldChunk(i, j);
    if (worldChunk != null) {
      return worldChunk.getLevelType() == ChunkHolder.LevelType.ENTITY_TICKING;
    } else {
      return false;
    }
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addConditionArguments(CommandNode<ServerCommandSource> root, LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, boolean positive, CommandRegistryAccess commandRegistryAccess) {
    argumentBuilder
        .then(literal("block")
            .then(argument("pos", BlockPosArgumentType.blockPos())
                // note the block predicate type is in this mod
                .then(addConditionLogic(root, argument("block", BlockPredicateArgumentType.blockPredicate(commandRegistryAccess)), positive, context -> BlockPredicateArgumentType.getBlockPredicate(context, "block").test(new CachedBlockPosition(context.getSource().getWorld(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), true))))))
        .then(literal("biome")
            .then(argument("pos", BlockPosArgumentType.blockPos())
                .then(addConditionLogic(root, argument("biome", RegistryEntryPredicateArgumentType.registryEntryPredicate(commandRegistryAccess, RegistryKeys.BIOME)), positive, context -> RegistryEntryPredicateArgumentType.getRegistryEntryPredicate(context, "biome", RegistryKeys.BIOME).test(context.getSource().getWorld().getBiome(BlockPosArgumentType.getLoadedBlockPos(context, "pos")))))))
        .then(literal("loaded")
            .then(addConditionLogic(root, argument("pos", BlockPosArgumentType.blockPos()), positive, commandContext -> isLoaded(commandContext.getSource().getWorld(), BlockPosArgumentType.getBlockPos(commandContext, "pos")))))
        .then(literal("dimension")
            .then(addConditionLogic(root, argument("dimension", DimensionArgumentType.dimension()), positive, context -> DimensionArgumentType.getDimensionArgument(context, "dimension") == context.getSource().getWorld())))
        .then(literal("score")
            .then(argument("target", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
                .then(getScoreTargetObjectiveArgument(root, positive))))
        .then(literal("blocks")
            .then(argument("start", BlockPosArgumentType.blockPos())
                .then(argument("end", BlockPosArgumentType.blockPos())
                    .then(argument("destination", BlockPosArgumentType.blockPos())
                        .then(addBlocksConditionLogic(root, literal("all"), positive, false))
                        .then(addBlocksConditionLogic(root, literal("masked"), positive, true))))))
        .then(literal("entity")
            .then(argument("entities", EntityArgumentType.entities()).fork(root, context -> getSourceOrEmptyForConditionFork(context, positive, !EntityArgumentType.getOptionalEntities(context, "entities").isEmpty())).executes(getExistsConditionExecute(positive, context -> EntityArgumentType.getOptionalEntities(context, "entities").size()))))
        .then(literal("predicate")
            .then(addConditionLogic(root, argument("predicate", IdentifierArgumentType.identifier()).suggests(LOOT_CONDITIONS), positive, context -> testLootCondition(context.getSource(), IdentifierArgumentType.getPredicateArgument(context, "predicate")))));

    for (DataCommand.ObjectType objectType : DataCommand.SOURCE_OBJECT_TYPES) {
      argumentBuilder
          .then(objectType.addArgumentsToBuilder(literal("data"), builder -> builder
              .then(argument("path", NbtPathArgumentType.nbtPath()).fork(root, context -> getSourceOrEmptyForConditionFork(context, positive, countPathMatches(objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path")) > 0)).executes(getExistsConditionExecute(positive, context -> countPathMatches(objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path")))))));
    }
    addExtraConditionArguments(root, argumentBuilder, positive, commandRegistryAccess);

    return argumentBuilder;
  }


  public static LiteralArgumentBuilder<ServerCommandSource> addExtraConditionArguments(CommandNode<ServerCommandSource> root, LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, boolean positive, CommandRegistryAccess commandRegistryAccess) {
    return argumentBuilder
        .then(literal("rand")
            .then(addConditionLogic(root, argument("probability", FloatArgumentType.floatArg(0, 1)), positive, context -> RandomUtils.nextFloat(0, 1) < FloatArgumentType.getFloat(context, "probability"))));
  }

  private static RequiredArgumentBuilder<ServerCommandSource, String> getScoreTargetObjectiveArgument(CommandNode<ServerCommandSource> root, boolean positive) {
    return argument("targetObjective", ScoreboardObjectiveArgumentType.scoreboardObjective())
        .then(literal("=")
            .then(argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
                .then(addConditionLogic(root, argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> testScoreCondition(context, Integer::equals)))))
        .then(literal("<")
            .then(argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
                .then(addConditionLogic(root, argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> testScoreCondition(context, (a, b) -> a < b)))))
        .then(literal("<=")
            .then(argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
                .then(addConditionLogic(root, argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> testScoreCondition(context, (a, b) -> a <= b)))))
        .then(literal(">")
            .then(argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
                .then(addConditionLogic(root, argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> testScoreCondition(context, (a, b) -> a > b)))))
        .then(literal(">=")
            .then(argument("source", ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
                .then(addConditionLogic(root, argument("sourceObjective", ScoreboardObjectiveArgumentType.scoreboardObjective()), positive, context -> testScoreCondition(context, (a, b) -> a >= b)))))
        .then(literal("matches")
            .then(addConditionLogic(root, argument("range", NumberRangeArgumentType.intRange()), positive, context -> testScoreMatch(context, NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(context, "range")))));
  }

  private static Command<ServerCommandSource> getExistsConditionExecute(boolean positive, ExistsCondition condition) {
    return positive ? context -> {
      int i = condition.test(context);
      if (i > 0) {
        context.getSource().sendFeedback(Text.translatable("commands.execute.conditional.pass_count", i), false);
        return i;
      } else {
        throw CONDITIONAL_FAIL_EXCEPTION.create();
      }
    } : context -> {
      int i = condition.test(context);
      if (i == 0) {
        context.getSource().sendFeedback(Text.translatable("commands.execute.conditional.pass"), false);
        return 1;
      } else {
        throw CONDITIONAL_FAIL_COUNT_EXCEPTION.create(i);
      }
    };
  }

  private static int countPathMatches(DataCommandObject object, NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
    return path.count(object.getNbt());
  }

  private static boolean testScoreCondition(CommandContext<ServerCommandSource> context, BiPredicate<Integer, Integer> condition) throws CommandSyntaxException {
    String string = ScoreHolderArgumentType.getScoreHolder(context, "target");
    ScoreboardObjective scoreboardObjective = ScoreboardObjectiveArgumentType.getObjective(context, "targetObjective");
    String string2 = ScoreHolderArgumentType.getScoreHolder(context, "source");
    ScoreboardObjective scoreboardObjective2 = ScoreboardObjectiveArgumentType.getObjective(context, "sourceObjective");
    Scoreboard scoreboard = context.getSource().getServer().getScoreboard();
    if (scoreboard.playerHasObjective(string, scoreboardObjective) && scoreboard.playerHasObjective(string2, scoreboardObjective2)) {
      ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(string, scoreboardObjective);
      ScoreboardPlayerScore scoreboardPlayerScore2 = scoreboard.getPlayerScore(string2, scoreboardObjective2);
      return condition.test(scoreboardPlayerScore.getScore(), scoreboardPlayerScore2.getScore());
    } else {
      return false;
    }
  }

  private static boolean testScoreMatch(CommandContext<ServerCommandSource> context, NumberRange.IntRange range) throws CommandSyntaxException {
    String string = ScoreHolderArgumentType.getScoreHolder(context, "target");
    ScoreboardObjective scoreboardObjective = ScoreboardObjectiveArgumentType.getObjective(context, "targetObjective");
    Scoreboard scoreboard = context.getSource().getServer().getScoreboard();
    return scoreboard.playerHasObjective(string, scoreboardObjective) && range.test(scoreboard.getPlayerScore(string, scoreboardObjective).getScore());
  }

  private static boolean testLootCondition(ServerCommandSource source, LootCondition condition) {
    ServerWorld serverWorld = source.getWorld();
    LootContext.Builder builder = new LootContext.Builder(serverWorld).parameter(LootContextParameters.ORIGIN, source.getPosition()).optionalParameter(LootContextParameters.THIS_ENTITY, source.getEntity());
    return condition.test(builder.build(LootContextTypes.COMMAND));
  }

  private static Collection<ServerCommandSource> getSourceOrEmptyForConditionFork(CommandContext<ServerCommandSource> context, boolean positive, boolean value) {
    return value == positive ? Collections.singleton(context.getSource()) : Collections.emptyList();
  }

  private static ArgumentBuilder<ServerCommandSource, ?> addConditionLogic(CommandNode<ServerCommandSource> root, ArgumentBuilder<ServerCommandSource, ?> builder, boolean positive, Condition condition) {
    return builder.fork(root, context -> getSourceOrEmptyForConditionFork(context, positive, condition.test(context))).executes(context -> {
      if (positive == condition.test(context)) {
        context.getSource().sendFeedback(Text.translatable("commands.execute.conditional.pass"), false);
        return 1;
      } else {
        throw CONDITIONAL_FAIL_EXCEPTION.create();
      }
    });
  }

  private static ArgumentBuilder<ServerCommandSource, ?> addBlocksConditionLogic(CommandNode<ServerCommandSource> root, ArgumentBuilder<ServerCommandSource, ?> builder, boolean positive, boolean masked) {
    return builder.fork(root, context -> getSourceOrEmptyForConditionFork(context, positive, testBlocksCondition(context, masked).isPresent())).executes(positive ? context -> executePositiveBlockCondition(context, masked) : context -> executeNegativeBlockCondition(context, masked));
  }

  private static int executePositiveBlockCondition(CommandContext<ServerCommandSource> context, boolean masked) throws CommandSyntaxException {
    OptionalInt optionalInt = testBlocksCondition(context, masked);
    if (optionalInt.isPresent()) {
      context.getSource().sendFeedback(Text.translatable("commands.execute.conditional.pass_count", optionalInt.getAsInt()), false);
      return optionalInt.getAsInt();
    } else {
      throw CONDITIONAL_FAIL_EXCEPTION.create();
    }
  }

  private static int executeNegativeBlockCondition(CommandContext<ServerCommandSource> context, boolean masked) throws CommandSyntaxException {
    OptionalInt optionalInt = testBlocksCondition(context, masked);
    if (optionalInt.isPresent()) {
      throw CONDITIONAL_FAIL_COUNT_EXCEPTION.create(optionalInt.getAsInt());
    } else {
      context.getSource().sendFeedback(Text.translatable("commands.execute.conditional.pass"), false);
      return 1;
    }
  }

  private static OptionalInt testBlocksCondition(CommandContext<ServerCommandSource> context, boolean masked) throws CommandSyntaxException {
    return testBlocksCondition(context.getSource().getWorld(), BlockPosArgumentType.getLoadedBlockPos(context, "start"), BlockPosArgumentType.getLoadedBlockPos(context, "end"), BlockPosArgumentType.getLoadedBlockPos(context, "destination"), masked);
  }

  private static OptionalInt testBlocksCondition(ServerWorld world, BlockPos start, BlockPos end, BlockPos destination, boolean masked) throws CommandSyntaxException {
    BlockBox blockBox = BlockBox.create(start, end);
    BlockBox blockBox2 = BlockBox.create(destination, destination.add(blockBox.getDimensions()));
    BlockPos blockPos = new BlockPos(blockBox2.getMinX() - blockBox.getMinX(), blockBox2.getMinY() - blockBox.getMinY(), blockBox2.getMinZ() - blockBox.getMinZ());
    int i = blockBox.getBlockCountX() * blockBox.getBlockCountY() * blockBox.getBlockCountZ();
    if (i > 32768) {
      throw BLOCKS_TOOBIG_EXCEPTION.create(32768, i);
    } else {
      int j = 0;

      for (int k = blockBox.getMinZ(); k <= blockBox.getMaxZ(); ++k) {
        for (int l = blockBox.getMinY(); l <= blockBox.getMaxY(); ++l) {
          for (int m = blockBox.getMinX(); m <= blockBox.getMaxX(); ++m) {
            BlockPos blockPos2 = new BlockPos(m, l, k);
            BlockPos blockPos3 = blockPos2.add(blockPos);
            BlockState blockState = world.getBlockState(blockPos2);
            if (!masked || !blockState.isOf(Blocks.AIR)) {
              if (blockState != world.getBlockState(blockPos3)) {
                return OptionalInt.empty();
              }

              BlockEntity blockEntity = world.getBlockEntity(blockPos2);
              BlockEntity blockEntity2 = world.getBlockEntity(blockPos3);
              if (blockEntity != null) {
                if (blockEntity2 == null) {
                  return OptionalInt.empty();
                }

                if (blockEntity2.getType() != blockEntity.getType()) {
                  return OptionalInt.empty();
                }

                NbtCompound nbtCompound = blockEntity.createNbt();
                NbtCompound nbtCompound2 = blockEntity2.createNbt();
                if (!nbtCompound.equals(nbtCompound2)) {
                  return OptionalInt.empty();
                }
              }

              ++j;
            }
          }
        }
      }

      return OptionalInt.of(j);
    }
  }

  private static RedirectModifier<ServerCommandSource> createEntityModifier(Function<Entity, Optional<Entity>> function) {
    return context -> {
      ServerCommandSource serverCommandSource = context.getSource();
      Entity entity = serverCommandSource.getEntity();
      return entity == null ? List.of() : function.apply(entity).filter(entityx -> !entityx.isRemoved()).map(entityx -> List.of(serverCommandSource.withEntity(entityx))).orElse(List.of());
    };
  }

  private static RedirectModifier<ServerCommandSource> createMultiEntityModifier(Function<Entity, Stream<Entity>> function) {
    return context -> {
      ServerCommandSource serverCommandSource = context.getSource();
      Entity entity = serverCommandSource.getEntity();
      return entity == null ? List.of() : function.apply(entity).filter(entityx -> !entityx.isRemoved()).map(serverCommandSource::withEntity).toList();
    };
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addOnArguments(CommandNode<ServerCommandSource> node, LiteralArgumentBuilder<ServerCommandSource> builder) {
    return builder
        .then(literal("owner").fork(node, createEntityModifier(entity -> entity instanceof Tameable tameable ? Optional.ofNullable(tameable.getOwner()) : Optional.empty())))
        .then(literal("leasher").fork(node, createEntityModifier(entity -> entity instanceof MobEntity mobEntity ? Optional.ofNullable(mobEntity.getHoldingEntity()) : Optional.empty())))
        .then(literal("target").fork(node, createEntityModifier(entity -> entity instanceof Targeter targeter ? Optional.ofNullable(targeter.getTarget()) : Optional.empty())))
        .then(literal("attacker").fork(node, createEntityModifier(entity -> entity instanceof Attackable attackable ? Optional.ofNullable(attackable.getLastAttacker()) : Optional.empty())))
        .then(literal("vehicle").fork(node, createEntityModifier(entity -> Optional.ofNullable(entity.getVehicle()))))
        .then(literal("controller").fork(node, createEntityModifier(entity -> Optional.ofNullable(entity.getControllingPassenger()))))
        .then(literal("origin").fork(node, createEntityModifier(entity -> entity instanceof Ownable ownable ? Optional.ofNullable(ownable.getOwner()) : Optional.empty())))
        .then(literal("passengers").fork(node, createMultiEntityModifier(entity -> entity.getPassengerList().stream())));
  }

  private static ServerCommandSource summon(ServerCommandSource source, RegistryEntry.Reference<EntityType<?>> entityType) throws CommandSyntaxException {
    Entity entity = SummonCommand.summon(source, entityType, source.getPosition(), new NbtCompound(), true);
    return source.withEntity(entity);
  }

  @FunctionalInterface
  interface Condition {
    boolean test(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
  }

  @FunctionalInterface
  interface ExistsCondition {
    int test(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
  }
}
