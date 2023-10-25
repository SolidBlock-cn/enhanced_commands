package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Direction;

public class AxisArgumentType extends SimpleEnumArgumentTypes.StringIdentifiableArgumentType<AxisArgument> implements ArgumentSerializer.ArgumentTypeProperties<AxisArgumentType> {
  private final boolean excludeRandom;

  public AxisArgumentType(boolean excludeRandom) {
    super(excludeRandom ? AxisArgument.VALUES_EXCEPT_RANDOM : AxisArgument.VALUES, AxisArgument.CODEC, AxisArgument::getDisplayName);
    this.excludeRandom = excludeRandom;
  }

  public static AxisArgumentType axis(boolean excludeRandom) {
    return new AxisArgumentType(excludeRandom);
  }

  public static Direction.Axis getAxis(CommandContext<ServerCommandSource> context, String name) {
    return context.getArgument(name, AxisArgument.class).apply(context.getSource());
  }

  @Override
  public AxisArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
    return this;
  }

  @Override
  public ArgumentSerializer<AxisArgumentType, ?> getSerializer() {
    return new Serializer();
  }

  public static class Serializer implements ArgumentSerializer<AxisArgumentType, AxisArgumentType> {
    @Override
    public void writePacket(AxisArgumentType properties, PacketByteBuf buf) {
      buf.writeBoolean(properties.excludeRandom);
    }

    @Override
    public AxisArgumentType fromPacket(PacketByteBuf buf) {
      return new AxisArgumentType(buf.readBoolean());
    }

    @Override
    public void writeJson(AxisArgumentType properties, JsonObject json) {
      json.addProperty("excludeRandom", properties.excludeRandom);
    }

    @Override
    public AxisArgumentType getArgumentTypeProperties(AxisArgumentType argumentType) {
      return argumentType;
    }
  }
}
