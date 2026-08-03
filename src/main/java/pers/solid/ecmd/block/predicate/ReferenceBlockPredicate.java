package pers.solid.ecmd.block.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public record ReferenceBlockPredicate(Holder.Reference<BlockPredicate> reference) implements BlockPredicate, ReferenceEntry<ReferenceBlockPredicate, BlockPredicate> {
  public static final MapCodec<ReferenceBlockPredicate> CODEC = ReferenceEntry.createCodec(DefaultNamespace.MINECRAFT.idCodec(true), BlockPredicate.REGISTRY_KEY, ReferenceBlockPredicate::new);

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockPredicate value = reference().value();
    return value.test(blockInWorld, executionContext);
  }

  @Override
  public BlockPredicateType<ReferenceBlockPredicate> getType() {
    return BlockPredicateTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(reference.key().location());
  }

  @Override
  public ResourceKey<? extends Registry<BlockPredicate>> registryKey() {
    return BlockPredicate.REGISTRY_KEY;
  }

  public static class ReferencePrefixedParser extends PrefixedIdParser<ReferenceBlockPredicate, BlockPredicate> {
    public static final ReferencePrefixedParser INSTANCE = new ReferencePrefixedParser();

    protected ReferencePrefixedParser() {
      super('$', Component.translatable("enhanced_commands.block_predicate.reference"), BlockPredicate.REGISTRY_KEY);
    }

    @Override
    protected ReferenceBlockPredicate getResultByReference(Holder.Reference<BlockPredicate> holderReference) {
      return new ReferenceBlockPredicate(holderReference);
    }
  }
}
