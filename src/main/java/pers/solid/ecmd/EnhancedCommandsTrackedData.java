package pers.solid.ecmd;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import pers.solid.ecmd.regionselection.RegionSelection;

import java.util.Optional;

public final class EnhancedCommandsTrackedData {
  public static final EntityDataSerializer<Optional<RegionSelection>> REGION_SELECTION = EntityDataSerializer.forValueType(ByteBufCodecs.optional(RegionSelection.PACKET_CODEC));
  public static final EntityDataAccessor<Optional<RegionSelection>> DATA_ACTIVE_REGION_ID = SynchedEntityData.defineId(Player.class, REGION_SELECTION);

  private EnhancedCommandsTrackedData() {
  }

  @ExpectPlatform
  public static void init(InitializeContext context) {
    EntityDataSerializers.registerSerializer(REGION_SELECTION);
  }
}
