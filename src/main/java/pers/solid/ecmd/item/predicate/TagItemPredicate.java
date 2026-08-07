package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.codec.CodecUtil;

public record TagItemPredicate(HolderSet.Named<Item> items) implements ItemPredicateWithoutContext {
  public static final Codec<TagItemPredicate> STRING_BASED_CODEC = CodecUtil.holderSetNamed(Registries.ITEM).xmap(TagItemPredicate::new, TagItemPredicate::items);

  public static final MapCodec<TagItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(CodecUtil.holderSetNamed(Registries.ITEM).fieldOf("items").forGetter(TagItemPredicate::items)).apply(i, TagItemPredicate::new));

  @Override
  public boolean test(ItemStack stack) {
    return stack.is(items);
  }

  @Override
  public ItemPredicateType<TagItemPredicate> getType() {
    return ItemPredicateTypes.SIMPLE_TAG;
  }

  @Override
  public String expressAsString() {
    return "#" + DefaultNamespace.MINECRAFT.toSimplerString(items.key().location());
  }
}
