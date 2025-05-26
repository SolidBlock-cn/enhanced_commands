package pers.solid.ecmd.function.nbt;

import com.google.common.base.Preconditions;
import net.minecraft.registry.Registry;
import pers.solid.ecmd.EnhancedCommands;

public final class NbtFunctionTypes {

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
}
