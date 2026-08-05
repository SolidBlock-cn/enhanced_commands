package pers.solid.ecmd.data.neoforge;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SharedCommonTagsImpl {
  private SharedCommonTagsImpl() {
  }

  public static TagKey<Block> oresConventionalTag() {
    return Tags.Blocks.ORES;
  }

  public static TagKey<Block> conventionalBudsTag() {
    return Tags.Blocks.BUDS;
  }
}
