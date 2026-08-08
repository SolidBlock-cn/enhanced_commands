package pers.solid.ecmd.data;

import net.minecraft.nbt.ShortTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.block.function.*;
import pers.solid.ecmd.block.predicate.*;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.nbt.function.CompoundNbtFunction;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.nbt.function.RegexReplaceNbtFunction;
import pers.solid.ecmd.nbt.function.SimpleNbtFunction;
import pers.solid.ecmd.nbt.predicate.MatchCompoundNbtPredicate;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.nbt.predicate.RangeNbtPredicate;
import pers.solid.ecmd.nbt.predicate.RegexNbtPredicate;
import pers.solid.ecmd.number.EnhancedCommandsNumberProvider;
import pers.solid.ecmd.property.function.AllRandomPropertyNameFunction;
import pers.solid.ecmd.property.function.SimplePropertyNameFunction;
import pers.solid.ecmd.property.predicate.Comparator;
import pers.solid.ecmd.property.predicate.ComparisonPropertyNamePredicate;
import pers.solid.ecmd.region.OutlineRegionProvider;
import pers.solid.ecmd.region.ReferenceRegionProvider;
import pers.solid.ecmd.region.RegionProvider;
import pers.solid.ecmd.region.SphereRegionProvider;
import pers.solid.ecmd.util.bridge.BridgeIntRange;
import pers.solid.ecmd.util.enums.OutlineType;
import pers.solid.ecmd.util.pack.RegistryHelper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Pattern;

public interface ExamplePackDataGenerations {
  private static ResourceLocation id(String name) {
    return EnhancedCommands.id("examples/" + name);
  }

  class ForBlockFunction implements DynamicRegistryGenerationBridge<BlockFunction> {
    private static ResourceKey<BlockFunction> of(String name) {
      return ResourceKey.create(BlockFunction.REGISTRY_KEY, id(name));
    }

    @Override
    public String getBridgeName() {
      return "Block Functions (Example Pack)";
    }

    @Override
    public void configureBridge(ContextBridge<BlockFunction> context) {
      context.add(of("diamond_or_gold"), new PickBlockFunction(new WeightedList.Uniform<>(new SimpleBlockFunction(Blocks.DIAMOND_BLOCK), new SimpleBlockFunction(Blocks.GOLD_BLOCK))));
      context.add(of("dry_stairs"), new TagBlockFunction(RegistryHelper.emptyNamedSet(BlockTags.STAIRS), List.of(new AllRandomPropertyNameFunction(), new SimplePropertyNameFunction("waterlogged", "false", false))));
      context.add(of("dry_slabs"), new TagBlockFunction(RegistryHelper.emptyNamedSet(BlockTags.STAIRS), List.of(new AllRandomPropertyNameFunction(), new SimplePropertyNameFunction("waterlogged", "false", false))));

      context.add(of("loop_ref/a"), new ConditionalBlockFunction(
          new ReferenceBlockPredicate(RegistryHelper.safeStandAloneHolderReference(ForBlockPredicate.of("loop_ref/b"))),
          EmptyBlockFunction.INSTANCE
      ));
    }
  }

  class ForBlockPredicate implements DynamicRegistryGenerationBridge<BlockPredicate> {
    private static ResourceKey<BlockPredicate> of(String name) {
      return ResourceKey.create(BlockPredicate.REGISTRY_KEY, id(name));
    }

    @Override
    public String getBridgeName() {
      return "Block Predicate (Example Pack)";
    }

    @Override
    public void configureBridge(ContextBridge<BlockPredicate> context) {
      context.add(of("has_redstone"), new IdContainBlockPredicate(Pattern.compile("redstone")));
      context.add(of("just_simple_oak"), new AllBlockPredicate(List.of(
          new IdContainBlockPredicate(Pattern.compile("oak")),
          new NegatingBlockPredicate(new IdContainBlockPredicate(Pattern.compile("dark_oak"))),
          new NegatingBlockPredicate(new IdContainBlockPredicate(Pattern.compile("pale_oak")))
      )));
      context.add(of("can_be_dried"), new BlockFunctionResultBlockPredicate(
          new DryBlockFunction(),
          new NegatingBlockPredicate(new PropertiesNamesBlockPredicate(new ComparisonPropertyNamePredicate("waterlogged", Comparator.EQ, "true")))
      ));

      context.add(of("correct_ref/a"), new ProbabilityBlockPredicate(0.4f, ConstantBlockPredicate.ALWAYS_TRUE, OptionalLong.empty()));
      context.add(of("correct_ref/b"), new ReferenceBlockPredicate(RegistryHelper.safeStandAloneHolderReference(ForBlockPredicate.of("correct_ref/a"))));
      context.add(of("correct_ref/c"), new ReferenceBlockPredicate(RegistryHelper.safeStandAloneHolderReference(ForBlockPredicate.of("correct_ref/b"))));

      context.add(of("loop_ref/b"), new BlockFunctionResultBlockPredicate(
          new ReferenceBlockFunction(RegistryHelper.safeStandAloneHolderReference(ForBlockFunction.of("loop_ref/a"))),
          new ReferenceBlockPredicate(RegistryHelper.safeStandAloneHolderReference(of("correct_ref/c")))
      ));

      context.add(of("absent_ref"), new ReferenceBlockPredicate(RegistryHelper.safeStandAloneHolderReference(of("absent_ref/123"))));
    }
  }

  class ForNbtFunction implements DynamicRegistryGenerationBridge<NbtFunction> {
    private static ResourceKey<NbtFunction> of(String name) {
      return ResourceKey.create(NbtFunction.REGISTRY_KEY, id(name));
    }

    @Override
    public String getBridgeName() {
      return "NBT Functions (Example Pack)";
    }

    @Override
    public void configureBridge(ContextBridge<NbtFunction> context) {
      context.add(of("convert_to_mangrove"), new RegexReplaceNbtFunction(Pattern.compile("(dark_oak|pale_oak|oak|spruce|jungle|acacia)"), "mangrove", true, true, Optional.empty()));
      context.add(of("set_count_to_zero"), new CompoundNbtFunction(Map.of("Count", Optional.of(new SimpleNbtFunction(ShortTag.valueOf((short) 0)))), true));
    }
  }

  class ForNbtPredicate implements DynamicRegistryGenerationBridge<NbtPredicate> {
    private static ResourceKey<NbtPredicate> of(String name) {
      return ResourceKey.create(NbtPredicate.REGISTRY_KEY, id(name));
    }

    @Override
    public String getBridgeName() {
      return "NBT Predicates (Example Pack)";
    }

    @Override
    public void configureBridge(ContextBridge<NbtPredicate> context) {
      context.add(of("has_gold"), new RegexNbtPredicate(Pattern.compile("gold")));
      context.add(of("count_more_than_1"), new MatchCompoundNbtPredicate(List.of(new MatchCompoundNbtPredicate.Entry("Count", new RangeNbtPredicate(BridgeIntRange.atLeast(2))))));
    }
  }

  class ForNumberProvider implements DynamicRegistryGenerationBridge<NumberProvider> {
    private static ResourceKey<NumberProvider> of(String name) {
      return ResourceKey.create(EnhancedCommandsNumberProvider.REGISTRY_KEY, id(name));
    }

    @Override
    public String getBridgeName() {
      return "Number Providers (Example Pack)";
    }

    @Override
    public void configureBridge(ContextBridge<NumberProvider> context) {
      context.add(of("uniform_0_100"), UniformGenerator.between(0, 100));
      context.add(of("binomial_standard"), BinomialDistributionGenerator.binomial(0, 1));
    }
  }

  class ForRegionProvider implements DynamicRegistryGenerationBridge<RegionProvider<?>> {
    private static ResourceKey<RegionProvider<?>> of(String name) {
      return ResourceKey.create(RegionProvider.REGISTRY_KEY, id(name));
    }

    @Override
    public String getBridgeName() {
      return "Region Provider (Example Pack)";
    }

    @Override
    public void configureBridge(ContextBridge<RegionProvider<?>> context) {
      context.add(of("original_sphere"), new SphereRegionProvider(16, EnhancedPosArgument.CURRENT_POS));
      context.add(of("original_sphere_hollow"), new OutlineRegionProvider(OutlineType.OUTLINE, new SphereRegionProvider(16, EnhancedPosArgument.CURRENT_POS)));

      context.add(of("self_ref"), new ReferenceRegionProvider(RegistryHelper.safeStandAloneHolderReference(of("self_ref"))));

      context.add(of("loop_ref/a"), new ReferenceRegionProvider(RegistryHelper.safeStandAloneHolderReference(of("loop_ref/b"))));
      context.add(of("loop_ref/b"), new ReferenceRegionProvider(RegistryHelper.safeStandAloneHolderReference(of("loop_ref/a"))));
    }
  }
}
