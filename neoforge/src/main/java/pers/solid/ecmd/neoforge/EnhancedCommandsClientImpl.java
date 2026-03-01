package pers.solid.ecmd.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import pers.solid.ecmd.ActiveRegionRenderer;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.neoforge.BeforeDebugRenderEvent;
import pers.solid.ecmd.render.DebugRenderLayerCommand;

@Mod(value = EnhancedCommands.MOD_ID, dist = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class EnhancedCommandsClientImpl {
  public EnhancedCommandsClientImpl() {
    registerClientCommands();
    registerDebugRenderEvent();
  }

  private static void registerDebugRenderEvent() {
    NeoForge.EVENT_BUS.addListener(BeforeDebugRenderEvent.class, event -> ActiveRegionRenderer.renderActiveRegion(event.poseStack, event.bufferSource, event.camera));
  }

  private static void registerClientCommands() {
    NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> DebugRenderLayerCommand.INSTANCE.register(event.getDispatcher(), event.getBuildContext()));
  }
}
