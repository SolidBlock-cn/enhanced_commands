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
import pers.solid.ecmd.nbt.NbtParserShared;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.nbt.predicate.NbtPredicateParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record NbtBlockPredicate(NbtPredicate nbtPredicate) implements BlockPredicate {
  public static final MapCodec<NbtBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(NbtBlockPredicate::new, NbtPredicate.CODEC.fieldOf("nbt").forGetter(NbtBlockPredicate::nbtPredicate)));

  @Override
  public String asString() {
    return nbtPredicate.asString(false);
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockEntity blockEntity = blockInWorld.getEntity();
    return blockEntity != null && nbtPredicate.test(blockEntity.saveWithoutMetadata(blockInWorld.getLevel().registryAccess()));
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockEntity blockEntity = blockInWorld.getEntity();
    final MutableComponent nameText = blockInWorld.getState().getBlock().getName();
    final MutableComponent posText = TextUtil.wrapVector(blockInWorld.getPos());
    final RegistryAccess registryManager = blockInWorld.getLevel().registryAccess();
    if (blockEntity == null) {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.nbt.not_block_entity", nameText, posText));
    } else if (nbtPredicate.test(blockEntity.saveWithoutMetadata(registryManager))) {
      return TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.nbt.pass", nameText, posText));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.nbt.fail"));
    }
  }

  @Override
  public Type getType() {
    return BlockPredicateTypes.NBT;
  }

  public enum Type implements BlockPredicateType<NbtBlockPredicate>, Parser<NbtBlockPredicate> {
    NBT_TYPE;

    @Override
    public MapCodec<NbtBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public NbtBlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
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
