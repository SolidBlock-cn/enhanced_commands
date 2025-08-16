package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.parse.FunctionParamsParser;

public interface RegionType<R extends Region> {
  RegistryKey<Registry<RegionType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("region_type"));
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
  default FunctionParamsParser<? extends RegionArgument<? extends R>> functionParamsParser() {
    return null;
  }

  @NotNull
  MapCodec<R> getCodec();

  @NotNull
  MapCodec<? extends RegionArgument<? extends R>> getArgumentCodec();
}
