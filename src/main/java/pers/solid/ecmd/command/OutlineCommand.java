package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
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
import pers.solid.ecmd.region.OutlineRegionArgument;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.enums.CommandEnumType;
import pers.solid.ecmd.util.enums.OutlineType;

import java.util.Collections;

import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum OutlineCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType kwArgsType = KeywordArgsArgumentType.builderFromShared(KeywordArgsCommon.FILLING, registryAccess)
        .addOptionalArg("inner", BlockFunctionArgumentType.blockFunction(registryAccess), null)
        .build();

    final ArgumentCommandNode<ServerCommandSource, ?> outlineTypeArgumentNode;
    ModCommands.registerWithRegionArgumentModification(
        dispatcher,
        literalR2("outline"),
        literalR2("/outline"),
        CommandManager.argument("region", RegionArgumentType.region(registryAccess))
            .then(CommandManager.argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
                .executes(context -> executeWithDefaultKeywordArgs(context, OutlineType.OUTLINE))
                .then(outlineTypeArgumentNode = CommandManager.argument("outline_type", SimpleEnumArgumentType.simpleEnum(CommandEnumType.OUTLINE_TYPE))
                    .executes(context -> executeWithDefaultKeywordArgs(context, context.getArgument("outline_type", OutlineType.class)))
                    .then(CommandManager.argument("keyword_args", kwArgsType)
                        .executes(context -> executeFromKeywordArgs(context, context.getArgument("outline_type", OutlineType.class), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args")))).build())));
    ModCommands.registerWithRegionArgumentModification(
        dispatcher, literalR2("wall"),
        literalR2("/wall"),
        CommandManager.argument("region", RegionArgumentType.region(registryAccess)).then(
            CommandManager.argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
                .executes(context -> executeWithDefaultKeywordArgs(context, OutlineType.WALL))
                .forward(outlineTypeArgumentNode, context -> {
                  final ServerCommandSource source = context.getSource();
                  source.addExtraArgument$ec("outline_type", OutlineType.WALL);
                  return Collections.singleton(source);
                }, false)));
  }

  public static int executeWithDefaultKeywordArgs(CommandContext<ServerCommandSource> context, OutlineType outlineType) throws CommandSyntaxException {
    return FillReplaceCommand.setBlocksWithDefaultKeywordArgs(OutlineRegion.of(RegionArgumentType.getRegion(context, "region"), outlineType), BlockFunctionArgumentType.getBlockFunction(context, "block"), context.getSource(), null);
  }

  public static int executeFromKeywordArgs(CommandContext<ServerCommandSource> context, OutlineType outlineType, KeywordArgs keywordArgs) throws CommandSyntaxException {
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
