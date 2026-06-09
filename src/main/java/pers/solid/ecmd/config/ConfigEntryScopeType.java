package pers.solid.ecmd.config;

import net.minecraft.util.StringRepresentable;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

/**
 * 本模组中的配置项的作用范围。不同配置项有不同的作用范围，例如，仅影响渲染或客户端行为的配置项作用范围为客户端，仅影响服务器各类行为的配置项作用范围为服务器，需要双端配合才能正常生效的配置项作用范围为双端。
 */
public enum ConfigEntryScopeType implements StringRepresentable {
  /**
   * 配置项仅在客户端使用，主要影响渲染等，服务器无需知晓。
   */
  CLIENT("client"),
  /**
   * 配置项仅在服务器使用，服务器根据配置影响内容操作，客户端无需知晓。
   */
  SERVER("server"),
  /**
   * 配置项在服务器和客户端双端均可用，并且通常双端需要保持一致才可正常运作，因此需要同步。
   */
  BOTH("both");

  private final String name;
  public static final StringIdentifiableCodec<ConfigEntryScopeType> CODEC = StringIdentifiableCodec.create(values());

  ConfigEntryScopeType(String name) {
    this.name = name;
  }

  @Override
  public String getSerializedName() {
    return name;
  }
}
