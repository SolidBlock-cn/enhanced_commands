package pers.solid.ecmd.function.block;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.FunctionsParser;
import pers.solid.ecmd.util.Parser;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockFunctionTypes {
  public static final Map<String, Supplier<FunctionParamsParser<? extends BlockFunctionArgument>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), BlockFunctionTypes::registerFunctions);
  public static final Map<String, Text> FUNCTION_NAMES = Util.make(new HashMap<>(), BlockFunctionTypes::registerFunctionNames);
  public static final Parser<BlockFunctionArgument> PARENTHESES_PARSER = (commandRegistryAccess, parser, suggestionsOnly, allowSparse) -> ParsingUtil.parseParentheses(() -> BlockFunctionArgument.parse(commandRegistryAccess, parser, suggestionsOnly, true), parser);
  public static final Parser<BlockFunctionArgument> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
  public static final List<Parser<BlockFunctionArgument>> PARSERS = Lists.newArrayList(PARENTHESES_PARSER, FUNCTIONS_PARSER);
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
  public static final BlockFunctionType<IdContainBlockFunction> ID_CONTAIN = register(IdContainBlockFunction.Type.ID_CONTAIN_TYPE, "id_contain");
  public static final BlockFunctionType<StonecutBlockFunction> STONE_CUT = register(StonecutBlockFunction.Type.STONE_CUT_TYPE, "stonecut");
  public static final BlockFunctionType<ConditionalBlockFunction> CONDITIONAL = register(ConditionalBlockFunction.Type.CONDITIONAL_TYPE, "conditional");
  public static final BlockFunctionType<IdReplaceBlockFunction> ID_REPLACE = register(IdReplaceBlockFunction.Type.ID_REPLACE_TYPE, "id_replace");
  public static final BlockFunctionType<RotateBlockFunction> ROTATE = register(RotateBlockFunction.Type.ROTATE_TYPE, "rotate");
  public static final BlockFunctionType<MirrorBlockFunction> MIRROR = register(MirrorBlockFunction.Type.MIRROR_TYPE, "mirror");

  private BlockFunctionTypes() {
  }

  @SuppressWarnings("unchecked")
  private static <T extends BlockFunctionType<?>> T register(T value, String name) {
    if (value != SimpleBlockFunction.Type.SIMPLE_TYPE && value instanceof Parser<?> parser) {
      PARSERS.add((Parser<BlockFunctionArgument>) parser);
    }
    return Registry.register(BlockFunctionType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), value);
  }

  public static void init() {
    Preconditions.checkState(BlockFunctionType.REGISTRY.size() != 0);
  }

  private static void registerFunctions(Map<String, Supplier<FunctionParamsParser<? extends BlockFunctionArgument>>> map) {
    map.put("pick", PickBlockFunction.Parser::new);
    map.put("dry", DryBlockFunction.Parser::new);
    map.put("overlay", OverlayBlockFunction.Parser::new);
    map.put("filter", FilterBlockFunction.Parser::new);
    map.put("idcontain", IdContainBlockFunction.Parser::new);
    map.put("stonecut", StonecutBlockFunction.Parser::new);
    map.put("if", ConditionalBlockFunction.Parser::new);
    map.put("idreplace", IdReplaceBlockFunction.Parser::new);
    map.put("rotate", RotateBlockFunction.Parser::new);
    map.put("mirror", MirrorBlockFunction.Parser::new);
  }

  private static void registerFunctionNames(Map<String, Text> map) {
    map.put("pick", Text.translatable("enhanced_commands.block_function.pick"));
    map.put("dry", Text.translatable("enhanced_commands.block_function.dry"));
    map.put("overlay", Text.translatable("enhanced_commands.block_function.overlay"));
    map.put("filter", Text.translatable("enhanced_commands.block_function.filter"));
    map.put("idcontain", Text.translatable("enhanced_commands.block_function.id_contain"));
    map.put("stonecut", Text.translatable("enhanced_commands.block_function.stone_cut"));
    map.put("if", Text.translatable("enhanced_commands.block_function.conditional"));
    map.put("idreplace", Text.translatable("enhanced_commands.block_function.id_replace"));
    map.put("rotate", Text.translatable("enhanced_commands.block_function.rotate"));
    map.put("mirror", Text.translatable("enhanced_commands.block_function.mirror"));
  }
}
