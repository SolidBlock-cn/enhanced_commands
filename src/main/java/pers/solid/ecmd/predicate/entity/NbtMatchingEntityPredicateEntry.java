package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

public record NbtMatchingEntityPredicateEntry(@NotNull NbtCompound nbt, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<NbtMatchingEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtCompound.CODEC.fieldOf("nbt").forGetter(NbtMatchingEntityPredicateEntry::nbt),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(NbtMatchingEntityPredicateEntry::inverted)
  ).apply(i, NbtMatchingEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    NbtCompound actualNbt = entity.writeNbt(new NbtCompound());
    if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
      ItemStack itemStack = serverPlayerEntity.getInventory().getMainHandStack();
      if (!itemStack.isEmpty()) {
        actualNbt.put("SelectedItem", itemStack.encode(serverPlayerEntity.getRegistryManager()));
      }
    }

    return NbtHelper.matches(nbt, actualNbt, true) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    NbtCompound actualNbt = entity.writeNbt(new NbtCompound());
    if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
      ItemStack itemStack = serverPlayerEntity.getInventory().getMainHandStack();
      if (!itemStack.isEmpty()) {
        actualNbt.put("SelectedItem", itemStack.encode(serverPlayerEntity.getRegistryManager()));
      }
    }

    boolean matches = NbtHelper.matches(nbt, actualNbt, true);
    final boolean result = matches != inverted;
    if (matches) {
      return TestResult.of(result, Text.translatable("enhanced_commands.entity_predicate.nbt.pass", entity));
    } else {
      return TestResult.of(result, Text.translatable("enhanced_commands.entity_predicate.nbt.fail", entity));
    }
  }

  @Override
  public @NotNull EntityPredicateType<NbtMatchingEntityPredicateEntry> getType() {
    return EntityPredicateTypes.NBT;
  }

  @Override
  public String toOptionEntry() {
    return "nbt=" + (inverted ? "!" : "") + nbt;
  }
}
