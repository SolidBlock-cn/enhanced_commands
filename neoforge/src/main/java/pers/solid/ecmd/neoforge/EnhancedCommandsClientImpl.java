package pers.solid.ecmd.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.common.NeoForge;
import pers.solid.ecmd.ActiveRegionRenderer;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.render.DebugRenderLayerCommand;

@Mod(value = EnhancedCommands.MOD_ID, dist = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class EnhancedCommandsClientImpl {
  private static void registerDebugRenderEvent() {
    NeoForge.EVENT_BUS.addListener(RenderHighlightEvent.class, event -> ActiveRegionRenderer.renderActiveRegion(event.getPoseStack(), event.getMultiBufferSource(), event.getCamera()));
  }

  private static void registerClientCommands() {
    NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> DebugRenderLayerCommand.INSTANCE.register(event.getDispatcher(), event.getBuildContext()));
  }
}
