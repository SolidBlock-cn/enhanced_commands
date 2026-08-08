package pers.solid.ecmd.block.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.SimpleListFunctionParser;
import pers.solid.ecmd.parse.SimpleOneArgFunctionParser;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public final class BlockPredicateTypes {
  private static final RegistryBridge<BlockPredicateType<?>> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, BlockPredicateType.REGISTRY);

  public static final BlockPredicateType<SimpleBlockPredicate> SIMPLE = register("simple", SimpleBlockPredicate.CODEC);

  public static final BlockPredicateType<AllBlockPredicate> ALL = register("all", AllBlockPredicate.CODEC);
  public static final BlockPredicateType<AnyBlockPredicate> ANY = register("any", AnyBlockPredicate.CODEC);
  public static final BlockPredicateType<BiPredicateBlockPredicate> BI_PREDICATE = register("bi_predicate", BiPredicateBlockPredicate.CODEC);
  public static final BlockPredicateType<BlockFunctionResultBlockPredicate> TEST_BLOCK_FUNCTION_RESULT = register("test_block_function_result", BlockFunctionResultBlockPredicate.CODEC);
  public static final BlockPredicateType<CheckerboardBlockPredicate> CHECKERBOARD = register("checkerboard", CheckerboardBlockPredicate.CODEC);
  public static final BlockPredicateType<ConstantBlockPredicate> CONSTANT = register("constant", ConstantBlockPredicate.CODEC, ConstantBlockPredicate.ConstantParser.INSTANCE);
  public static final BlockPredicateType<ExposeBlockPredicate> EXPOSE = register("expose", ExposeBlockPredicate.CODEC);
  public static final BlockPredicateType<HorizontalOffsetBlockPredicate> HORIZONTAL_OFFSET = register("horizontal_offset", HorizontalOffsetBlockPredicate.CODEC, HorizontalOffsetBlockPredicate.HorizontalOffsetParser.INSTANCE);
  public static final BlockPredicateType<IdContainBlockPredicate> ID_CONTAIN = register("id_contain", IdContainBlockPredicate.CODEC);
  public static final BlockPredicateType<LootConditionBlockPredicate> LOOT_CONDITION = register("loot_condition", LootConditionBlockPredicate.CODEC);
  public static final BlockPredicateType<NbtBlockPredicate> NBT = register("nbt", NbtBlockPredicate.CODEC, NbtBlockPredicate.NbtParser.INSTANCE);
  public static final BlockPredicateType<NegatingBlockPredicate> NOT = register("not", NegatingBlockPredicate.CODEC, NegatingBlockPredicate.NegationParser.INSTANCE);
  public static final BlockPredicateType<NoiseBlockPredicate> NOISE = register("noise", NoiseBlockPredicate.CODEC);
  public static final BlockPredicateType<PropertiesNamesBlockPredicate> PROPERTY_NAMES = register("property_names", PropertiesNamesBlockPredicate.CODEC, PropertiesNamesBlockPredicate.PropertyNamesParser.INSTANCE);
  public static final BlockPredicateType<ProbabilityBlockPredicate> RAND = register("probability", ProbabilityBlockPredicate.CODEC);
  public static final BlockPredicateType<PropertiesNbtCombinationBlockPredicate> PROPERTIES_NBT_COMBINATION = register("properties_nbt_combination", PropertiesNbtCombinationBlockPredicate.CODEC);
  public static final BlockPredicateType<ReferenceBlockPredicate> REFERENCE = register("reference", ReferenceBlockPredicate.CODEC, ReferenceBlockPredicate.PREFIXED_ID_PARSER);
  public static final BlockPredicateType<RegionBlockPredicate> REGION = register("region", RegionBlockPredicate.CODEC);
  public static final BlockPredicateType<RelBlockPredicate> REL = register("rel", RelBlockPredicate.CODEC);
  public static final BlockPredicateType<TagBlockPredicate> TAG = register("tag", TagBlockPredicate.CODEC, TagBlockPredicate.TagParser.TAG_TYPE);

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
    final FunctionsParser<BlockPredicate> functionsParser = BlockPredicateParsing.FUNCTIONS_PARSER;
    functionsParser.register("all", SimpleListFunctionParser.ALL_PREDICATE_DESCRIPTION, () -> new SimpleListFunctionParser<>(BlockPredicate::parse, AllBlockPredicate::new));
    functionsParser.register("any", SimpleListFunctionParser.ANY_PREDICATE_DESCRIPTION, () -> new SimpleListFunctionParser<>(BlockPredicate::parse, AnyBlockPredicate::new));
    functionsParser.register("block-function-result", Component.translatable("enhanced_commands.block_predicate.block_function_result"), BlockFunctionResultBlockPredicate.Parser::new);
    functionsParser.register("checkerboard", Component.translatable("enhanced_commands.block_predicate.checkerboard"), CheckerboardBlockPredicate.Parser::new);
    functionsParser.register("diff", Component.translatable("enhanced_commands.block_predicate.bi_predicate_diff"), () -> new BiPredicateBlockPredicate.Parser(false));
    functionsParser.register("expose", Component.translatable("enhanced_commands.block_predicate.expose"), ExposeBlockPredicate.Parser::new);
    functionsParser.register("idcontain", Component.translatable("enhanced_commands.block_predicate.id_contain"), IdContainBlockPredicate.Parser::new);
    functionsParser.register("noise", Component.translatable("enhanced_commands.block_predicate.noise"), NoiseBlockPredicate.Parser::new);
    functionsParser.register("not", SimpleOneArgFunctionParser.NOT_PREDICATE_DESCRIPTION, () -> new SimpleOneArgFunctionParser<>(BlockPredicate::parse, NegatingBlockPredicate::new));
    functionsParser.register("predicate", Component.translatable("enhanced_commands.block_predicate.loot_condition"), LootConditionBlockPredicate.Parser::new);
    functionsParser.register("probability", Component.translatable("enhanced_commands.block_predicate.probability"), ProbabilityBlockPredicate.Parser::new);
    functionsParser.register("reference", Component.translatable("enhanced_commands.block_predicate.reference"), () -> new ReferenceEntry.ReferenceFunctionGrammarParser<>(ReferenceBlockPredicate.PREFIXED_ID_PARSER));
    functionsParser.register("region", Component.translatable("enhanced_commands.block_predicate.region"), RegionBlockPredicate.Parser::new);
    functionsParser.register("rel", Component.translatable("enhanced_commands.block_predicate.rel"), RelBlockPredicate.Parser::new);
    functionsParser.register("same", Component.translatable("enhanced_commands.block_predicate.bi_predicate_same"), () -> new BiPredicateBlockPredicate.Parser(true));
  }

  public static void init(InitializeContext context) {
    RegistryBridge.registerToRootRegistry(BlockPredicateType.REGISTRY, context);
    REGISTRY_BRIDGE.validateAndRegisterContents(context);
    registerFunctions();
  }
}
