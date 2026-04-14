package pers.solid.ecmd.nbt.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Objects;
import java.util.regex.Pattern;

public record RegexNbtPredicate(Pattern pattern) implements NbtPredicate {
  public static final MapCodec<RegexNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      CodecUtil.PATTERN.fieldOf("pattern").forGetter(RegexNbtPredicate::pattern)
  ).apply(i, RegexNbtPredicate::new));

  @Override
  public String asString() {
    return "~ " + StringTag.quoteAndEscape(pattern.toString());
  }

  @Override
  public boolean test(Tag nbtElement) {
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
    return RegexNbtPredicate.Type.REGEX_TYPE;
  }

  public enum Type implements NbtPredicateType<RegexNbtPredicate> {
    REGEX_TYPE;

    @Override
    public MapCodec<RegexNbtPredicate> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionContentParser<RegexNbtPredicate> {
    private @Nullable Pattern pattern;

    @Override
    public RegexNbtPredicate getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(pattern, "pattern");
      return new RegexNbtPredicate(pattern);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      pattern = ParsingUtil.readRegex(parseContext.reader());
    }
  }
}
