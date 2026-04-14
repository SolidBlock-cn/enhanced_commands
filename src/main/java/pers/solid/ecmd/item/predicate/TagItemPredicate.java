package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Collectors;

public record TagItemPredicate(HolderSet<Item> items) implements ItemPredicateWithoutContext {
  public static final Codec<TagItemPredicate> STRING_BASED_CODEC = TagKey.hashedCodec(Registries.ITEM).<HolderSet<Item>>flatXmap(tagKey -> BuiltInRegistries.ITEM.getTag(tagKey).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown tag: " + tagKey.location())), items -> items.unwrapKey().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown tag"))).xmap(TagItemPredicate::new, TagItemPredicate::items);

  public static final MapCodec<TagItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(TagItemPredicate::items)).apply(i, TagItemPredicate::new));

  public TagItemPredicate(TagKey<Item> tagKey) {
    this(BuiltInRegistries.ITEM.getTag(tagKey).orElseThrow(() -> new RuntimeException("Item " + tagKey + " not found!")));
  }

  @Override
  public boolean test(ItemStack stack) {
    return stack.is(items);
  }

  @Override
  public Type getType() {
    return ItemPredicateTypes.SIMPLE_TAG;
  }

  @Override
  public String asString() {
    return items.unwrap().map(itemTagKey -> "#" + itemTagKey.location(), holders -> holders.stream().map(Holder::getRegisteredName).collect(Collectors.joining("|")));
  }

  public enum Type implements ItemPredicateType<TagItemPredicate> {
    SIMPLE_TAG_TYPE;

    @Override
    public MapCodec<TagItemPredicate> getCodec() {
      return CODEC;
    }
  }
}
