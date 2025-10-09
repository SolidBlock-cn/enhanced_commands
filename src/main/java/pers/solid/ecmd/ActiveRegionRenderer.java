package pers.solid.ecmd;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import pers.solid.ecmd.regionselection.RegionSelection;

@Environment(EnvType.CLIENT)
public enum ActiveRegionRenderer implements WorldRenderEvents.DebugRender {
  INSTANCE;

  @Override
  public void beforeDebugRender(WorldRenderContext context) {
    final MinecraftClient client = MinecraftClient.getInstance();
    final ClientPlayerEntity player = client.player;
    if (player == null) return;
    final RegionSelection activeRegion = player.getActiveRegion$ec();
    if (activeRegion == null) return;

    final MatrixStack matrices = context.matrixStack();
    final VertexConsumerProvider consumers = context.consumers();
    if (matrices == null || consumers == null) return;

    activeRegion.render(matrices, consumers, context.camera().getPos());
  }
}
