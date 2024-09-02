package pers.solid.ecmd.function.block;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.FunctionsParser;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockFunctionTypes {
  public static final Map<String, Supplier<FunctionLikeParser<? extends BlockFunctionArgument>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), BlockFunctionTypes::registerFunctions);
  public static final Map<String, Text> FUNCTION_NAMES = Util.make(new HashMap<>(), BlockFunctionTypes::registerFunctionNames);
  public static final Parser<BlockFunctionArgument> PARENTHESES_PARSER = (registryAccess, parser, suggestionsOnly, allowSparse) -> ParsingUtil.parseParentheses(() -> BlockFunctionArgument.parse(registryAccess, parser, suggestionsOnly, true), parser);
  public static final Parser<BlockFunctionArgument> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
  public static final List<Parser<BlockFunctionArgument>> PARSERS = Lists.newArrayList(PARENTHESES_PARSER, FUNCTIONS_PARSER);

  public static final SimpleBlockFunction.Type SIMPLE = register("simple", SimpleBlockFunction.Type.SIMPLE_TYPE);
  public static final PropertyNamesBlockFunction.Type PROPERTY_NAMES = register("property_names", PropertyNamesBlockFunction.Type.PROPERTY_NAMES_TYPE);
  public static final NbtBlockFunction.Type NBT = register("nbt", NbtBlockFunction.Type.NBT_TYPE);
  public static final PropertiesNbtCombinationBlockFunction.Type PROPERTIES_NBT_COMBINATION = register("properties_nbt_combination", PropertiesNbtCombinationBlockFunction.Type.PROPERTIES_NBT_COMBINATION_TYPE);
  public static final EmptyBlockFunction EMPTY = register("empty", EmptyBlockFunction.INSTANCE);
  public static final RandomBlockFunction.Type RANDOM = register("random", RandomBlockFunction.Type.RANDOM_TYPE);
  public static final TagBlockFunction.Type TAG = register("tag", TagBlockFunction.Type.TAG_TYPE);
  public static final UseOriginalBlockFunction.Type USE_ORIGINAL = register("use_original", UseOriginalBlockFunction.Type.USE_ORIGINAL_TYPE);
  public static final PickBlockFunction.Type PICK = register("pick", PickBlockFunction.Type.PICK_TYPE);
  public static final DryBlockFunction.Type DRY = register("dry", DryBlockFunction.Type.DRY_TYPE);
  public static final OverlayBlockFunction.Type OVERLAY = register("overlay", OverlayBlockFunction.Type.OVERLAY_TYPE);
  public static final FilterBlockFunction.Type FILTER = register("filter", FilterBlockFunction.Type.FILTER_TYPE);
  public static final IdContainBlockFunction.Type ID_CONTAIN = register("id_contain", IdContainBlockFunction.Type.ID_CONTAIN_TYPE);
  public static final StonecutBlockFunction.Type STONE_CUT = register("stonecut", StonecutBlockFunction.Type.STONE_CUT_TYPE);
  public static final ConditionalBlockFunction.Type CONDITIONAL = register("conditional", ConditionalBlockFunction.Type.CONDITIONAL_TYPE);
  public static final ConditionsBlockFunction.Type CONDITIONS = register("conditions", ConditionsBlockFunction.Type.CONDITIONS_TYPE);
  public static final IdReplaceBlockFunction.Type ID_REPLACE = register("id_replace", IdReplaceBlockFunction.Type.ID_REPLACE_TYPE);
  public static final RotateBlockFunction.Type ROTATE = register("rotate", RotateBlockFunction.Type.ROTATE_TYPE);
  public static final MirrorBlockFunction.Type MIRROR = register("mirror", MirrorBlockFunction.Type.MIRROR_TYPE);
  public static final CheckerboardBlockFunction.Type CHECKERBOARD = register("checkerboard", CheckerboardBlockFunction.Type.CHECKERBOARD_TYPE);
  public static final CheckerboardTagBlockFunction.Type CHECKERBOARD_TAG = register("checkerboard-tag", CheckerboardTagBlockFunction.Type.CHECKERBOARD_TAG_TYPE);
  public static final ReferenceBlockFunction.Type REFERENCE = register("reference", ReferenceBlockFunction.Type.INSTANCE);

  private BlockFunctionTypes() {
  }

  @SuppressWarnings("unchecked")
  private static <T extends BlockFunctionType<?>> T register(String name, T value) {
    if (value != SimpleBlockFunction.Type.SIMPLE_TYPE && value instanceof Parser<?> parser) {
      PARSERS.add((Parser<BlockFunctionArgument>) parser);
    }
    return Registry.register(BlockFunctionType.REGISTRY, EnhancedCommands.id(name), value);
  }

  public static void init() {
    Preconditions.checkState(BlockFunctionType.REGISTRY.size() != 0);
  }

  private static void registerFunctions(Map<String, Supplier<FunctionLikeParser<? extends BlockFunctionArgument>>> map) {
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
    map.put("checkerboard", CheckerboardBlockFunction.Parser::new);
    map.put("checkerboard-tag", CheckerboardTagBlockFunction.Parser::new);
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
    map.put("checkerboard", Text.translatable("enhanced_commands.block_function.checkerboard"));
    map.put("checkerboard-tag", Text.translatable("enhanced_commands.block_function.checkerboard-tag"));
  }
}
