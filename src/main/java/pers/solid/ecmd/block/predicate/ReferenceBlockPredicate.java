package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ReferenceEntry;

public record ReferenceBlockPredicate(Holder.Reference<BlockPredicate> value) implements BlockPredicate, ReferenceEntry<ReferenceBlockPredicate, BlockPredicate> {
  public static final MapCodec<ReferenceBlockPredicate> CODEC = ReferenceEntry.createCodec(BlockPredicate.REGISTRY_KEY, BlockPredicate.CODEC, ReferenceBlockPredicate::new);

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockPredicate value = value().value();
    return value.test(blockInWorld, executionContext);
  }

  @Override
  public BlockPredicateType<ReferenceBlockPredicate> getType() {
    return BlockPredicateTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + value.getRegisteredName();
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
    protected ReferenceBlockPredicate getResultByHolderReference(Holder.Reference<BlockPredicate> holderReference) {
      return new ReferenceBlockPredicate(holderReference);
    }

    @Override
    protected CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
      return EnhancedCommandsCommandExceptionTypes.UNKNOWN_BLOCK_PREDICATE_ID.createWithContext(reader, identifier);
    }
  }
}
