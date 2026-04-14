package pers.solid.ecmd.nbt.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record AllNbtPredicate(List<NbtPredicate> predicates) implements NbtPredicate {
  public static final MapCodec<AllNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AllNbtPredicate::predicates)
  ).apply(i, AllNbtPredicate::new));

  @Override
  public String asString() {
    return predicates.stream().map(NbtPredicate::asString).collect(Collectors.joining(", ", "all(", ")"));
  }

  @Override
  public boolean test(Tag nbtElement) {
    return predicates.stream().allMatch(p -> p.test(nbtElement));
  }

  @Override
  public Type getType() {
    return Type.ALL_TYPE;
  }

  public enum Type implements NbtPredicateType<AllNbtPredicate> {
    ALL_TYPE;

    @Override
    public MapCodec<AllNbtPredicate> getCodec() {
      return CODEC;
    }
  }

  public record Parser(List<NbtPredicate> nbtPredicates) implements FunctionContentParser.SequentialParams<AllNbtPredicate> {
    public Parser() {
      this(new ArrayList<>());
    }

    @Override
    public AllNbtPredicate getParseResult(ParseContext<?> parseContext) {
      return new AllNbtPredicate(nbtPredicates);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      nbtPredicates.add(NbtPredicateParser.parseNbtPredicate(parseContext, false, false));
    }
  }
}
