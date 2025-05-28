package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.block.ExecutionContext;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public interface NbtFunction extends ExpressionConvertible, NbtFunctionArgument {
  Codec<NbtFunction> CODEC = NbtFunctionType.REGISTRY.getCodec().dispatch(NbtFunction::getType, NbtFunctionType::getCodec);

  static @NotNull NbtFunction parse(CommandRegistryAccess registryAccess, String s, ServerCommandSource source) throws CommandSyntaxException {
    return NbtFunctionArgument.parse(new ParseContext<>(registryAccess, new StringReader(s), false, true), false, false).toAbsolute(source);
  }

  @Override
  @NotNull
  default String asString() {
    return asString(false);
  }

  @NotNull String asString(boolean requirePrefix);

  @NotNull NbtFunctionType<?> getType();

  /**
   * 根据现有的 NBT 元素（可能为 null）返回所需要的 NBT 元素。原先的 NBT 元素可能会被完全忽略。当接收的 NBT 元素为可变对象时，可能会直接修改并返回它。
   */
  @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException;

  default @NotNull FailableFunction<NbtElement, NbtElement, CommandSyntaxException> asJavaFunction(ExecutionContext context) {
    return input -> apply(input, context);
  }

  default @NotNull NbtElement recursivelyApply(NbtElement nbtElement, NbtPredicate predicate, ExecutionContext context) throws CommandSyntaxException {
    return recursivelyApply(input -> this.apply(input, context), nbtElement, predicate);
  }

  static @NotNull NbtElement recursivelyApply(FailableFunction<@Nullable NbtElement, @Nullable NbtElement, CommandSyntaxException> nbtFunction, NbtElement nbtElement, @Nullable Predicate<@NotNull NbtElement> predicate) throws CommandSyntaxException {
    if (predicate == null || predicate.test(nbtElement)) {
      final NbtElement applied = nbtFunction.apply(nbtElement);
      if (applied != null) {
        return applied;
      }
      if (Objects.requireNonNull(nbtElement) instanceof NbtCompound nbtCompound) {
        final Set<String> keys = nbtCompound.getKeys();
        for (String key : keys) {
          final NbtElement value = nbtCompound.get(key);
          if (value == null) continue;
          final NbtElement appliedValue = recursivelyApply(nbtFunction, value, predicate);
          if (appliedValue != value) {
            nbtCompound.put(key, appliedValue);
          }
        }
        return nbtCompound;
      } else if (nbtElement instanceof NbtList nbtList) {
        for (int i = 0; i < nbtList.size(); i++) {
          final NbtElement value = nbtList.get(i);
          final NbtElement appliedElement = recursivelyApply(nbtFunction, value, predicate);
          if (appliedElement != value) {
            nbtList.setElement(i, appliedElement);
          }
        }
        return nbtList;
      }
      return nbtElement;
    } else {
      if (nbtElement instanceof NbtCompound nbtCompound) {
        final Set<String> keys = nbtCompound.getKeys();
        for (String key : keys) {
          final NbtElement value = nbtCompound.get(key);
          if (value == null) continue;
          final NbtElement appliedValue = recursivelyApply(nbtFunction, value, predicate);
          if (appliedValue != value) {
            nbtCompound.put(key, appliedValue);
          }
        }
        return nbtCompound;
      } else if (nbtElement instanceof NbtList nbtList) {
        for (int i = 0; i < nbtList.size(); i++) {
          final NbtElement value = nbtList.get(i);
          final NbtElement appliedElement = recursivelyApply(nbtFunction, value, predicate);
          if (appliedElement != value) {
            nbtList.setElement(i, appliedElement);
          }
        }
        return nbtList;
      }
      return nbtElement;
    }
  }

  @Override
  default NbtFunction toAbsolute(ServerCommandSource source) {
    return this;
  }
}
