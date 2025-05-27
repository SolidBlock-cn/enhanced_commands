package pers.solid.ecmd.predicate.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.regex.Pattern;

public record RegexNbtPredicate(Pattern pattern, boolean inverted) implements NbtPredicate {
  public static final MapCodec<RegexNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      CodecUtil.PATTERN.fieldOf("pattern").forGetter(RegexNbtPredicate::pattern),
      Codec.BOOL.fieldOf("inverted").forGetter(RegexNbtPredicate::inverted)
  ).apply(i, RegexNbtPredicate::new));

  @Override
  public @NotNull String asString() {
    return (inverted ? "!" : "") + "~ " + NbtString.escape(pattern.toString());
  }

  @Override
  public boolean test(@NotNull NbtElement nbtElement) {
    if (!(nbtElement instanceof NbtString nbtString))
      return inverted;
    return inverted != pattern.matcher(nbtString.asString()).find();
  }

  @Override
  public @NotNull NbtPredicateType<RegexNbtPredicate> getType() {
    return Type.REGEX_TYPE;
  }

  public enum Type implements NbtPredicateType<RegexNbtPredicate> {
    REGEX_TYPE;

    @Override
    public MapCodec<RegexNbtPredicate> getCodec() {
      return CODEC;
    }
  }
}
