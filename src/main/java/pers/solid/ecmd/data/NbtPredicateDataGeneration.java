package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.nbt.predicate.MatchCompoundNbtPredicate;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.nbt.predicate.RangeNbtPredicate;
import pers.solid.ecmd.nbt.predicate.RegexNbtPredicate;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

import java.util.List;
import java.util.regex.Pattern;

public class NbtPredicateDataGeneration implements DynamicRegistryGenerationBridge<NbtPredicate> {
  private static ResourceKey<NbtPredicate> of(String value) {
    return ResourceKey.create(NbtPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "NBT Predicates (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<NbtPredicate> context) {
    context.add(of("examples/has_gold"), new RegexNbtPredicate(Pattern.compile("gold")));
    context.add(of("examples/count_more_than_1"), new MatchCompoundNbtPredicate(List.of(new MatchCompoundNbtPredicate.Entry("Count", new RangeNbtPredicate(BridgeIntRange.atLeast(2))))));
  }
}
