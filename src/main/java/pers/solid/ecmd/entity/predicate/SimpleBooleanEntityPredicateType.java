package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;

import java.util.function.Predicate;

public class SimpleBooleanEntityPredicateType implements EntityPredicateType<SimpleBooleanEntityPredicateEntry> {
  protected final Predicate<Entity> predicate;
  protected final String baseTranslationKey;
  protected final String trueTranslationKey;
  protected final String falseTranslationKey;
  protected final String optionName;
  private final MapCodec<SimpleBooleanEntityPredicateEntry> codec;

  protected SimpleBooleanEntityPredicateType(Predicate<Entity> predicate, String baseTranslationKey, String trueTranslationKey, String falseTranslationKey, String optionName) {
    this.predicate = predicate;
    this.baseTranslationKey = baseTranslationKey;
    this.trueTranslationKey = trueTranslationKey;
    this.falseTranslationKey = falseTranslationKey;
    this.optionName = optionName;
    this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.BOOL.fieldOf("expected").forGetter(SimpleBooleanEntityPredicateEntry::expected)
    ).apply(i, this::createPredicate));
  }

  public static SimpleBooleanEntityPredicateType create(Predicate<Entity> predicate, String baseTranslationKey, String optionName) {
    return new SimpleBooleanEntityPredicateType(predicate, baseTranslationKey, baseTranslationKey + "." + true, baseTranslationKey + "." + false, optionName);
  }

  public SimpleBooleanEntityPredicateEntry createPredicate(boolean expected) {
    return new SimpleBooleanEntityPredicateEntry(this, expected);
  }

  @Override
  public MapCodec<SimpleBooleanEntityPredicateEntry> codec() {
    return codec;
  }
}
