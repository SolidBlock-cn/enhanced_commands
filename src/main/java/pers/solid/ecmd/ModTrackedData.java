package pers.solid.ecmd;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import pers.solid.ecmd.regionselection.RegionSelection;

import java.util.Optional;

public final class ModTrackedData {
  public static final EntityDataSerializer<Optional<RegionSelection>> REGION_SELECTION = EntityDataSerializer.forValueType(ByteBufCodecs.optional(RegionSelection.PACKET_CODEC));
  public static final EntityDataAccessor<Optional<RegionSelection>> PLAYER_REGION_SELECTION = SynchedEntityData.defineId(Player.class, REGION_SELECTION);

  private ModTrackedData() {
  }

  public static void init() {
    EntityDataSerializers.registerSerializer(REGION_SELECTION);
    ;
  }
}
