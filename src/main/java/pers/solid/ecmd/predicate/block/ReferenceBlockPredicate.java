package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableSupplier;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.ReferenceEntry;

public record ReferenceBlockPredicate(RegistryEntry<BlockPredicate> entry) implements BlockPredicate, ReferenceEntry<ReferenceBlockPredicate, BlockPredicate> {
  public static final MapCodec<ReferenceBlockPredicate> CODEC = ReferenceEntry.createCodec(BlockPredicate.ENTRY_CODEC, ReferenceBlockPredicate::new);

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return value().test(cachedBlockPosition);
  }

  @Override
  public @NotNull ReferenceType getType() {
    return BlockPredicateTypes.REFERENCE;
  }

  @Override
  public @NotNull String asString() {
    return "$" + entry.getIdAsString();
  }

  public static class ReferenceType extends ReferenceEntry.PrefixedIdParser<BlockPredicateArgument, BlockPredicate> implements BlockPredicateType<ReferenceBlockPredicate> {
    public static final ReferenceType INSTANCE = new ReferenceType();

    protected ReferenceType() {
      super('$', Text.translatable("enhanced_commands.block_predicate.reference"), BlockPredicate.REGISTRY_KEY);
    }

    @Override
    public @NotNull MapCodec<ReferenceBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    protected BlockPredicateArgument getResultByEntrySupplier(FailableSupplier<RegistryEntry<BlockPredicate>, CommandSyntaxException> supplier) {
      return source -> new ReferenceBlockPredicate(supplier.get());
    }

    @Override
    protected CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
      return ModCommandExceptionTypes.UNKNOWN_BLOCK_PREDICATE_ID.createWithContext(reader, identifier);
    }
  }
}
