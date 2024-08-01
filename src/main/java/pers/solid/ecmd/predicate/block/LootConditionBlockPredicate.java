package pers.solid.ecmd.predicate.block;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.ReloadableRegistries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.*;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface LootConditionBlockPredicate extends BlockPredicate {
  MapCodec<LootConditionBlockPredicate> CODEC = Codec.BOOL.dispatchMap("anonymous", p -> p instanceof Anonymous, b -> b ? Anonymous.CODEC : Named.CODEC);

  LootCondition lootCondition(CachedBlockPosition cachedBlockPosition);

  @Override
  default boolean test(CachedBlockPosition cachedBlockPosition) {
    final LootCondition lootCondition = lootCondition(cachedBlockPosition);
    final WorldView world = cachedBlockPosition.getWorld();
    if (!(world instanceof final ServerWorld serverWorld)) return false;
    final LootContextParameterSet lootContextParameterSet = new LootContextParameterSet.Builder(serverWorld)
        .add(LootContextParameters.ORIGIN, cachedBlockPosition.getBlockPos().toCenterPos())
        .add(LootContextParameters.BLOCK_STATE, cachedBlockPosition.getBlockState())
        .add(LootContextParameters.BLOCK_ENTITY, cachedBlockPosition.getBlockEntity())
        .addOptional(LootContextParameters.TOOL, ItemStack.EMPTY)
        .build(LootContextTypes.BLOCK);
    return lootCondition.test(new LootContext.Builder(lootContextParameterSet)
        .random(serverWorld.random)
        .build(Optional.empty()));
  }

  @Override
  @NotNull
  default Type getType() {
    return BlockPredicateTypes.LOOT_CONDITION;
  }

  enum Type implements BlockPredicateType<LootConditionBlockPredicate> {
    LOOT_CONDITION_TYPE;

    @Override
    public @NotNull MapCodec<LootConditionBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  record Anonymous(@NotNull LootCondition lootCondition) implements LootConditionBlockPredicate {
    public static final MapCodec<Anonymous> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(Anonymous::new, LootCondition.CODEC.fieldOf("loot_condition").forGetter(Anonymous::lootCondition)));

    @Override
    public @NotNull String asString() {
      return "predicate(" + LootCondition.CODEC.encodeStart(JsonOps.INSTANCE, lootCondition).resultOrPartial() + ")";
    }

    @Override
    public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
      if (test(cachedBlockPosition)) {
        return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.loot_condition.anonymous.pass", TextUtil.wrapVector(cachedBlockPosition.getBlockPos())));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.loot_condition.anonymous.fail", TextUtil.wrapVector(cachedBlockPosition.getBlockPos())));
      }
    }

    @Override
    public LootCondition lootCondition(CachedBlockPosition cachedBlockPosition) {
      return lootCondition;
    }
  }

  final class Named implements LootConditionBlockPredicate {
    public static final MapCodec<Named> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(Named::new, Identifier.CODEC.fieldOf("id").forGetter(named -> named.identifier)));
    private final @NotNull Identifier identifier;
    private transient LootCondition cached = null;

    public Named(@NotNull Identifier identifier) {
      this.identifier = identifier;
    }

    @Override
    public @NotNull String asString() {
      return "predicate(" + identifier + ")";
    }

    @Override
    public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
      if (test(cachedBlockPosition)) {
        return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.loot_condition.named.pass", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), TextUtil.literal(identifier).styled(Styles.EXPECTED)));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.loot_condition.named.fail", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), TextUtil.literal(identifier).styled(Styles.EXPECTED)));
      }
    }

    @Override
    public LootCondition lootCondition(CachedBlockPosition cachedBlockPosition) {
      if (cachedBlockPosition.getWorld() instanceof ServerWorld serverWorld) {
        final Optional<RegistryEntry.Reference<LootCondition>> entry = serverWorld.getServer().getReloadableRegistries().createRegistryLookup().getOptionalEntry(RegistryKeys.PREDICATE, RegistryKey.of(RegistryKeys.PREDICATE, identifier));
        // todo check
        return entry.orElseThrow().value();
      }
      throw new UnsupportedOperationException("LootConditionBlockPredicate with a predicate ID can only be run on the server!");
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (Named) obj;
      return Objects.equals(this.identifier, that.identifier);
    }

    @Override
    public int hashCode() {
      return Objects.hash(identifier);
    }

    @Override
    public String toString() {
      return "Named[" +
          "identifier=" + identifier + ']';
    }
  }

  class Parser implements FunctionParamsParser<BlockPredicateArgument> {
    protected Identifier id;
    protected LootCondition anonymous;
    protected int cursorBeforeId, cursorAfterId;

    private static CompletableFuture<Suggestions> getLootConditionIdSuggestions(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder, int cursorBeforeId) {
      if (context.getSource() instanceof final ServerCommandSource source) {
        ReloadableRegistries.Lookup lookup = source.getServer().getReloadableRegistries();
        return CommandSource.suggestIdentifiers(lookup.getIds(RegistryKeys.PREDICATE), suggestionsBuilder.createOffset(cursorBeforeId));
      } else if (context.getSource() instanceof CommandSource commandSource) {
        return commandSource.getCompletions(context);
      } else {
        return Suggestions.empty();
      }
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) throws CommandSyntaxException {
      if (id != null) {
        return source -> {
          final Optional<LootCondition> lootCondition = commandRegistryAccess.createRegistryLookup().getOptionalEntry(RegistryKeys.PREDICATE, RegistryKey.of(RegistryKeys.PREDICATE, id)).map(RegistryEntry.Reference::value);
          if (lootCondition.isEmpty()) {
            parser.reader.setCursor(cursorBeforeId);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.UNKNOWN_LOOT_TABLE_PREDICATE_ID.createWithContext(parser.reader, id.toString()), cursorAfterId);
          }
          return new Named(id);
        };
      } else if (anonymous != null) {
        return new Anonymous(anonymous);
      } else {
        return null;
      }
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      final int cursorBeforeId = parser.reader.getCursor();
      parser.suggestionProviders.add(SuggestionProvider.offset((context1, suggestionsBuilder1) -> getLootConditionIdSuggestions(context1, suggestionsBuilder1, cursorBeforeId)));
      if (reader.canRead()) {
        final char peek = reader.peek();
        if (peek == '{' || peek == '[' || StringReader.isQuotedStringStart(peek)) {
          parser.suggestionProviders.clear();
          // todo check
          this.anonymous = ParsingUtil.parseJson(reader, input -> LootCondition.CODEC.decode(JsonOps.INSTANCE, new Gson().fromJson(input, JsonElement.class)).getOrThrow().getFirst(), ModCommandExceptionTypes.INVALID_LOOT_TABLE_JSON);
          return;
        }
      }
      // 读取 id
      this.cursorBeforeId = reader.getCursor();
      this.id = Identifier.fromCommandInput(reader);
      this.cursorAfterId = reader.getCursor();
      if (!reader.canRead() && suggestionsOnly) {
        // 在提供建议的过程中，如果后面没有内容，则提前中断建议，不提供“,”或“)”的建议。
        parser.suggestionProviders.remove(parser.suggestionProviders.size() - 1);
        parser.suggestionProviders.add(SuggestionProvider.offset((context, suggestionsBuilder) -> getLootConditionIdSuggestions(context, suggestionsBuilder, cursorBeforeId).thenCompose(suggestions -> {
          if (suggestions.isEmpty()) {
            return suggestionsBuilder.suggest(")").buildFuture();
          } else {
            return CompletableFuture.completedFuture(suggestions);
          }
        })));
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, ")");
      }
    }
  }
}
