package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.BlockTransformationCommand;
import pers.solid.ecmd.mixin.CommandContextAccessor;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionbuilder.RegionBuilder;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.function.Function;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.server.command.CommandManager.argument;
import static pers.solid.ecmd.argument.KeywordArgsArgumentType.getKeywordArgs;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum MoveCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType keywordArgs = BlockTransformationCommand.createKeywordArgs(registryAccess)
        .build();

    ModCommands.registerWithArgumentModification(dispatcher,
        literalR2("move"),
        literalR2("/move")
            .executes(context -> {
              ((CommandContextAccessor<?>) context).getArguments().put("region", new ParsedArgument<>(0, 0, ((ServerPlayerEntityExtension) context.getSource().getPlayerOrThrow()).ec$getOrEvaluateActiveRegionOrThrow(context.getSource())));
              return executeMove(Either.left(ObjectIntPair.of(DirectionArgument.FRONT.apply(context.getSource()), 1)), keywordArgs.defaultArgs(), context);
            }),
        argument("region", RegionArgumentType.region(registryAccess))
            .executes(context -> executeMove(Either.left(ObjectIntPair.of(DirectionArgument.FRONT.apply(context.getSource()), 1)), keywordArgs.defaultArgs(), context))
            .then(argument("offset", IntegerArgumentType.integer())
                .executes(context -> executeMoveFromDirection(DirectionArgument.FRONT.apply(context.getSource()), getInteger(context, "offset"), keywordArgs.defaultArgs(), context))
                .then(argument("direction", DirectionArgumentType.direction())
                    .executes(context -> executeMoveFromDirection(DirectionArgumentType.getDirection(context, "direction"), getInteger(context, "offset"), keywordArgs.defaultArgs(), context))
                    .then(argument("keyword_args", keywordArgs)
                        .executes(context -> executeMoveFromDirection(DirectionArgumentType.getDirection(context, "direction"), getInteger(context, "offset"), getKeywordArgs(context, "keyword_args"), context)))))
//            .then(argument("direction", DirectionArgumentType.direction())
//                .executes(context -> executeMoveFromDirection(DirectionArgumentType.getDirection(context, "direction"), 1, keywordArgs.defaultArgs(), context))
//                .then(argument("keyword_args", keywordArgs)
//                    .executes(context -> executeMove(Either.left(ObjectIntPair.of(DirectionArgumentType.getDirection(context, "direction"), 1)), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context))))
            .then(argument("x", IntegerArgumentType.integer())
                .then(argument("y", IntegerArgumentType.integer())
                    .then(argument("z", IntegerArgumentType.integer())
                        .executes(context -> executeMoveFromVectorArgs(keywordArgs.defaultArgs(), context))
                        .then(argument("keyword_args", keywordArgs)
                            .executes(context -> executeMoveFromVectorArgs(getKeywordArgs(context, "keyword_args"), context)))))),
        ModCommands.REGION_ARGUMENTS_MODIFIER);
  }

  public static int executeMoveFromDirection(Direction direction, int offset, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMove(Either.left(ObjectIntPair.of(direction, offset)), keywordArgs, context);
  }

  public static int executeMoveFromVectorArgs(KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMove(Either.right(new Vec3i(getInteger(context, "x"), getInteger(context, "y"), getInteger(context, "z"))), keywordArgs, context);
  }

  public static int executeMove(Either<ObjectIntPair<Direction>, Vec3i> relativePos, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final Vec3i relativeVector = relativePos.map(pair -> Vec3i.ZERO.offset(pair.left(), pair.rightInt()), Function.identity());
    final BlockTransformationCommand blockTransformationCommand = new BlockTransformationCommand() {
      @Override
      public Vec3i transformBlockPos(Vec3i original) {
        return original.add(relativeVector);
      }

      @Override
      public Vec3d transformPos(Vec3d original) {
        return original.add(relativeVector.getX(), relativeVector.getY(), relativeVector.getZ());
      }

      @Override
      public Vec3d transformPosBack(Vec3d transformed) {
        return transformed.subtract(relativeVector.getX(), relativeVector.getY(), relativeVector.getZ());
      }

      @Override
      public void transformEntity(Entity entity) {
      }

      @Override
      public BlockState transformBlockState(BlockState original) {
        return original;
      }

      @Override
      public Region transformRegion(Region region) {
        return region.moved(relativeVector);
      }

      @Override
      public void transformRegionBuilder(RegionBuilder regionBuilder) {
        regionBuilder.move(relativeVector);
      }

      @Override
      public void notifyCompletion(ServerCommandSource source, int affectedNum) {
        CommandBridge.sendFeedback(source, () -> relativePos.map(pair -> TextUtil.enhancedTranslatable("enhancedCommands.commands.move.complete.direction", Integer.toString(pair.rightInt()), TextUtil.wrapDirection(pair.left()), Integer.toString(affectedNum)), vec3i -> TextUtil.enhancedTranslatable("enhancedCommands.commands.move.complete.vector", TextUtil.wrapVector(vec3i), Integer.toString(affectedNum))), true);
      }

      @Override
      public @NotNull MutableText getIteratorTaskName(Region region) {
        return relativePos.map(pair -> Text.translatable("enhancedCommands.commands.move.task.direction", region.asString(), Integer.toString(pair.rightInt()), TextUtil.wrapDirection(pair.left())), vec3i -> Text.translatable("enhancedCommands.commands.move.task.vector", region.asString(), TextUtil.wrapVector(vec3i)));
      }
    };

    return blockTransformationCommand.execute(RegionArgumentType.getRegion(context, "region"), keywordArgs, context);
  }
}