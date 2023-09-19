package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.StringUtil;

import java.util.regex.Pattern;

/**
 * 对方块的 id 进行替换。如果方块的 id 替换后不存在，则不进行修改。例如：
 * <pre>
 *   idreplace('wool', terracotta)
 *   idreplace('_planks$', 'wood')
 * </pre>
 */
public record IdReplaceBlockFunction(Pattern pattern, String replacement, RegistryWrapper<Block> registryWrapper) implements BlockFunction {
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof IdReplaceBlockFunction that))
      return false;
    // 忽略 registryWrapper
    return pattern.pattern().equals(that.pattern.pattern()) && replacement.equals(that.replacement);
  }

  @Override
  public int hashCode() {
    // 忽略 registryWrapper
    return 31 * pattern.pattern().hashCode() + replacement.hashCode();
  }

  @Override
  public @NotNull String asString() {
    return "idreplace(" + NbtString.escape(pattern.pattern()) + ", " + NbtString.escape(replacement) + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final Block block = blockState.getBlock();
    final String old = Registries.BLOCK.getId(block).toString();
    final String replaced = pattern.matcher(old).replaceAll(replacement);
    final Identifier identifier = Identifier.tryParse(replaced);
    if (identifier == null)
      return blockState;
    return registryWrapper.getOptional(RegistryKey.of(RegistryKeys.BLOCK, identifier)).map(blockReference -> blockReference.value().getDefaultState()).orElse(blockState);
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("pattern", pattern.pattern());
    nbtCompound.putString("replacement", replacement);
  }

  @Override
  public @NotNull BlockFunctionType<IdReplaceBlockFunction> getType() {
    return BlockFunctionTypes.ID_REPLACE;
  }

  public enum Type implements BlockFunctionType<IdReplaceBlockFunction> {
    ID_REPLACE_TYPE;

    @Override
    public IdReplaceBlockFunction fromNbt(NbtCompound nbtCompound) {
      return new IdReplaceBlockFunction(
          Pattern.compile(nbtCompound.getString("pattern")),
          nbtCompound.getString("replacement"),
          Registries.BLOCK.getReadOnlyWrapper()
      );
    }

    @Override
    public @Nullable BlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new FunctionLikeParser<IdReplaceBlockFunction>() {
        private Pattern pattern;
        private String replacement;

        @Override
        public @NotNull String functionName() {
          return "idreplace";
        }

        @Override
        public Text tooltip() {
          return Text.translatable("enhancedCommands.argument.block_function.id_replace");
        }

        @Override
        public IdReplaceBlockFunction getParseResult(SuggestedParser parser) {
          return new IdReplaceBlockFunction(pattern, replacement, commandRegistryAccess.createWrapper(RegistryKeys.BLOCK));
        }

        @Override
        public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
          if (paramIndex == 0) {
            pattern = StringUtil.readRegex(parser.reader);
          } else if (paramIndex == 1) {
            replacement = parser.reader.readString();
          }
        }

        @Override
        public int minParamsCount() {
          return 2;
        }

        @Override
        public int maxParamsCount() {
          return 2;
        }
      }.parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
