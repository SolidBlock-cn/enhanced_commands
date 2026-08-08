package pers.solid.ecmd.nbt.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record AnyNbtPredicate(List<NbtPredicate> predicates) implements NbtPredicate {
  public static final MapCodec<AnyNbtPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AnyNbtPredicate::predicates)
  ).apply(i, AnyNbtPredicate::new));

  @Override
  public String expressAsString() {
    return predicates.stream().map(NbtPredicate::expressAsString).collect(Collectors.joining(", ", "any(", ")"));
  }

  @Override
  public boolean test(Tag nbtElement, ExecutionContext context) {
    return predicates.stream().anyMatch(p -> p.test(nbtElement, context));
  }

  @Override
  public NbtPredicateType<AnyNbtPredicate> getType() {
    return NbtPredicateTypes.ANY;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return predicates;
  }

  public record Parser(List<NbtPredicate> nbtPredicates) implements FunctionContentParser.SequentialParams<AnyNbtPredicate> {
    public Parser() {
      this(new ArrayList<>());
    }

    @Override
    public AnyNbtPredicate getParseResult(ParseContext<?> parseContext) {
      return new AnyNbtPredicate(nbtPredicates);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      nbtPredicates.add(NbtPredicateParser.parseNbtPredicate(parseContext, false, false));
    }
  }
}
