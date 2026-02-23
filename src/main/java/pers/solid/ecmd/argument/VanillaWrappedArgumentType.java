package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.mixin.command.ArgumentTypesAccessor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
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

  public static class Serializer<T, F extends ArgumentType<T>, FP extends ArgumentTypeInfo.Template<F>> implements ArgumentTypeInfo<VanillaWrappedArgumentType<T, F>, Properties<T, F, FP>> {
    @SuppressWarnings("rawtypes")
    public static final Serializer<?, ?, ?> INSTANCE = new Serializer();

    @SuppressWarnings("unchecked")
    @Override
    public void serializeToNetwork(Properties<T, F, FP> properties, FriendlyByteBuf buf) {
      final ArgumentTypeInfo<F, FP> forwardSerializer = (ArgumentTypeInfo<F, FP>) properties.forwardProperties.type();
      buf.writeResourceLocation(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(forwardSerializer));
      forwardSerializer.serializeToNetwork(properties.forwardProperties, buf);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Properties<T, F, FP> deserializeFromNetwork(FriendlyByteBuf buf) {
      final ArgumentTypeInfo<F, FP> forwardSerializer = (ArgumentTypeInfo<F, FP>) BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getValue(buf.readResourceLocation());
      final FP forwardProperties = forwardSerializer.deserializeFromNetwork(buf);
      return new Properties<>(forwardProperties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serializeToJson(Properties<T, F, FP> properties, JsonObject json) {
      final ArgumentTypeInfo<F, FP> forwardSerializer = (ArgumentTypeInfo<F, FP>) properties.forwardProperties.type();
      json.addProperty("type", BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(forwardSerializer).toString());
      final JsonObject forward = new JsonObject();
      forwardSerializer.serializeToJson(properties.forwardProperties, forward);
      json.add("entityPredicate", forward);
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    @Override
    public @NotNull Properties<T, F, FP> unpack(VanillaWrappedArgumentType<T, F> argumentType) {
      final ArgumentTypeInfo<F, FP> forwardSerializer = (ArgumentTypeInfo<F, FP>) ArgumentTypesAccessor.fabric_getClassMap().get(argumentType.forward.getClass());
      return new Properties<>(forwardSerializer.unpack(argumentType.forward));
    }
  }

  public record Properties<T, F extends ArgumentType<T>, FP extends ArgumentTypeInfo.Template<F>>(FP forwardProperties) implements ArgumentTypeInfo.Template<VanillaWrappedArgumentType<T, F>> {

    @Override
    public @NotNull VanillaWrappedArgumentType<T, F> instantiate(CommandBuildContext commandBuildContext) {
      return new VanillaWrappedArgumentType<>(forwardProperties.instantiate(commandBuildContext));
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Serializer<T, F, FP> type() {
      return (Serializer<T, F, FP>) Serializer.INSTANCE;
    }
  }
}
