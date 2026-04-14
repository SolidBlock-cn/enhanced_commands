package pers.solid.ecmd.block.function;

import com.google.common.base.Supplier;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.Parser;

import java.util.Map;

/**
 * 本模组的所有方块函数类型。每个类型都需要通过 {@link #register} 方法注册。只有注册了类型的方块函数才能正确编码与解码。
 */
public final class BlockFunctionTypes {
  private static final RegistryBridge<BlockFunctionType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, BlockFunctionType.REGISTRY);


  public static final SimpleBlockFunction.Type SIMPLE = register("simple", SimpleBlockFunction.Type.SIMPLE_TYPE);
  public static final PropertyNamesBlockFunction.Type PROPERTY_NAMES = register("property_names", PropertyNamesBlockFunction.Type.PROPERTY_NAMES_TYPE);
  public static final NbtBlockFunction.Type NBT = register("nbt", NbtBlockFunction.Type.NBT_TYPE);
  public static final PropertiesNbtCombinationBlockFunction.Type PROPERTIES_NBT_COMBINATION = register("properties_nbt_combination", PropertiesNbtCombinationBlockFunction.Type.PROPERTIES_NBT_COMBINATION_TYPE);
  public static final EmptyBlockFunction EMPTY = register("empty", EmptyBlockFunction.INSTANCE);
  public static final RandomBlockFunction.Type RANDOM = register("random", RandomBlockFunction.Type.RANDOM_TYPE);
  public static final TagBlockFunction.Type TAG = register("tag", TagBlockFunction.Type.TAG_TYPE);
  public static final UseOriginalBlockFunction.Type USE_ORIGINAL = register("use_original", UseOriginalBlockFunction.Type.USE_ORIGINAL_TYPE);

  public static final CheckerboardBlockFunction.Type CHECKERBOARD = register("checkerboard", CheckerboardBlockFunction.Type.CHECKERBOARD_TYPE);
  public static final CheckerboardTagBlockFunction.Type CHECKERBOARD_TAG = register("checkerboard-tag", CheckerboardTagBlockFunction.Type.CHECKERBOARD_TAG_TYPE);
  public static final ConditionalBlockFunction.Type CONDITIONAL = register("conditional", ConditionalBlockFunction.Type.CONDITIONAL_TYPE);
  public static final ConditionsBlockFunction.Type CONDITIONS = register("conditions", ConditionsBlockFunction.Type.CONDITIONS_TYPE);
  public static final DryBlockFunction.Type DRY = register("dry", DryBlockFunction.Type.DRY_TYPE);
  public static final FilterBlockFunction.Type FILTER = register("filter", FilterBlockFunction.Type.FILTER_TYPE);
  public static final IdContainBlockFunction.Type ID_CONTAIN = register("id_contain", IdContainBlockFunction.Type.ID_CONTAIN_TYPE);
  public static final IdReplaceBlockFunction.Type ID_REPLACE = register("id_replace", IdReplaceBlockFunction.Type.ID_REPLACE_TYPE);
  public static final MirrorBlockFunction.Type MIRROR = register("mirror", MirrorBlockFunction.Type.MIRROR_TYPE);
  public static final NoiseBlockFunction.Type NOISE = register("noise", NoiseBlockFunction.Type.INSTANCE);
  public static final OverlayBlockFunction.Type OVERLAY = register("overlay", OverlayBlockFunction.Type.OVERLAY_TYPE);
  public static final PickBlockFunction.Type PICK = register("pick", PickBlockFunction.Type.PICK_TYPE);
  public static final PostProcessBlockFunction.Type POST_PROCESS = register("post_process", PostProcessBlockFunction.Type.POST_PROCESS_TYPE);
  public static final ReferenceBlockFunction.Type REFERENCE = register("reference", ReferenceBlockFunction.Type.INSTANCE);
  public static final RotateBlockFunction.Type ROTATE = register("rotate", RotateBlockFunction.Type.ROTATE_TYPE);
  public static final StonecutBlockFunction.Type STONE_CUT = register("stonecut", StonecutBlockFunction.Type.STONE_CUT_TYPE);

  private BlockFunctionTypes() {
  }

  @SuppressWarnings("unchecked")
  private static <T extends BlockFunctionType<?>> T register(String name, T value) {
    if (value != SimpleBlockFunction.Type.SIMPLE_TYPE && value instanceof Parser<?> parser) {
      BlockFunctionParsing.PARSERS.add((Parser<BlockFunction>) parser);
    }
    return REGISTRY_BRIDGE.register(name, value);
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(BlockFunctionType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);

    registerFunctions();
    registerFunctionNames();
  }

  private static void registerFunctions() {
    final Map<String, Supplier<FunctionContentParser<? extends BlockFunction>>> map = BlockFunctionParsing.FUNCTIONS;
    map.put("pick", PickBlockFunction.Parser::new);
    map.put("dry", DryBlockFunction.Parser::new);
    map.put("overlay", OverlayBlockFunction.Parser::new);
    map.put("filter", FilterBlockFunction.Parser::new);
    map.put("idcontain", IdContainBlockFunction.Parser::new);
    map.put("stonecut", StonecutBlockFunction.Parser::new);
    map.put("if", ConditionalBlockFunction.Parser::new);
    map.put("ifs", ConditionsBlockFunction.Parser::new);
    map.put("idreplace", IdReplaceBlockFunction.Parser::new);
    map.put("postprocess", PostProcessBlockFunction.Parser::new);
    map.put("random", RandomBlockFunction.RandFuncParser::new);
    map.put("rotate", RotateBlockFunction.Parser::new);
    map.put("mirror", MirrorBlockFunction.Parser::new);
    map.put("noise", NoiseBlockFunction.Parser::new);
    map.put("checkerboard", CheckerboardBlockFunction.Parser::new);
    map.put("checkerboard-tag", CheckerboardTagBlockFunction.Parser::new);
  }

  private static void registerFunctionNames() {
    final Map<String, Component> map = BlockFunctionParsing.FUNCTION_NAMES;
    map.put("pick", Component.translatable("enhanced_commands.block_function.pick"));
    map.put("dry", Component.translatable("enhanced_commands.block_function.dry"));
    map.put("overlay", Component.translatable("enhanced_commands.block_function.overlay"));
    map.put("filter", Component.translatable("enhanced_commands.block_function.filter"));
    map.put("idcontain", Component.translatable("enhanced_commands.block_function.id_contain"));
    map.put("stonecut", Component.translatable("enhanced_commands.block_function.stone_cut"));
    map.put("if", Component.translatable("enhanced_commands.block_function.conditional"));
    map.put("ifs", Component.translatable("enhanced_commands.block_function.conditions"));
    map.put("idreplace", Component.translatable("enhanced_commands.block_function.id_replace"));
    map.put("postprocess", Component.translatable("enhanced_commands.block_function.post_process"));
    map.put("random", Component.translatable("enhanced_commands.block_function.random"));
    map.put("rotate", Component.translatable("enhanced_commands.block_function.rotate"));
    map.put("mirror", Component.translatable("enhanced_commands.block_function.mirror"));
    map.put("noise", Component.translatable("enhanced_commands.block_function.noise"));
    map.put("checkerboard", Component.translatable("enhanced_commands.block_function.checkerboard"));
    map.put("checkerboard-tag", Component.translatable("enhanced_commands.block_function.checkerboard-tag"));
  }
}
