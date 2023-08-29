package pers.solid.ecmd.function.block;

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

public final class BlockFunctionTypes {
  private BlockFunctionTypes() {
  }

  private static <T extends BlockFunction> BlockFunctionType<T> register(BlockFunctionType<T> value, String name) {
    return Registry.register(BlockFunctionType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), value);
  }

  public static final BlockFunctionType<SimpleBlockFunction> SIMPLE = register(SimpleBlockFunction.Type.SIMPLE_TYPE, "simple");
  public static final BlockFunctionType<PropertyNamesBlockFunction> PROPERTY_NAMES = register(PropertyNamesBlockFunction.Type.PROPERTY_NAMES_TYPE, "property_names");
  public static final BlockFunctionType<NbtBlockFunction> NBT = register(NbtBlockFunction.Type.NBT_TYPE, "nbt");
  public static final BlockFunctionType<PropertiesNbtCombinationBlockFunction> PROPERTIES_NBT_COMBINATION = register(PropertiesNbtCombinationBlockFunction.Type.PROPERTIES_NBT_COMBINATION_TYPE, "property_name_combination");
  public static final BlockFunctionType<RandomBlockFunction> RANDOM = register(RandomBlockFunction.Type.RANDOM_TYPE, "random");
  public static final BlockFunctionType<TagBlockFunction> TAG = register(TagBlockFunction.Type.TAG_TYPE, "tag");
  public static final BlockFunctionType<UseOriginalBlockFunction> USE_ORIGINAL = register(UseOriginalBlockFunction.Type.USE_ORIGINAL_TYPE, "use_original");
  public static final BlockFunctionType<PickBlockFunction> PICK = register(PickBlockFunction.Type.PICK_TYPE, "pick");
  public static final BlockFunctionType<DryBlockFunction> DRY = register(DryBlockFunction.Type.DRY_TYPE, "dry");
  public static final BlockFunctionType<OverlayBlockFunction> OVERLAY = register(OverlayBlockFunction.Type.OVERLAY_TYPE, "overlay");
  public static final BlockFunctionType<FilterBlockFunction> FILTER = register(FilterBlockFunction.Type.FILTER_TYPE, "filter");
  public static final BlockFunctionType<IdMatchBlockFunction> ID_MATCH = register(IdMatchBlockFunction.Type.ID_MATCH_TYPE, "id_match");
  public static final BlockFunctionType<StonecutBlockFunction> STONE_CUT = register(StonecutBlockFunction.Type.STONE_CUT_TYPE, "stonecut");
  public static final BlockFunctionType<ConditionalBlockFunction> CONDITIONAL = register(ConditionalBlockFunction.Type.CONDITIONAL_TYPE, "conditional");

  public static void init() {
  }
}
