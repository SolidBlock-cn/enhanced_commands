package pers.solid.ecmd;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodecs;
import pers.solid.ecmd.regionselection.RegionSelection;

import java.util.Optional;

public final class ModTrackedData {
  public static final TrackedDataHandler<Optional<RegionSelection>> REGION_SELECTION = TrackedDataHandler.create(PacketCodecs.optional(RegionSelection.PACKET_CODEC));
  public static final TrackedData<Optional<RegionSelection>> PLAYER_REGION_SELECTION = DataTracker.registerData(PlayerEntity.class, REGION_SELECTION);

  private ModTrackedData() {
  }

  public static void init() {
    TrackedDataHandlerRegistry.register(REGION_SELECTION);
    ;
  }
}
