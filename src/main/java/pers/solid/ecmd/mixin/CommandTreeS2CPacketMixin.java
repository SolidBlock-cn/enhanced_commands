package pers.solid.ecmd.mixin;

import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.EnhancedCommands;

@Mixin(CommandTreeS2CPacket.class)
public class CommandTreeS2CPacketMixin {
  @Inject(method = "readArgumentBuilder", at = @At("HEAD"))
  private static void readArgumentBuilderM(PacketByteBuf buf, byte flags, CallbackInfoReturnable cir) {
    int i = flags & 3;
    if (i == 2) {
      final int i1 = buf.readerIndex();
      String string = buf.readString();
      int j = buf.readVarInt();
      ArgumentSerializer<?, ?> argumentSerializer = Registries.COMMAND_ARGUMENT_TYPE.get(j);
      if (argumentSerializer == null) {
        EnhancedCommands.LOGGER.warn("Unknown argument serializer id {} max {}", j, Registries.COMMAND_ARGUMENT_TYPE.size());
      } else {
        EnhancedCommands.LOGGER.info("reading properties for argument node {} of type {}", string, argumentSerializer.getClass().getName());
      }
      buf.readerIndex(i1);
    } else if (i == 1) {
      final int i1 = buf.readerIndex();
      final String literal = buf.readString();
      EnhancedCommands.LOGGER.info("reading literal node {}", literal);
      buf.readerIndex(i1);
    }
  }
}
