package pers.solid.ecmd.region;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionContentParser;

import java.util.function.Supplier;

public interface RegionType<R extends Region> {
  ResourceKey<Registry<RegionType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("region_type"));
  Registry<RegionType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);

  /**
   * 该区域类型对应的函数名称，用于解析。如果为 null，则表示此区域不是使用函数表示的。
   */
  default @Nullable String functionName() {
    return null;
  }

  /**
   * 在解析区域函数名称所给出的建议中，显示相应的提示文本。
   */
  default @Nullable Component tooltip() {
    return null;
  }

  /**
   * 在解析完函数名称并确定为此函数之后，解析相应的函数名称后的内容。如果返回 null，则跳过此解析。注意：只有当函数名称匹配时，此方法才会被调用。
   */
  default @Nullable FunctionContentParser<? extends RegionProvider<? extends R>> parser() {
    return null;
  }

  MapCodec<R> codec();

  MapCodec<? extends RegionProvider<? extends R>> providerCodec();

  record Simple<R extends Region>(MapCodec<R> codec, MapCodec<? extends RegionProvider<R>> providerCodec, @Nullable String functionName, @Nullable Component tooltip, Supplier<@Nullable FunctionContentParser<? extends RegionProvider<? extends R>>> parserSupplier) implements RegionType<R> {
    @Override
    public @Nullable FunctionContentParser<? extends RegionProvider<? extends R>> parser() {
      return parserSupplier.get();
    }
  }
}
