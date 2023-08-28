package pers.solid.ecmd.function.block;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 此接口用于对方块实体进行修改。注意：这些修改都只是对修改进行记录，记录修改后的数据，最后会在 {@link BlockFunction#setBlock(World, BlockPos, int)} 中，在设置方块状态之后，一次性地修改这个方块实体。
 */
public interface BlockEntityDataAccess {
  /**
   * 在整个修改过程中的 NBT。在初始的时候，即使方块实体存在，其 NBT 仍会是 null，因为并不会先去获取整个方块实体的 NBT 数据。
   */
  @Nullable NbtCompound getCurrentNbt();

  /**
   * 设置当前的 NBT。
   */

  void setCurrentNbt(NbtCompound nbt);

  /**
   * @return 进行方块状态设置之后的方块实体。
   */
  @Nullable BlockEntity getBlockEntity();

  static BlockEntityDataAccess of(BlockEntity blockEntity) {
    return new BlockEntityDataAccess() {
      NbtCompound nbt = null;

      @Override
      public @Nullable NbtCompound getCurrentNbt() {
        return nbt;
      }

      @Override
      public void setCurrentNbt(NbtCompound nbt) {
        this.nbt = nbt;
      }

      @Override
      public @Nullable BlockEntity getBlockEntity() {
        return blockEntity;
      }
    };
  }
}
