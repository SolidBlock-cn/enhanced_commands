package pers.solid.ecmd.config.enums;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

/**
 * 处理如何对虚空伤害进行免疫。
 */
public enum VoidImmuneType implements StringRepresentable {
  /**
   * 不免疫虚空伤害（原版行为）。
   */
  NONE("none"),
  /**
   * 仅限创造模式玩家。
   */
  CREATIVE_ONLY("creative_only"),
  /**
   * 所有玩家均免疫虚空伤害。
   */
  PLAYER_ONLY("player_only"),
  /**
   * 仅限生物免疫虚空伤害，其他实体坠入虚空仍将直接消失。
   */
  LIVING_ENTITY_ONLY("living_entity_only"),
  /**
   * 所有实体均免疫虚空伤害。
   */
  ALL("all");

  public static final StringIdentifiableCodec<VoidImmuneType> CODEC = StringIdentifiableCodec.create(values());
  public static final StreamCodec<ByteBuf, VoidImmuneType> PACKET_CODEC = ByteBufCodecs.fromCodec(CODEC);
  private final String name;

  VoidImmuneType(String name) {
    this.name = name;
  }

  @Override
  public @NotNull String getSerializedName() {
    return this.name;
  }
}
