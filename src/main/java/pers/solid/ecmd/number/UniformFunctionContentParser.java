package pers.solid.ecmd.number;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Objects;

public class UniformFunctionContentParser implements FunctionContentParser.SequentialParams<UniformGenerator> {
  private @Nullable NumberProvider min, max;

  @Override
  public UniformGenerator getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
    Objects.requireNonNull(min, "min");
    Objects.requireNonNull(max, "max");
    return new UniformGenerator(min, max);
  }

  @Override
  public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
    switch (paramIndex) {
      case 0 -> min = NumberProviderParser.parse(parseContext);
      case 1 -> max = NumberProviderParser.parse(parseContext);
    }
  }
}
