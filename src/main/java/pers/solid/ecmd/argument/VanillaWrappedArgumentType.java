package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.mixin.command.ArgumentTypesAccessor;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import pers.solid.ecmd.util.mixin.ArgumentTypeExtension;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 将一个原版的参数类型套用，并设置 {@link ArgumentTypeExtension#enhanced_setExtension(boolean)} 以单独避免其受到本模组的影响。
 *
 * @param <F> 需要导向至的参数类型
 */
public record VanillaWrappedArgumentType<T, F extends ArgumentType<T>>(F forward) implements ArgumentType<T> {
  public VanillaWrappedArgumentType {
    if (forward instanceof ArgumentTypeExtension argumentTypeExtension) {
      argumentTypeExtension.enhanced_setExtension(false);
    } else {
      throw new IllegalArgumentException("ArgumentType is " + forward.getClass() + ", which is not instance of ArgumentExtension interface!");
    }
  }

  @Override
  public T parse(StringReader reader) throws CommandSyntaxException {
    return forward.parse(reader);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return forward.listSuggestions(context, builder);
  }

  @Override
  public Collection<String> getExamples() {
    return forward.getExamples();
  }

  public static class Serializer<T, F extends ArgumentType<T>, FP extends ArgumentSerializer.ArgumentTypeProperties<F>> implements ArgumentSerializer<VanillaWrappedArgumentType<T, F>, Properties<T, F, FP>> {
    @SuppressWarnings("rawtypes")
    public static final Serializer<?, ?, ?> INSTANCE = new Serializer();

    @SuppressWarnings("unchecked")
    @Override
    public void writePacket(Properties<T, F, FP> properties, PacketByteBuf buf) {
      final ArgumentSerializer<F, FP> forwardSerializer = (ArgumentSerializer<F, FP>) properties.forwardProperties.getSerializer();
      buf.writeIdentifier(Registries.COMMAND_ARGUMENT_TYPE.getId(forwardSerializer));
      forwardSerializer.writePacket(properties.forwardProperties, buf);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Properties<T, F, FP> fromPacket(PacketByteBuf buf) {
      final ArgumentSerializer<F, FP> forwardSerializer = (ArgumentSerializer<F, FP>) Registries.COMMAND_ARGUMENT_TYPE.get(buf.readIdentifier());
      final FP forwardProperties = forwardSerializer.fromPacket(buf);
      return new Properties<>(forwardProperties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeJson(Properties<T, F, FP> properties, JsonObject json) {
      final ArgumentSerializer<F, FP> forwardSerializer = (ArgumentSerializer<F, FP>) properties.forwardProperties.getSerializer();
      json.addProperty("type", Registries.COMMAND_ARGUMENT_TYPE.getId(forwardSerializer).toString());
      final JsonObject forward = new JsonObject();
      forwardSerializer.writeJson(properties.forwardProperties, forward);
      json.add("forward", forward);
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    @Override
    public Properties<T, F, FP> getArgumentTypeProperties(VanillaWrappedArgumentType<T, F> argumentType) {
      final ArgumentSerializer<F, FP> forwardSerializer = (ArgumentSerializer<F, FP>) ArgumentTypesAccessor.fabric_getClassMap().get(argumentType.forward.getClass());
      return new Properties<>(forwardSerializer.getArgumentTypeProperties(argumentType.forward));
    }
  }

  public record Properties<T, F extends ArgumentType<T>, FP extends ArgumentSerializer.ArgumentTypeProperties<F>>(FP forwardProperties) implements ArgumentSerializer.ArgumentTypeProperties<VanillaWrappedArgumentType<T, F>> {

    @Override
    public VanillaWrappedArgumentType<T, F> createType(CommandRegistryAccess commandRegistryAccess) {
      return new VanillaWrappedArgumentType<>(forwardProperties.createType(commandRegistryAccess));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Serializer<T, F, FP> getSerializer() {
      return (Serializer<T, F, FP>) Serializer.INSTANCE;
    }
  }
}
