package pers.solid.ecmd.util.enums;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.LevelTimeAccess;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

/**
 * 月相，对应当前月亮的显示方式，与 {@link SkyRenderer#MOON_LOCATION} 以及 {@link LevelTimeAccess#getMoonPhase()} 的返回值对应。
 */
public enum MoonPhase implements StringRepresentable {
  FULL_MOON("full_moon"),
  WANING_GIBBOUS("waning_gibbous"),
  THIRD_QUARTER("third_quarter"),
  WANING_CRESCENT("waning_crescent"),
  NEW_MOON("new_moon"),
  WAXING_CRESCENT("waxing_crescent"),
  FIRST_QUARTER("first_quarter"),
  WAXING_GIBBOUS("waxing_gibbous");

  private final String name;
  public final Component displayName;
  public final float size;

  public static final ImmutableList<MoonPhase> VALUES = ImmutableList.copyOf(values());
  public static final StringIdentifiableCodec<MoonPhase> CODEC = StringIdentifiableCodec.create(values());

  MoonPhase(String name) {
    this.name = name;
    this.displayName = Component.translatable("enhanced_commands.moon_phase." + name);
    this.size = DimensionType.MOON_BRIGHTNESS_PER_PHASE[ordinal()];
  }

  public static MoonPhase byNumericId(int id) {
    return VALUES.get(Mth.positiveModulo(id, VALUES.size()));
  }

  @Override
  public @NotNull String getSerializedName() {
    return name;
  }
}
