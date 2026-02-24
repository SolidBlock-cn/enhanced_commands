package pers.solid.ecmd.config;

public class BlockOperationConfig implements Cloneable {
  public static final BlockOperationConfig DEFAULT = new BlockOperationConfig();
  public static BlockOperationConfig current = DEFAULT;
  /**
   * 最大的历史记录的次数。
   */
  public int maxHistoryCount = 50;

  /**
   * 最大的区域大小，对于 {@code /setblocks} 等命令，在该数值内的区域操作不需要参数 {@code bypass_limit = true}。
   */
  public int regionSizeLimit = 16777215;

  @Override
  public BlockOperationConfig clone() {
    try {
      return (BlockOperationConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
