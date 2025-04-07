package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TestResult;

public record NbtMatchingEntityPredicateEntry(@NotNull NbtCompound expectedNbt, boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
    NbtCompound actualNbt = entity.writeNbt(new NbtCompound());
    if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
      ItemStack itemStack = serverPlayerEntity.getInventory().getMainHandStack();
      if (!itemStack.isEmpty()) {
        actualNbt.put("SelectedItem", itemStack.encode(serverPlayerEntity.getRegistryManager()));
      }
    }

    return NbtHelper.matches(expectedNbt, actualNbt, true) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    NbtCompound actualNbt = entity.writeNbt(new NbtCompound());
    if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
      ItemStack itemStack = serverPlayerEntity.getInventory().getMainHandStack();
      if (!itemStack.isEmpty()) {
        actualNbt.put("SelectedItem", itemStack.encode(serverPlayerEntity.getRegistryManager()));
      }
    }

    boolean matches = NbtHelper.matches(expectedNbt, actualNbt, true);
    final boolean result = matches != inverted;
    if (matches) {
      return TestResult.of(result, Text.translatable("enhanced_commands.entity_predicate.nbt.pass", entity));
    } else {
      return TestResult.of(result, Text.translatable("enhanced_commands.entity_predicate.nbt.fail", entity));
    }
  }

  @Override
  public @Nullable String toOptionEntry() {
    return "nbt=" + (inverted ? "!" : "") + expectedNbt;
  }
}
