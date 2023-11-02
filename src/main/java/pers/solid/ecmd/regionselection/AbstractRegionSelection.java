package pers.solid.ecmd.regionselection;

import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.Region;

public abstract class AbstractRegionSelection<R extends Region> implements RegionSelection {
  protected @Nullable R calculatedRegion;

  /**
   * 建立起绝对的区域。
   */
  public abstract R buildRegion();

  @Override
  public R region() {
    if (calculatedRegion == null) {
      calculatedRegion = buildRegion();
    }
    return calculatedRegion;
  }

  public void resetCalculation() {
    this.calculatedRegion = null;
  }

  @Override
  public RegionSelection clone() {
    try {
      return (RegionSelection) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new InternalError(e);
    }
  }
}
