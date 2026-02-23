package pers.solid.ecmd;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import pers.solid.ecmd.regionselection.RegionSelection;

@Environment(EnvType.CLIENT)
public enum ActiveRegionRenderer implements WorldRenderEvents.DebugRender {
  INSTANCE;

  @Override
  public void beforeDebugRender(WorldRenderContext context) {
    final Minecraft client = Minecraft.getInstance();
    final LocalPlayer player = client.player;
    if (player == null) return;
    final RegionSelection activeRegion = player.getActiveRegion$ec();
    if (activeRegion == null) return;

    final PoseStack matrices = context.matrixStack();
    final MultiBufferSource consumers = context.consumers();
    if (matrices == null || consumers == null) return;

    activeRegion.render(matrices, consumers, context.camera().getPosition());
  }
}
