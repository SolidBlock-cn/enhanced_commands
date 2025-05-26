package pers.solid.ecmd.function.nbt;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.parse.FunctionLikeParser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NbtFunctionTypes {

  public static final Map<String, Supplier<FunctionLikeParser<? extends NbtFunctionArgument>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), NbtFunctionTypes::registerFunctions);
  public static final Map<String, Text> FUNCTION_NAMES = Util.make(new HashMap<>(), NbtFunctionTypes::registerFunctionNames);

  // 基本的 NBT 函数

  public static final NbtFunctionType<CompoundNbtFunction> COMPOUND = register("compound", CompoundNbtFunction.Type.COMPOUND_TYPE);
  public static final NbtFunctionType<ListOpsNbtFunction> LIST_OPS = register("list_ops", ListOpsNbtFunction.Type.LIST_OPS_TYPE);
  public static final NbtFunctionType<NumberValueNbtFunction> NUMBER_VALUE = register("number_value", NumberValueNbtFunction.Type.NUMBER_VALUE_TYPE);
  public static final NbtFunctionType<SimpleNbtFunction> SIMPLE = register("simple", SimpleNbtFunction.Type.SIMPLE_TYPE);

  // 特殊的 NBT 函数

  public static final NbtFunctionType<StringReplaceNbtFunction> STRING_REPLACE = register("string_replace", StringReplaceNbtFunction.Type.STRING_REPLACE_TYPE);

  private static <T extends NbtFunctionType<?>> T register(String name, T value) {
    return Registry.register(NbtFunctionType.REGISTRY, EnhancedCommands.id(name), value);
  }

  public static void init() {
    Preconditions.checkState(NbtFunctionType.REGISTRY.size() != 0);
  }


  private static void registerFunctions(Map<String, Supplier<FunctionLikeParser<? extends NbtFunctionArgument>>> map) {

  }

  private static void registerFunctionNames(Map<String, Text> map) {

  }
}
