package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.argument.KeywordArgsCommon;
import pers.solid.ecmd.argument.RegionArgumentType;
import pers.solid.ecmd.function.block.PostProcessBlockFunction;
import pers.solid.ecmd.region.RegionArgument;

import static net.minecraft.server.command.CommandManager.argument;
import static pers.solid.ecmd.argument.RegionArgumentType.region;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum PostProcessCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    LiteralArgumentBuilder<ServerCommandSource> directBuilder = literalR2("postprocess");
    LiteralArgumentBuilder<ServerCommandSource> indirectBuilder = literalR2("/postprocess");
    final KeywordArgsArgumentType keywordArgs = KeywordArgsArgumentType.builderFromShared(KeywordArgsCommon.FILLING, registryAccess).build();
    ModCommands.registerWithRegionArgumentModification(dispatcher, directBuilder, indirectBuilder, argument("region", region(registryAccess))
        .executes(this::executeWithDefaultKeywordArgs)
        .then(argument("keyword_args", keywordArgs)
            .executes(context -> execute(context, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args")))).build());
  }

  private int executeWithDefaultKeywordArgs(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final RegionArgument<?> region = RegionArgumentType.getRegionArgument(context, "region");
    return FillReplaceCommand.setBlocksWithDefaultKeywordArgs(region.toAbsoluteRegion(context.getSource()), new PostProcessBlockFunction(PostProcessBlockFunction.ALL_DIRECTIONS), context.getSource(), null);
  }

  private int execute(CommandContext<ServerCommandSource> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final RegionArgument<?> region = RegionArgumentType.getRegionArgument(context, "region");
    return FillReplaceCommand.setBlocksFromKeywordArgs(region.toAbsoluteRegion(context.getSource()), new PostProcessBlockFunction(PostProcessBlockFunction.ALL_DIRECTIONS), context.getSource(), null, keywordArgs);
  }
}
