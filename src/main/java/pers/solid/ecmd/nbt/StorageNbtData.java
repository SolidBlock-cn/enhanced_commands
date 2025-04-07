package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.DataCommandStorage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public record StorageNbtData(DataCommandStorage storage, Identifier value) implements NbtTarget.Single<Identifier> {
  @Override
  public Text feedbackQuery(NbtElement nbtElement) {
    return Text.translatable("commands.data.storage.query", this.value, NbtHelper.toPrettyPrintedText(nbtElement));
  }

  @Override
  public NbtCompound getNbtFor(Identifier source, @NotNull RegistryWrapper.WrapperLookup registryLookup) {
    return this.storage.get(source);
  }

  @Override
  public void setNbtFor(Identifier target, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    storage.set(target, nbt);
  }

  @Override
  public Text feedbackModify() {
    return Text.translatable("commands.data.storage.modified", this.value);
  }
}
