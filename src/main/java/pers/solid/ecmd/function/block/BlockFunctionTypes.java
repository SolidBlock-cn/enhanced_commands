package pers.solid.ecmd.function.block;

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

public final class BlockFunctionTypes {
  private BlockFunctionTypes() {
  }

  private static <T extends BlockFunction> BlockFunctionType<T> register(BlockFunctionType<T> value, String name) {
    return Registry.register(BlockFunctionType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), value);
  }

  public static final BlockFunctionType<SimpleBlockFunction> SIMPLE = register(SimpleBlockFunction.Type.SIMPLE_TYPE, "simple");

  public static void init() {
  }
}
