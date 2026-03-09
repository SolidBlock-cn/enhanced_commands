package pers.solid.ecmd.function.nbt;

import com.google.common.base.Supplier;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.parse.FunctionLikeParser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NbtFunctionParsing {
  public static final Map<String, Supplier<FunctionLikeParser<? extends NbtFunction>>> FUNCTIONS = new LinkedHashMap<>();
  public static final Map<String, Component> FUNCTION_NAMES = new HashMap<>();

  private NbtFunctionParsing() {
  }
}
