package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.regex.Pattern;

public record IdContainBlockPredicate(@NotNull Pattern pattern) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return "idcontain(" + NbtString.escape(pattern.pattern()) + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return pattern.matcher(Registries.BLOCK.getId(cachedBlockPosition.getBlockState().getBlock()).toString()).find();
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final String id = Registries.BLOCK.getId(cachedBlockPosition.getBlockState().getBlock()).toString();
    final boolean matches = pattern.matcher(id).matches();
    return TestResult.of(matches, Text.translatable("enhanced_commands.block_predicate.id_contain." + (matches ? "pass" : "fail"), Text.literal(pattern.toString()).styled(TextUtil.STYLE_FOR_EXPECTED), Text.literal(id).styled(TextUtil.STYLE_FOR_ACTUAL)));
  }

  @Override
  public @NotNull BlockPredicateType<IdContainBlockPredicate> getType() {
    return BlockPredicateTypes.ID_CONTAIN;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("pattern", pattern.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof IdContainBlockPredicate that))
      return false;

    return pattern.pattern().equals(that.pattern.pattern());
  }

  @Override
  public int hashCode() {
    return pattern.pattern().hashCode();
  }

  public enum Type implements BlockPredicateType<IdContainBlockPredicate> {
    ID_CONTAIN_TYPE;

    @Override
    public @NotNull IdContainBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new IdContainBlockPredicate(Pattern.compile(nbtCompound.getString("pattern")));
    }
  }

  public static class Parser implements FunctionParamsParser<IdContainBlockPredicate> {
    private Pattern pattern;

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }

    @Override
    public IdContainBlockPredicate getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) { // @formatter:on
      return new IdContainBlockPredicate(pattern);
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestionProviders.clear();
      pattern = ParsingUtil.readRegex(parser.reader);
    }
  }
}
