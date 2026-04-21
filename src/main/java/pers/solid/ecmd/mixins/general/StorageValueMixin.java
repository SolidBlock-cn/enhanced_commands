package pers.solid.ecmd.mixins.general;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.StorageValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.extension.NumberProviderExtension;

import java.util.List;
import java.util.Optional;

@Mixin(StorageValue.class)
public abstract class StorageValueMixin implements NumberProviderExtension {
  @Shadow
  @Final
  private ResourceLocation storage;

  @Shadow
  @Final
  private NbtPathArgument.NbtPath path;

  /**
   * @see StorageValue#getNumericTag(LootContext)
   */
  @Unique
  private Optional<NumericTag> getNumericTag(ExecutionContext context) {
    final MinecraftServer server = context.positionProvider.getWorld$ec().getServer();
    if (server == null) {
      return Optional.empty();
    }
    CompoundTag compoundTag = server.getCommandStorage().get(storage);

    try {
      List<Tag> list = path.get(compoundTag);
      if (list.size() == 1) {
        Object var5 = list.get(0);
        if (var5 instanceof NumericTag numericTag) {
          return Optional.of(numericTag);
        }
      }
    } catch (CommandSyntaxException ignored) {
    }

    return Optional.empty();
  }

  public float getFloat(ExecutionContext executionContext) {
    return this.getNumericTag(executionContext).map(NumericTag::getAsFloat).orElse(0.0F);
  }

  public int getInt(ExecutionContext executionContext) {
    return this.getNumericTag(executionContext).map(NumericTag::getAsInt).orElse(0);
  }

  @Override
  public String asString$enhancedCommands() {
    return "storage(" + storage + ", " + path.asString() + ")";
  }
}
