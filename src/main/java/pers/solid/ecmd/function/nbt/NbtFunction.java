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

/**
 * <p>NBT 函数指定了对 NBT 数据的处理，可以直接替换一个 NBT 值，也可以进行具体修改，例如连接字符串、截取子字符串、修改列表等。每个 NBT 函数对 NBT 的具体处理由 {@link #apply} 决定，该方法接收一个 NBT 元素作为参数。
 * <p>每个 NBT 函数都有对应的类型，即 {@link NbtFunctionType}，决定了该 NBT 函数将进行序列化和反序列化。
 * <p>在命令参数中，NBT 函数在解析时实际上是 {@link NbtFunctionArgument}，并根据 {@link ServerCommandSource} 转换成具体的 NBT 函数。
 */
public interface NbtFunction extends ExpressionConvertible, NbtFunctionArgument {
  Codec<NbtFunction> CODEC = NbtFunctionType.REGISTRY.getCodec().dispatch(NbtFunction::getType, NbtFunctionType::getCodec);

  static @NotNull NbtFunction parse(CommandRegistryAccess registryAccess, String s, ServerCommandSource source) throws CommandSyntaxException {
    return NbtFunctionArgument.parse(new ParseContext<>(registryAccess, new StringReader(s), false, true), false, false).toAbsolute(source);
  }

  /**
   * 对 NBT 函数在 NBT 元素上进行递归应用。如果该 NBT 元素是列表、复合标签等，则它的元素也会被应用，除非该列表或者复合标签自身即被应用。
   *
   * @param nbtFunction 需要对 NBT 应用的函数。虽然 NBT 函数通常不能返回 null，但此参数的 apply 是可以返回 null 的，以表示没有对 NBT 进行处理。对于 NBT 列表、复合标签而言，如果是未被处理的，则它的子元素也会被应用。
   * @param nbtElement  需要被应用的 NBT 元素。如果该元素是列表、复合标签等，则该元素的元素也会被应用，除非该列表或复合标签自身已经被应用了函数。
   * @param predicate   仅符合该谓词的 NBT 元素会被应用。
   * @return 被修改后的 NBT 元素
   */
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

  /**
   * 将 NBT 函数用字符串表示后的结果。
   */
  @Override
  @NotNull
  String asString();

  /**
   * 将 NBT 函数用字符串表示后的结果。
   *
   * @param requirePrefix 是否强制在返回的结果前加上冒号或等于号。通常用于 NBT 复合标签的值。比如，NBT 函数“<code>:3</code>”可以写成“<code>3</code>”，但是“<code>{x: 3}</code>”显然不能写成“<code>{x 3}</code>”。
   */
  @NotNull
  default String asString(boolean requirePrefix) {
    return requirePrefix ? ": " + asString() : asString();
  }

  /**
   * @return NBT 函数的类型，将用于序列化。
   */
  @NotNull NbtFunctionType<?> getType();

  /**
   * 根据现有的 NBT 元素（可能为 null）返回所需要的 NBT 元素。允许完全忽略原先的 NBT 元素。当接收的 NBT 元素为可变对象时，可能会直接修改并返回它。
   *
   * @param nbtElement 需要被应用的 NBT 元素，可能自身会被修改
   */
  @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException;

  default @NotNull FailableFunction<NbtElement, NbtElement, CommandSyntaxException> asJavaFunction(ExecutionContext context) {
    return input -> apply(input, context);
  }

  default @NotNull NbtElement recursivelyApply(NbtElement nbtElement, NbtPredicate predicate, ExecutionContext context) throws CommandSyntaxException {
    return recursivelyApply(input -> this.apply(input, context), nbtElement, predicate);
  }

  @Override
  default NbtFunction toAbsolute(ServerCommandSource source) {
    return this;
  }
}
