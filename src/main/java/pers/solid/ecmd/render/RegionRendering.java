package pers.solid.ecmd.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 处理区域渲染相关的类。
 */
@Environment(EnvType.CLIENT)
public final class RegionRendering {
  /**
   * 在渲染区域时默认使用的渲染层。
   *
   * @see DebugRenderLayerCommand
   */
  public static RenderType regionRenderLayer = RenderType.LINES;

  private RegionRendering() {
  }

  /**
   * 渲染由两个方块坐标指定的方块区域。会渲染区域边界以及两个顶点的方块边界。两个坐标可以为 null，如果其中一个为 null，表示区域构建还没有完成，此时渲染非 null 的那个方块坐标，并进行闪烁。
   *
   * @param first     第一个方块坐标。
   * @param second    第二个方块坐标。
   * @param cameraPos 相机坐标。
   */
  public static void renderBlockCuboid(@Nullable Vec3i first, @Nullable Vec3i second, PoseStack matrices, VertexConsumer vertexConsumer, @NotNull Vec3 cameraPos) {
    if ((first == null) != (second == null)) {
      // 构建未完成时，闪烁
      final long measuringTimeMs = Util.getMillis();
      if (measuringTimeMs % 600 > 300) {
        return;
      }
    }

    if (first != null && second != null) {
      double x1 = Math.min(first.getX(), second.getX());
      double y1 = Math.min(first.getY(), second.getY());
      double z1 = Math.min(first.getZ(), second.getZ());
      double x2 = Math.max(first.getX(), second.getX()) + 1d;
      double z2 = Math.max(first.getZ(), second.getZ()) + 1d;
      double y2 = Math.max(first.getY(), second.getY()) + 1d;
      ShapeRenderer.renderLineBox(matrices, vertexConsumer, x1 - cameraPos.x, y1 - cameraPos.y, z1 - cameraPos.z, x2 - cameraPos.x, y2 - cameraPos.y, z2 - cameraPos.z, 0.9F, 0.9F, 0.9F, 1.0F, 0.5F, 0.5F, 0.5F);
    }
    if (first != null) {
      matrices.pushPose();
      matrices.translate(first.getX() - cameraPos.x, first.getY() - cameraPos.y, first.getZ() - cameraPos.z);
      VertexUtil.drawUnitBox(matrices, vertexConsumer, Vec3i.ZERO, 0.2f, 0.8f, 1f, 0.9f);
      matrices.popPose();
    }
    if (second != null) {
      matrices.pushPose();
      matrices.translate(second.getX() - cameraPos.x, second.getY() - cameraPos.y, second.getZ() - cameraPos.z);
      VertexUtil.drawUnitBox(matrices, vertexConsumer, Vec3i.ZERO, 0.2f, 1f, 0.8f, 0.9f);
      matrices.popPose();
    }
  }
}
