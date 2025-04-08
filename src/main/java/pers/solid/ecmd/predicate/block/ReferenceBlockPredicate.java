package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import org.apache.commons.lang3.function.FailableSupplier;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.IdentityReference;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.ReferenceEntry;

import java.util.Objects;
import java.util.WeakHashMap;

public final class ReferenceBlockPredicate implements BlockPredicate, ReferenceEntry<ReferenceBlockPredicate, BlockPredicate> {
  public static final MapCodec<ReferenceBlockPredicate> CODEC = ReferenceEntry.createCodec(BlockPredicate.REGISTRY_KEY, ReferenceBlockPredicate::new);
  private final RegistryKey<BlockPredicate> id;
  private transient final IdentityReference ref = new IdentityReference();
  private final WeakHashMap<IdentityReference, BlockPredicate> REF_CACHE = new WeakHashMap<>();

  public ReferenceBlockPredicate(RegistryKey<BlockPredicate> id) {
    this.id = id;
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    try {
      final WorldView world = cachedBlockPosition.getWorld();
      if (!(world instanceof ServerWorld serverWorld)) {
        return false;
      }
      final BlockPredicate value = value(serverWorld.getServer().getReloadableRegistries().getRegistryManager());
      final Random random = serverWorld.getRandom();
      return REF_CACHE.computeIfAbsent(ref, identityReference -> value.getRefreshed(random)).test(cachedBlockPosition);
    } catch (CommandSyntaxException e) {
      throw new CommandRuntimeException(e);
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.REFERENCE;
  }

  @Override
  public @NotNull String asString() {
    return "$" + id.getValue();
  }

  @Override
  public CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
    return Type.INSTANCE.createExceptionForUnknownId(reader, identifier);
  }

  @Override
  public RegistryKey<BlockPredicate> id() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ReferenceBlockPredicate) obj;
    return Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "ReferenceBlockPredicate[" +
        "id=" + id + ']';
  }


  public static class Type extends PrefixedIdParser<BlockPredicateArgument, BlockPredicate> implements BlockPredicateType<ReferenceBlockPredicate> {
    public static final Type INSTANCE = new Type();

    protected Type() {
      super('$', Text.translatable("enhanced_commands.block_predicate.reference"), BlockPredicate.REGISTRY_KEY);
    }

    @Override
    public @NotNull MapCodec<ReferenceBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    protected BlockPredicateArgument getResultByEntrySupplier(FailableSupplier<RegistryKey<BlockPredicate>, CommandSyntaxException> supplier) {
      return source -> new ReferenceBlockPredicate(supplier.get());
    }

    @Override
    protected CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
      return ModCommandExceptionTypes.UNKNOWN_BLOCK_PREDICATE_ID.createWithContext(reader, identifier);
    }
  }
}
