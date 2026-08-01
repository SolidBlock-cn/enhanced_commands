package pers.solid.ecmd.block.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;
import pers.solid.ecmd.util.pack.SafeReference;

public record ReferenceBlockPredicate(SafeReference<BlockPredicate> reference) implements BlockPredicate, ReferenceEntry<ReferenceBlockPredicate, BlockPredicate> {
  public static final MapCodec<ReferenceBlockPredicate> CODEC = ReferenceEntry.createCodec(DefaultNamespace.MINECRAFT.idCodec(true), BlockPredicate.REGISTRY_KEY, ReferenceBlockPredicate::new);

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final @Nullable BlockPredicate value = reference().value(executionContext, null);
    return value != null && value.test(blockInWorld, executionContext);
  }

  @Override
  public BlockPredicateType<ReferenceBlockPredicate> getType() {
    return BlockPredicateTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(reference.identifier());
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
    protected ReferenceBlockPredicate getResultByReference(SafeReference<BlockPredicate> holderReference) {
      return new ReferenceBlockPredicate(holderReference);
    }
  }
}
