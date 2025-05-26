package pers.solid.ecmd.predicate.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.function.Predicate;

public interface BlockPredicate extends Predicate<CachedBlockPosition>, ExpressionConvertible, BlockPredicateArgument {
  Codec<BlockPredicate> MAP_CODEC = BlockPredicateType.REGISTRY.getCodec().dispatch(BlockPredicate::getType, BlockPredicateType::getCodec);
  Codec<BlockPredicate> CODEC = CodecUtil.combined(Registries.BLOCK.getCodec().xmap(block -> new SimpleBlockPredicate(block, ImmutableList.of()), SimpleBlockPredicate::block), MAP_CODEC, blockPredicate -> blockPredicate instanceof SimpleBlockPredicate s && s.properties().isEmpty() ? s : null);
  RegistryKey<Registry<BlockPredicate>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("block_predicate"));
  Codec<RegistryEntry<BlockPredicate>> ENTRY_CODEC = RegistryElementCodec.of(REGISTRY_KEY, CODEC);

  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.block_predicate.cannot_parse"));

  static @NotNull BlockPredicate parse(CommandRegistryAccess registryAccess, String s, ServerCommandSource source) throws CommandSyntaxException {
    return BlockPredicateArgument.parse(new ParseContext<>(registryAccess, new StringReader(s), false, true)).apply(source);
  }

  static TestResult successResult(BlockPos blockPos) {
    return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.pass", TextUtil.wrapVector(blockPos)));
  }

  static TestResult failResult(BlockPos blockPos) {
    return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.fail", TextUtil.wrapVector(blockPos)));
  }

  static TestResult successOrFail(boolean successes, BlockPos blockPos) {
    return successes ? successResult(blockPos) : failResult(blockPos);
  }

  @ApiStatus.NonExtendable
  @Override
  @Deprecated
  default boolean test(CachedBlockPosition cachedBlockPosition) {
    final Random random = ((WorldAccess) cachedBlockPosition.getWorld()).getRandom();
    return test(cachedBlockPosition, new BlockPredicateContext(random, null));
  }

  boolean test(CachedBlockPosition cachedBlockPosition, BlockPredicateContext context);

  default TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, BlockPredicateContext context) {
    final boolean test = test(cachedBlockPosition, context);
    return successOrFail(test, cachedBlockPosition.getBlockPos());
  }

  @NotNull
  BlockPredicateType<?> getType();

  @Override
  default BlockPredicate apply(ServerCommandSource source) throws CommandSyntaxException {
    return this;
  }

}
