package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.regionbuilder.RegionBuilder;
import pers.solid.ecmd.regionbuilder.RegionBuilderType;

public interface ServerPlayerEntityExtension {
  @Nullable RegionArgument<?> ec$getActiveRegion();

  @Nullable
  default Region ec$getOrEvaluateActiveRegion(ServerCommandSource source) throws CommandSyntaxException {
    final RegionBuilder regionBuilder = ec$getRegionBuilder();
    final RegionArgument<?> activeRegion = ec$getActiveRegion();
    if (activeRegion == null && regionBuilder != null) {
      final Region buildRegion = regionBuilder.buildRegion();
      ec$setActiveRegion(buildRegion);
      return buildRegion;
    } else if (activeRegion != null) {
      return activeRegion.toAbsoluteRegion(source);
    } else {
      return null;
    }
  }

  void ec$setActiveRegion(RegionArgument<?> regionArgument);

  @Nullable RegionBuilder ec$getRegionBuilder();

  default void ec$requireUpdateRegionBuilder() {
    ec$setActiveRegion(null);
  }

  void ec$setRegionBuilder(RegionBuilder regionBuilder);

  RegionBuilderType ec$getRegionBuilderType();

  void ec$setRegionBuilderType(RegionBuilderType regionBuilderType);

  default void ec$switchRegionBuilderType(RegionBuilderType regionBuilderType) {
    final RegionBuilder regionBuilder = ec$getRegionBuilder();
    if (regionBuilder != null) {
      ec$setRegionBuilder(regionBuilderType.createRegionBuilderFrom(regionBuilder));
      ec$requireUpdateRegionBuilder();
    }
    ec$setRegionBuilderType(regionBuilderType);
  }
}
