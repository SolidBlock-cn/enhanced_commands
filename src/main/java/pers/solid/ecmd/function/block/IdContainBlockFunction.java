package pers.solid.ecmd.function.block;

import com.google.common.base.Suppliers;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 从 id 包含指定正则表达式的方块中随机选择一个。
 */
public final class IdContainBlockFunction implements BlockFunction {
  public @NotNull
  final Pattern pattern;
  private transient final Supplier<Block[]> blocks;

  public IdContainBlockFunction(@NotNull Pattern pattern, RegistryWrapper<Block> registryWrapper) {
    this.pattern = pattern;
    blocks = Suppliers.memoize(() -> registryWrapper.streamEntries().filter(reference -> pattern.matcher(reference.registryKey().getValue().toString()).find()).map(RegistryEntry.Reference::value).toArray(Block[]::new));
  }

  @Override
  public @NotNull String asString() {
    return "idcontain(" + NbtString.escape(pattern.toString()) + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final Block[] blocks = this.blocks.get();
    if (blocks.length == 0) {
      return blockState;
    }
    return blocks[world.getRandom().nextInt(blocks.length)].getDefaultState();
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("pattern", pattern.toString());
  }

  @Override
  public @NotNull BlockFunctionType<IdContainBlockFunction> getType() {
    return BlockFunctionTypes.ID_CONTAIN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof IdContainBlockFunction that))
      return false;

    return pattern.pattern().equals(that.pattern.pattern());
  }

  @Override
  public int hashCode() {
    return pattern.pattern().hashCode();
  }

  @Override
  public String toString() {
    return "IdContainBlockFunction{" +
        "pattern=" + pattern +
        '}';
  }

  public enum Type implements BlockFunctionType<IdContainBlockFunction> {
    ID_CONTAIN_TYPE;

    @Override
    public IdContainBlockFunction fromNbt(NbtCompound nbtCompound) {
      return new IdContainBlockFunction(Pattern.compile(nbtCompound.getString("pattern")), Registries.BLOCK.getReadOnlyWrapper());
    }

    @Override
    public @Nullable IdContainBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new FunctionLikeParser<IdContainBlockFunction>() {
        // @formatter:off
        Pattern pattern;
        @Override public int minParamsCount() {return 1;}
        @Override public int maxParamsCount() {return 1;}
        @Override public @NotNull String functionName() {return "idcontain";}
        @Override public Text tooltip() {return Text.translatable("enhancedCommands.argument.block_function.id_contain");}
        // @formatter:one
        @Override
        public IdContainBlockFunction getParseResult(SuggestedParser parser) {
          return new IdContainBlockFunction(pattern, commandRegistryAccess.createWrapper(RegistryKeys.BLOCK));
        }

        @Override
        public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
          final StringReader reader = parser.reader;
          final int cursorAtRegexBegin = reader.getCursor() + (reader.canRead() && StringReader.isQuotedStringStart(reader.peek()) ? 1 : 0);
          try {
            parser.suggestions.clear();
            pattern = Pattern.compile(reader.readString());
          } catch (PatternSyntaxException e) {
            reader.setCursor(cursorAtRegexBegin);
            throw ModCommandExceptionTypes.INVALID_REGEX.createWithContext(reader, e.getMessage().replace(StringUtils.CR, StringUtils.EMPTY));
          }
        }
      }.parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
