package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.function.ConditionalBlockFunction;
import pers.solid.ecmd.block.predicate.RegionBlockPredicate;
import pers.solid.ecmd.region.OutlineRegion;
import pers.solid.ecmd.region.OutlineRegionProvider;
import pers.solid.ecmd.region.RegionProvider;
import pers.solid.ecmd.util.enums.CommandEnumType;
import pers.solid.ecmd.util.enums.OutlineType;

import java.util.Collections;

import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum OutlineCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final KeywordArgsArgument kwArgsType = KeywordArgsArgument.builderFromShared(KeywordArgsCommon.FILLING, commandBuildContext)
        .addOptionalArg("inner", BlockFunctionArgument.blockFunction(commandBuildContext), null)
        .build();

    final ArgumentCommandNode<CommandSourceStack, ?> outlineTypeArgumentNode;
    EnhancedCommandsCommands.registerWithRegionArgumentModification(
        dispatcher,
        literalR2("outline"),
        literalR2("/outline"),
        Commands.argument("region", RegionArgument.region(commandBuildContext))
            .then(Commands.argument("block", BlockFunctionArgument.blockFunction(commandBuildContext))
                .executes(context -> executeWithDefaultKeywordArgs(context, OutlineType.OUTLINE))
                .then(outlineTypeArgumentNode = Commands.argument("outline_type", SimpleEnumArgument.simpleEnum(CommandEnumType.OUTLINE_TYPE))
                    .executes(context -> executeWithDefaultKeywordArgs(context, context.getArgument("outline_type", OutlineType.class)))
                    .then(Commands.argument("keyword_args", kwArgsType)
                        .executes(context -> executeFromKeywordArgs(context, context.getArgument("outline_type", OutlineType.class), KeywordArgsArgument.getKeywordArgs(context, "keyword_args")))).build())));
    EnhancedCommandsCommands.registerWithRegionArgumentModification(
        dispatcher, literalR2("wall"),
        literalR2("/wall"),
        Commands.argument("region", RegionArgument.region(commandBuildContext)).then(
            Commands.argument("block", BlockFunctionArgument.blockFunction(commandBuildContext))
                .executes(context -> executeWithDefaultKeywordArgs(context, OutlineType.WALL))
                .forward(outlineTypeArgumentNode, context -> {
                  final CommandSourceStack source = context.getSource();
                  source.addExtraArgument$ec("outline_type", OutlineType.WALL);
                  return Collections.singleton(source);
                }, false)));
  }

  public static int executeWithDefaultKeywordArgs(CommandContext<CommandSourceStack> context, OutlineType outlineType) throws CommandSyntaxException {
    return FillReplaceCommand.setBlocksWithDefaultKeywordArgs(OutlineRegion.of(RegionArgument.getRegion(context, "region"), outlineType), BlockFunctionArgument.getBlockFunction(context, "block"), context.getSource(), null);
  }

  public static int executeFromKeywordArgs(CommandContext<CommandSourceStack> context, OutlineType outlineType, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final @Nullable BlockFunction inner = keywordArgs.getArg("inner");
    final RegionProvider<?> region = RegionArgument.getRegionProvider(context, "region");
    final RegionProvider<?> outlineRegion = new OutlineRegionProvider(outlineType, region);
    final BlockFunction blockFunction = BlockFunctionArgument.getBlockFunction(context, "block");
    if (inner == null) {
      return FillReplaceCommand.setBlocksFromKeywordArgs(outlineRegion.toAbsoluteRegion(context.getSource()), blockFunction, context.getSource(), null, keywordArgs);
    } else {
      return FillReplaceCommand.setBlocksFromKeywordArgs(region.toAbsoluteRegion(context.getSource()), new ConditionalBlockFunction(new RegionBlockPredicate(outlineRegion), blockFunction, inner), context.getSource(), null, keywordArgs);
    }
  }
}
