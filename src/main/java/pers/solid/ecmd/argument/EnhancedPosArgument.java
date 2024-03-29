package pers.solid.ecmd.argument;

import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

/**
 * @see net.minecraft.command.argument.PosArgument
 */
public interface EnhancedPosArgument extends PosArgument {
  /**
   * @return Whether the returned position refers to integer of double.
   */
  boolean isInt();

  static boolean isInt(PosArgument posArgument) {
    return posArgument instanceof EnhancedPosArgument enhancedPosArgument && enhancedPosArgument.isInt();
  }

  abstract class DefaultPos implements EnhancedPosArgument {
    protected final boolean xRelative, yRelative, zRelative;

    protected DefaultPos(boolean xRelative, boolean yRelative, boolean zRelative) {
      this.xRelative = xRelative;
      this.yRelative = yRelative;
      this.zRelative = zRelative;
    }

    @Override
    public boolean isXRelative() {
      return xRelative;
    }

    @Override
    public boolean isYRelative() {
      return yRelative;
    }

    @Override
    public boolean isZRelative() {
      return zRelative;
    }
  }

  class DoublePos extends DefaultPos {
    private final double x, y, z;

    protected DoublePos(double x, double y, double z, boolean xRelative, boolean yRelative, boolean zRelative) {
      super(xRelative, yRelative, zRelative);
      this.x = x;
      this.y = y;
      this.z = z;
    }

    @Override
    public Vec3d toAbsolutePos(ServerCommandSource source) {
      if (!xRelative && !yRelative && !zRelative) {
        return new Vec3d(x, y, z);
      }
      final Vec3d position = source.getPosition();
      return new Vec3d(xRelative ? position.x + x : x, yRelative ? position.y + y : y, zRelative ? position.z + z : z);
    }

    @Override
    public Vec2f toAbsoluteRotation(ServerCommandSource source) {
      if (!xRelative && !yRelative) {
        return new Vec2f((float) x, (float) y);
      }
      final Vec2f rotation = source.getRotation();
      return new Vec2f((float) (xRelative ? rotation.x + x : x), (float) (yRelative ? rotation.y + y : y));
    }

    @Override
    public boolean isInt() {
      return false;
    }
  }

  class IntPos extends DefaultPos {
    private final int x, y, z;
    private final EnhancedPosArgumentType.IntAlignType intAlignType;

    protected IntPos(int x, int y, int z, boolean xRelative, boolean yRelative, boolean zRelative, EnhancedPosArgumentType.IntAlignType intAlignType) {
      super(xRelative, yRelative, zRelative);
      this.x = x;
      this.y = y;
      this.z = z;
      this.intAlignType = intAlignType;
    }

    protected IntPos(int x, int y, int z, boolean xRelative, boolean yRelative, boolean zRelative) {
      this(x, y, z, xRelative, yRelative, zRelative, EnhancedPosArgumentType.IntAlignType.CENTERED);
    }

    @Override
    public boolean isInt() {
      return true;
    }

    @Override
    public Vec3d toAbsolutePos(ServerCommandSource source) {
      final BlockPos blockPos = toAbsoluteBlockPos(source);
      return intAlignType.mayAdjustToCenter(blockPos);
    }

    @Override
    public Vec2f toAbsoluteRotation(ServerCommandSource source) {
      if (!xRelative && !yRelative) {
        return new Vec2f((float) x, (float) y);
      }
      final Vec2f rotation = source.getRotation();
      return new Vec2f(xRelative ? rotation.x + x : x, yRelative ? rotation.y + y : y);
    }

    @Override
    public BlockPos toAbsoluteBlockPos(ServerCommandSource source) {
      if (!xRelative && !yRelative && !zRelative) {
        return new BlockPos(x, y, z);
      }
      final Vec3d position = source.getPosition();
      return new BlockPos(xRelative ? MathHelper.floor(position.getX() + x) : x, yRelative ? MathHelper.floor(position.getY() + y) : y, zRelative ? MathHelper.floor(position.getZ() + z) : z);
    }
  }
}
