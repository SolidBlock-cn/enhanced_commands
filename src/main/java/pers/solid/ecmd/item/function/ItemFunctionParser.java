package pers.solid.ecmd.item.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.mixins.ItemParserAccessor;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;

import java.util.Optional;

public final class ItemFunctionParser {
  public static final Component OVERLAY_TOOLTIP = Component.translatable("enhanced_commands.function.overlay.symbol_tooltip");
  public static final Component PICK_TOOLTIP = Component.translatable("enhanced_commands.function.pick.symbol_tooltip");

  private ItemFunctionParser() {
  }

  public static ItemFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return parsePick(parseContext);
  }

  public static ItemFunction parsePick(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseOverlay(parseContext), functions -> {
      ImmutableList.Builder<ItemFunction> builder = new ImmutableList.Builder<>();
      for (ItemFunction function : functions) {
        builder.add(function);
      }
      return new PickItemFunction(new WeightedList.Uniform<>(builder.build()));
    }, "|", PICK_TOOLTIP, parseContext);
  }

  public static ItemFunction parseOverlay(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseUnit(parseContext), functions -> {
      ImmutableList.Builder<ItemFunction> builder = new ImmutableList.Builder<>();
      for (ItemFunction blockFunction : functions) {
        builder.add(blockFunction);
      }
      return new OverlayItemFunction(builder.build());
    }, "*", OVERLAY_TOOLTIP, parseContext);
  }

  public static <S> ItemFunction parseUnit(ParseContext<S> context) throws CommandSyntaxException {
    return parseBase(context);
  }

  public static <S> ItemFunction parseBase(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();

    final HolderLookup.RegistryLookup<Item> itemLookup = parseContext.registries().lookupOrThrow(Registries.ITEM);
    parseContext.addSuggestion((context, builder) -> {
      if (builder.getRemaining().isEmpty()) {
        builder.suggest("*");
        builder.suggest("@");
      }
      return SharedSuggestionProvider.suggestResource(itemLookup.listElements(), builder, entry -> entry.key().location(), entry -> entry.value().getDescription());
    });

    if (reader.canRead()) {
      switch (reader.peek()) {
        case '*' -> {
          reader.skip();
          parseContext.clearSuggestion();
          return new RandomItemFunction();
        }
        case '@' -> {
          reader.skip();
          parseContext.clearSuggestion();
          return parseUnlimitedId(parseContext);
        }
      }
    }

    final int cursorBeforeId = reader.getCursor();
    final ResourceLocation identifier = ResourceLocation.read(reader);
    final int cursorAfterId = reader.getCursor();

    final Optional<Holder.Reference<Item>> itemReference = itemLookup.get(ResourceKey.create(Registries.ITEM, identifier));
    if (itemReference.isEmpty()) {
      reader.setCursor(cursorBeforeId);
      throw EnhancedCommandSyntaxException.withCursorEnd(ItemParserAccessor.getERROR_UNKNOWN_ITEM().createWithContext(reader, identifier), cursorAfterId);
    }

    parseContext.clearSuggestion();
    return new SimpleItemFunction(itemReference.get());
  }

  private static <S> ItemFunction parseUnlimitedId(ParseContext<S> parseContext) throws CommandSyntaxException {
    parseContext.setSuggestion((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.entrySet(), builder, entry -> entry.getKey().location(), entry -> entry.getValue().getName(entry.getValue().getDefaultInstance())));

    final StringReader reader = parseContext.reader();
    final int cursorBeforeId = reader.getCursor();
    final ResourceLocation identifier = ResourceLocation.read(reader);
    final int cursorAfterId = reader.getCursor();

    final Optional<Holder.Reference<Item>> itemReference = BuiltInRegistries.ITEM.getHolder(identifier);
    if (itemReference.isEmpty()) {
      reader.setCursor(cursorBeforeId);
      throw EnhancedCommandSyntaxException.withCursorEnd(ItemParserAccessor.getERROR_UNKNOWN_ITEM().createWithContext(reader, identifier), cursorAfterId);
    }

    parseContext.clearSuggestion();
    return new SimpleItemFunction(itemReference.get());
  }
}
