package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.ConditionalBlockFunction;
import pers.solid.ecmd.predicate.block.RegionBlockPredicate;
import pers.solid.ecmd.region.OutlineRegion;
import pers.solid.ecmd.region.OutlineRegionArgument;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.enums.CommandEnumType;
import pers.solid.ecmd.util.enums.OutlineType;

import java.util.Collections;

import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum OutlineCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final KeywordArgsArgumentType kwArgsType = KeywordArgsArgumentType.builderFromShared(KeywordArgsCommon.FILLING, commandBuildContext)
        .addOptionalArg("inner", BlockFunctionArgumentType.blockFunction(commandBuildContext), null)
        .build();

    final ArgumentCommandNode<CommandSourceStack, ?> outlineTypeArgumentNode;
    ModCommands.registerWithRegionArgumentModification(
        dispatcher,
        literalR2("outline"),
        literalR2("/outline"),
        Commands.argument("region", RegionArgumentType.region(commandBuildContext))
            .then(Commands.argument("block", BlockFunctionArgumentType.blockFunction(commandBuildContext))
                .executes(context -> executeWithDefaultKeywordArgs(context, OutlineType.OUTLINE))
                .then(outlineTypeArgumentNode = Commands.argument("outline_type", SimpleEnumArgumentType.simpleEnum(CommandEnumType.OUTLINE_TYPE))
                    .executes(context -> executeWithDefaultKeywordArgs(context, context.getArgument("outline_type", OutlineType.class)))
                    .then(Commands.argument("keyword_args", kwArgsType)
                        .executes(context -> executeFromKeywordArgs(context, context.getArgument("outline_type", OutlineType.class), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args")))).build())));
    ModCommands.registerWithRegionArgumentModification(
        dispatcher, literalR2("wall"),
        literalR2("/wall"),
        Commands.argument("region", RegionArgumentType.region(commandBuildContext)).then(
            Commands.argument("block", BlockFunctionArgumentType.blockFunction(commandBuildContext))
                .executes(context -> executeWithDefaultKeywordArgs(context, OutlineType.WALL))
                .forward(outlineTypeArgumentNode, context -> {
                  final CommandSourceStack source = context.getSource();
                  source.addExtraArgument$ec("outline_type", OutlineType.WALL);
                  return Collections.singleton(source);
                }, false)));
  }

  public static int executeWithDefaultKeywordArgs(CommandContext<CommandSourceStack> context, OutlineType outlineType) throws CommandSyntaxException {
    return FillReplaceCommand.setBlocksWithDefaultKeywordArgs(OutlineRegion.of(RegionArgumentType.getRegion(context, "region"), outlineType), BlockFunctionArgumentType.getBlockFunction(context, "block"), context.getSource(), null);
  }

  public static int executeFromKeywordArgs(CommandContext<CommandSourceStack> context, OutlineType outlineType, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final @Nullable BlockFunction inner = keywordArgs.getArg("inner");
    final RegionArgument<?> region = RegionArgumentType.getRegionArgument(context, "region");
    final RegionArgument<?> outlineRegion = new OutlineRegionArgument(outlineType, region);
    final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block");
    if (inner == null) {
      return FillReplaceCommand.setBlocksFromKeywordArgs(outlineRegion.toAbsoluteRegion(context.getSource()), blockFunction, context.getSource(), null, keywordArgs);
    } else {
      return FillReplaceCommand.setBlocksFromKeywordArgs(region.toAbsoluteRegion(context.getSource()), new ConditionalBlockFunction(new RegionBlockPredicate(outlineRegion), blockFunction, inner), context.getSource(), null, keywordArgs);
    }
  }
}
