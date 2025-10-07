package pers.solid.ecmd.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

/**
 * 处理渲染相关的类，主要用于几何图形的渲染。此类仅在客户端下可用，非客户端代码不得使用这里面的方法。
 */
@Environment(EnvType.CLIENT)
public final class VertexUtil {

  /**
   * 绘制一个单位为 1 的立方体，指定三个 int 表示立方体的坐标。
   */
  public static void drawUnitBox(MatrixStack matrices, VertexConsumer vertexConsumer, int x, int y, int z, float red, float green, float blue, float alpha) {
    VertexRendering.drawBox(matrices, vertexConsumer, x, y, z, x + 1.0d, y + 1.0d, z + 1.0d, red, green, blue, alpha);
  }

  /**
   * 绘制一个单位为 1 的立方体，指定一个 {@link Vec3i} 表示立方体的坐标。。
   */
  public static void drawUnitBox(MatrixStack matrices, VertexConsumer vertexConsumer, Vec3i vec3i, float red, float green, float blue, float alpha) {
    drawUnitBox(matrices, vertexConsumer, vec3i.getX(), vec3i.getY(), vec3i.getZ(), red, green, blue, alpha);
  }

  /**
   * 绘制一个以指定的 {@link Vec3d} 坐标为中心的单位立方体。
   */
  public static void drawUnitBoxCentered(MatrixStack matrices, VertexConsumer vertexConsumer, Vec3d center, float red, float green, float blue, float alpha) {
    drawUnitBoxCentered(matrices, vertexConsumer, center.x, center.y, center.z, red, green, blue, alpha);
  }

  /**
   * 绘制一个以指定坐标为中心的单位立方体。
   */
  public static void drawUnitBoxCentered(MatrixStack matrices, VertexConsumer vertexConsumer, double x, double y, double z, float red, float green, float blue, float alpha) {
    VertexRendering.drawBox(matrices, vertexConsumer, x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5, red, green, blue, alpha);
  }

  /**
   * <p>绘制一条连接两点 (x1, y1, z1) 和 (x2, y2, z2) 的直线段，并指定两端的颜色。
   * <p>相比 {@link VertexRendering#drawVector}，此方法更灵活，且更加有助于优化内存，因为避免反复创建对象。
   *
   * @see VertexRendering#drawVector
   */
  public static void drawLineConnecting(MatrixStack matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, int argb, int argb2) {
    MatrixStack.Entry entry = matrices.peek();
    float dx = x2 - x1;
    float dy = y2 - y1;
    float dz = z2 - z1;
    vertexConsumer.vertex(entry, x1, y1, z1)
        .color(argb)
        .normal(entry, dx, dy, dz);
    vertexConsumer.vertex(entry, x2, y2, z2)
        .color(argb2)
        .normal(entry, dx, dy, dz);
  }

  /**
   * <p>绘制一条连接两点 (x1, y1, z1) 和 (x2, y2, z2) 的直线段，并指定两端的颜色。
   * <p>此方法在执行时，会把 double 转化为 float，这是考虑到世界内许多坐标都是 double 格式的，转化为 float 是为了方便。
   * <p>相比 {@link VertexRendering#drawVector}，此方法更灵活，且更加有助于优化内存，因为避免反复创建对象。
   *
   * @see VertexRendering#drawVector
   */
  public static void drawLineConnecting(MatrixStack matrices, VertexConsumer vertexConsumer, double x1, double y1, double z1, double x2, double y2, double z2, int argb, int argb2) {
    drawLineConnecting(matrices, vertexConsumer, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, argb, argb2);
  }


  /**
   * <p>绘制一条连接两点 (x1, y1, z1) 和 (x2, y2, z2) 的直线段，并指定两端为相同的颜色。
   * <p>相比 {@link VertexRendering#drawVector}，此方法更灵活，且更加有助于优化内存，因为避免反复创建对象。
   *
   * @see VertexRendering#drawVector
   */
  public static void drawLineConnecting(MatrixStack matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
    drawLineConnecting(matrices, vertexConsumer, x1, y1, z1, x2, y2, z2, argb, argb);
  }

  /**
   * <p>绘制一条连接两点 (x1, y1, z1) 和 (x2, y2, z2) 的直线段，并指定两端为相同的颜色。
   * <p>此方法在执行时，会把 double 转化为 float，这是考虑到世界内许多坐标都是 double 格式的，转化为 float 是为了方便。
   * <p>相比 {@link VertexRendering#drawVector}，此方法更灵活，且更加有助于优化内存，因为避免反复创建对象。
   *
   * @see VertexRendering#drawVector
   */
  public static void drawLineConnecting(MatrixStack matrices, VertexConsumer vertexConsumer, double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
    drawLineConnecting(matrices, vertexConsumer, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, argb, argb);
  }

  private VertexUtil() {
  }
}
