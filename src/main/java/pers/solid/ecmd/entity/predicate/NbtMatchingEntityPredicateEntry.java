package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

public record NbtMatchingEntityPredicateEntry(CompoundTag nbt, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<NbtMatchingEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      CompoundTag.CODEC.fieldOf("nbt").forGetter(NbtMatchingEntityPredicateEntry::nbt),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(NbtMatchingEntityPredicateEntry::inverted)
  ).apply(i, NbtMatchingEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    CompoundTag actualNbt = entity.saveWithoutId(new CompoundTag());
    if (entity instanceof ServerPlayer serverPlayerEntity) {
      ItemStack itemStack = serverPlayerEntity.getInventory().getSelected();
      if (!itemStack.isEmpty()) {
        actualNbt.put("SelectedItem", itemStack.save(serverPlayerEntity.registryAccess()));
      }
    }

    return NbtUtils.compareNbt(nbt, actualNbt, true) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    CompoundTag actualNbt = entity.saveWithoutId(new CompoundTag());
    if (entity instanceof ServerPlayer serverPlayerEntity) {
      ItemStack itemStack = serverPlayerEntity.getInventory().getSelected();
      if (!itemStack.isEmpty()) {
        actualNbt.put("SelectedItem", itemStack.save(serverPlayerEntity.registryAccess()));
      }
    }

    boolean matches = NbtUtils.compareNbt(nbt, actualNbt, true);
    final boolean result = matches != inverted;
    if (matches) {
      return TestResult.of(result, Component.translatable("enhanced_commands.entity_predicate.nbt.pass", entity));
    } else {
      return TestResult.of(result, Component.translatable("enhanced_commands.entity_predicate.nbt.fail", entity));
    }
  }

  @Override
  public EntityPredicateType<NbtMatchingEntityPredicateEntry> getType() {
    return EntityPredicateTypes.NBT;
  }

  @Override
  public String toOptionEntry() {
    return "nbt=" + (inverted ? "!" : "") + nbt;
  }
}
