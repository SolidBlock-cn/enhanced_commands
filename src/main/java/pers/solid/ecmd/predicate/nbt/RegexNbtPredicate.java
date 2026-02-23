package pers.solid.ecmd.predicate.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.regex.Pattern;

public record RegexNbtPredicate(Pattern pattern, boolean inverted) implements NbtPredicate {
  public static final MapCodec<RegexNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      CodecUtil.PATTERN.fieldOf("pattern").forGetter(RegexNbtPredicate::pattern),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(RegexNbtPredicate::inverted)
  ).apply(i, RegexNbtPredicate::new));

  @Override
  public @NotNull String asString() {
    return (inverted ? "!" : "") + "~ " + StringTag.quoteAndEscape(pattern.toString());
  }

  @Override
  public boolean test(@NotNull Tag nbtElement) {
    if (!(nbtElement instanceof StringTag nbtString))
      return inverted;
    return inverted != pattern.matcher(nbtString.getAsString()).find();
  }

  @Override
  public @NotNull NbtPredicateType<RegexNbtPredicate> getType() {
    return pers.solid.ecmd.predicate.nbt.RegexNbtPredicate.Type.REGEX_TYPE;
  }

  public enum Type implements NbtPredicateType<RegexNbtPredicate> {
    REGEX_TYPE;

    @Override
    public MapCodec<RegexNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
