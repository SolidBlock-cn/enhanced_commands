package pers.solid.ecmd.predicate.block;

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

public final class BlockPredicateTypes {
  public static final Map<String, Supplier<FunctionLikeParser<? extends BlockPredicateArgument>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), BlockPredicateTypes::registerFunctions);
  public static final Map<String, Text> FUNCTION_NAMES = Util.make(new HashMap<>(), BlockPredicateTypes::registerFunctionNames);
  public static final Parser<BlockPredicateArgument> PARENTHESES_PARSER = (registryAccess, parser, suggestionsOnly, allowSparse) -> ParsingUtil.parseParentheses(() -> BlockPredicateArgument.parse(registryAccess, parser, suggestionsOnly, true), parser);
  public static final Parser<BlockPredicateArgument> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
  public static final List<Parser<BlockPredicateArgument>> PARSERS = Lists.newArrayList(PARENTHESES_PARSER, FUNCTIONS_PARSER);

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
  public static final RandBlockPredicate.Type RAND = register("rand", RandBlockPredicate.Type.RAND_TYPE);
  public static final BiPredicateBlockPredicate.Type BI_PREDICATE = register("bi_predicate", BiPredicateBlockPredicate.Type.BI_PREDICATE_TYPE);
  public static final RelBlockPredicate.Type REL = register("rel", RelBlockPredicate.Type.REL_TYPE);
  public static final ExposeBlockPredicate.Type EXPOSE = register("expose", ExposeBlockPredicate.Type.EXPOSE_TYPE);
  public static final IdContainBlockPredicate.Type ID_CONTAIN = register("id_contain", IdContainBlockPredicate.Type.ID_CONTAIN_TYPE);
  public static final RegionBlockPredicate.Type REGION = register("region", RegionBlockPredicate.Type.REGION_TYPE);
  public static final LootConditionBlockPredicate.Type LOOT_CONDITION = register("loot_condition", LootConditionBlockPredicate.Type.LOOT_CONDITION_TYPE);
  public static final CheckerboardBlockPredicate.Type CHECKERBOARD = register("checkerboard", CheckerboardBlockPredicate.Type.CHECKERBOARD_TYPE);
  public static final ReferenceBlockPredicate.Type REFERENCE = register("reference", ReferenceBlockPredicate.Type.INSTANCE);

  private BlockPredicateTypes() {
  }

  @SuppressWarnings("unchecked")
  private static <T extends BlockPredicateType<?>> T register(String name, T value) {
    if (value != SimpleBlockPredicate.Type.SIMPLE_TYPE && value instanceof Parser<?> parser) {
      PARSERS.add((Parser<BlockPredicateArgument>) parser);
    }
    return Registry.register(BlockPredicateType.REGISTRY, EnhancedCommands.id(name), value);
  }

  private static void registerFunctions(Map<String, Supplier<FunctionLikeParser<? extends BlockPredicateArgument>>> map) {
    map.put("all", AllBlockPredicate.Parser::new);
    map.put("any", AnyBlockPredicate.Parser::new);
    map.put("checkerboard", CheckerboardBlockPredicate.Parser::new);
    map.put("diff", () -> new BiPredicateBlockPredicate.Parser("diff", Text.translatable("enhanced_commands.block_predicate.bi_predicate_diff"), false));
    map.put("expose", ExposeBlockPredicate.Parser::new);
    map.put("idcontain", IdContainBlockPredicate.Parser::new);
    map.put("predicate", LootConditionBlockPredicate.Parser::new);
    map.put("rand", RandBlockPredicate.Parser::new);
    map.put("region", RegionBlockPredicate.Parser::new);
    map.put("rel", RelBlockPredicate.Parser::new);
    map.put("same", () -> new BiPredicateBlockPredicate.Parser("same", Text.translatable("enhanced_commands.block_predicate.bi_predicate_same"), true));
  }

  private static void registerFunctionNames(Map<String, Text> map) {
    map.put("all", Text.translatable("enhanced_commands.block_predicate.all"));
    map.put("any", Text.translatable("enhanced_commands.block_predicate.any"));
    map.put("checkerboard", Text.translatable("enhanced_commands.block_predicate.checkerboard"));
    map.put("diff", Text.translatable("enhanced_commands.block_predicate.bi_predicate_diff"));
    map.put("expose", Text.translatable("enhanced_commands.block_predicate.expose"));
    map.put("idcontain", Text.translatable("enhanced_commands.block_predicate.id_contain"));
    map.put("predicate", Text.translatable("enhanced_commands.block_predicate.loot_condition"));
    map.put("rand", Text.translatable("enhanced_commands.block_predicate.probability"));
    map.put("region", Text.translatable("enhanced_commands.block_predicate.region"));
    map.put("rel", Text.translatable("enhanced_commands.block_predicate.rel"));
    map.put("same", Text.translatable("enhanced_commands.block_predicate.bi_predicate_same"));
  }

  public static void init() {
    Preconditions.checkState(BlockPredicateType.REGISTRY.size() != 0);
  }
}
