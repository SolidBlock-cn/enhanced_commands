package pers.solid.ecmd.mixins.general;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.argument.ItemFunctionArgument;
import pers.solid.ecmd.util.mixin.ArgumentTypeExtension;
import pers.solid.ecmd.util.mixin.EnhancedItemInput;

import java.util.concurrent.CompletableFuture;

@Mixin(ItemArgument.class)
public abstract class ItemArgumentMixin implements ArgumentTypeExtension {
  @Unique
  private @Nullable ItemFunctionArgument enhanced_commands$modArgumentType;
  @Unique
  private boolean enhanced_commands$extension = true;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void injectedInit(CommandBuildContext context, CallbackInfo ci) {
    this.enhanced_commands$modArgumentType = new ItemFunctionArgument(context);
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/item/ItemInput;", at = @At("HEAD"), cancellable = true)
  private void injectedParse(StringReader reader, CallbackInfoReturnable<ItemInput> cir) throws CommandSyntaxException {
    if (enhanced_commands$modArgumentType != null && enhanced_commands$extension) {
      cir.setReturnValue(new EnhancedItemInput(enhanced_commands$modArgumentType.parse(reader)));
    }
  }

  @Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
  private <S> void injectedListSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    if (enhanced_commands$modArgumentType != null && enhanced_commands$extension) {
      cir.setReturnValue(enhanced_commands$modArgumentType.listSuggestions(commandContext, suggestionsBuilder));
    }
  }

  @Inject(method = "getItem", at = @At("RETURN"))
  private static void injectedGetBlockState(CommandContext<CommandSourceStack> context, String name, CallbackInfoReturnable<ItemInput> cir) {
    final ItemInput returnValue = cir.getReturnValue();
    if (returnValue instanceof EnhancedItemInput enhancedItemInput) {
      enhancedItemInput.setSource(context.getSource());
    }
  }

  @Override
  public boolean enhanced_hasExtension() {
    return enhanced_commands$extension;
  }

  @Override
  public void enhanced_setExtension(boolean extension) {
    this.enhanced_commands$extension = extension;
  }
}
