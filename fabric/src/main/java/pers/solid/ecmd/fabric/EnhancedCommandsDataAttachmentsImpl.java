package pers.solid.ecmd.fabric;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.regionselection.RegionSelection;

public class EnhancedCommandsDataAttachmentsImpl {
  public static final AttachmentType<RegionSelection> REGION_SELECTION = AttachmentRegistry.create(EnhancedCommands.id("region_selection"), builder -> builder.persistent(RegionSelection.CODEC).syncWith(RegionSelection.PACKET_CODEC, AttachmentSyncPredicate.targetOnly()).copyOnDeath());

  public static void init() {
  }

  public static @Nullable RegionSelection getActiveRegionForPlayer(Player player) {
    return player.getAttached(REGION_SELECTION);
  }

  public static void setActiveRegionForPlayer(Player player, @Nullable RegionSelection regionSelection) {
    player.setAttached(REGION_SELECTION, regionSelection);
  }
}
