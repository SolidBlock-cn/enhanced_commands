package pers.solid.ecmd.util.pack;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.pack.problems.RecursiveReferenceProblem;
import pers.solid.ecmd.util.pack.problems.ValidationProblem;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 此类所有通过数据包访问可数据驱动对象的共用接口，例如方块谓词、方块函数等的“reference”类型，其通常是存储一个 Holder.Reference。
 *
 * @param <E> 可数据驱动对象的类型
 */
public interface ReferenceEntry<E> extends RequiresValidation {
  static <T extends ReferenceEntry<E>, E> MapCodec<T> createCodec(Codec<ResourceLocation> idCodec, ResourceKey<Registry<E>> registryKey, Function<Holder.Reference<E>, T> function) {
    return RecordCodecBuilder.mapCodec(i -> i.group(new SafeReferenceCodec<>(idCodec, registryKey).fieldOf("reference").forGetter(ReferenceEntry::reference)).apply(i, function));
  }

  Holder.Reference<E> reference();

  static <E> ReferenceEntry<E> of(Holder.Reference<E> reference) {
    return () -> reference;
  }

  /**
   * 用于解析“$ + ID”形式的语法，并将其转化为 ReferenceEntry 的对象，在解析过程中，能自动提供关于 ID 的建议，并为不存在的 ID 报错。
   *
   * @param <T> 该 reference 类自身所代表的类型
   * @param <E> 可数据驱动对象的类型
   */
  abstract class PrefixedIdParser<T, E> implements Parser<T> {
    private final char prefix;
    private final Component tooltip;
    private final ResourceKey<? extends Registry<E>> registryKey;

    protected PrefixedIdParser(char prefix, Component tooltip, ResourceKey<? extends Registry<E>> registryKey) {
      this.prefix = prefix;
      this.tooltip = tooltip;
      this.registryKey = registryKey;
    }

    @Override
    public @Nullable T parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString(Character.toString(prefix), tooltip, suggestionsBuilder).buildFuture());
      boolean prefixed = false;
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == prefix) {
        reader.skip();
        prefixed = true;
      }
      if (!prefixed) return null;
      parseContext.clearSuggestion();

      final Holder.Reference<E> holderReference = parseAndGetReference(parseContext);
      return getResultByReference(holderReference);
    }

    /**
     * 解析前缀之后的 ID 的内容，并返回一个 Holder.Reference。
     */
    public Holder.Reference<E> parseAndGetReference(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final int cursorBeforeId = reader.getCursor();
      parseContext.setSuggestion((context, builder) -> getIdSuggestion(parseContext, context, builder, cursorBeforeId));
      parseContext.terminateSuggestionsIfNotEmpty();
      if (parseContext.allowSparse()) reader.skipWhitespace();
      final ResourceLocation id = DefaultNamespace.ENHANCED_COMMANDS.fromStringReader(reader);
      final int cursorAfterId = reader.getCursor();
      final HolderGetter.Provider registryLookup = parseContext.registries();
      final ResourceKey<E> entryKey = ResourceKey.create(registryKey, id);
        final Optional<? extends HolderGetter<E>> registryEntryLookup = registryLookup.lookup(registryKey);
      if (registryEntryLookup.isEmpty()) {
        // 考虑到有时客户端在解析命令时，会不知道该数据包中的内容，不应在客户端判定为解析错误。
        return LazyReference.of(entryKey);
      }
      final Optional<Holder.Reference<E>> entry = registryEntryLookup.get().get(entryKey);
      if (entry.isEmpty()) {
        reader.setCursor(cursorBeforeId);
        throw createExceptionForUnknownId(reader, id, cursorAfterId);
      }
      return entry.get();
    }

    private CompletableFuture<Suggestions> getIdSuggestion(ParseContext<?> parseContext, @UnknownNullability CommandContext<?> context, @UnknownNullability SuggestionsBuilder builder, int cursorBeforeId) {
      if (context.getSource() instanceof CommandSourceStack) {
        return DefaultNamespace.ENHANCED_COMMANDS.suggestIdentifiers(parseContext.registries().lookupOrThrow(registryKey).listElementIds().map(ResourceKey::location), builder.createOffset(cursorBeforeId));
      } else if (context.getSource() instanceof SharedSuggestionProvider commandSource) {
        return commandSource.customSuggestion(context);
      } else {
        return Suggestions.empty();
      }
    }

    protected abstract T getResultByReference(Holder.Reference<E> holderReference) throws CommandSyntaxException;

    protected CommandSyntaxException createExceptionForUnknownId(StringReader reader, ResourceLocation identifier, int cursorEnd) {
      return EnhancedCommandsCommandExceptionTypes.registryEntryException(registryKey, reader, identifier, cursorEnd);
    }
  }

  /**
   * 用于解析“reference(id)”形式的函数式语法，在解析时，需要提供一个 {@link PrefixedIdParser} 以辅助 ID 的建议和解析。
   *
   * @param <T> 该 reference 类自身所代表的类型
   * @param <E> 可数据驱动对象的类型
   */
  class ReferenceFunctionGrammarParser<T, E> implements FunctionContentParser<T> {
    /**
     * 与函数语法解析器相关的 {@link PrefixedIdParser}，用于提供 ID 建议和解析。
     */
    public final PrefixedIdParser<T, E> affiliatedPrefixedIdParser;


    private @Nullable Holder.Reference<E> holderReference = null;

    public ReferenceFunctionGrammarParser(PrefixedIdParser<T, E> affiliatedPrefixedIdParser) {
      this.affiliatedPrefixedIdParser = affiliatedPrefixedIdParser;
    }

    @Override
    public T getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      Objects.requireNonNull(holderReference, "value");
      return affiliatedPrefixedIdParser.getResultByReference(holderReference);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      holderReference = affiliatedPrefixedIdParser.parseAndGetReference(parseContext);
    }
  }

  @Override
  default Iterable<? extends @Nullable Object> membersToValidate() {
    return Collections.emptyList();
  }

  @Override
  default void validate(ValidationContext context) {
    final Holder.Reference<E> reference = reference();
    if (!context.isElementReferenced(reference.key())) {
      final ValidationContext newContext = context.forReferencedElement(reference.key());
      final Optional<HolderGetter<E>> optionalLookup = context.resolver().lookup(reference.key().registryKey());
      if (optionalLookup.isEmpty()) {
        return;
      }
      final Optional<Holder.Reference<E>> optionalReference = optionalLookup.get().get(reference.key());
      if (optionalReference.isPresent()) {
        if (optionalReference.get().value() instanceof RequiresValidation r) {
          r.validate(newContext);
          if (newContext.hasProblems()) {
            if (reference instanceof LazyReference<E> lazy) {
              lazy.setProblem(ValidationProblem.concentrateMultiple(newContext.problems()));
            }
          }
        }
      }
    } else {
      final RecursiveReferenceProblem<E> problem = new RecursiveReferenceProblem<>(reference.key());
      context.recordProblem(problem);

      if (reference instanceof LazyReference<E> lazy) {
        lazy.setProblem(problem);
      }
    }
  }
}
