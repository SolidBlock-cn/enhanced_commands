package pers.solid.ecmd.nbt.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

/**
 * <p>处理 NBT 列表的 NBT 函数。如果原有 NBT 不是列表，会直接替换为空列表，然后再应用 函数。
 * <p>{@link ListNbtFunction} 有两种类型：{@link ListOpsNbtFunction} 用于替换列表内容和设置列表元素，{@link ListInsertionNbtFunction} 用于在列表中插入元素。在 NBT 函数语法中，如果列表中有省略号，将识别为 {@link ListInsertionNbtFunction}，否则识别为 {@link ListOpsNbtFunction}。
 * <p>例如，以下 NBT 函数均会被解析为 {@link ListOpsNbtFunction}：
 * <ul>
 *   <li>{@code [a, b, c]}</li>
 *   <li>{@code [1: a, 3: b]}</li>
 *   <li>{@code [a, b, 1: c]}</li>
 * </ul>
 * <p>以下 NBT 函数均会被解析为 {@link ListInsertionNbtFunction}：
 * <ul>
 *   <li>{@code [..., a, b]}</li>
 *   <li>{@code [a, b, ...]}</li>
 *   <li>{@code [a, ..., b]}</li>
 *   <li>{@code [a, 1: b, ..., c, 5: d]}</li>
 * </ul>
 * <p><strong>注意：</strong>在 1.21.4 及之前的版本中，NBT 列表不支持混合多种不同类型的元素。NBT 函数在解析时不会进行检查。在应用修改时，如果遇到了因元素类型差异而设置/插入失败的情况，则会直接忽略。
 *
 * @see ListOpsNbtFunction
 * @see ListInsertionNbtFunction
 */
public interface ListNbtFunction extends NbtFunction {
  @Override
  default Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    final ListTag targetList = nbtElement instanceof final ListTag nbtList ? nbtList : new ListTag();
    return applyOnList(targetList, context);
  }

  ListTag applyOnList(ListTag listTag, ExecutionContext context) throws CommandSyntaxException;
}
