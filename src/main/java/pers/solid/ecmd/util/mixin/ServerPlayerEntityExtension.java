package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.regionselection.RegionSelectionType;

public interface ServerPlayerEntityExtension {
  @Nullable
  RegionSelection getActiveRegion$ec();

  DynamicCommandExceptionType PLAYER_HAS_NO_ACTIVE_REGION = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.region.no_active_region", o));

  default @NotNull RegionSelection getActiveRegionOrThrow$ec() throws CommandSyntaxException {
    final RegionSelection region = getActiveRegion$ec();
    if (region == null) {
      throw PLAYER_HAS_NO_ACTIVE_REGION.create(((ServerPlayerEntity) this).getName());
    }
    return region;
  }

  void setActiveRegion$ec(@Nullable RegionSelection region);

  default void switchActiveRegion$ec(RegionSelection regionSelection) {
    setActiveRegion$ec(regionSelection);
    setRegionSelectionType$ec(regionSelection.getSelectionType());
  }

  default RegionSelection getOrResetRegionSelection$ec() {
    final RegionSelection region = getActiveRegion$ec();
    if (region != null) {
      return region;
    } else {
      RegionSelection regionSelection = getRegionSelectionType$ec().createRegionSelection();
      setActiveRegion$ec(regionSelection);
      return regionSelection;
    }
  }

  RegionSelectionType getRegionSelectionType$ec();

  void setRegionSelectionType$ec(RegionSelectionType regionSelectionType);

  default void switchRegionSelectionType$ec(RegionSelectionType regionSelectionType) {
    final RegionSelection activeRegion = getActiveRegion$ec();
    if (activeRegion != null) {
      setActiveRegion$ec(regionSelectionType.createRegionSelectionFrom(activeRegion));
    }
    setRegionSelectionType$ec(regionSelectionType);
  }
}
