package pers.solid.ecmd.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccessor {
  @Accessor
  String getPlayerName();

  @Accessor
  UUID getUuid();

  @Invoker
  Predicate<Entity> callGetPositionPredicate(Vec3d pos);

  @Accessor
  Function<Vec3d, Vec3d> getPositionOffset();

  @Invoker
  void callCheckSourcePermission(ServerCommandSource source) throws CommandSyntaxException;

  @Accessor
  Box getBox();

  @Accessor
  NumberRange.FloatRange getDistance();
}
