package pers.solid.ecmd.command;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryPredicateArgumentType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.ecmd.argument.EnhancedEntryPredicate;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import static net.minecraft.command.argument.RegistryEntryPredicateArgumentType.registryEntryPredicate;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.KeywordArgsArgumentType.getKeywordArgs;

public enum TestForBiomeCommand implements TestForCommands.Entry {
  INSTANCE;
  public static final KeywordArgsArgumentType BIOME_KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("force_load", BoolArgumentType.bool(), false)
      .build();
  public static final DynamicCommandExceptionType TEST_FOR_BIOME_NOT_LOADED = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.testfor.biome.not_loaded", o));
  public static final DynamicCommandExceptionType TEST_FOR_BIOME_PREDICATE_NOT_LOADED = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.testfor.biome.not_loaded_for_predicate", o));

  @Override
  public void addArguments(LiteralArgumentBuilder<ServerCommandSource> testForBuilder, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    testForBuilder.then(literal("biome")
        .executes(context -> executeTestForBiome(context, BlockPos.ofFloored(context.getSource().getPosition())))
        .then(argument("pos", EnhancedPosArgumentType.blockPos())
            .executes(context -> executeTestForBiome(context, EnhancedPosArgumentType.getBlockPos(context, "pos")))
            .then(argument("biome", registryEntryPredicate(registryAccess, RegistryKeys.BIOME))
                .executes(context -> executeTestForBiomePredicate(context, false))
                .then(argument("keyword_args", BIOME_KEYWORD_ARGS)
                    .executes(context -> executeTestForBiomePredicate(context, getKeywordArgs(context, "keyword_args").getBoolean("force_load")))))));
  }

  private static int executeTestForBiome(CommandContext<ServerCommandSource> context, BlockPos blockPos) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    if (!world.isChunkLoaded(blockPos)) {
      throw TEST_FOR_BIOME_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final RegistryEntry<Biome> biome = world.getBiome(blockPos);
    CommandBridge.sendFeedback(source, () -> biome.getKeyOrValue().map(key -> Text.translatable("enhanced_commands.commands.testfor.biome.info", TextUtil.wrapVector(blockPos), TextUtil.biome(key).styled(TextUtil.STYLE_FOR_RESULT), TextUtil.literal(key.getValue()).styled(TextUtil.STYLE_FOR_RESULT)), value -> Text.translatable("enhanced_commands.commands.testfor.biome.info_unregistered", TextUtil.wrapVector(blockPos), Text.literal(value.toString()).styled(TextUtil.STYLE_FOR_RESULT))), false);
    return 1;
  }

  private static int executeTestForBiomePredicate(CommandContext<ServerCommandSource> context, boolean forceLoad) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos blockPos = EnhancedPosArgumentType.getBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    if (!forceLoad && !world.isChunkLoaded(blockPos)) {
      throw TEST_FOR_BIOME_PREDICATE_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final var predicate = RegistryEntryPredicateArgumentType.getRegistryEntryPredicate(context, "biome", RegistryKeys.BIOME);
    final RegistryEntry<Biome> actualBiome = world.getBiome(blockPos);
    final boolean test = predicate.test(actualBiome);
    CommandBridge.sendFeedback(source, () -> {
      final MutableText posText = TextUtil.wrapVector(blockPos);
      final MutableText actualText = actualBiome.getKeyOrValue().map(TextUtil::biome, biome1 -> Text.literal(biome1.toString())).styled(TextUtil.STYLE_FOR_ACTUAL);
      if (predicate instanceof EnhancedEntryPredicate.AnyOf<Biome> anyOf) {
        final MutableText expectedText = Texts.join(Collections2.transform(anyOf.predicates, element -> element instanceof EnhancedEntryPredicate.TagBased<Biome> tagBased ? TextUtil.literal(tagBased.tag().getTag().id()) : element instanceof EnhancedEntryPredicate.EntryBased<Biome> entryBased ? TextUtil.biome(entryBased.value().registryKey()) : Text.literal(element.toString())), Texts.DEFAULT_SEPARATOR_TEXT, text -> text.styled(TextUtil.STYLE_FOR_EXPECTED));
        if (test) {
          return Text.translatable("enhanced_commands.commands.testfor.biome.multiple.true", posText, actualText, expectedText);
        } else {
          return Text.translatable("enhanced_commands.commands.testfor.biome.multiple.false", posText, actualText, expectedText);
        }
      } else {
        return predicate.getEntry().map(ref -> {
          if (test) {
            return Text.translatable("enhanced_commands.commands.testfor.biome.entry.true", posText, actualText);
          } else {
            return Text.translatable("enhanced_commands.commands.testfor.biome.entry.false", posText, actualText, TextUtil.biome(ref.registryKey()).styled(TextUtil.STYLE_FOR_EXPECTED));
          }
        }, named -> {
          final MutableText expectedText = Text.literal("#" + named.getTag().id()).styled(TextUtil.STYLE_FOR_EXPECTED);
          if (test) {
            return Text.translatable("enhanced_commands.commands.testfor.biome.tag.true", posText, actualText, expectedText);
          } else {
            return Text.translatable("enhanced_commands.commands.testfor.biome.tag.false", posText, actualText, expectedText);
          }
        });
      }
    }, false);
    return BooleanUtils.toInteger(test);
  }
}
