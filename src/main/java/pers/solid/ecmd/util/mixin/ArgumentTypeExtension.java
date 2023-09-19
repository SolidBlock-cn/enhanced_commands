package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.arguments.ArgumentType;

public interface ArgumentTypeExtension {
  boolean enhanced_hasExtension();

  void enhanced_setExtension(boolean extensionEnabled);

  static <A extends ArgumentType<?>> A ofVanilla(A argumentType) {
    if (argumentType instanceof ArgumentTypeExtension argumentTypeExtension) {
      argumentTypeExtension.enhanced_setExtension(false);
    }
    return argumentType;
  }
}
