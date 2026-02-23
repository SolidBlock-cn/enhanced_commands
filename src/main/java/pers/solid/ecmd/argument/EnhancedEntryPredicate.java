package pers.solid.ecmd.argument;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface EnhancedEntryPredicate<T> extends ResourceOrTagArgument.Result<T> {
  /**
   * @see ResourceOrTagArgument.ResourceResult
   */
  record EntryBased<T>(Holder.Reference<T> value) implements EnhancedEntryPredicate<T> {
    @Override
    public @NotNull Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
      return Either.left(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> @NotNull Optional<ResourceOrTagArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> registryRef) {
      return this.value.key().isFor(registryRef) ? Optional.of((ResourceOrTagArgument.Result<E>) this) : Optional.empty();
    }

    public boolean test(Holder<T> registryEntry) {
      return registryEntry.equals(this.value);
    }

    @Override
    public @NotNull String asPrintable() {
      return this.value.key().location().toString();
    }
  }

  /**
   * @see ResourceOrTagArgument.TagResult
   */
  record TagBased<T>(HolderSet.Named<T> tag) implements EnhancedEntryPredicate<T> {
    @Override
    public @NotNull Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
      return Either.right(this.tag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> @NotNull Optional<ResourceOrTagArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> registryRef) {
      return this.tag.key().isFor(registryRef) ? Optional.of((ResourceOrTagArgument.Result<E>) this) : Optional.empty();
    }

    public boolean test(Holder<T> registryEntry) {
      return this.tag.contains(registryEntry);
    }

    @Override
    public @NotNull String asPrintable() {
      return "#" + this.tag.key().location();
    }
  }

  final class AnyOf<T> implements EnhancedEntryPredicate<T> {
    public final Collection<EnhancedEntryPredicate<T>> predicates;
    private final HolderSet<T> entries;
    private final List<EnhancedEntryPredicate<T>> others;

    public AnyOf(Collection<EnhancedEntryPredicate<T>> predicates) {
      this.predicates = predicates;
      final Stream<Holder.Reference<T>> stream = predicates.stream().map(p -> {
        if (p instanceof EnhancedEntryPredicate.AnyOf<T>) {
          throw new IllegalArgumentException("The parameter of EnhancedEntryPredicate.AnyOf cannot contain instance of EnhancedEntryPredicate.AnyOf");
        }
        return p instanceof EnhancedEntryPredicate.EntryBased<T> entryBased ? entryBased.value : null;
      }).filter(Objects::nonNull);
      this.entries = HolderSet.direct(stream.collect(Collectors.toList()));
      this.others = predicates.stream().map(p -> p instanceof EnhancedEntryPredicate.EntryBased<T> ? null : p).filter(Objects::nonNull).toList();
    }

    public static final SimpleCommandExceptionType MULTIPLE_VALUE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.registry_entry_predicate.multiple"));

    @Override
    public @NotNull Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
      try {
        throw MULTIPLE_VALUE.create();
      } catch (CommandSyntaxException e) {
        throw new CommandRuntimeException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> @NotNull Optional<ResourceOrTagArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> registryRef) {
      return predicates.iterator().next().cast(registryRef).isPresent() ? Optional.of((ResourceOrTagArgument.Result<E>) this) : Optional.empty();
    }

    @Override
    public @NotNull String asPrintable() {
      return predicates.stream().map(ResourceOrTagArgument.Result::asPrintable).collect(Collectors.joining("|"));
    }

    @Override
    public boolean test(Holder<T> entry) {
      return entries.contains(entry) || Iterables.any(others, predicate -> predicate.test(entry));
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (AnyOf<?>) obj;
      return Objects.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode() {
      return Objects.hash(predicates);
    }

    @Override
    public String toString() {
      return "AnyOf[" + "predicates=" + predicates + ']';
    }
  }

  Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType(
      (tag, type) -> Component.translatableEscape("argument.resource_tag.not_found", tag, type)
  );
  Dynamic3CommandExceptionType WRONG_TYPE_EXCEPTION = new Dynamic3CommandExceptionType(
      (tag, type, expectedType) -> Component.translatable("argument.resource_tag.invalid_type", tag, type, expectedType)
  );

  /**
   * 此方法用于 mixin 中，在解析完一个值后，如果有竖线，则继续解析直到没有竖线的位置为止。
   */
  static <T> AnyOf<T> mixinGetCompoundPredicate(HolderLookup<T> registryWrapper, ResourceKey<? extends Registry<T>> registryRef, StringReader reader, EnhancedEntryPredicate<T> firstValue) throws CommandSyntaxException {
    final List<EnhancedEntryPredicate<T>> values = Lists.newArrayList(firstValue);
    final Set<ResourceLocation> duplicateTagIds = new HashSet<>();
    final Set<ResourceLocation> duplicateEntryIds = new HashSet<>();
    if (firstValue instanceof EnhancedEntryPredicate.EntryBased<T> entryBased) {
      duplicateEntryIds.add(entryBased.value().key().location());
    } else if (firstValue instanceof EnhancedEntryPredicate.TagBased<T> tagBased) {
      duplicateTagIds.add(tagBased.tag().key().location());
    }
    while (reader.canRead() && reader.peek() == '|') {
      reader.skip();

      int cursorBeforeId = reader.getCursor();
      if (reader.canRead() && reader.peek() == '#') {

        try {
          reader.skip();
          ResourceLocation tagId = ResourceLocation.read(reader);
          final int cursorAfterId = reader.getCursor();
          if (duplicateTagIds.contains(tagId)) {
            reader.setCursor(cursorBeforeId);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, tagId), cursorAfterId);
          } else {
            duplicateTagIds.add(tagId);
          }
          TagKey<T> tagKey = TagKey.create(registryRef, tagId);
          HolderSet.Named<T> named = registryWrapper
              .get(tagKey)
              .orElseThrow(() -> CommandSyntaxExceptionExtension.withCursorEnd(NOT_FOUND_EXCEPTION.createWithContext(reader, tagId, registryRef.location()), cursorAfterId));
          values.add(new TagBased<>(named));
        } catch (CommandSyntaxException var6) {
          reader.setCursor(cursorBeforeId);
          throw var6;
        }
      } else {
        ResourceLocation entryId = ResourceLocation.read(reader);
        final int cursorAfterId = reader.getCursor();
        if (duplicateEntryIds.contains(entryId)) {
          reader.setCursor(cursorBeforeId);
          throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, entryId), cursorAfterId);
        } else {
          duplicateEntryIds.add(entryId);
        }
        ResourceKey<T> registryKey = ResourceKey.create(registryRef, entryId);
        Holder.Reference<T> reference = registryWrapper
            .get(registryKey)
            .orElseThrow(() -> {
              reader.setCursor(cursorBeforeId);
              return MixinShared.modifiedRegistryEntryException(registryRef, reader, entryId, cursorAfterId);
            });
        values.add(new EntryBased<>(reference));
      }
    }

    return new AnyOf<>(values);
  }
}
