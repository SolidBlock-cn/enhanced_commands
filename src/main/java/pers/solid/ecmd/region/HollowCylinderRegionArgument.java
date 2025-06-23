package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.enums.OutlineType;

public record HollowCylinderRegionArgument(OutlineType outlineType, CylinderRegionArgument region) implements RegionArgument<HollowCylinderRegion> {
  public static final MapCodec<HollowCylinderRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          OutlineType.OUTLINE_TYPE_FIELD.forGetter(HollowCylinderRegionArgument::outlineType),
          CylinderRegionArgument.CODEC.fieldOf("region").forGetter(HollowCylinderRegionArgument::region))
      .apply(i, HollowCylinderRegionArgument::new));

  @Override
  public HollowCylinderRegion toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return new HollowCylinderRegion(outlineType, region.toAbsoluteRegion(source));
  }

  @Override
  public @NotNull RegionType<HollowCylinderRegion> getType() {
    return RegionTypes.HOLLOW_CYLINDER;
  }

  @Override
  public @NotNull String asString() {
    return "hcyl(" + region.radius() + ", " + region.height() + ", " + region.center().asString() + ", " + outlineType.asString() + ")";
  }
}
