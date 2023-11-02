package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.regionselection.RegionSelectionType;

public interface ServerPlayerEntityExtension {
  @Nullable
  Region ec$getActiveRegion();

  @Nullable
  default Region ec$getOrEvaluateActiveRegion() {
    final Region activeRegion = ec$getActiveRegion();
    if (activeRegion instanceof RegionSelection regionSelection) {
      return regionSelection.region();
    } else {
      return activeRegion;
    }
  }

  DynamicCommandExceptionType PLAYER_HAS_NO_ACTIVE_REGION = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.region.no_active_region", o));

  default @NotNull Region ec$getOrEvaluateActiveRegionOrThrow() throws CommandSyntaxException {
    final Region region = ec$getOrEvaluateActiveRegion();
    if (region == null) {
      throw PLAYER_HAS_NO_ACTIVE_REGION.create(((ServerPlayerEntity) this).getName());
    }
    return region;
  }

  void ec$setActiveRegion(Region region);

  default void ec$switchRegionSelection(RegionSelection regionSelection) {
    ec$setActiveRegion(regionSelection);
    ec$setRegionSelectionType(regionSelection.getBuilderType());
  }

  default RegionSelection ec$getOrResetRegionSelection() {
    final Region region = ec$getActiveRegion();
    if (region instanceof RegionSelection regionSelection) {
      return regionSelection;
    } else {
      RegionSelection regionSelection = ec$getRegionSelectionType().createRegionSelection();
      ec$setActiveRegion(regionSelection);
      return regionSelection;
    }
  }

  RegionSelectionType ec$getRegionSelectionType();

  void ec$setRegionSelectionType(RegionSelectionType regionSelectionType);

  default void ec$switchRegionSelectionType(RegionSelectionType regionSelectionType) {
    final Region activeRegion = ec$getActiveRegion();
    if (activeRegion instanceof RegionSelection regionSelection) {
      ec$setActiveRegion(regionSelectionType.createRegionSelectionFrom(regionSelection));
    }
    ec$setRegionSelectionType(regionSelectionType);
  }
}
