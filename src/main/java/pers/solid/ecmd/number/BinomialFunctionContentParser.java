package pers.solid.ecmd.number;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Objects;

public class BinomialFunctionContentParser implements FunctionContentParser.SequentialParams<BinomialDistributionGenerator> {
  private @Nullable NumberProvider n, p;

  @Override
  public BinomialDistributionGenerator getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
    Objects.requireNonNull(n, "n");
    Objects.requireNonNull(p, "p");
    return new BinomialDistributionGenerator(n, p);
  }

  @Override
  public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
    switch (paramIndex) {
      case 0 -> n = NumberProviderParser.parse(parseContext);
      case 1 -> p = NumberProviderParser.parse(parseContext);
    }
  }

  @Override
  public int minSequentialParamsCount() {
    return 2;
  }

  @Override
  public int maxSequentialParamsCount() {
    return 2;
  }
}
