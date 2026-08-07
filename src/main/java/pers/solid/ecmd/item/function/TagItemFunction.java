package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.List;

public record TagItemFunction(HolderSet.Named<Item> tag) implements ItemFunction {
  public static final MapCodec<TagItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      CodecUtil.holderSetNamed(Registries.ITEM).fieldOf("tag").forGetter(TagItemFunction::tag)
  ).apply(i, TagItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    return tag.getRandomElement(context.random).map(ItemStack::new).orElse(ItemStack.EMPTY);
  }

  @Override
  public ItemFunctionType<TagItemFunction> getType() {
    return ItemFunctionTypes.TAG;
  }

  @Override
  public String expressAsString() {
    return "#" + DefaultNamespace.MINECRAFT.toSimplerString(tag.key().location());
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(tag);
  }
}
