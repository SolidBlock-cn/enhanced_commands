package pers.solid.ecmd.number;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;

public class ConstantFunctionContentParser implements FunctionContentParser<ConstantValue> {
  private float value;

  @Override
  public ConstantValue getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
    return new ConstantValue(value);
  }

  @Override
  public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
    value = parseContext.reader().readFloat();
  }
}
