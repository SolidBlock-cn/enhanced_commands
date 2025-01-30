package pers.solid.ecmd.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LunarWorldView;
import net.minecraft.world.dimension.DimensionType;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

/**
 * 月相，对应当前月亮的显示方式，与 {@link WorldRenderer#MOON_PHASES} 以及 {@link LunarWorldView#getMoonPhase()} 的返回值对应。
 */
public enum MoonPhase implements StringIdentifiable {
  FULL_MOON("full_moon"),
  WANING_GIBBOUS("waning_gibbous"),
  THIRD_QUARTER("third_quarter"),
  WANING_CRESCENT("waning_crescent"),
  NEW_MOON("new_moon"),
  WAXING_CRESCENT("waxing_crescent"),
  FIRST_QUARTER("first_quarter"),
  WAXING_GIBBOUS("waxing_gibbous");

  private final String name;
  public final Text displayName;
  public final float size;

  public static final ImmutableList<MoonPhase> VALUES = ImmutableList.copyOf(values());
  public static final StringIdentifiableCodec<MoonPhase> CODEC = StringIdentifiableCodec.create(values());

  MoonPhase(String name) {
    this.name = name;
    this.displayName = Text.translatable("enhanced_commands.moon_phase." + name);
    this.size = DimensionType.MOON_SIZES[ordinal()];
  }

  public static MoonPhase byNumericId(int id) {
    return VALUES.get(MathHelper.floorMod(id, VALUES.size()));
  }

  @Override
  public String asString() {
    return name;
  }
}
