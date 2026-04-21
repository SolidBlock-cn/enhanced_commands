package pers.solid.ecmd.item.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

public enum ConstantItemPredicate implements ItemPredicateWithoutContext {
  ALWAYS_TRUE(true),
  ALWAYS_FALSE(false);
  private final boolean value;

  public static final MapCodec<ConstantItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.BOOL.optionalFieldOf("value", true).forGetter(ConstantItemPredicate::value)).apply(i, ConstantItemPredicate::of));

  ConstantItemPredicate(boolean value) {
    this.value = value;
  }

  public boolean value() {
    return value;
  }

  public static ConstantItemPredicate of(boolean value) {
    return value ? ALWAYS_TRUE : ALWAYS_FALSE;
  }

  @Override
  public String expressAsString() {
    return value ? "*" : "!*";
  }

  @Override
  public boolean test(ItemStack stack) {
    return value;
  }

  @Override
  public ItemPredicateType<ConstantItemPredicate> getType() {
    return ItemPredicateTypes.CONSTANT;
  }

  public enum ConstantParser implements Parser<ConstantItemPredicate> {
    INSTANCE;

    @Override
    public @Nullable ConstantItemPredicate parse(ParseContext<?> parseContext) {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("*", Component.translatable("enhanced_commands.block_predicate.constant"), suggestionsBuilder).buildFuture());
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == '*') {
        reader.skip();
        parseContext.clearSuggestion();
        return ALWAYS_TRUE;
      } else {
        return null;
      }
    }
  }
}
