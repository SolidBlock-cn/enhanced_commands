package pers.solid.ecmd.region;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.FunctionParamsParser;

public interface RegionType<R extends Region> {
  RegistryKey<Registry<RegionType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(new Identifier(EnhancedCommands.MOD_ID, "region_type"));
  Registry<RegionType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  /**
   * 该区域类型对应的函数名称，用于解析。如果为 null，则表示此区域不是使用函数表示的。
   */
  default String functionName() {
    return null;
  }

  /**
   * 在解析区域函数名称所给出的建议中，显示相应的提示文本。
   */
  default Text tooltip() {
    return null;
  }

  /**
   * 在解析完函数名称并确定为此函数之后，解析相应的函数名称后的内容。如果返回 null，则跳过此解析。注意：只有当函数名称匹配时，此方法才会被调用。
   */
  default FunctionParamsParser<RegionArgument> functionParamsParser() {
    return null;
  }

  /**
   * 从 NBT 中读取数据，并返回符合该类型的 {@link Region} 对象。
   */
  @NotNull R fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world);
}
