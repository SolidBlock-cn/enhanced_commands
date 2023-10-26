package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.ConditionalBlockFunction;
import pers.solid.ecmd.predicate.block.RegionBlockPredicate;
import pers.solid.ecmd.region.OutlineRegion;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.mixin.EnhancedRedirectModifier;

import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum OutlineCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType kwArgsType = KeywordArgsArgumentType.builder(FillReplaceCommand.KEYWORD_ARGS)
        .addOptionalArg("inner", BlockFunctionArgumentType.blockFunction(registryAccess), null)
        .build();

    final ArgumentCommandNode<ServerCommandSource, ?> outlineTypeArgumentNode;
    ModCommands.registerWithRegionArgumentModification(dispatcher, registryAccess, literalR2("outline"), literalR2("/outline"), CommandManager.argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
        .executes(context1 -> executeWithDefaultKeywordArgs(context1, OutlineRegion.OutlineTypes.OUTLINE))
        .then(outlineTypeArgumentNode = CommandManager.argument("outline_type", new SimpleEnumArgumentTypes.OutlineTypeArgumentType())
            .executes(context1 -> executeWithDefaultKeywordArgs(context1, context1.getArgument("outline_type", OutlineRegion.OutlineTypes.class)))
            .then(CommandManager.argument("keyword_args", kwArgsType)
                .executes(context1 -> executeFromKeywordArgs(context1, context1.getArgument("outline_type", OutlineRegion.OutlineTypes.class), KeywordArgsArgumentType.getKeywordArgs(context1, "keyword_args")))).build()));
    ModCommands.registerWithRegionArgumentModification(dispatcher, registryAccess, literalR2("wall"), literalR2("/wall"), CommandManager.argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
        .executes(context -> executeWithDefaultKeywordArgs(context, OutlineRegion.OutlineTypes.WALL))
        .forward(outlineTypeArgumentNode, (EnhancedRedirectModifier.Constant<ServerCommandSource>) (args, previousArguments, source) -> {
          args.putAll(previousArguments);
          args.put("outline_type", new ParsedArgument<>(0, 0, OutlineRegion.OutlineTypes.WALL));
        }, false));
  }

  public static int executeWithDefaultKeywordArgs(CommandContext<ServerCommandSource> context, OutlineRegion.OutlineTypes outlineType) throws CommandSyntaxException {
    return FillReplaceCommand.setBlocksWithDefaultKeywordArgs(OutlineRegion.of(RegionArgumentType.getRegion(context, "region"), outlineType), BlockFunctionArgumentType.getBlockFunction(context, "block"), context.getSource(), null);
  }

  public static int executeFromKeywordArgs(CommandContext<ServerCommandSource> context, OutlineRegion.OutlineTypes outlineType, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final @Nullable BlockFunction inner = keywordArgs.getArg("inner");
    final Region region = RegionArgumentType.getRegion(context, "region");
    final Region outlineRegion = OutlineRegion.of(region, outlineType);
    final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block");
    if (inner == null) {
      return FillReplaceCommand.setBlocksFromKeywordArgs(outlineRegion, blockFunction, context.getSource(), null, keywordArgs);
    } else {
      return FillReplaceCommand.setBlocksFromKeywordArgs(region, new ConditionalBlockFunction(new RegionBlockPredicate(outlineRegion), blockFunction, inner), context.getSource(), null, keywordArgs);
    }
  }
}
