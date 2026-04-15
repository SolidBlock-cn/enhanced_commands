package pers.solid.ecmd.block.function;

import com.google.common.base.Supplier;
import com.mojang.serialization.MapCodec;
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

  public static final BlockFunctionType<SimpleBlockFunction> SIMPLE = register("simple", SimpleBlockFunction.CODEC);
  public static final BlockFunctionType<PropertyNamesBlockFunction> PROPERTY_NAMES = register("property_names", PropertyNamesBlockFunction.CODEC, PropertyNamesBlockFunction.PropertyNamesParser.INSTANCE);
  public static final BlockFunctionType<NbtBlockFunction> NBT = register("nbt", NbtBlockFunction.CODEC, NbtBlockFunction.NbtParser.INSTANCE);
  public static final BlockFunctionType<PropertiesNbtCombinationBlockFunction> PROPERTIES_NBT_COMBINATION = register("properties_nbt_combination", PropertiesNbtCombinationBlockFunction.CODEC);
  public static final BlockFunctionType<EmptyBlockFunction> EMPTY = register("empty", EmptyBlockFunction.CODEC);
  public static final BlockFunctionType<RandomBlockFunction> RANDOM = register("random", RandomBlockFunction.CODEC, RandomBlockFunction.RandomParser.INSTANCE);
  public static final BlockFunctionType<TagBlockFunction> TAG = register("tag", TagBlockFunction.CODEC, TagBlockFunction.TagParser.INSTANCE);
  public static final BlockFunctionType<UseOriginalBlockFunction> USE_ORIGINAL = register("use_original", UseOriginalBlockFunction.CODEC, UseOriginalBlockFunction.WaveParser.INSTANCE);

  public static final BlockFunctionType<CheckerboardBlockFunction> CHECKERBOARD = register("checkerboard", CheckerboardBlockFunction.CODEC);
  public static final BlockFunctionType<CheckerboardTagBlockFunction> CHECKERBOARD_TAG = register("checkerboard-tag", CheckerboardTagBlockFunction.CODEC);
  public static final BlockFunctionType<ConditionalBlockFunction> CONDITIONAL = register("conditional", ConditionalBlockFunction.CODEC);
  public static final BlockFunctionType<ConditionsBlockFunction> CONDITIONS = register("conditions", ConditionsBlockFunction.CODEC);
  public static final BlockFunctionType<DryBlockFunction> DRY = register("dry", DryBlockFunction.CODEC);
  public static final BlockFunctionType<FilterBlockFunction> FILTER = register("filter", FilterBlockFunction.CODEC);
  public static final BlockFunctionType<IdContainBlockFunction> ID_CONTAIN = register("id_contain", IdContainBlockFunction.CODEC);
  public static final BlockFunctionType<IdReplaceBlockFunction> ID_REPLACE = register("id_replace", IdReplaceBlockFunction.CODEC);
  public static final BlockFunctionType<MirrorBlockFunction> MIRROR = register("mirror", MirrorBlockFunction.CODEC);
  public static final BlockFunctionType<NoiseBlockFunction> NOISE = register("noise", NoiseBlockFunction.CODEC);
  public static final BlockFunctionType<OverlayBlockFunction> OVERLAY = register("overlay", OverlayBlockFunction.CODEC);
  public static final BlockFunctionType<PickBlockFunction> PICK = register("pick", PickBlockFunction.CODEC);
  public static final BlockFunctionType<PostProcessBlockFunction> POST_PROCESS = register("post_process", PostProcessBlockFunction.CODEC);
  public static final BlockFunctionType<ReferenceBlockFunction> REFERENCE = register("reference", ReferenceBlockFunction.CODEC, ReferenceBlockFunction.Parser.INSTANCE);
  public static final BlockFunctionType<RotateBlockFunction> ROTATE = register("rotate", RotateBlockFunction.CODEC);
  public static final BlockFunctionType<StonecutBlockFunction> STONE_CUT = register("stonecut", StonecutBlockFunction.CODEC);

  private BlockFunctionTypes() {
  }

  @SuppressWarnings("unchecked")
  private static <T extends BlockFunctionType<?>> T register(String name, T value) {
    if (value instanceof Parser<?> parser) {
      BlockFunctionParsing.PARSERS.add((Parser<BlockFunction>) parser);
    }
    return REGISTRY_BRIDGE.register(name, value);
  }

  private static <T extends BlockFunction> BlockFunctionType<T> register(String name, MapCodec<T> codec) {
    return register(name, new BlockFunctionType.Simple<>(codec));
  }

  private static <T extends BlockFunction> BlockFunctionType<T> register(String name, MapCodec<T> codec, Parser<? extends BlockFunction> parser) {
    BlockFunctionParsing.PARSERS.add(parser);
    return register(name, codec);
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
    map.put("pick", Component.translatable("enhanced_commands.function.pick"));
    map.put("dry", Component.translatable("enhanced_commands.block_function.dry"));
    map.put("overlay", Component.translatable("enhanced_commands.function.overlay"));
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
