package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockPredicateTypes {
  public static final Map<String, Supplier<FunctionLikeParser<? extends BlockPredicate>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), BlockPredicateTypes::registerFunctions);
  public static final Map<String, Component> FUNCTION_NAMES = Util.make(new HashMap<>(), BlockPredicateTypes::registerFunctionNames);
  public static final Parser<BlockPredicate> PARENTHESES_PARSER = (parseContext) -> ParsingUtil.parseParentheses(() -> BlockPredicate.parse(parseContext.withAllowSparse(true)), parseContext);
  public static final Parser<BlockPredicate> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
  public static final List<Parser<BlockPredicate>> PARSERS = Lists.newArrayList(PARENTHESES_PARSER, FUNCTIONS_PARSER);

  public static final SimpleBlockPredicate.Type SIMPLE = register("simple", SimpleBlockPredicate.Type.SIMPLE_TYPE);
  public static final NegatingBlockPredicate.Type NEGATING = register("negating", NegatingBlockPredicate.Type.NEGATING_TYPE);
  public static final HorizontalOffsetBlockPredicate.Type HORIZONTAL_OFFSET = register("horizontal_offset", HorizontalOffsetBlockPredicate.Type.HORIZONTAL_OFFSET_TYPE);
  public static final PropertiesNamesBlockPredicate.Type PROPERTY_NAMES = register("property_names", PropertiesNamesBlockPredicate.Type.PROPERTY_NAMES_TYPE);
  public static final NbtBlockPredicate.Type NBT = register("nbt", NbtBlockPredicate.Type.NBT_TYPE);
  public static final PropertiesNbtCombinationBlockPredicate.Type PROPERTIES_NBT_COMBINATION = register("properties_nbt_combination", PropertiesNbtCombinationBlockPredicate.Type.PROPERTIES_NBT_COMBINATION_TYPE);
  public static final ConstantBlockPredicate.Type CONSTANT = register("constant", ConstantBlockPredicate.Type.CONSTANT_TYPE);
  public static final TagBlockPredicate.Type TAG = register("tag", TagBlockPredicate.Type.TAG_TYPE);
  public static final AnyBlockPredicate.Type ANY = register("any", AnyBlockPredicate.Type.ANY_TYPE);
  public static final AllBlockPredicate.Type ALL = register("all", AllBlockPredicate.Type.ALL_TYPE);
  public static final ProbabilityBlockPredicate.Type RAND = register("rand", ProbabilityBlockPredicate.Type.RAND_TYPE);
  public static final BiPredicateBlockPredicate.Type BI_PREDICATE = register("bi_predicate", BiPredicateBlockPredicate.Type.BI_PREDICATE_TYPE);
  public static final RelBlockPredicate.Type REL = register("rel", RelBlockPredicate.Type.REL_TYPE);
  public static final ExposeBlockPredicate.Type EXPOSE = register("expose", ExposeBlockPredicate.Type.EXPOSE_TYPE);
  public static final IdContainBlockPredicate.Type ID_CONTAIN = register("id_contain", IdContainBlockPredicate.Type.ID_CONTAIN_TYPE);
  public static final NoiseBlockPredicate.Type NOISE = register("noise", NoiseBlockPredicate.Type.NOISE_TYPE);
  public static final RegionBlockPredicate.Type REGION = register("region", RegionBlockPredicate.Type.REGION_TYPE);
  public static final LootConditionBlockPredicate.Type LOOT_CONDITION = register("loot_condition", LootConditionBlockPredicate.Type.LOOT_CONDITION_TYPE);
  public static final CheckerboardBlockPredicate.Type CHECKERBOARD = register("checkerboard", CheckerboardBlockPredicate.Type.CHECKERBOARD_TYPE);
  public static final ReferenceBlockPredicate.Type REFERENCE = register("reference", ReferenceBlockPredicate.Type.INSTANCE);

  private BlockPredicateTypes() {
  }

  @SuppressWarnings("unchecked")
  private static <T extends BlockPredicateType<?>> T register(String name, T value) {
    if (value != SimpleBlockPredicate.Type.SIMPLE_TYPE && value instanceof Parser<?> parser) {
      PARSERS.add((Parser<BlockPredicate>) parser);
    }
    return Registry.register(BlockPredicateType.REGISTRY, EnhancedCommands.id(name), value);
  }

  private static void registerFunctions(Map<String, Supplier<FunctionLikeParser<? extends BlockPredicate>>> map) {
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

  private static void registerFunctionNames(Map<String, Component> map) {
    map.put("all", Component.translatable("enhanced_commands.block_predicate.all"));
    map.put("any", Component.translatable("enhanced_commands.block_predicate.any"));
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

  public static void init() {
    Preconditions.checkState(BlockPredicateType.REGISTRY.size() != 0);
  }
}
