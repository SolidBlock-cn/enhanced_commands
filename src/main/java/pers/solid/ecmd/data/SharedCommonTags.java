package pers.solid.ecmd.data;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SharedCommonTags {
  private SharedCommonTags() {
  }

  @ExpectPlatform
  public static TagKey<Block> ores() {
    throw new AssertionError();
  }

  @ExpectPlatform
  static TagKey<Block> buds() {
    throw new AssertionError();
  }

  @ExpectPlatform
  static TagKey<Block> villagerJobSites() {
    throw new AssertionError();
  }
}
