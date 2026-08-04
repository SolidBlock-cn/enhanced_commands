package pers.solid.ecmd.util.pack;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.nbt.function.PositionalListEntry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface RequiresValidation {
  default void validate(ValidationContext context) {
    for (@Nullable Object member : membersToValidate()) {
      if (member instanceof RequiresValidation r) {
        r.validate(context);
      } else if (member instanceof Holder.Reference<?> ref) {
        ReferenceEntry.of(ref).validate(context);
      } else if (member instanceof Collection<?>
          || member instanceof String
          || member instanceof Number
          || member instanceof Boolean
          || member instanceof Enum<?>
          || member instanceof Map<?, ?>
          || member instanceof Optional<?>
          || member instanceof Pair<?, ?>
          || member instanceof PositionalListEntry<?>) {
        throw new IllegalStateException(String.format("Cannot validate %s! Invalid type of member to validate: %s", context.getName().asString(), member.getClass().getName()));
      }
    }
  }

  Iterable<? extends @Nullable Object> membersToValidate();
}
