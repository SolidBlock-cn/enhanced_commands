package pers.solid.ecmd.argument;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.datafixers.util.Either;
import net.minecraft.command.CommandException;
import net.minecraft.command.argument.RegistryEntryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface EnhancedEntryPredicate<T> extends RegistryEntryPredicateArgumentType.EntryPredicate<T> {
  /**
   * @see RegistryEntryPredicateArgumentType.EntryBased
   */
  record EntryBased<T>(RegistryEntry.Reference<T> value) implements EnhancedEntryPredicate<T> {
    @Override
    public Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry() {
      return Either.left(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> Optional<RegistryEntryPredicateArgumentType.EntryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
      return this.value.registryKey().isOf(registryRef) ? Optional.of((RegistryEntryPredicateArgumentType.EntryPredicate<E>) this) : Optional.empty();
    }

    public boolean test(RegistryEntry<T> registryEntry) {
      return registryEntry.equals(this.value);
    }

    @Override
    public String asString() {
      return this.value.registryKey().getValue().toString();
    }
  }

  /**
   * @see RegistryEntryPredicateArgumentType.TagBased
   */
  record TagBased<T>(RegistryEntryList.Named<T> tag) implements EnhancedEntryPredicate<T> {
    @Override
    public Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry() {
      return Either.right(this.tag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> Optional<RegistryEntryPredicateArgumentType.EntryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
      return this.tag.getTag().isOf(registryRef) ? Optional.of((RegistryEntryPredicateArgumentType.EntryPredicate<E>) this) : Optional.empty();
    }

    public boolean test(RegistryEntry<T> registryEntry) {
      return this.tag.contains(registryEntry);
    }

    @Override
    public String asString() {
      return "#" + this.tag.getTag().id();
    }
  }

  final class AnyOf<T> implements EnhancedEntryPredicate<T> {
    private final Collection<EnhancedEntryPredicate<T>> predicates;
    private final RegistryEntryList<T> entries;
    private final List<EnhancedEntryPredicate<T>> others;

    public AnyOf(Collection<EnhancedEntryPredicate<T>> predicates) {
      this.predicates = predicates;
      final Stream<RegistryEntry.Reference<T>> stream = predicates.stream().map(p -> {
        if (p instanceof EnhancedEntryPredicate.AnyOf<T>) {
          throw new IllegalArgumentException("The parameter of EnhancedEntryPredicate.AnyOf cannot contain instance of EnhancedEntryPredicate.AnyOf");
        }
        return p instanceof EnhancedEntryPredicate.EntryBased<T> entryBased ? entryBased.value : null;
      }).filter(Objects::nonNull);
      this.entries = RegistryEntryList.of(stream.collect(Collectors.toList()));
      this.others = predicates.stream().map(p -> p instanceof EnhancedEntryPredicate.EntryBased<T> ? null : p).filter(Objects::nonNull).toList();
    }

    @Override
    public Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry() {
      throw new CommandException(Text.translatable("enhanced_commands.argument.registry_entry_predicate.multiple"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> Optional<RegistryEntryPredicateArgumentType.EntryPredicate<E>> tryCast(RegistryKey<? extends Registry<E>> registryRef) {
      return predicates.iterator().next().tryCast(registryRef).isPresent() ? Optional.of((RegistryEntryPredicateArgumentType.EntryPredicate<E>) this) : Optional.empty();
    }

    @Override
    public String asString() {
      return predicates.stream().map(RegistryEntryPredicateArgumentType.EntryPredicate::asString).collect(Collectors.joining("|"));
    }

    @Override
    public boolean test(RegistryEntry<T> entry) {
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
      (tag, type) -> Text.translatable("argument.resource_tag.not_found", tag, type)
  );
  Dynamic3CommandExceptionType WRONG_TYPE_EXCEPTION = new Dynamic3CommandExceptionType(
      (tag, type, expectedType) -> Text.translatable("argument.resource_tag.invalid_type", tag, type, expectedType)
  );

  /**
   * 此方法用于 mixin 中，在解析完一个值后，如果有竖线，则继续解析直到没有竖线的位置为止。
   */
  static <T> AnyOf<T> mixinGetCompoundPredicate(RegistryWrapper<T> registryWrapper, RegistryKey<? extends Registry<T>> registryRef, StringReader reader, EnhancedEntryPredicate<T> firstValue) throws CommandSyntaxException {
    final List<EnhancedEntryPredicate<T>> values = Lists.newArrayList(firstValue);
    final Set<Identifier> duplicateTagIds = new HashSet<>();
    final Set<Identifier> duplicateEntryIds = new HashSet<>();
    if (firstValue instanceof EnhancedEntryPredicate.EntryBased<T> entryBased) {
      duplicateEntryIds.add(entryBased.value().registryKey().getValue());
    } else if (firstValue instanceof EnhancedEntryPredicate.TagBased<T> tagBased) {
      duplicateTagIds.add(tagBased.tag().getTag().id());
    }
    while (reader.canRead() && reader.peek() == '|') {
      reader.skip();

      int cursorBeforeId = reader.getCursor();
      if (reader.canRead() && reader.peek() == '#') {

        try {
          reader.skip();
          Identifier tagId = Identifier.fromCommandInput(reader);
          final int cursorAfterId = reader.getCursor();
          if (duplicateTagIds.contains(tagId)) {
            reader.setCursor(cursorBeforeId);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, tagId), cursorAfterId);
          } else {
            duplicateTagIds.add(tagId);
          }
          TagKey<T> tagKey = TagKey.of(registryRef, tagId);
          RegistryEntryList.Named<T> named = registryWrapper
              .getOptional(tagKey)
              .orElseThrow(() -> CommandSyntaxExceptionExtension.withCursorEnd(NOT_FOUND_EXCEPTION.createWithContext(reader, tagId, registryRef.getValue()), cursorAfterId));
          values.add(new TagBased<>(named));
        } catch (CommandSyntaxException var6) {
          reader.setCursor(cursorBeforeId);
          throw var6;
        }
      } else {
        Identifier entryId = Identifier.fromCommandInput(reader);
        final int cursorAfterId = reader.getCursor();
        if (duplicateEntryIds.contains(entryId)) {
          reader.setCursor(cursorBeforeId);
          throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, entryId), cursorAfterId);
        } else {
          duplicateEntryIds.add(entryId);
        }
        RegistryKey<T> registryKey = RegistryKey.of(registryRef, entryId);
        RegistryEntry.Reference<T> reference = registryWrapper
            .getOptional(registryKey)
            .orElseThrow(() -> MixinShared.modifiedRegistryEntryException(registryRef, reader, entryId, cursorAfterId));
        values.add(new EntryBased<>(reference));
      }
    }

    return new AnyOf<>(values);
  }
}
