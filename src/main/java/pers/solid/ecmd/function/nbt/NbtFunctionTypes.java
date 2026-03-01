package pers.solid.ecmd.function.nbt;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.nbt.PosNbtFunction;
import pers.solid.ecmd.parse.FunctionLikeParser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NbtFunctionTypes {
  @ApiStatus.Internal
  public static final RegistryBridge<NbtFunctionType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, NbtFunctionType.REGISTRY);

  public static final Map<String, Supplier<FunctionLikeParser<? extends NbtFunction>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), NbtFunctionTypes::registerFunctions);
  public static final Map<String, Component> FUNCTION_NAMES = Util.make(new HashMap<>(), NbtFunctionTypes::registerFunctionNames);

  // 基本的 NBT 函数

  public static final NbtFunctionType<CompoundNbtFunction> COMPOUND = register("compound", CompoundNbtFunction.Type.COMPOUND_TYPE);
  public static final NbtFunctionType<ListOpsNbtFunction> LIST_OPS = register("list_ops", ListOpsNbtFunction.Type.LIST_OPS_TYPE);
  public static final NbtFunctionType<NumberValueNbtFunction> NUMBER_VALUE = register("number_value", NumberValueNbtFunction.Type.NUMBER_VALUE_TYPE);
  public static final NbtFunctionType<SimpleNbtFunction> SIMPLE = register("simple", SimpleNbtFunction.Type.SIMPLE_TYPE);

  // 特殊的 NBT 函数

  public static final NbtFunctionType<ConcatNbtFunction> CONCAT = register("concat", ConcatNbtFunction.Type.CONCAT_TYPE);
  public static final NbtFunctionType<GetDataNbtFunction> GET_DATA = register("get_data", GetDataNbtFunction.Type.GET_DATA_TYPE);
  public static final NbtFunctionType<PosNbtFunction> POS = register("pos", PosNbtFunction.Type.POS_TYPE);
  public static final NbtFunctionType<RegexReplaceNbtFunction> REGEX_REPLACE = register("regex_replace", RegexReplaceNbtFunction.Type.REGEX_TYPE);
  public static final NbtFunctionType<ReplaceNbtFunction> REPLACE = register("replace", ReplaceNbtFunction.Type.REPLACE_TYPE);
  public static final NbtFunctionType<StringReplaceNbtFunction> STRING_REPLACE = register("string_replace", StringReplaceNbtFunction.Type.STRING_REPLACE_TYPE);
  public static final NbtFunctionType<SubstringNbtFunction> SUBSTRING = register("substring", SubstringNbtFunction.Type.SUBSTRING_TYPE);

  private static <T extends NbtFunctionType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  public static void init() {
    Preconditions.checkState(!REGISTRY_BRIDGE.isEmpty(), "NbtFunctionType registry is empty!");
  }


  private static void registerFunctions(Map<String, Supplier<FunctionLikeParser<? extends NbtFunction>>> map) {
    map.put("concat", ConcatNbtFunction.Parser::new);
    map.put("from", GetDataNbtFunction.Parser::new);
    map.put("pos", PosNbtFunction.Parser::new);
    map.put("regex_replace", RegexReplaceNbtFunction.Parser::new);
    map.put("replace", ReplaceNbtFunction.Parser::new);
    map.put("string_replace", StringReplaceNbtFunction.Parser::new);
    map.put("substring", SubstringNbtFunction.Parser::new);
  }

  private static void registerFunctionNames(Map<String, Component> map) {
    map.put("concat", Component.translatable("enhanced_commands.nbt_function.concat"));
    map.put("from", Component.translatable("enhanced_commands.nbt_function.from"));
    map.put("pos", Component.translatable("enhanced_commands.nbt_function.pos"));
    map.put("regex_replace", Component.translatable("enhanced_commands.nbt_function.regex_replace"));
    map.put("replace", Component.translatable("enhanced_commands.nbt_function.replace"));
    map.put("string_replace", Component.translatable("enhanced_commands.nbt_function.string_replace"));
    map.put("substring", Component.translatable("enhanced_commands.nbt_function.substring"));
  }
}
