package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

public final class BlockPredicateTypes {
  private BlockPredicateTypes() {
  }

  private static <T extends BlockPredicate> BlockPredicateType<T> register(BlockPredicateType<T> value, String name) {
    return Registry.register(BlockPredicateType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), value);
  }

  public static final BlockPredicateType<SimpleBlockPredicate> SIMPLE = register(SimpleBlockPredicate.Type.INSTANCE, "simple");
  public static final BlockPredicateType<NegatingBlockPredicate> NOT = register(NegatingBlockPredicate.Type.INSTANCE, "not");

  public static final BlockPredicateType<HorizontalOffsetBlockPredicate> HORIZONTAL_OFFSET = register(HorizontalOffsetBlockPredicate.Type.INSTANCE, "horizontal_offset");
  public static final BlockPredicateType<PropertyNamesPredicate> PROPERTY_NAMES = register(PropertyNamesPredicate.Type.PROPERTY_NAMES_TYPE, "property_names");
  public static final BlockPredicateType<NbtBlockPredicate> NBT = register(NbtBlockPredicate.Type.NBT_TYPE, "nbt");
  public static final BlockPredicateType<ConstantBlockPredicate> CONSTANT = register(ConstantBlockPredicate.Type.CONSTANT_TYPE, "constant");
  public static final BlockPredicateType<TagBlockPredicate> TAG = register(TagBlockPredicate.Type.INSTANCE, "tag");
  public static final BlockPredicateType<UnionBlockPredicate> UNION = register(UnionBlockPredicate.Type.INSTANCE, "union");
  public static final BlockPredicateType<IntersectBlockPredicate> INTERSECT = register(IntersectBlockPredicate.Type.INSTANCE, "intersect");

  public static final BlockPredicateType<RandBlockPredicate> RAND = register(RandBlockPredicate.Type.INSTANCE, "rand");

  public static final BlockPredicateType<BiPredicateBlockPredicate> BI_PREDICATE = register(BiPredicateBlockPredicate.Type.INSTANCE, "bi_predicate");
  public static final BlockPredicateType<RelBlockPredicate> REL = register(RelBlockPredicate.Type.INSTANCE, "rel");
  public static final BlockPredicateType<ExposeBlockPredicate> EXPOSE = register(ExposeBlockPredicate.Type.INSTANCE, "expose");

  public static void init() {
    Preconditions.checkState(BlockPredicateType.REGISTRY.size() != 0);
  }
}
