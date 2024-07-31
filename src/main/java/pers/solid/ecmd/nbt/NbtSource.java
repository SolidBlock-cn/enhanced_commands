package pers.solid.ecmd.nbt;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.NbtConcentrationType;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.function.Function;

public interface NbtSource {
  default Collection<NbtCompound> getNbts(RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    return getNbts(Function.identity(), registryLookup);
  }

  <T> Collection<T> getNbts(Function<NbtCompound, T> mappingFunction, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException;

  NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) throws CommandSyntaxException;

  default NbtElement getConcentratedNbts(Function<NbtCompound, ? extends NbtElement> mappingFunction, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
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

  Text feedbackQuery(NbtElement nbtElement, NbtConcentrationType nbtConcentrationType);

  interface Single extends NbtSource {
    NbtCompound getNbt();

    @Override
    default <T> Collection<T> getNbts(Function<NbtCompound, T> mappingFunction, RegistryWrapper.@NotNull WrapperLookup registryLookup) throws CommandSyntaxException {
      return Collections.singletonList(mappingFunction.apply(getNbt()));
    }

    @Override
    default NbtElement concentrateNbts(Collection<? extends NbtElement> nbtElements) {
      return Iterables.getOnlyElement(nbtElements);
    }
  }
}
