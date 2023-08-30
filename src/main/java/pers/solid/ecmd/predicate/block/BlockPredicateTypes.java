package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

public final class BlockPredicateTypes {
  public static final BlockPredicateType<SimpleBlockPredicate> SIMPLE = register(SimpleBlockPredicate.Type.SIMPLE_TYPE, "simple");
  public static final BlockPredicateType<NegatingBlockPredicate> NEGATING = register(NegatingBlockPredicate.Type.NEGATING_TYPE, "negating");
  public static final BlockPredicateType<HorizontalOffsetBlockPredicate> HORIZONTAL_OFFSET = register(HorizontalOffsetBlockPredicate.Type.HORIZONTAL_OFFSET_TYPE, "horizontal_offset");
  public static final BlockPredicateType<PropertyNamesBlockPredicate> PROPERTY_NAMES = register(PropertyNamesBlockPredicate.Type.PROPERTY_NAMES_TYPE, "property_names");
  public static final BlockPredicateType<NbtBlockPredicate> NBT = register(NbtBlockPredicate.Type.NBT_TYPE, "nbt");
  public static final BlockPredicateType<PropertiesNbtCombinationBlockPredicate> PROPERTIES_NBT_COMBINATION = register(PropertiesNbtCombinationBlockPredicate.Type.PROPERTIES_NBT_COMBINATION_TYPE, "properties_nbt_combination");
  public static final BlockPredicateType<ConstantBlockPredicate> CONSTANT = register(ConstantBlockPredicate.Type.CONSTANT_TYPE, "constant");
  public static final BlockPredicateType<TagBlockPredicate> TAG = register(TagBlockPredicate.Type.TAG_TYPE, "tag");
  public static final BlockPredicateType<UnionBlockPredicate> UNION = register(UnionBlockPredicate.Type.UNION_TYPE, "union");
  public static final BlockPredicateType<IntersectBlockPredicate> INTERSECT = register(IntersectBlockPredicate.Type.INTERSECT_TYPE, "intersect");
  public static final BlockPredicateType<RandBlockPredicate> RAND = register(RandBlockPredicate.Type.RAND_TYPE, "rand");
  public static final BlockPredicateType<BiPredicateBlockPredicate> BI_PREDICATE = register(BiPredicateBlockPredicate.Type.BI_PREDICATE_TYPE, "bi_predicate");
  public static final BlockPredicateType<RelBlockPredicate> REL = register(RelBlockPredicate.Type.REL_TYPE, "rel");
  public static final BlockPredicateType<ExposeBlockPredicate> EXPOSE = register(ExposeBlockPredicate.Type.EXPOSE_TYPE, "expose");
  public static final BlockPredicateType<IdContainBlockPredicate> ID_CONTAIN = register(IdContainBlockPredicate.Type.ID_CONTAIN_TYPE, "id_contain");
  public static final BlockPredicateType<RegionBlockPredicate> REGION = register(RegionBlockPredicate.Type.REGION_TYPE, "region");

  private BlockPredicateTypes() {
  }

  private static <T extends BlockPredicate> BlockPredicateType<T> register(BlockPredicateType<T> value, String name) {
    return Registry.register(BlockPredicateType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), value);
  }

  public static void init() {
    Preconditions.checkState(BlockPredicateType.REGISTRY.size() != 0);
  }
}
