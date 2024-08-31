package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.NbtPredicateSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record NbtBlockPredicate(@NotNull NbtPredicate nbtPredicate) implements BlockPredicate {
  public static final MapCodec<NbtBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(NbtBlockPredicate::new, NbtPredicate.CODEC.fieldOf("nbt").forGetter(NbtBlockPredicate::nbtPredicate)));

  @Override
  public @NotNull String asString() {
    return nbtPredicate.asString(false);
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final BlockEntity blockEntity = cachedBlockPosition.getBlockEntity();
    return blockEntity != null && nbtPredicate.test(blockEntity.createNbt(cachedBlockPosition.getWorld().getRegistryManager()));
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final BlockEntity blockEntity = cachedBlockPosition.getBlockEntity();
    final MutableText nameText = cachedBlockPosition.getBlockState().getBlock().getName();
    final MutableText posText = TextUtil.wrapVector(cachedBlockPosition.getBlockPos());
    final DynamicRegistryManager registryManager = cachedBlockPosition.getWorld().getRegistryManager();
    if (blockEntity == null) {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.nbt.not_block_entity", nameText, posText));
    } else if (nbtPredicate.test(blockEntity.createNbt(registryManager))) {
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.nbt.pass", nameText, posText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.nbt.fail"));
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.NBT;
  }

  public enum Type implements BlockPredicateType<NbtBlockPredicate>, Parser<BlockPredicateArgument> {
    NBT_TYPE;

    @Override
    public @NotNull MapCodec<NbtBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable NbtBlockPredicate parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("{", NbtPredicateSuggestedParser.START_OF_COMPOUND, suggestionsBuilder).buildFuture());
      if (parser.reader.canRead() && parser.reader.peek() == '{') {
        return new NbtBlockPredicate(new NbtPredicateSuggestedParser<>(parser).parseCompound(false, false));
      } else {
        return null;
      }
    }
  }
}
