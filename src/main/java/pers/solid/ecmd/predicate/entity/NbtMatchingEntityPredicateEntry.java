package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;

public record NbtMatchingEntityPredicateEntry(NbtCompound nbtCompound, boolean hasNegation) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    NbtCompound actualNbt = entity.writeNbt(new NbtCompound());
    if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
      ItemStack itemStack = serverPlayerEntity.getInventory().getMainHandStack();
      if (!itemStack.isEmpty()) {
        actualNbt.put("SelectedItem", itemStack.writeNbt(new NbtCompound()));
      }
    }
    boolean matches = NbtHelper.matches(nbtCompound, actualNbt, true);
    return TestResult.of(matches != hasNegation, Text.translatable("enhanced_commands.entity_predicate.nbt." + (matches ? "pass" : "fail"), entity));
  }

  @Override
  public String toOptionEntry() {
    return "nbt=" + (hasNegation ? "!" : "") + nbtCompound.toString();
  }
}
