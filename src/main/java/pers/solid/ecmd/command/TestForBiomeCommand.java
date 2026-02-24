package pers.solid.ecmd.command;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.ecmd.argument.EnhancedEntryPredicate;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.KeywordArgsArgument;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.ResourceOrTagArgument.resourceOrTag;
import static pers.solid.ecmd.argument.KeywordArgsArgument.getKeywordArgs;

public enum TestForBiomeCommand implements TestForCommands.Entry {
  INSTANCE;
  public static final KeywordArgsArgument BIOME_KEYWORD_ARGS = KeywordArgsArgument.builder()
      .addOptionalArg("force_load", BoolArgumentType.bool(), false)
      .build();
  public static final DynamicCommandExceptionType TEST_FOR_BIOME_NOT_LOADED = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.commands.testfor.biome.not_loaded", o));
  public static final DynamicCommandExceptionType TEST_FOR_BIOME_PREDICATE_NOT_LOADED = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.commands.testfor.biome.not_loaded_for_predicate", o));

  @Override
  public void addArguments(LiteralArgumentBuilder<CommandSourceStack> testForBuilder, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    testForBuilder.then(literal("biome")
        .executes(context -> executeTestForBiome(context, BlockPos.containing(context.getSource().getPosition())))
        .then(argument("pos", EnhancedPosArgument.blockPos())
            .executes(context -> executeTestForBiome(context, EnhancedPosArgument.getBlockPos(context, "pos")))
            .then(argument("biome", resourceOrTag(commandBuildContext, Registries.BIOME))
                .executes(context -> executeTestForBiomePredicate(context, false))
                .then(argument("keyword_args", BIOME_KEYWORD_ARGS)
                    .executes(context -> executeTestForBiomePredicate(context, getKeywordArgs(context, "keyword_args").getBoolean("force_load")))))));
  }

  private static int executeTestForBiome(CommandContext<CommandSourceStack> context, BlockPos blockPos) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final ServerLevel world = source.getLevel();
    @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(blockPos);
    if (!chunkLoaded) {
      throw TEST_FOR_BIOME_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final Holder<Biome> biome = world.getBiome(blockPos);
    source.sendFeedback$ecBridge(() -> biome.unwrap().map(key -> Component.translatable("enhanced_commands.commands.testfor.biome.info", TextUtil.wrapVector(blockPos), TextUtil.biome(key).withStyle(Styles.RESULT), TextUtil.literal(key.location()).withStyle(Styles.RESULT)), value -> Component.translatable("enhanced_commands.commands.testfor.biome.info_unregistered", TextUtil.wrapVector(blockPos), Component.literal(value.toString()).withStyle(Styles.RESULT))), false);
    return 1;
  }

  private static int executeTestForBiomePredicate(CommandContext<CommandSourceStack> context, boolean forceLoad) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final BlockPos blockPos = EnhancedPosArgument.getBlockPos(context, "pos");
    final ServerLevel world = source.getLevel();
    @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(blockPos);
    if (!forceLoad && !chunkLoaded) {
      throw TEST_FOR_BIOME_PREDICATE_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final var predicate = ResourceOrTagArgument.getResourceOrTag(context, "biome", Registries.BIOME);
    final Holder<Biome> actualBiome = world.getBiome(blockPos);
    final boolean test = predicate.test(actualBiome);
    source.sendFeedback$ecBridge(() -> {
      final MutableComponent posText = TextUtil.wrapVector(blockPos);
      final MutableComponent actualText = actualBiome.unwrap().map(TextUtil::biome, biome1 -> Component.literal(biome1.toString())).withStyle(Styles.ACTUAL);
      if (predicate instanceof EnhancedEntryPredicate.AnyOf<Biome> anyOf) {
        final MutableComponent expectedText = ComponentUtils.formatList(Collections2.transform(anyOf.predicates, element -> element instanceof EnhancedEntryPredicate.TagBased<Biome> tagBased ? Component.literal("#" + tagBased.tag().key().location()) : element instanceof EnhancedEntryPredicate.EntryBased<Biome> entryBased ? TextUtil.biome(entryBased.value().key()) : Component.literal(element.toString())), ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR, text -> text.withStyle(Styles.EXPECTED));
        if (test) {
          return Component.translatable("enhanced_commands.commands.testfor.biome.multiple.true", posText, actualText, expectedText).withStyle(Styles.TRUE);
        } else {
          return Component.translatable("enhanced_commands.commands.testfor.biome.multiple.false", posText, actualText, expectedText).withStyle(Styles.FALSE);
        }
      } else {
        return predicate.unwrap().map(ref -> {
          if (test) {
            return Component.translatable("enhanced_commands.commands.testfor.biome.entry.true", posText, actualText).withStyle(Styles.TRUE);
          } else {
            return Component.translatable("enhanced_commands.commands.testfor.biome.entry.false", posText, actualText, TextUtil.biome(ref.key()).withStyle(Styles.EXPECTED)).withStyle(Styles.FALSE);
          }
        }, named -> {
          final MutableComponent expectedText = Component.literal("#" + named.key().location()).withStyle(Styles.EXPECTED);
          if (test) {
            return Component.translatable("enhanced_commands.commands.testfor.biome.tag.true", posText, actualText, expectedText).withStyle(Styles.TRUE);
          } else {
            return Component.translatable("enhanced_commands.commands.testfor.biome.tag.false", posText, actualText, expectedText).withStyle(Styles.FALSE);
          }
        });
      }
    }, false);
    return BooleanUtils.toInteger(test);
  }
}
