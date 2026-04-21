package pers.solid.ecmd.enchantment.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class EnchantmentModificationTargetParser {
  public static final Dynamic2CommandExceptionType UNKNOWN_TAG = new Dynamic2CommandExceptionType((a, b) -> Component.translatable("argument.resource_tag.not_found", a, b));

  private EnchantmentModificationTargetParser() {
  }

  public static <S> EnchantmentModificationTarget parse(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorStart = reader.getCursor();
    parseContext.addSuggestion((context, builder) -> {
      builder = builder.createOffset(cursorStart);
      SharedSuggestionProvider.suggest(List.of("any of", "all of"), builder);
      return SharedSuggestionProvider.suggest(Arrays.asList(EnchantmentModificationTarget.Special.values()), builder, EnchantmentModificationTarget.Special::getSerializedName, EnchantmentModificationTarget.Special::getDescription);
    });
    final String unquotedString = reader.readUnquotedString();
    boolean isAll = false;
    boolean hasFoundOf = false;
    if ("all".equals(unquotedString)) {
      if (tryReadWordOf(reader)) {
        isAll = true;
        hasFoundOf = true;
        parseContext.clearSuggestion();
        ParsingUtil.expectAndSkipWhitespace(reader);
      }
    } else if ("any".equals(unquotedString)) {
      if (tryReadWordOf(reader)) {
        hasFoundOf = true;
        parseContext.clearSuggestion();
        ParsingUtil.expectAndSkipWhitespace(reader);
      }
    }
    if (!hasFoundOf) {
      final EnchantmentModificationTarget.@Nullable Special special = EnchantmentModificationTarget.Special.CODEC.byId(unquotedString);
      if (special != null) {
        return special;
      } else {
        reader.setCursor(cursorStart);
      }
    }

    final HolderSet<Enchantment> holders = parseEnchantmentList(parseContext);
    if (holders.size() == 1) {
      return new EnchantmentModificationTarget.Single(holders.iterator().next());
    } else {
      return new EnchantmentModificationTarget.Tag(holders, isAll);
    }
  }

  /**
   * 在解析到关键字“all”或“any”后，尝试读取后面的“of”，如果没有，则返回 {@code restoreCursor} 的位置。
   *
   * @return 是否读取到了单词“of”。
   */
  private static boolean tryReadWordOf(StringReader reader) {
    final int cursorBeforeWhite = reader.getCursor();
    reader.skipWhitespace();
    final int cursorStart = reader.getCursor();
    if (cursorBeforeWhite >= cursorStart) {
      reader.setCursor(cursorBeforeWhite);
      return false;
    }
    final String unquotedString = reader.readUnquotedString();
    if ("of".equals(unquotedString)) {
      return true;
    }
    reader.setCursor(cursorBeforeWhite);
    return false;
  }

  public static <S> HolderSet<Enchantment> parseEnchantmentList(ParseContext<S> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseEnchantmentOrTag(parseContext), input -> switch (input.size()) {
      case 0 -> HolderSet.<Enchantment>empty();
      case 1 -> input.getFirst();
      default -> HolderSet.direct(input.stream().flatMap(HolderSet::stream).toList());
    }, "|", Component.translatable("enhanced_commands.argument.enchantment_modification"), parseContext);
  }

  public static <S> HolderSet<Enchantment> parseEnchantmentOrTag(ParseContext<S> parseContext) throws CommandSyntaxException {
    final HolderLookup.RegistryLookup<Enchantment> lookup = parseContext.registries().lookupOrThrow(Registries.ENCHANTMENT);
    final StringReader reader = parseContext.reader();
    final int cursorStart = reader.getCursor();
    parseContext.addSuggestion((context, builder) -> suggestEnchantmentOrTag(builder.createOffset(cursorStart), lookup));
    if (reader.canRead() && reader.peek() == '#') {
      reader.skip();
      final ResourceLocation tagId = ResourceLocation.read(reader);
      final TagKey<Enchantment> tagKey = TagKey.create(Registries.ENCHANTMENT, tagId);
      final int cursorEnd = reader.getCursor();
      return lookup.get(tagKey).orElseThrow(() -> {
        reader.setCursor(cursorStart);
        return EnhancedCommandSyntaxException.withCursorEnd(UNKNOWN_TAG.createWithContext(reader, tagId.toString(), Registries.ENCHANTMENT.registryKey().location()), cursorEnd);
      });
    } else {
      final ResourceLocation id = ResourceLocation.read(reader);
      final ResourceKey<Enchantment> resourceKey = ResourceKey.create(Registries.ENCHANTMENT, id);
      final int cursorEnd = reader.getCursor();
      final Holder.Reference<Enchantment> holder = lookup.get(resourceKey).orElseThrow(() -> {
        reader.setCursor(cursorStart);
        return EnhancedCommandSyntaxException.withCursorEnd(ResourceArgument.ERROR_UNKNOWN_RESOURCE.createWithContext(reader, id.toString(), Registries.ENCHANTMENT.registryKey().location()), cursorEnd);
      });
      return HolderSet.direct(holder);
    }
  }

  public static CompletableFuture<Suggestions> suggestEnchantmentOrTag(SuggestionsBuilder builder, HolderLookup<Enchantment> lookup) {
    SharedSuggestionProvider.suggestResource(lookup.listElements(), builder, ref -> ref.key().location(), ref -> ref.value().description());
    return SharedSuggestionProvider.suggestResource(lookup.listTags().map(holders -> holders.key().location()), builder, "#");
  }
}
