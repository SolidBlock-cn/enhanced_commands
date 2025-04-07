package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

public interface NbtSource {
  default Collection<NbtCompound> getNbts(RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    return getNbts(FailableFunction.identity(), registryLookup);
  }

  <T> Collection<T> getNbts(FailableFunction<NbtCompound, T, CommandSyntaxException> mappingFunction, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException;

  NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) throws CommandSyntaxException;

  default NbtElement getConcentratedNbts(FailableFunction<NbtCompound, ? extends NbtElement, CommandSyntaxException> mappingFunction, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    return concentrateNbts(getNbts(mappingFunction, registryLookup));
  }

  default NbtElement getConcentratedNbts(NbtPathArgumentType.NbtPath path, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    try {
      return getConcentratedNbts(element -> {
        try {
          return Iterables.getOnlyElement(path.get(element));
        } catch (CommandSyntaxException e) {
          throw new RuntimeException(e);
        }
      }, registryLookup);
    } catch (NoSuchElementException | IllegalArgumentException e) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(e.getMessage());
    } catch (RuntimeException e) {
      if (e.getCause() instanceof CommandSyntaxException e1) {
        throw e1;
      } else {
        throw e;
      }
    }
  }

  Text feedbackQuery(NbtElement nbtElement);

  interface Single extends NbtSource {
    NbtCompound getNbt();

    @Override
    default <T> Collection<T> getNbts(FailableFunction<NbtCompound, T, CommandSyntaxException> mappingFunction, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException {
      return Collections.singletonList(mappingFunction.apply(getNbt()));
    }

    @Override
    default NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) {
      return Iterables.getOnlyElement(nbtElements);
    }
  }

  SimpleCommandExceptionType EXPECTED_WHITESPACE_AND_CONCENTRATION_TYPE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.parsing.expected_whitespace_and_concentration_type"));
  SimpleCommandExceptionType EXPECTED_CONCENTRATION_TYPE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.parsing.expected_concentration_type"));

  /**
   * 在读取了 NBT 来源的数据之后，如果必须指定聚合类型，则调用此方法以提醒输入者需要聚合类型。该方法也将自动跳过聚合类型前的空格。
   */
  static void expectConcentrationType(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead() || !Character.isWhitespace(reader.peek())) {
      throw EXPECTED_WHITESPACE_AND_CONCENTRATION_TYPE.createWithContext(reader);
    }
    reader.skipWhitespace();
    if (!reader.canRead()) {
      throw EXPECTED_CONCENTRATION_TYPE.createWithContext(reader);
    }
  }
}
