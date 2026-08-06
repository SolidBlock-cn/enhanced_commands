package pers.solid.ecmd.region;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.util.pack.ReferenceEntry;

import java.util.function.Supplier;

public final class RegionTypes {
  private static final RegistryBridge<RegionType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, RegionType.REGISTRY);

  public static final RegionType<SingleBlockPosRegion> SINGLE = register("single", SingleBlockPosRegion.CODEC, SingleBlockPosRegionProvider.CODEC, Suppliers.ofInstance(SingleBlockPosRegion.FunctionParser.INSTANCE));
  public static final RegionType<BlockCuboidRegion> CUBOID = register("cuboid", BlockCuboidRegion.CODEC, BlockCuboidRegionProvider.CODEC, "cuboid", Component.translatable("enhanced_commands.region.cuboid"), BlockCuboidRegion.Parser::new);
  public static final RegionType<PreciseCuboidRegion> CUBOID_PRECISE = register("cuboid_precise", PreciseCuboidRegion.CODEC, PreciseCuboidRegionProvider.CODEC, "cuboid_precise", Component.translatable("enhanced_commands.region.cuboid"), PreciseCuboidRegion.Parser::new);
  public static final RegionType<SphereRegion> SPHERE = register("sphere", SphereRegion.CODEC, SphereRegionProvider.CODEC, SphereRegion.Parser::new);
  public static final RegionType<IntersectRegion> INTERSECT = register("intersect", IntersectRegion.CODEC, IntersectRegionProvider.CODEC, IntersectRegion.Parser::new);
  public static final RegionType<UnionRegion> UNION = register("union", UnionRegion.CODEC, UnionRegionProvider.CODEC, UnionRegion.Parser::new);
  public static final RegionType<OutlineRegion> OUTLINE = register("outline", OutlineRegion.CODEC, OutlineRegionProvider.CODEC, OutlineRegion.Parser::new);
  public static final RegionType<CylinderRegion> CYLINDER = register("cylinder", CylinderRegion.CODEC, CylinderRegionProvider.CODEC, "cyl", Component.translatable("enhanced_commands.region.cylinder"), CylinderRegion.Parser::new);
  public static final RegionType<HollowCylinderRegion> HOLLOW_CYLINDER = register("hollow_cylinder", HollowCylinderRegion.CODEC, HollowCylinderRegionProvider.CODEC, "hcyl", Component.translatable("enhanced_commands.region.hollow_cylinder"), HollowCylinderRegion.Parser::new);
  public static final RegionType<CuboidOutlineRegion> CUBOID_OUTLINE = register("cuboid_outline", CuboidOutlineRegion.CODEC, CuboidOutlineRegionProvider.CODEC, CuboidOutlineRegion.Parser::new);
  public static final RegionType<CuboidWallRegion> CUBOID_WALL = register("cuboid_wall", CuboidWallRegion.CODEC, CuboidWallRegionProvider.CODEC, CuboidWallRegion.Parser::new);
  public static final RegionType<OutwardsRegion> OUTWARDS = register("outwards", OutwardsRegion.CODEC, OutwardsRegionProvider.CODEC, OutwardsRegion.Parser::new);

  public static final RegionType<Region> ACTIVE_REGION = register("active_region", null, ActiveRegionProvider.CODEC, null, null, Suppliers.ofInstance(null));
  public static final RegionType<Region> REFERENCE = register("reference", null, ReferenceRegionProvider.CODEC, "reference", Component.translatable("enhanced_commands.region.reference"), () -> new ReferenceEntry.ReferenceFunctionGrammarParser<>(ReferenceRegionProvider.PREFIXED_ID_PARSER));

  private RegionTypes() {
  }

  private static <T extends RegionType<?>> T register(String name, T value) {
    final String functionName = value.functionName();
    if (functionName != null) {
      RegionParsing.FUNCTIONS_PARSER.register(functionName, value::tooltip, value::parser);
    }
    return REGISTRY_BRIDGE.register(name, value);
  }

  private static <T extends Region> RegionType<T> register(String name, @Nullable MapCodec<T> codec, MapCodec<? extends RegionProvider<T>> providerCodec, Supplier<@Nullable FunctionContentParser<? extends RegionProvider<? extends T>>> parserSupplier) {
    return register(name, new RegionType.Simple<>(codec == null ? failingCodec(name) : codec, providerCodec, name, Component.translatable("enhanced_commands.region." + name), parserSupplier));
  }

  private static <T extends Region> RegionType<T> register(String name, @Nullable MapCodec<T> codec, MapCodec<? extends RegionProvider<T>> providerCodec, @Nullable String functionName, @Nullable Component tooltip, Supplier<@Nullable FunctionContentParser<? extends RegionProvider<? extends T>>> parserSupplier) {
    return register(name, new RegionType.Simple<>(codec == null ? failingCodec(name) : codec, providerCodec, functionName, tooltip, parserSupplier));
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(RegionType.REGISTRY, context);
    RegionParsing.PARSERS.add(ActiveRegionParser.INSTANCE);
    RegionParsing.PARSERS.add(ReferenceRegionProvider.PREFIXED_ID_PARSER);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
  }

  private static <T extends Region> MapCodec<T> failingCodec(String name) {
    final Supplier<String> stringSupplier = () -> "The region type '" + name + "' does not support serialization";
    return Codec.EMPTY.flatXmap(unit -> DataResult.error(stringSupplier), o -> DataResult.error(stringSupplier));
  }
}
