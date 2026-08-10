package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.regex.Pattern;

public record IdContainItemPredicate(Pattern pattern) implements ItemPredicateWithoutContext, DoesNotRequireValidation {
  public static final MapCodec<IdContainItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ExtraCodecs.PATTERN.fieldOf("pattern").forGetter(IdContainItemPredicate::pattern)).apply(i, IdContainItemPredicate::new));

  @Override
  public boolean test(ItemStack stack) {
    return pattern.matcher(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).find();
  }

  @Override
  public ItemPredicateType<IdContainItemPredicate> getType() {
    return ItemPredicateTypes.ID_CONTAIN;
  }

  @Override
  public String expressAsString() {
    return "id-contain(" + pattern.pattern() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof IdContainItemPredicate that)) return false;

    return pattern.pattern().equals(that.pattern.pattern());
  }

  @Override
  public int hashCode() {
    return pattern.pattern().hashCode();
  }
}
