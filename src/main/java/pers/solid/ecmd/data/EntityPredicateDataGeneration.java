package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.entity.predicate.EntityPredicate;
import pers.solid.ecmd.entity.predicate.EntityPredicateTypes;
import pers.solid.ecmd.entity.predicate.GameModeEntityPredicateEntry;

import java.util.Set;

public class EntityPredicateDataGeneration implements DynamicRegistryGenerationBridge<EntityPredicate> {
  private static ResourceKey<EntityPredicate> of(String value) {
    return ResourceKey.create(EntityPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "Entity Predicates (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<EntityPredicate> context) {
    context.add(of("examples/babies"), EntityPredicateTypes.BABY.createPredicate(true));
    context.add(of("examples/creative_and_spectator"), new GameModeEntityPredicateEntry.Multiple(Set.of(GameType.CREATIVE, GameType.SPECTATOR), false));
  }
}
