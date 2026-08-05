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

  public static TagKey<Block> oresConventionalTag() {
    return ConventionalBlockTags.ORES;
  }

  public static TagKey<Block> conventionalBudsTag() {
    return ConventionalBlockTags.BUDS;
  }
}
