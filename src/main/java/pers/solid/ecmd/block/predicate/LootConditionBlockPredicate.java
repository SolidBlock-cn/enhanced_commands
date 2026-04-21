package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.bridge.LootBridge;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record LootConditionBlockPredicate(Holder<LootItemCondition> entry) implements BlockPredicate {
  public static final MapCodec<LootConditionBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(LootItemCondition.CODEC.fieldOf("condition").forGetter(LootConditionBlockPredicate::entry)).apply(i, LootConditionBlockPredicate::new));

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final LootItemCondition lootCondition = entry.value();
    final LevelReader world = blockInWorld.getLevel();
    if (!(world instanceof final ServerLevel serverWorld)) return false;
    return lootCondition.test(LootBridge.createContextForBlock(blockInWorld, serverWorld, executionContext.getSeed(this)));
  }

  @Override
  public BlockPredicateType<LootConditionBlockPredicate> getType() {
    return BlockPredicateTypes.LOOT_CONDITION;
  }

  @Override
  public String expressAsString() {
    return "predicate(" + entry.unwrap().map(key -> key.location().toString(), lootCondition -> LootItemCondition.CODEC.encodeStart(NbtOps.INSTANCE, entry).toString()) + ")";
  }

  public static class Parser implements FunctionContentParser.SequentialParams<LootConditionBlockPredicate> {
    protected @Nullable ResourceLocation id;
    protected @Nullable LootItemCondition anonymous;
    protected int cursorBeforeId, cursorAfterId;

    private static CompletableFuture<Suggestions> getLootConditionIdSuggestions(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder, int cursorBeforeId) {
      if (context.getSource() instanceof final CommandSourceStack source) {
        return SharedSuggestionProvider.suggestResource(LootBridge.getLootConditionIds(source), suggestionsBuilder.createOffset(cursorBeforeId));
      } else if (context.getSource() instanceof SharedSuggestionProvider commandSource) {
        return commandSource.customSuggestion(context);
      } else {
        return Suggestions.empty();
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 1;
    }

    @Override
    public @Nullable LootConditionBlockPredicate getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      if (id != null) {
        final Optional<Holder.Reference<LootItemCondition>> lootCondition = parseContext.registries().asGetterLookup().get(Registries.PREDICATE, ResourceKey.create(Registries.PREDICATE, id));
        if (lootCondition.isEmpty()) {
          parseContext.reader().setCursor(cursorBeforeId);
          throw EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.UNKNOWN_LOOT_TABLE_PREDICATE_ID.createWithContext(parseContext.reader(), id.toString()), cursorAfterId);
        }
        return new LootConditionBlockPredicate(lootCondition.get());
      } else if (anonymous != null) {
        return new LootConditionBlockPredicate(Holder.direct(anonymous));
      } else {
        return null;
      }
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final int cursorBeforeId = reader.getCursor();
      parseContext.setSuggestion((context1, suggestionsBuilder1) -> getLootConditionIdSuggestions(context1, suggestionsBuilder1, cursorBeforeId));
      if (reader.canRead()) {
        final char peek = reader.peek();
        if (peek == '{' || peek == '[' || StringReader.isQuotedStringStart(peek)) {
          parseContext.clearSuggestion();
          this.anonymous = ParsingUtil.parseNbt(reader, LootItemCondition.DIRECT_CODEC, EnhancedCommandsCommandExceptionTypes.INVALID_LOOT_TABLE::create);
          return;
        }
      }
      // 读取 id
      this.cursorBeforeId = reader.getCursor();
      this.id = ResourceLocation.read(reader);
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
