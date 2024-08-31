package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Optional;

public record ReferenceBlockFunction(RegistryEntry<BlockFunction> entry) implements BlockFunction {
  public static final MapCodec<ReferenceBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockFunction.ENTRY_CODEC.fieldOf("id").forGetter(ReferenceBlockFunction::entry)).apply(i, ReferenceBlockFunction::new));

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    return entry.value().getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
  }

  @Override
  public @NotNull BlockFunctionType<?> getType() {
    return BlockFunctionTypes.REFERENCE;
  }

  @Override
  public @NotNull String asString() {
    return "reference(" + entry.getIdAsString() + ")";
  }

  public enum ReferenceType implements BlockFunctionType<ReferenceBlockFunction>, Parser<BlockFunctionArgument> {
    INSTANCE;

    @Override
    public @NotNull MapCodec<ReferenceBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public BlockFunctionArgument parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException {
      parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("$", Text.translatable("enhanced_commands.block_predicate.reference"), suggestionsBuilder).buildFuture());
      boolean suffixed = false;
      while (parser.reader.canRead() && parser.reader.peek() == '$') {
        parser.reader.skip();
        suffixed = true;
      }
      if (!suffixed) return null;
      parser.clearSuggestion();

      // try to optimize with id?
      final int cursorBeforeId = parser.reader.getCursor();
      parser.addSuggestion((context, builder) -> {
        if (context.getSource() instanceof ServerCommandSource) {
          return CommandSource.suggestIdentifiers(registryAccess.getWrapperOrThrow(BlockFunction.REGISTRY_KEY).streamKeys().map(RegistryKey::getValue), builder.createOffset(cursorBeforeId));
        } else if (context.getSource() instanceof CommandSource commandSource) {
          return commandSource.getCompletions(context);
        } else {
          return Suggestions.empty();
        }
      });
      if (allowSparse) parser.reader.skipWhitespace();
      final Identifier id = Identifier.fromCommandInput(parser.reader);
      final int cursorAfterId = parser.reader.getCursor();
      return source -> {
        final Optional<RegistryEntry.Reference<BlockFunction>> entry = registryAccess.createRegistryLookup().getOptionalEntry(BlockFunction.REGISTRY_KEY, RegistryKey.of(BlockFunction.REGISTRY_KEY, id));
        if (entry.isEmpty()) {
          parser.reader.setCursor(cursorBeforeId);
          // todo separate function
          throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.UNKNOWN_FUNCTION.createWithContext(parser.reader, id.toString()), cursorAfterId);
        }
        return new ReferenceBlockFunction(entry.get());
      };
    }
  }
}
