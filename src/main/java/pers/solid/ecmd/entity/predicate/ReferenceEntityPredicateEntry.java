package pers.solid.ecmd.entity.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.pack.ReferenceEntry;

import java.util.List;

public record ReferenceEntityPredicateEntry(Holder.Reference<EntityPredicate> reference) implements EntityPredicateEntry, ReferenceEntry<EntityPredicate> {
  public static final MapCodec<ReferenceEntityPredicateEntry> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), REGISTRY_KEY, ReferenceEntityPredicateEntry::new);

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(identifier());
  }

  @Override
  public String toOptionEntry() {
    return "reference=" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(identifier());
  }

  @Override
  public boolean test(Entity entity, ExecutionContext context) {
    return value().test(entity, context);
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) throws CommandSyntaxException {
    final TestResult testResult = value().testAndDescribe(entity, context);
    final MutableComponent idText = TextUtil.literal(identifier()).withStyle(Styles.EXPECTED);
    if (testResult.successes()) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.reference.pass", displayName, idText), List.of(testResult));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.reference.fail", displayName, idText), List.of(testResult));
    }
  }

  @Override
  public EntityPredicateType<ReferenceEntityPredicateEntry> getType() {
    return EntityPredicateTypes.REFERENCE;
  }
}
