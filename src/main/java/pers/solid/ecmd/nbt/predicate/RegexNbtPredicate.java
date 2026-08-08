package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.regex.Pattern;

public record RegexNbtPredicate(Pattern pattern) implements NbtPredicate, DoesNotRequireValidation {
  public static final MapCodec<RegexNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      ExtraCodecs.PATTERN.fieldOf("pattern").forGetter(RegexNbtPredicate::pattern)
  ).apply(i, RegexNbtPredicate::new));

  @Override
  public String expressAsString() {
    return "~ " + StringTag.quoteAndEscape(pattern.toString());
  }

  @Override
  public boolean test(Tag nbtElement, ExecutionContext context) {
    if (!(nbtElement instanceof StringTag nbtString))
      return false;
    return pattern.matcher(nbtString.getAsString()).find();
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof RegexNbtPredicate that)) return false;

    return pattern.toString().equals(that.pattern.toString());
  }

  @Override
  public int hashCode() {
    int result = pattern.toString().hashCode();
    result = 31 * result + Boolean.hashCode(false);
    return result;
  }

  @Override
  public NbtPredicateType<RegexNbtPredicate> getType() {
    return NbtPredicateTypes.REGEX;
  }
}
