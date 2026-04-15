package pers.solid.ecmd.block.predicate;

import com.google.common.base.Supplier;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.Parser;

import java.util.Map;

public final class BlockPredicateTypes {
  private static final RegistryBridge<BlockPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, BlockPredicateType.REGISTRY);

  public static final BlockPredicateType<SimpleBlockPredicate> SIMPLE = register("simple", SimpleBlockPredicate.CODEC);
  public static final BlockPredicateType<NegatingBlockPredicate> NEGATING = register("negating", NegatingBlockPredicate.CODEC, NegatingBlockPredicate.NegationParser.INSTANCE);
  public static final BlockPredicateType<HorizontalOffsetBlockPredicate> HORIZONTAL_OFFSET = register("horizontal_offset", HorizontalOffsetBlockPredicate.CODEC, HorizontalOffsetBlockPredicate.HorizontalOffsetParser.INSTANCE);
  public static final BlockPredicateType<ProbabilityBlockPredicate> PROPERTY_NAMES = register("property_names", ProbabilityBlockPredicate.CODEC, PropertiesNamesBlockPredicate.PropertyNamesParser.INSTANCE);
  public static final BlockPredicateType<NbtBlockPredicate> NBT = register("nbt", NbtBlockPredicate.CODEC, NbtBlockPredicate.NbtParser.INSTANCE);
  public static final BlockPredicateType<PropertiesNbtCombinationBlockPredicate> PROPERTIES_NBT_COMBINATION = register("properties_nbt_combination", PropertiesNbtCombinationBlockPredicate.CODEC);
  public static final BlockPredicateType<ConstantBlockPredicate> CONSTANT = register("constant", ConstantBlockPredicate.CODEC, ConstantBlockPredicate.ConstantParser.INSTANCE);
  public static final BlockPredicateType<TagBlockPredicate> TAG = register("tag", TagBlockPredicate.CODEC, TagBlockPredicate.TagParser.TAG_TYPE);
  public static final BlockPredicateType<AnyBlockPredicate> ANY = register("any", AnyBlockPredicate.CODEC);
  public static final BlockPredicateType<AllBlockPredicate> ALL = register("all", AllBlockPredicate.CODEC);
  public static final BlockPredicateType<ProbabilityBlockPredicate> RAND = register("rand", ProbabilityBlockPredicate.CODEC);
  public static final BlockPredicateType<BiPredicateBlockPredicate> BI_PREDICATE = register("bi_predicate", BiPredicateBlockPredicate.CODEC);
  public static final BlockPredicateType<RelBlockPredicate> REL = register("rel", RelBlockPredicate.CODEC);
  public static final BlockPredicateType<ExposeBlockPredicate> EXPOSE = register("expose", ExposeBlockPredicate.CODEC);
  public static final BlockPredicateType<IdContainBlockPredicate> ID_CONTAIN = register("id_contain", IdContainBlockPredicate.CODEC);
  public static final BlockPredicateType<NoiseBlockPredicate> NOISE = register("noise", NoiseBlockPredicate.CODEC);
  public static final BlockPredicateType<RegionBlockPredicate> REGION = register("region", RegionBlockPredicate.CODEC);
  public static final BlockPredicateType<LootConditionBlockPredicate> LOOT_CONDITION = register("loot_condition", LootConditionBlockPredicate.CODEC);
  public static final BlockPredicateType<CheckerboardBlockPredicate> CHECKERBOARD = register("checkerboard", CheckerboardBlockPredicate.CODEC);
  public static final BlockPredicateType<ReferenceBlockPredicate> REFERENCE = register("reference", ReferenceBlockPredicate.CODEC);

  private BlockPredicateTypes() {
  }

  private static <T extends BlockPredicateType<?>> T register(String name, T value) {
    return REGISTRY_BRIDGE.register(name, value);
  }

  private static <T extends BlockPredicate> BlockPredicateType<T> register(String name, MapCodec<T> codec) {
    return register(name, new BlockPredicateType.Simple<>(codec));
  }

  private static <T extends BlockPredicate> BlockPredicateType<T> register(String name, MapCodec<T> codec, Parser<? extends BlockPredicate> parser) {
    BlockPredicateParsing.PARSERS.add(parser);
    return register(name, codec);
  }

  private static void registerFunctions() {
    final Map<String, Supplier<FunctionContentParser<? extends BlockPredicate>>> map = BlockPredicateParsing.FUNCTIONS;
    map.put("all", AllBlockPredicate.Parser::new);
    map.put("any", AnyBlockPredicate.Parser::new);
    map.put("checkerboard", CheckerboardBlockPredicate.Parser::new);
    map.put("diff", () -> new BiPredicateBlockPredicate.Parser(false));
    map.put("expose", ExposeBlockPredicate.Parser::new);
    map.put("idcontain", IdContainBlockPredicate.Parser::new);
    map.put("noise", NoiseBlockPredicate.Parser::new);
    map.put("predicate", LootConditionBlockPredicate.Parser::new);
    map.put("probability", ProbabilityBlockPredicate.Parser::new);
    map.put("region", RegionBlockPredicate.Parser::new);
    map.put("rel", RelBlockPredicate.Parser::new);
    map.put("same", () -> new BiPredicateBlockPredicate.Parser(true));
  }

  private static void registerFunctionNames() {
    final Map<String, Component> map = BlockPredicateParsing.FUNCTION_NAMES;
    map.put("all", Component.translatable("enhanced_commands.predicate.all"));
    map.put("any", Component.translatable("enhanced_commands.predicate.any"));
    map.put("checkerboard", Component.translatable("enhanced_commands.block_predicate.checkerboard"));
    map.put("diff", Component.translatable("enhanced_commands.block_predicate.bi_predicate_diff"));
    map.put("expose", Component.translatable("enhanced_commands.block_predicate.expose"));
    map.put("idcontain", Component.translatable("enhanced_commands.block_predicate.id_contain"));
    map.put("noise", Component.translatable("enhanced_commands.block_predicate.noise"));
    map.put("predicate", Component.translatable("enhanced_commands.block_predicate.loot_condition"));
    map.put("probability", Component.translatable("enhanced_commands.block_predicate.probability"));
    map.put("region", Component.translatable("enhanced_commands.block_predicate.region"));
    map.put("rel", Component.translatable("enhanced_commands.block_predicate.rel"));
    map.put("same", Component.translatable("enhanced_commands.block_predicate.bi_predicate_same"));
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(BlockPredicateType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions();
    registerFunctionNames();
  }
}
