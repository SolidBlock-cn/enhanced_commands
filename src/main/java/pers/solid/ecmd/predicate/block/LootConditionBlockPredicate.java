package pers.solid.ecmd.predicate.block;

import com.google.gson.Gson;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootGsons;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionManager;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.concurrent.CompletableFuture;

public interface LootConditionBlockPredicate extends BlockPredicate {
  Gson GSON = LootGsons.getConditionGsonBuilder().setLenient().create();

  LootCondition lootCondition();

  @Override
  default boolean test(CachedBlockPosition cachedBlockPosition) {
    final LootCondition lootCondition = lootCondition();
    final WorldView world = cachedBlockPosition.getWorld();
    if (!(world instanceof final ServerWorld serverWorld)) return false;
    return lootCondition.test(new LootContext.Builder(serverWorld)
        .random(serverWorld.random)
        .parameter(LootContextParameters.ORIGIN, cachedBlockPosition.getBlockPos().toCenterPos())
        .parameter(LootContextParameters.BLOCK_STATE, cachedBlockPosition.getBlockState())
        .optionalParameter(LootContextParameters.BLOCK_ENTITY, cachedBlockPosition.getBlockEntity())
        .parameter(LootContextParameters.TOOL, ItemStack.EMPTY)
        .build(LootContextTypes.BLOCK));
  }

  @Override
  @NotNull
  default Type getType() {
    return BlockPredicateTypes.LOOT_CONDITION;
  }

  record Anonymous(@NotNull LootCondition lootCondition) implements LootConditionBlockPredicate {
    @Override
    public @NotNull String asString() {
      return "predicate(" + GSON.toJson(lootCondition) + ")";
    }

    @Override
    public void writeNbt(@NotNull NbtCompound nbtCompound) {
      nbtCompound.putString("predicate", GSON.toJson(lootCondition));
    }

    @Override
    public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
      if (test(cachedBlockPosition)) {
        return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.loot_condition.anonymous.pass", TextUtil.wrapVector(cachedBlockPosition.getBlockPos())));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.loot_condition.anonymous.fail", TextUtil.wrapVector(cachedBlockPosition.getBlockPos())));
      }
    }
  }

  record Named(@NotNull Identifier identifier, @NotNull LootCondition lootCondition) implements LootConditionBlockPredicate {
    @Override
    public @NotNull String asString() {
      return "predicate(" + identifier + ")";
    }

    @Override
    public void writeNbt(@NotNull NbtCompound nbtCompound) {
      nbtCompound.putString("predicate", identifier.toString());
    }

    @Override
    public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
      if (test(cachedBlockPosition)) {
        return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.loot_condition.named.pass", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), TextUtil.literal(identifier).styled(Styles.EXPECTED)));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.loot_condition.named.fail", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), TextUtil.literal(identifier).styled(Styles.EXPECTED)));
      }
    }
  }

  enum Type implements BlockPredicateType<LootConditionBlockPredicate> {
    LOOT_CONDITION_TYPE;

    @Override
    public @NotNull LootConditionBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final String predicateString = nbtCompound.getString("predicate");
      if (predicateString.isEmpty()) throw new IllegalArgumentException("predicate expected in NBT");
      final char c = predicateString.charAt(0);
      if (c == '[' || c == '{' || StringReader.isQuotedStringStart(c)) {
        return new Anonymous(GSON.fromJson(predicateString, LootCondition.class));
      } else {
        final Identifier identifier = new Identifier(predicateString);
        if (world instanceof ServerWorld serverWorld) {
          final LootCondition lootCondition = serverWorld.getServer().getPredicateManager().get(identifier);
          if (lootCondition == null) {
            throw new IllegalArgumentException("Unknown loot table predicate: " + identifier);
          }
          return new Named(identifier, lootCondition);
        } else {
          throw new IllegalStateException("ServerWorld required");
        }
      }
    }
  }

  class Parser implements FunctionParamsParser<BlockPredicateArgument> {
    protected Identifier id;
    protected LootCondition anonymous;
    protected int cursorBeforeId, cursorAfterId;

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
          final LootCondition lootCondition = source.getServer().getPredicateManager().get(id);
          if (lootCondition == null) {
            parser.reader.setCursor(cursorBeforeId);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.UNKNOWN_LOOT_TABLE_PREDICATE_ID.createWithContext(parser.reader, id.toString()), cursorAfterId);
          }
          return new Named(id, lootCondition);
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
          this.anonymous = ParsingUtil.parseJson(reader, input -> GSON.fromJson(input, LootCondition.class), ModCommandExceptionTypes.INVALID_LOOT_TABLE_JSON);
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

    private static CompletableFuture<Suggestions> getLootConditionIdSuggestions(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder, int cursorBeforeId) {
      if (context.getSource() instanceof final ServerCommandSource source) {
        LootConditionManager lootConditionManager = source.getServer().getPredicateManager();
        return CommandSource.suggestIdentifiers(lootConditionManager.getIds(), suggestionsBuilder.createOffset(cursorBeforeId));
      } else if (context.getSource() instanceof CommandSource commandSource) {
        return commandSource.getCompletions(context);
      } else {
        return Suggestions.empty();
      }
    }
  }
}
