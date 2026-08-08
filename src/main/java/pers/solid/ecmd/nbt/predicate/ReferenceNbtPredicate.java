package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public record ReferenceNbtPredicate(Holder.Reference<NbtPredicate> reference) implements NbtPredicate, ReferenceEntry<NbtPredicate> {
  public static final MapCodec<ReferenceNbtPredicate> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), REGISTRY_KEY, ReferenceNbtPredicate::new);
  public static final PrefixedIdParser<ReferenceNbtPredicate, NbtPredicate> PREFIXED_ID_PARSER = new PrefixedIdParser<>('$', Component.translatable("enhanced_commands.nbt_predicate.reference"), REGISTRY_KEY, ReferenceNbtPredicate::new);

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(identifier());
  }

  @Override
  public boolean test(Tag nbtElement, ExecutionContext context) {
    return value().test(nbtElement, context);
  }

  @Override
  public NbtPredicateType<ReferenceNbtPredicate> getType() {
    return NbtPredicateTypes.REFERENCE;
  }
}
