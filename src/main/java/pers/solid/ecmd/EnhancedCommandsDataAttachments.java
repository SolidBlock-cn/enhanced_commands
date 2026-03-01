package pers.solid.ecmd;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.regionselection.RegionSelection;

public final class EnhancedCommandsDataAttachments {

  private EnhancedCommandsDataAttachments() {
  }

  @ExpectPlatform
  public static void init() {
    throw new AssertionError();
  }

  @ExpectPlatform
  public static @Nullable RegionSelection getActiveRegionForPlayer(Player player) {
    throw new AssertionError();
  }

  @ExpectPlatform
  public static void setActiveRegionForPlayer(Player player, @Nullable RegionSelection regionSelection) {
    throw new AssertionError();
  }
}
