package pers.solid.ecmd.util;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalLong;

public class ExecutionContext {
  public final RandomSource random;
  public final PositionProvider positionProvider;
  private @Nullable Long seed;
  private @Nullable SeedStorage<Object> splitterStorage;
  private @Nullable LootContext lootContext;

  public ExecutionContext(RandomSource random, PositionProvider positionProvider, @Nullable Long seed) {
    this.random = random;
    this.positionProvider = positionProvider;
    this.seed = seed;
  }

  public ExecutionContext(PositionProvider source, @Nullable Long seed) {
    this(source.getWorld$ec().getRandom(), source, seed);
  }

  public ExecutionContext(PositionProvider source) {
    this(source.getWorld$ec().getRandom(), source, null);
  }

  public static ExecutionContext fromLootContext(LootContext lootContext) {
    return new ExecutionContext(lootContext.getRandom(), new PositionProvider.Simple(
        lootContext.getParameter(LootContextParams.ORIGIN),
        Optional.ofNullable(lootContext.getOptionalParameter(LootContextParams.THIS_ENTITY)).map(Entity::getRotationVector).orElse(Vec2.ZERO),
        lootContext.getOptionalParameter(LootContextParams.THIS_ENTITY),
        EntityAnchorArgument.Anchor.FEET
    ), null);
  }

  public long getSeed() {
    if (seed == null) {
      return seed = random.nextLong();
    } else {
      return seed;
    }
  }

  protected SeedStorage<Object> getSplitterStorage() {
    if (splitterStorage == null) {
      return splitterStorage = new SeedStorage<>(getSeed());
    } else {
      return splitterStorage;
    }
  }

  public long getSeed(Object key) {
    return getSplitterStorage().getSeed(key);
  }

  public PositionalRandomFactory getSplitter(Object key) {
    return getSplitterStorage().getSplitter(key);
  }

  public PositionalRandomFactory getSplitterForSeed(long seed) {
    return getSplitterStorage().getSplitterForSeed(seed);
  }

  public PositionalRandomFactory getSplitterForOptionalSeed(Object key, OptionalLong seed) {
    return seed.isPresent() ? getSplitterForSeed(seed.getAsLong()) : getSplitter(key);
  }

  public HolderLookup.Provider registries() {
    return positionProvider.getWorld$ec().registryAccess();
  }

  public LootContext lootContext() {
    if (lootContext != null) {
      return lootContext;
    }
    lootContext = new LootContext.Builder(new LootParams.Builder((ServerLevel) positionProvider.getWorld$ec())
        .withParameter(LootContextParams.ORIGIN, positionProvider.getPosition$ec())
        .withParameter(LootContextParams.THIS_ENTITY, positionProvider.getEntity$ec())
        .create(LootContextParamSets.COMMAND)).create(Optional.empty());
    return lootContext;
  }
}
