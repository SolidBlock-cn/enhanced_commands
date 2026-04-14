package pers.solid.ecmd.entity.predicate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record BoxEntityPredicate(AABB box, PositionOffsetInfo offset) implements SpecialEntityPredicate {
  public static final MapCodec<BoxEntityPredicate> CODEC = MapCodec.unit(() -> new BoxEntityPredicate(AABB.unitCubeFromLowerCorner(Vec3.ZERO), PositionOffsetInfo.NO_OP));
  private static final LoadingCache<BoxEntityPredicate, LoadingCache<Vec3, AABB>> cache = CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(input -> CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(vec3d -> input.box.move(input.offset.apply(vec3d))))));

  @Override
  public boolean test(Entity entity, ExecutionContext context) {
    return cache.getUnchecked(this).getUnchecked(context.positionProvider.getPosition$ec()).intersects(entity.getBoundingBox());
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final Level expected = context.positionProvider.getWorld$ec();
    final Level actual = entity.level();
    if (!expected.equals(actual)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.local_world.false", displayName, TextUtil.literal(actual.dimension().location()), TextUtil.literal(expected.dimension().location())));
    }
    final AABB box = cache.getUnchecked(this).getUnchecked(context.positionProvider.getPosition$ec());
    if (box.intersects(entity.getBoundingBox())) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.box.true", displayName, box.toString()));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.box.false", displayName, box.toString()));
    }
  }

  @Override
  public EntityPredicateType<BoxEntityPredicate> getType() {
    return EntityPredicateTypes.BOX;
  }

  @Override
  public String asString() {
    return "<box>";
  }
}
