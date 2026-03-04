package pers.solid.ecmd.neoforge;

import com.google.common.base.Suppliers;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.regionselection.RegionSelection;

public class EnhancedCommandsDataAttachmentsImpl {
  public static final DeferredRegister<AttachmentType<?>> DEFERRED_REGISTER = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EnhancedCommands.MOD_ID);
  public static final AttachmentType<RegionSelection> REGION_SELECTION = AttachmentType.<RegionSelection>builder(() -> null).serialize(RegionSelection.CODEC).build();  // todo sync?

  public static void init() {
    DEFERRED_REGISTER.register("region_selection", Suppliers.ofInstance(REGION_SELECTION));
  }

  public static @Nullable RegionSelection getActiveRegionForPlayer(Player player) {
    return player.hasData(REGION_SELECTION) ? player.getData(REGION_SELECTION) : null;
  }

  public static void setActiveRegionForPlayer(Player player, @Nullable RegionSelection regionSelection) {
    if (regionSelection == null) {
      player.removeData(REGION_SELECTION);
    } else {
      player.setData(REGION_SELECTION, regionSelection);
    }
  }
}
