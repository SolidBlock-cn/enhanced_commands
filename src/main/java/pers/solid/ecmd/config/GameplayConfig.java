package pers.solid.ecmd.config;

import pers.solid.ecmd.config.annotations.ConfigEntryScope;

public class GameplayConfig implements Cloneable {
  public static final GameplayConfig DEFAULT = new GameplayConfig();
  public static GameplayConfig current = DEFAULT;

  /**
   * 即使在耕地上践踏、跳跃，也不会破坏耕地。
   */
  @ConfigEntryScope(ConfigEntryScopeType.SERVER)
  public boolean protectFarmland;

  /**
   * 创造模式玩家飞行时，可以飞过方块，且在方块内部时不会被阻挡视野。
   */
  @ConfigEntryScope(ConfigEntryScopeType.BOTH)
  public boolean flyThroughBlocks;

  @Override
  public GameplayConfig clone() {
    try {
      return (GameplayConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
