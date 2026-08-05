package pers.solid.ecmd.enchantment.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

public interface EnchantmentsFunction extends ExpressionConvertible {
  ResourceKey<Registry<EnchantmentsFunction>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("enchantments_function"));
  MapCodec<EnchantmentsFunction> MAP_CODEC = Codec.lazyInitialized(() -> EnchantmentModificationType.CODEC).dispatchMap(EnchantmentsFunction::getType, EnchantmentModificationType::codec);
  Codec<EnchantmentsFunction> CODEC = MAP_CODEC.codec();

  void modify(ItemStack stack, ItemEnchantments.Mutable enchantments, ExecutionContext context);

  EnchantmentModificationType<?> getType();

  static <S> EnchantmentsFunction parse(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorStart = reader.getCursor();
    parseContext.addSuggestion((context, builder) -> {
      builder = builder.createOffset(cursorStart);
      ParsingUtil.suggestString("!", Component.translatable("enhanced_commands.argument.enchantment_modification.remove_enchantment"), builder);
      ParsingUtil.suggestString("natural", Component.translatable("enhanced_commands.argument.enchantment_modification.natural"), builder);
      return builder.buildFuture();
    });

    if (reader.canRead() && reader.peek() == '!') {
      parseContext.clearSuggestion();
      return RemoveEnchantmentsFunction.parse(parseContext);
    }

    final String unquotedString = reader.readUnquotedString();
    if ("natural".equals(unquotedString)) {
      parseContext.clearSuggestion();
      ParsingUtil.expectAndSkipWhitespace(reader);
      return NaturalEnchantmentsFunction.parseAfterKeyword(parseContext);
    } else {
      reader.setCursor(cursorStart);
    }

    return AddEnchantmentsFunction.parse(parseContext);
  }
}
