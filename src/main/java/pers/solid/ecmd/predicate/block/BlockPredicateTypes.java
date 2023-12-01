package pers.solid.ecmd.predicate.block;

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

public final class BlockPredicateTypes {
  public static final Map<String, Supplier<FunctionParamsParser<? extends BlockPredicateArgument>>> FUNCTIONS = Util.make(new LinkedHashMap<>(), BlockPredicateTypes::registerFunctions);
  public static final Map<String, Text> FUNCTION_NAMES = Util.make(new HashMap<>(), BlockPredicateTypes::registerFunctionNames);
  public static final Parser<BlockPredicateArgument> PARENTHESES_PARSER = (commandRegistryAccess, parser, suggestionsOnly, allowSparse) -> ParsingUtil.parseParentheses(() -> BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly, true), parser);
  public static final Parser<BlockPredicateArgument> FUNCTIONS_PARSER = new FunctionsParser<>(FUNCTIONS, FUNCTION_NAMES);
  public static final List<Parser<BlockPredicateArgument>> PARSERS = Lists.newArrayList(PARENTHESES_PARSER, FUNCTIONS_PARSER);
  public static final BlockPredicateType<SimpleBlockPredicate> SIMPLE = register(SimpleBlockPredicate.Type.SIMPLE_TYPE, "simple");
  public static final BlockPredicateType<NegatingBlockPredicate> NEGATING = register(NegatingBlockPredicate.Type.NEGATING_TYPE, "negating");
  public static final BlockPredicateType<HorizontalOffsetBlockPredicate> HORIZONTAL_OFFSET = register(HorizontalOffsetBlockPredicate.Type.HORIZONTAL_OFFSET_TYPE, "horizontal_offset");
  public static final BlockPredicateType<PropertiesNamesBlockPredicate> PROPERTY_NAMES = register(PropertiesNamesBlockPredicate.Type.PROPERTY_NAMES_TYPE, "property_names");
  public static final BlockPredicateType<NbtBlockPredicate> NBT = register(NbtBlockPredicate.Type.NBT_TYPE, "nbt");
  public static final BlockPredicateType<PropertiesNbtCombinationBlockPredicate> PROPERTIES_NBT_COMBINATION = register(PropertiesNbtCombinationBlockPredicate.Type.PROPERTIES_NBT_COMBINATION_TYPE, "properties_nbt_combination");
  public static final BlockPredicateType<ConstantBlockPredicate> CONSTANT = register(ConstantBlockPredicate.Type.CONSTANT_TYPE, "constant");
  public static final BlockPredicateType<TagBlockPredicate> TAG = register(TagBlockPredicate.Type.TAG_TYPE, "tag");
  public static final BlockPredicateType<UnionBlockPredicate> UNION = register(UnionBlockPredicate.Type.UNION_TYPE, "union");
  public static final BlockPredicateType<IntersectBlockPredicate> INTERSECT = register(IntersectBlockPredicate.Type.INTERSECT_TYPE, "intersect");
  public static final BlockPredicateType<RandBlockPredicate> RAND = register(RandBlockPredicate.Type.RAND_TYPE, "rand");
  public static final BlockPredicateType<BiPredicateBlockPredicate> BI_PREDICATE = register(BiPredicateBlockPredicate.Type.BI_PREDICATE_TYPE, "bi_predicate");
  public static final BlockPredicateType<RelBlockPredicate> REL = register(RelBlockPredicate.Type.REL_TYPE, "rel");
  public static final BlockPredicateType<ExposeBlockPredicate> EXPOSE = register(ExposeBlockPredicate.Type.EXPOSE_TYPE, "expose");
  public static final BlockPredicateType<IdContainBlockPredicate> ID_CONTAIN = register(IdContainBlockPredicate.Type.ID_CONTAIN_TYPE, "id_contain");
  public static final BlockPredicateType<RegionBlockPredicate> REGION = register(RegionBlockPredicate.Type.REGION_TYPE, "region");
  public static final LootConditionBlockPredicate.Type LOOT_CONDITION = register(LootConditionBlockPredicate.Type.LOOT_CONDITION_TYPE, "loot_condition");

  private BlockPredicateTypes() {
  }

  @SuppressWarnings("unchecked")
  private static <T extends BlockPredicateType<?>> T register(T value, String name) {
    if (value != SimpleBlockPredicate.Type.SIMPLE_TYPE && value instanceof Parser<?> parser) {
      PARSERS.add((Parser<BlockPredicateArgument>) parser);
    }
    return Registry.register(BlockPredicateType.REGISTRY, new Identifier(EnhancedCommands.MOD_ID, name), value);
  }

  private static void registerFunctions(Map<String, Supplier<FunctionParamsParser<? extends BlockPredicateArgument>>> map) {
    map.put("all", IntersectBlockPredicate.Parser::new);
    map.put("any", UnionBlockPredicate.Parser::new);
    map.put("diff", () -> new BiPredicateBlockPredicate.Parser("diff", Text.translatable("enhanced_commands.argument.block_predicate.bi_predicate_diff"), false));
    map.put("expose", ExposeBlockPredicate.Parser::new);
    map.put("idcontain", IdContainBlockPredicate.Parser::new);
    map.put("predicate", LootConditionBlockPredicate.Parser::new);
    map.put("rand", RandBlockPredicate.Parser::new);
    map.put("region", RegionBlockPredicate.Parser::new);
    map.put("rel", RelBlockPredicate.Parser::new);
    map.put("same", () -> new BiPredicateBlockPredicate.Parser("same", Text.translatable("enhanced_commands.argument.block_predicate.bi_predicate_same"), true));
  }

  private static void registerFunctionNames(Map<String, Text> map) {
    map.put("all", Text.translatable("enhanced_commands.argument.block_predicate.intersect"));
    map.put("any", Text.translatable("enhanced_commands.argument.block_predicate.union"));
    map.put("diff", Text.translatable("enhanced_commands.argument.block_predicate.bi_predicate_diff"));
    map.put("expose", Text.translatable("enhanced_commands.argument.block_predicate.expose"));
    map.put("idcontain", Text.translatable("enhanced_commands.argument.block_predicate.id_contain"));
    map.put("predicate", Text.translatable("enhanced_commands.argument.block_predicate.loot_condition"));
    map.put("rand", Text.translatable("enhanced_commands.argument.block_predicate.probability"));
    map.put("region", Text.translatable("enhanced_commands.argument.block_predicate.region"));
    map.put("rel", Text.translatable("enhanced_commands.argument.block_predicate.rel"));
    map.put("same", Text.translatable("enhanced_commands.argument.block_predicate.bi_predicate_same"));
  }

  public static void init() {
    Preconditions.checkState(BlockPredicateType.REGISTRY.size() != 0);
  }
}
