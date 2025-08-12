package pers.solid.ecmd.predicate.entity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record BoxEntityPredicate(Box box, PositionOffsetInfo offset) implements SpecialEntityPredicate {
  public static final MapCodec<BoxEntityPredicate> CODEC = MapCodec.unit(() -> new BoxEntityPredicate(Box.from(Vec3d.ZERO), PositionOffsetInfo.NO_OP));
  private static final LoadingCache<@NotNull BoxEntityPredicate, LoadingCache<@NotNull Vec3d, Box>> cache = CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(input -> CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(vec3d -> input.box.offset(input.offset.apply(vec3d))))));

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    return cache.getUnchecked(this).getUnchecked(context.positionProvider.getPosition$ec()).intersects(entity.getBoundingBox());
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final World expected = context.positionProvider.getWorld$ec();
    final World actual = entity.getWorld();
    if (!expected.equals(actual)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.local_world.false", displayName, TextUtil.literal(actual.getRegistryKey().getValue()), TextUtil.literal(expected.getRegistryKey().getValue())));
    }
    final Box box = cache.getUnchecked(this).getUnchecked(context.positionProvider.getPosition$ec());
    if (box.intersects(entity.getBoundingBox())) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.box.true", displayName, box.toString()));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.box.false", displayName, box.toString()));
    }
  }

  @Override
  public @NotNull EntityPredicateType<BoxEntityPredicate> getType() {
    return EntityPredicateTypes.BOX;
  }

  @Override
  public @NotNull String asString() {
    return "<box>";
  }
}
