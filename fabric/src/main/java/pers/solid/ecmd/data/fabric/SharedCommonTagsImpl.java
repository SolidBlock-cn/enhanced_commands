package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.data.SharedCommonTags;

/**
 * @see SharedCommonTags
 */
@ApiStatus.Internal
public class SharedCommonTagsImpl {
  private SharedCommonTagsImpl() {
  }

  public static TagKey<Block> ores() {
    return ConventionalBlockTags.ORES;
  }

  public static TagKey<Block> buds() {
    return ConventionalBlockTags.BUDS;
  }

  public static TagKey<Block> villagerJobSites() {
    return ConventionalBlockTags.VILLAGER_JOB_SITES;
  }
}
