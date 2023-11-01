package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
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
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionbuilder.RegionBuilder;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.function.Function;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static pers.solid.ecmd.argument.DirectionArgumentType.getDirection;
import static pers.solid.ecmd.argument.KeywordArgsArgumentType.getKeywordArgs;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum MoveCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType keywordArgs = BlockTransformationCommand.createKeywordArgs(registryAccess)
        .build();

    ModCommands.registerWithRegionArgumentModificationDefaults(
        dispatcher,
        registryAccess,
        literalR2("move"),
        literalR2("/move"),
        argument("offset", integer())
            .executes(context -> executeMoveFromDirection(DirectionArgument.FRONT.apply(context.getSource()), getInteger(context, "offset"), keywordArgs.defaultArgs(), context))
            .then(argument("direction", DirectionArgumentType.direction())
                .executes(context -> executeMoveFromDirection(getDirection(context, "direction"), getInteger(context, "offset"), keywordArgs.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgs)
                    .executes(context -> executeMoveFromDirection(getDirection(context, "direction"), getInteger(context, "offset"), getKeywordArgs(context, "keyword_args"), context))))
            .then(argument("keyword_args", keywordArgs)
                .executes(context -> executeMoveFromDirection(DirectionArgument.FRONT.apply(context.getSource()), getInteger(context, "offset"), getKeywordArgs(context, "keyword_args"), context)))
            .build(),
        context -> executeMove(Either.left(ObjectIntPair.of(DirectionArgument.FRONT.apply(context.getSource()), 1)), keywordArgs.defaultArgs(), context)
    );
    ModCommands.registerWithRegionArgumentModification(dispatcher, registryAccess,
        literalR2("move"),
        literalR2("/move"),
        argument("x", integer())
            .then(argument("y", integer())
                .then(argument("z", integer())
                    .executes(context -> executeMoveFromVectorArgs(keywordArgs.defaultArgs(), context))
                    .then(argument("keyword_args", keywordArgs)
                        .executes(context -> executeMoveFromVectorArgs(getKeywordArgs(context, "keyword_args"), context))))));
    ModCommands.registerWithRegionArgumentModification(dispatcher, registryAccess,
        literalR2("move"),
        literalR2("/move"),
        argument("direction", DirectionArgumentType.direction())
            .executes(context -> executeMove(Either.left(ObjectIntPair.of(getDirection(context, "direction"), 1)), keywordArgs.defaultArgs(), context))
            .then(argument("keyword_args", keywordArgs)
                .executes(context -> executeMove(Either.left(ObjectIntPair.of(getDirection(context, "direction"), 1)), getKeywordArgs(context, "keyword_args"), context)))
    );
    ModCommands.registerWithRegionArgumentModification(dispatcher, registryAccess,
        literalR2("move"),
        literalR2("/move"),
        argument("keyword_args", keywordArgs)
            .executes(context -> executeMoveFromDirection(DirectionArgument.FRONT.apply(context.getSource()), 1, getKeywordArgs(context, "keyword_args"), context)));
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
      public void notifyCompletion(ServerCommandSource source, int affectedBlocks, int affectedEntities) {
        if (affectedEntities == -1) {
          CommandBridge.sendFeedback(source, () -> relativePos.map(pair -> TextUtil.enhancedTranslatable("enhancedCommands.commands.move.complete.direction", Integer.toString(pair.rightInt()), TextUtil.wrapDirection(pair.left()), Integer.toString(affectedBlocks)), vec3i -> TextUtil.enhancedTranslatable("enhancedCommands.commands.move.complete.vector", TextUtil.wrapVector(vec3i), Integer.toString(affectedBlocks))), true);
        } else {
          CommandBridge.sendFeedback(source, () -> relativePos.map(pair -> TextUtil.enhancedTranslatable("enhancedCommands.commands.move.complete_with_entities.direction", Integer.toString(pair.rightInt()), TextUtil.wrapDirection(pair.left()), Integer.toString(affectedBlocks), Integer.toString(affectedEntities)), vec3i -> TextUtil.enhancedTranslatable("enhancedCommands.commands.move.complete_with_entities.vector", TextUtil.wrapVector(vec3i), Integer.toString(affectedBlocks), Integer.toString(affectedEntities))), true);
        }
      }

      @Override
      public @NotNull MutableText getIteratorTaskName(Region region) {
        return relativePos.map(pair -> Text.translatable("enhancedCommands.commands.move.task.direction", region.asString(), Integer.toString(pair.rightInt()), TextUtil.wrapDirection(pair.left())), vec3i -> Text.translatable("enhancedCommands.commands.move.task.vector", region.asString(), TextUtil.wrapVector(vec3i)));
      }
    };

    return blockTransformationCommand.execute(RegionArgumentType.getRegion(context, "region"), keywordArgs, context);
  }
}
