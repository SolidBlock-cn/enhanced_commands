package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandSource;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.bridge.LootBridge;
import pers.solid.ecmd.parse.FunctionParamsParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record LootConditionBlockPredicate(RegistryEntry<LootCondition> entry) implements BlockPredicate {
  public static final MapCodec<LootConditionBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(LootCondition.ENTRY_CODEC.fieldOf("condition").forGetter(LootConditionBlockPredicate::entry)).apply(i, LootConditionBlockPredicate::new));

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final LootCondition lootCondition = entry.value();
    final WorldView world = cachedBlockPosition.getWorld();
    if (!(world instanceof final ServerWorld serverWorld)) return false;
    return lootCondition.test(LootBridge.createContextForBlock(cachedBlockPosition, serverWorld, context.getSeed(this)));
  }

  @Override
  @NotNull
  public Type getType() {
    return BlockPredicateTypes.LOOT_CONDITION;
  }

  @Override
  public @NotNull String asString() {
    return "predicate(" + entry.getKeyOrValue().map(key -> key.getValue().toString(), lootCondition -> LootCondition.ENTRY_CODEC.encodeStart(NbtOps.INSTANCE, entry).toString()) + ")";
  }

  public enum Type implements BlockPredicateType<LootConditionBlockPredicate> {
    LOOT_CONDITION_TYPE;

    @Override
    public @NotNull MapCodec<LootConditionBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionParamsParser<LootConditionBlockPredicate> {
    protected Identifier id;
    protected LootCondition anonymous;
    protected int cursorBeforeId, cursorAfterId;

    private static CompletableFuture<Suggestions> getLootConditionIdSuggestions(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder, int cursorBeforeId) {
      if (context.getSource() instanceof final ServerCommandSource source) {
        return CommandSource.suggestIdentifiers(LootBridge.getLootConditionIds(source), suggestionsBuilder.createOffset(cursorBeforeId));
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
    public LootConditionBlockPredicate getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      if (id != null) {
        final Optional<RegistryEntry.Reference<LootCondition>> lootCondition = parseContext.registryAccess().createRegistryLookup().getOptionalEntry(RegistryKeys.PREDICATE, RegistryKey.of(RegistryKeys.PREDICATE, id));
        if (lootCondition.isEmpty()) {
          parseContext.reader().setCursor(cursorBeforeId);
          throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.UNKNOWN_LOOT_TABLE_PREDICATE_ID.createWithContext(parseContext.reader(), id.toString()), cursorAfterId);
        }
        return new LootConditionBlockPredicate(lootCondition.get());
      } else if (anonymous != null) {
        return new LootConditionBlockPredicate(RegistryEntry.of(anonymous));
      } else {
        return null;
      }
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final int cursorBeforeId = reader.getCursor();
      parseContext.setSuggestion((context1, suggestionsBuilder1) -> getLootConditionIdSuggestions(context1, suggestionsBuilder1, cursorBeforeId));
      if (reader.canRead()) {
        final char peek = reader.peek();
        if (peek == '{' || peek == '[' || StringReader.isQuotedStringStart(peek)) {
          parseContext.clearSuggestion();
          this.anonymous = ParsingUtil.parseNbt(reader, LootCondition.CODEC, ModCommandExceptionTypes.INVALID_LOOT_TABLE::create);
          return;
        }
      }
      // 读取 id
      this.cursorBeforeId = reader.getCursor();
      this.id = Identifier.fromCommandInput(reader);
      this.cursorAfterId = reader.getCursor();
      if (!reader.canRead() && parseContext.suggestionsOnly()) {
        // 在提供建议的过程中，如果后面没有内容，则提前中断建议，不提供“,”或“)”的建议。
        parseContext.setSuggestion((context, suggestionsBuilder) -> getLootConditionIdSuggestions(context, suggestionsBuilder, cursorBeforeId).thenCompose(suggestions -> {
          if (suggestions.isEmpty()) {
            return suggestionsBuilder.suggest(")").buildFuture();
          } else {
            return CompletableFuture.completedFuture(suggestions);
          }
        }));
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, ")");
      }
    }
  }
}
