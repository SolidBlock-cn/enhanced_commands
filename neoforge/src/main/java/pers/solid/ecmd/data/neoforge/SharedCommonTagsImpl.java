package pers.solid.ecmd.data.neoforge;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SharedCommonTagsImpl {
  private SharedCommonTagsImpl() {
  }

  public static TagKey<Block> ores() {
    return Tags.Blocks.ORES;
  }

  public static TagKey<Block> buds() {
    return Tags.Blocks.BUDS;
  }

  public static TagKey<Block> villagerJobSites() {
    return Tags.Blocks.VILLAGER_JOB_SITES;
  }
}
