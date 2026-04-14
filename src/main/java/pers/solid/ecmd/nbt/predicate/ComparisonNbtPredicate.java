package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.property.predicate.Comparator;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

/**
 * 匹配一个 NBT 数值是否在数值上与指定的值相等，而不考虑其类型。例如：
 * <pre>
 *   1 match 1s -> true
 *   3b match 3.0f -> true
 *   8s match 8L -> true
 * </pre>
 */
public record ComparisonNbtPredicate(Comparator comparator, Tag expected) implements NbtPredicate {
  public static final MapCodec<ComparisonNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Comparator.CODEC.fieldOf("comparator").forGetter(ComparisonNbtPredicate::comparator),
      CodecUtil.NBT_ELEMENT.fieldOf("expected").forGetter(ComparisonNbtPredicate::expected)
  ).apply(i, ComparisonNbtPredicate::new));

  @Override
  public @NotNull String asString() {
    return comparator.getSerializedName() + " " + TextUtil.toSpacedStringNbt(expected);
  }

  @Override
  public boolean test(@NotNull Tag nbtElement) {
    if (nbtElement instanceof NumericTag actualNumber && expected instanceof NumericTag expectedNumber) {
      final byte actualType = actualNumber.getId();
      final byte expectedType = expectedNumber.getId();
      if (actualType == Tag.TAG_DOUBLE || expectedType == Tag.TAG_DOUBLE) {
        return comparator.compareDouble(actualNumber.getAsDouble(), expectedNumber.getAsDouble());
      } else if (actualType == Tag.TAG_FLOAT || expectedType == Tag.TAG_FLOAT) {
        return comparator.compareFloat(actualNumber.getAsFloat(), expectedNumber.getAsFloat());
      } else if (actualType == Tag.TAG_LONG || expectedType == Tag.TAG_LONG) {
        return comparator.compareLong(actualNumber.getAsLong(), expectedNumber.getAsLong());
      } else if (actualType == Tag.TAG_INT || expectedType == Tag.TAG_INT) {
        return comparator.compareInt(actualNumber.getAsInt(), expectedNumber.getAsInt());
      } else if (actualType == Tag.TAG_SHORT || expectedType == Tag.TAG_SHORT) {
        return comparator.compareShort(actualNumber.getAsShort(), expectedNumber.getAsShort());
      } else if (actualType == Tag.TAG_BYTE || expectedType == Tag.TAG_BYTE) {
        return comparator.compareByte(actualNumber.getAsByte(), expectedNumber.getAsByte());
      } else {
        return false;
      }
    } else if (nbtElement instanceof StringTag actualString && expected instanceof StringTag expectedString) {
      return comparator.test(actualString.getAsString(), expectedString.getAsString());
    } else {
      return false;
    }
  }

  @Override
  public @NotNull NbtPredicateType<ComparisonNbtPredicate> getType() {
    return ComparisonNbtPredicate.Type.COMPARISON_TYPE;
  }

  public enum Type implements NbtPredicateType<ComparisonNbtPredicate> {
    COMPARISON_TYPE;

    @Override
    public MapCodec<ComparisonNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
