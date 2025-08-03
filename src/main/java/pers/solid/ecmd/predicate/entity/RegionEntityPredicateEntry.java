package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

public record RegionEntityPredicateEntry(@NotNull RegionArgument<?> region) implements EntityPredicateEntry {
  public static final MapCodec<RegionEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      RegionArgument.CODEC.fieldOf("region").forGetter(RegionEntityPredicateEntry::region)
  ).apply(i, RegionEntityPredicateEntry::new));

  @Override
  public String toOptionEntry() {
    return "region=" + region.asString();
  }

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    return Region.getCached(region, context.positionProvider).contains(entity.getPos());
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final Region cached = Region.getCached(region, context.positionProvider);
    if (cached.contains(entity.getPos())) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.region.true", displayName, cached.asString()));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.region.false", displayName, cached.asString()));
    }
  }

  @Override
  public @NotNull EntityPredicateType<RegionEntityPredicateEntry> getType() {
    return EntityPredicateTypes.REGION;
  }
}
