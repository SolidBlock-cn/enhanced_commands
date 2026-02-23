package pers.solid.ecmd.mixins.accessor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccessor {
  @Accessor
  @Nullable String getPlayerName();

  @Accessor
  @Nullable UUID getEntityUUID();

  @Invoker
  Predicate<Entity> callGetPredicate(Vec3 pos, @Nullable AABB box, @Nullable FeatureFlagSet enabledFeatures);

  @Accessor
  Function<Vec3, Vec3> getPosition();

  @Invoker
  void callCheckPermissions(CommandSourceStack source) throws CommandSyntaxException;

  @Accessor
  @Nullable AABB getAabb();

  @Accessor
  MinMaxBounds.Doubles getRange();

  @Invoker
  AABB callGetAbsoluteAabb(Vec3 offset);

  @Accessor
  List<Predicate<Entity>> getContextFreePredicates();

  @Accessor
  EntityTypeTest<Entity, ?> getType();

  @Accessor
  BiConsumer<Vec3, List<? extends Entity>> getOrder();
}
