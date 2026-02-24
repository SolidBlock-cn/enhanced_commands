package pers.solid.ecmd.util.enums;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

/**
 * 批量方块操作在遇到没有加载的坐标时的行为。
 */
public enum UnloadedPosBehavior implements StringRepresentable {
  /**
   * 在执行操作前，如果检测到没有加载的坐标，直接提前拒绝整个操作。
   */
  REJECT("reject"),
  /**
   * 在执行方块操作的过程中，如果遇到没有加载的坐标，直接跳过它，后面在执行的过程中遇到已加载的坐标可正常执行。
   */
  SKIP("skip"),
  /**
   * 在执行方块操作的过程中，如果遇到没有加载的坐标，则中止整个操作，即使在后面遇到已加载的坐标也忽略，但是此前的操作不受影响。
   */
  BREAK("break"),
  /**
   * 强制加载未加载的坐标。
   */
  FORCE("force");
  private final String name;
  public static final StringIdentifiableCodec<UnloadedPosBehavior> CODEC = StringIdentifiableCodec.create(UnloadedPosBehavior.values());
  public static final ImmutableList<UnloadedPosBehavior> VALUES = ImmutableList.copyOf(values());

  UnloadedPosBehavior(String name) {
    this.name = name;
  }

  @Override
  public @NotNull String getSerializedName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  public MutableComponent getDescription() {
    return Component.translatable("enhanced_commands.argument.unloaded_pos_behavior." + name);
  }
}
