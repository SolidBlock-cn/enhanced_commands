package pers.solid.ecmd.enchantment.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.apache.commons.lang3.mutable.MutableBoolean;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.ExecutionContext;

public record AddEnchantmentsFunction(EnchantmentModificationTarget enchantment, EnchantmentLevelProvider level, boolean supportedOnly, boolean clamp, boolean upgradeOnly) implements EnchantmentsFunction {
  public static final MapCodec<AddEnchantmentsFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnchantmentModificationTarget.CODEC.fieldOf("enchantment").forGetter(AddEnchantmentsFunction::enchantment),
      EnchantmentLevelProvider.CODEC.fieldOf("level").forGetter(AddEnchantmentsFunction::level),
      Codec.BOOL.optionalFieldOf("supported_only", false).forGetter(AddEnchantmentsFunction::supportedOnly),
      Codec.BOOL.optionalFieldOf("clamp", false).forGetter(AddEnchantmentsFunction::clamp),
      Codec.BOOL.optionalFieldOf("upgrade_only", false).forGetter(AddEnchantmentsFunction::upgradeOnly)
  ).apply(i, AddEnchantmentsFunction::new));

  @Override
  public void modify(ItemStack stack, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
    enchantment.streamEnchantments(stack, context).forEach(enchantment -> {
      if (supportedOnly && (!enchantment.value().canEnchant(stack) || !EnchantmentHelper.isEnchantmentCompatible(enchantments.keySet(), enchantment))) {
        return;
      }
      int levelCalculated = level.get(enchantment, enchantments, context);
      if (clamp) {
        levelCalculated = Mth.clamp(levelCalculated, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
      }
      enchantments.set(enchantment, levelCalculated);
    });
  }

  @Override
  public EnchantmentModificationType<AddEnchantmentsFunction> getType() {
    return EnchantmentModificationTypes.ADD;
  }

  public static <S> AddEnchantmentsFunction parse(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final EnchantmentModificationTarget enchantment = EnchantmentModificationTargetParser.parse(parseContext);
    final int cursorAfterEnchantment = reader.getCursor();

    reader.skipWhitespace();
    final MutableBoolean supportedOnly = new MutableBoolean();
    final MutableBoolean clamp = new MutableBoolean();
    final MutableBoolean upgradeOnly = new MutableBoolean();
    if (reader.canRead()) {
      final char peek = reader.peek();
      final boolean r = tryParseParameters(parseContext, reader, supportedOnly, clamp, upgradeOnly);
      if (peek == ')' || peek == ',' || peek == ';' || r) {
        if (!r) {
          reader.setCursor(cursorAfterEnchantment);
        }
        return new AddEnchantmentsFunction(enchantment, new EnchantmentLevelProvider.Basic(ConstantValue.exactly(1)), supportedOnly.booleanValue(), clamp.booleanValue(), upgradeOnly.booleanValue());
      }
    }
    reader.setCursor(cursorAfterEnchantment);
    ParsingUtil.expectAndSkipWhitespace(reader);
    final EnchantmentLevelProvider levelProvider = EnchantmentLevelProvider.parse(parseContext);

    parseContext.clearSuggestion();
    tryParseParameters(parseContext, reader, supportedOnly, clamp, upgradeOnly);

    return new AddEnchantmentsFunction(enchantment, levelProvider, supportedOnly.booleanValue(), clamp.booleanValue(), upgradeOnly.booleanValue());
  }

  /**
   * 尝试读取参数，如果读取到了就进行相应的修改，否则将 cursor 退回到 whitespace 之前。
   *
   * @return 是否解析到了参数语法
   */
  private static <S> boolean tryParseParameters(ParseContext<S> parseContext, StringReader reader, MutableBoolean supportedOnly, MutableBoolean clamp, MutableBoolean upgradeOnly) throws CommandSyntaxException {
    final int cursorAfterLevel = reader.getCursor();
    reader.skipWhitespace();
    parseContext.addSuggestion((context, builder) -> {
      if (builder.getRemaining().isEmpty()) {
        builder.suggest("-", Component.translatable("enhanced_commands.enchantments_function.add.parameters"));
      }
      return builder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == '-') {
      // 进入后续参数部分的解析
      reader.skip();
      reader.skipWhitespace();
      parseContext.setSuggestion((context, builder) -> {
        if (builder.getRemaining().isEmpty()) {
          if (!supportedOnly.booleanValue()) {
            builder.suggest("s", Component.translatable("enhanced_commands.enchantments_function.add.parameters.s"));
          }
          if (!clamp.booleanValue()) {
            builder.suggest("c", Component.translatable("enhanced_commands.enchantments_function.add.parameters.c"));
          }
          if (!upgradeOnly.booleanValue()) {
            builder.suggest("u", Component.translatable("enhanced_commands.enchantments_function.add.parameters.u"));
          }
        }
        return builder.buildFuture();
      });

      if (!reader.canRead()) {
        throw EnhancedCommandsCommandExceptionTypes.EXPECTED_3_SYMBOLS.createWithContext(reader, "s", "c", "u");
      }

      while (reader.canRead() && Character.isAlphabetic(reader.peek())) {
        final int cursor = reader.getCursor();
        switch (reader.peek()) {
          case 's' -> {
            if (supportedOnly.booleanValue()) {
              throw EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, "s"), cursor + 1);
            } else {
              reader.skip();
              supportedOnly.setTrue();
            }
          }
          case 'c' -> {
            if (clamp.booleanValue()) {
              throw EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, "c"), cursor + 1);
            } else {
              reader.skip();
              clamp.setTrue();
            }
          }
          case 'u' -> {
            if (upgradeOnly.booleanValue()) {
              throw EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.DUPLICATE_KEYWORD.createWithContext(reader, "u"), cursor + 1);
            } else {
              reader.skip();
              upgradeOnly.setTrue();
            }
          }
          default -> throw EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.UNKNOWN_KEYWORD.createWithContext(reader, String.valueOf(reader.peek())), cursor + 1);
        }
      }
      if (reader.canRead() && !Character.isAlphabetic(reader.peek())) {
        parseContext.clearSuggestion();
      }
      return true;
    } else {
      // 没有解析到参数部分，回到原来的位置，并完成解析
      reader.setCursor(cursorAfterLevel);
      return false;
    }
  }

  @Override
  public String expressAsString() {
    final StringBuilder sb = new StringBuilder(enchantment.asEntryString());
    sb.append(' ');
    sb.append(level.expressAsString());
    if (supportedOnly || clamp) {
      sb.append(" -");
      if (supportedOnly) sb.append("s");
      if (clamp) sb.append("c");
      if (upgradeOnly) sb.append("u");
    }
    return sb.toString();
  }
}
