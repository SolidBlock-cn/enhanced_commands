package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgument;
import pers.solid.ecmd.argument.KeywordArgsCommon;
import pers.solid.ecmd.argument.RegionArgument;
import pers.solid.ecmd.block.function.PostProcessBlockFunction;
import pers.solid.ecmd.region.RegionProvider;

import static net.minecraft.commands.Commands.argument;
import static pers.solid.ecmd.argument.RegionArgument.region;
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum PostProcessCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    LiteralArgumentBuilder<CommandSourceStack> directBuilder = literalR2("postprocess");
    LiteralArgumentBuilder<CommandSourceStack> indirectBuilder = literalR2("/postprocess");
    final KeywordArgsArgument keywordArgs = KeywordArgsArgument.builderFromShared(KeywordArgsCommon.FILLING, commandBuildContext).build();
    EnhancedCommandsCommands.registerWithRegionArgumentModification(dispatcher, directBuilder, indirectBuilder, argument("region", region(commandBuildContext))
        .executes(this::executeWithDefaultKeywordArgs)
        .then(argument("keyword_args", keywordArgs)
            .executes(context -> execute(context, KeywordArgsArgument.getKeywordArgs(context, "keyword_args")))).build());
  }

  private int executeWithDefaultKeywordArgs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final RegionProvider<?> region = RegionArgument.getRegionProvider(context, "region");
    return FillReplaceCommand.setBlocksWithDefaultKeywordArgs(region.toAbsoluteRegion(context.getSource()), new PostProcessBlockFunction(PostProcessBlockFunction.ALL_DIRECTIONS), context.getSource(), null);
  }

  private int execute(CommandContext<CommandSourceStack> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final RegionProvider<?> region = RegionArgument.getRegionProvider(context, "region");
    return FillReplaceCommand.setBlocksFromKeywordArgs(region.toAbsoluteRegion(context.getSource()), new PostProcessBlockFunction(PostProcessBlockFunction.ALL_DIRECTIONS), context.getSource(), null, keywordArgs);
  }
}
