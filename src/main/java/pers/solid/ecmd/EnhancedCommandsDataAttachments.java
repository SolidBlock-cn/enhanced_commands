package pers.solid.ecmd;

import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * 此模组中的 data attachment，目前仅作预留。
 */
public final class EnhancedCommandsDataAttachments {

  private EnhancedCommandsDataAttachments() {
  }

  @ExpectPlatform
  public static void init() {
    throw new AssertionError();
  }
}
