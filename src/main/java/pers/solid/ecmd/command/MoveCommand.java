package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.BlockTransformationCommand;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Function;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.argument.DirectionArgument.getDirection;
import static pers.solid.ecmd.argument.KeywordArgsArgument.getKeywordArgs;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum MoveCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final KeywordArgsArgument keywordArgs = BlockTransformationCommand.createKeywordArgs(commandBuildContext)
        .build();

    ModCommands.registerWithRegionArgumentModification(
        dispatcher,
        literalR2("move"),
        literalR2("/move"),
        argument("region", RegionArgument.region(commandBuildContext))
            .then(argument("offset", integer())
                .executes(context -> executeMoveFromDirection(DirectionProvider.FRONT.apply(context.getSource()), getInteger(context, "offset"), keywordArgs.defaultArgs(), context))
                .then(argument("direction", DirectionArgument.direction())
                    .executes(context -> executeMoveFromDirection(getDirection(context, "direction"), getInteger(context, "offset"), keywordArgs.defaultArgs(), context))
                    .then(argument("keyword_args", keywordArgs)
                        .executes(context -> executeMoveFromDirection(getDirection(context, "direction"), getInteger(context, "offset"), getKeywordArgs(context, "keyword_args"), context))))
                .then(argument("keyword_args", keywordArgs)
                    .executes(context -> executeMoveFromDirection(DirectionProvider.FRONT.apply(context.getSource()), getInteger(context, "offset"), getKeywordArgs(context, "keyword_args"), context))))
            .then(literal("vector")
                .then(argument("x", integer())
                    .then(argument("y", integer())
                        .then(argument("z", integer())
                            .executes(context -> executeMoveFromVectorArgs(keywordArgs.defaultArgs(), context))
                            .then(argument("keyword_args", keywordArgs)
                                .executes(context -> executeMoveFromVectorArgs(getKeywordArgs(context, "keyword_args"), context)))))))
            .then(argument("direction", DirectionArgument.direction())
                .executes(context -> executeMove(Either.left(ObjectIntPair.of(getDirection(context, "direction"), 1)), keywordArgs.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgs)
                    .executes(context -> executeMove(Either.left(ObjectIntPair.of(getDirection(context, "direction"), 1)), getKeywordArgs(context, "keyword_args"), context))))
            .then(argument("keyword_args", keywordArgs)
                .executes(context -> executeMoveFromDirection(DirectionProvider.FRONT.apply(context.getSource()), 1, getKeywordArgs(context, "keyword_args"), context)))
            .executes(context -> executeMove(Either.left(ObjectIntPair.of(DirectionProvider.FRONT.apply(context.getSource()), 1)), keywordArgs.defaultArgs(), context))
    );
  }

  public static int executeMoveFromDirection(Direction direction, int offset, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeMove(Either.left(ObjectIntPair.of(direction, offset)), keywordArgs, context);
  }

  public static int executeMoveFromVectorArgs(KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeMove(Either.right(new Vec3i(getInteger(context, "x"), getInteger(context, "y"), getInteger(context, "z"))), keywordArgs, context);
  }

  public static int executeMove(Either<ObjectIntPair<Direction>, Vec3i> relativePos, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final Vec3i relativeVector = relativePos.map(pair -> Vec3i.ZERO.relative(pair.left(), pair.rightInt()), Function.identity());
    final BlockTransformationCommand blockTransformationCommand = new BlockTransformationCommand() {
      @Override
      public Vec3i transformBlockPos(Vec3i original) {
        return original.offset(relativeVector);
      }

      @Override
      public Vec3 transformPos(Vec3 original) {
        return original.add(relativeVector.getX(), relativeVector.getY(), relativeVector.getZ());
      }

      @Override
      public Vec3 transformPosBack(Vec3 transformed) {
        return transformed.subtract(relativeVector.getX(), relativeVector.getY(), relativeVector.getZ());
      }

      @Override
      public void transformEntity(@NotNull Entity entity) {
      }

      @Override
      public void transformEntityBack(@NotNull Entity entity) {
      }

      @Override
      public @NotNull BlockState transformBlockState(@NotNull BlockState original) {
        return original;
      }

      @Override
      public @NotNull Region transformRegion(@NotNull Region region) {
        return region.moved(relativeVector);
      }

      @Override
      public void notifyCompletion(CommandSourceStack source, int affectedBlocks, int affectedEntities) {
        if (affectedEntities == -1) {
          source.sendFeedback$ecBridge(() -> relativePos.map(pair -> Component.translatable("enhanced_commands.commands.move.complete.direction", Integer.toString(pair.rightInt()), TextUtil.wrapDirection(pair.left()), Integer.toString(affectedBlocks)).enhanced$$(), vec3i -> Component.translatable("enhanced_commands.commands.move.complete.vector", TextUtil.wrapVector(vec3i), Integer.toString(affectedBlocks)).enhanced$$()), true);
        } else {
          source.sendFeedback$ecBridge(() -> relativePos.map(pair -> Component.translatable("enhanced_commands.commands.move.complete_with_entities.direction", Integer.toString(pair.rightInt()), TextUtil.wrapDirection(pair.left()), Integer.toString(affectedBlocks), Integer.toString(affectedEntities)).enhanced$$(), vec3i -> Component.translatable("enhanced_commands.commands.move.complete_with_entities.vector", TextUtil.wrapVector(vec3i), Integer.toString(affectedBlocks), Integer.toString(affectedEntities)).enhanced$$()), true);
        }
      }

      @Override
      public @NotNull MutableComponent getIteratorTaskName(Region region) {
        return relativePos.map(pair -> Component.translatable("enhanced_commands.commands.move.task.direction", region.asString(), Integer.toString(pair.rightInt()), TextUtil.wrapDirection(pair.left())), vec3i -> Component.translatable("enhanced_commands.commands.move.task.vector", region.asString(), TextUtil.wrapVector(vec3i)));
      }
    };

    return blockTransformationCommand.execute(RegionArgument.getRegion(context, "region"), keywordArgs, context);
  }
}
