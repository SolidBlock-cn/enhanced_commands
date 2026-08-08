package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.nbt.NbtParserShared;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.nbt.predicate.NbtPredicateParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;

public record NbtBlockPredicate(NbtPredicate nbtPredicate) implements BlockPredicate {
  public static final MapCodec<NbtBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(NbtBlockPredicate::new, NbtPredicate.CODEC.fieldOf("nbt").forGetter(NbtBlockPredicate::nbtPredicate)));

  @Override
  public String expressAsString() {
    return nbtPredicate.asString(false);
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext context) {
    final BlockEntity blockEntity = blockInWorld.getEntity();
    return blockEntity != null && nbtPredicate.test(blockEntity.saveWithoutMetadata(blockInWorld.getLevel().registryAccess()), context);
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext context) {
    final BlockEntity blockEntity = blockInWorld.getEntity();
    final MutableComponent nameText = blockInWorld.getState().getBlock().getName();
    final MutableComponent posText = TextUtil.wrapVector(blockInWorld.getPos());
    final RegistryAccess registryManager = blockInWorld.getLevel().registryAccess();
    if (blockEntity == null) {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.nbt.not_block_entity", nameText, posText));
    } else if (nbtPredicate.test(blockEntity.saveWithoutMetadata(registryManager), context)) {
      return TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.nbt.pass", nameText, posText));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.nbt.fail"));
    }
  }

  @Override
  public BlockPredicateType<NbtBlockPredicate> getType() {
    return BlockPredicateTypes.NBT;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(nbtPredicate);
  }

  public enum NbtParser implements Parser<NbtBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable NbtBlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("{", NbtParserShared.START_OF_COMPOUND, suggestionsBuilder).buildFuture());
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == '{') {
        return new NbtBlockPredicate(NbtPredicateParser.parseCompound(parseContext, false));
      } else {
        return null;
      }
    }
  }
}
