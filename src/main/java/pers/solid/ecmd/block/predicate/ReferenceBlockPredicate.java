package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.apache.commons.lang3.function.FailableSupplier;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ReferenceEntry;

public record ReferenceBlockPredicate(ResourceKey<BlockPredicate> id) implements BlockPredicate, ReferenceEntry<ReferenceBlockPredicate, BlockPredicate> {
  public static final MapCodec<ReferenceBlockPredicate> CODEC = ReferenceEntry.createCodec(BlockPredicate.REGISTRY_KEY, ReferenceBlockPredicate::new);

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    try {
      final LevelReader world = blockInWorld.getLevel();
      if (!(world instanceof ServerLevel serverWorld)) {
        return false;
      }
      final BlockPredicate value = value(serverWorld.getServer().reloadableRegistries().get());
      return value.test(blockInWorld, executionContext);
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
    return "$" + id.location();
  }

  @Override
  public CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
    return Type.INSTANCE.createExceptionForUnknownId(reader, identifier);
  }

  public static class Type extends PrefixedIdParser<ReferenceBlockPredicate, BlockPredicate> implements BlockPredicateType<ReferenceBlockPredicate> {
    public static final Type INSTANCE = new Type();

    protected Type() {
      super('$', Component.translatable("enhanced_commands.block_predicate.reference"), BlockPredicate.REGISTRY_KEY);
    }

    @Override
    public @NotNull MapCodec<ReferenceBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    protected ReferenceBlockPredicate getResultByEntrySupplier(FailableSupplier<ResourceKey<BlockPredicate>, CommandSyntaxException> supplier) throws CommandSyntaxException {
      return new ReferenceBlockPredicate(supplier.get());
    }

    @Override
    protected CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier) {
      return EnhancedCommandsCommandExceptionTypes.UNKNOWN_BLOCK_PREDICATE_ID.createWithContext(reader, identifier);
    }
  }
}
