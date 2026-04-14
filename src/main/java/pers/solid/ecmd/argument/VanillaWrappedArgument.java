package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import pers.solid.ecmd.mixins.accessor.ArgumentTypeInfosAccessor;
import pers.solid.ecmd.util.mixin.ArgumentTypeExtension;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 将一个原版的参数类型套用，并设置 {@link ArgumentTypeExtension#enhanced_setExtension(boolean)} 以单独避免其受到本模组的影响。
 *
 * @param <F> 需要导向至的参数类型
 */
public record VanillaWrappedArgument<T, F extends ArgumentType<T>>(F forward) implements ArgumentType<T> {
  public VanillaWrappedArgument {
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

  public static class Info<T, F extends ArgumentType<T>, FP extends ArgumentTypeInfo.Template<F>> implements ArgumentTypeInfo<VanillaWrappedArgument<T, F>, Template<T, F, FP>> {
    @SuppressWarnings("rawtypes")
    public static final Info<?, ?, ?> INSTANCE = new Info();

    @SuppressWarnings("unchecked")
    @Override
    public void serializeToNetwork(VanillaWrappedArgument.Template<T, F, FP> template, FriendlyByteBuf buf) {
      final ArgumentTypeInfo<F, FP> forwardSerializer = (ArgumentTypeInfo<F, FP>) template.forwardProperties.type();
      buf.writeResourceLocation(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(forwardSerializer));
      forwardSerializer.serializeToNetwork(template.forwardProperties, buf);
    }

    @SuppressWarnings("unchecked")
    @Override
    public VanillaWrappedArgument.Template<T, F, FP> deserializeFromNetwork(FriendlyByteBuf buf) {
      final ArgumentTypeInfo<F, FP> forwardSerializer = (ArgumentTypeInfo<F, FP>) BuiltInRegistries.COMMAND_ARGUMENT_TYPE.get(buf.readResourceLocation());
      final FP forwardProperties = forwardSerializer.deserializeFromNetwork(buf);
      return new VanillaWrappedArgument.Template<>(forwardProperties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serializeToJson(VanillaWrappedArgument.Template<T, F, FP> template, JsonObject json) {
      final ArgumentTypeInfo<F, FP> forwardSerializer = (ArgumentTypeInfo<F, FP>) template.forwardProperties.type();
      json.addProperty("type", BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(forwardSerializer).toString());
      final JsonObject forward = new JsonObject();
      forwardSerializer.serializeToJson(template.forwardProperties, forward);
      json.add("entityPredicate", forward);
    }

    @SuppressWarnings("unchecked")
    @Override
    public VanillaWrappedArgument.Template<T, F, FP> unpack(VanillaWrappedArgument<T, F> argumentType) {
      final ArgumentTypeInfo<F, FP> forwardSerializer = (ArgumentTypeInfo<F, FP>) ArgumentTypeInfosAccessor.getBY_CLASS().get(argumentType.forward.getClass());
      return new VanillaWrappedArgument.Template<>(forwardSerializer.unpack(argumentType.forward));
    }
  }

  public record Template<T, F extends ArgumentType<T>, FP extends ArgumentTypeInfo.Template<F>>(FP forwardProperties) implements ArgumentTypeInfo.Template<VanillaWrappedArgument<T, F>> {

    @Override
    public VanillaWrappedArgument<T, F> instantiate(CommandBuildContext commandBuildContext) {
      return new VanillaWrappedArgument<>(forwardProperties.instantiate(commandBuildContext));
    }

    @SuppressWarnings("unchecked")
    @Override
    public VanillaWrappedArgument.Info<T, F, FP> type() {
      return (Info<T, F, FP>) Info.INSTANCE;
    }
  }
}
