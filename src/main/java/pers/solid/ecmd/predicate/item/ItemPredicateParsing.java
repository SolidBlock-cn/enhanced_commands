package pers.solid.ecmd.predicate.item;

import com.google.common.base.Supplier;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemPredicateParsing {
  public static final Map<String, Supplier<FunctionContentParser<? extends ItemPredicate>>> FUNCTIONS = new LinkedHashMap<>();
  public static final Map<String, Component> FUNCTION_NAMES = new HashMap<>();
  /**
   * 解析方块函数中的函数语法、
   */
  public static final Parser<ItemPredicate> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
}
