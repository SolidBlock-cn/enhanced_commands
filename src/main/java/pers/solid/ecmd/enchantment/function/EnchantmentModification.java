package pers.solid.ecmd.enchantment.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

public interface EnchantmentModification extends ExpressionConvertible {
  MapCodec<EnchantmentModification> CODEC = Codec.lazyInitialized(() -> EnchantmentModificationType.CODEC).dispatchMap(EnchantmentModification::getType, EnchantmentModificationType::codec);

  void modify(ItemStack stack, ItemEnchantments.Mutable enchantments, ExecutionContext context);

  EnchantmentModificationType<?> getType();

  static <S> EnchantmentModification parse(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorStart = reader.getCursor();
    parseContext.addSuggestion((context, builder) -> {
      ParsingUtil.suggestString("!", Component.translatable("enhanced_commands.argument.enchantment_modification.remove_enchantment"), builder);
      ParsingUtil.suggestString("natural", Component.translatable("enhanced_commands.argument.enchantment_modification.natural"), builder);
      return builder.buildFuture();
    });

    if (reader.canRead() && reader.peek() == '!') {
      parseContext.clearSuggestion();
      return RemoveEnchantmentModification.parse(parseContext);
    }

    final String unquotedString = reader.readUnquotedString();
    if ("natural".equals(unquotedString)) {
      parseContext.clearSuggestion();
      ParsingUtil.expectAndSkipWhitespace(reader);
      NaturalEnchantmentModification.parseAfterKeyword(parseContext);
    } else {
      reader.setCursor(cursorStart);
    }

    return AddEnchantmentModification.parse(parseContext);
  }
}
