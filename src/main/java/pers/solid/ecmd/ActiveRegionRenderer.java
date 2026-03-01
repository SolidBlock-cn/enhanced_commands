package pers.solid.ecmd;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import pers.solid.ecmd.regionselection.RegionSelection;

@Environment(EnvType.CLIENT)
public enum ActiveRegionRenderer {
  INSTANCE;

  public static void renderActiveRegion(PoseStack matrices, MultiBufferSource consumers, Camera camera) {
    final LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) return;
    final RegionSelection activeRegion = player.getActiveRegion$ec();
    if (activeRegion == null) return;

    if (matrices == null || consumers == null) return;

    activeRegion.render(matrices, consumers, camera.getPosition());
  }
}
