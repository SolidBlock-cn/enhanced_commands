package pers.solid.ecmd.nbt.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record RegexNbtPredicate(Pattern pattern) implements NbtPredicate {
  public static final MapCodec<RegexNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      ExtraCodecs.PATTERN.fieldOf("pattern").forGetter(RegexNbtPredicate::pattern)
  ).apply(i, RegexNbtPredicate::new));

  @Override
  public String expressAsString() {
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
    return NbtPredicateTypes.REGEX;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of();
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
