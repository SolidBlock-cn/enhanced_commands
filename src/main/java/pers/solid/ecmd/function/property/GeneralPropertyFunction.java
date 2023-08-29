package pers.solid.ecmd.function.property;

import com.google.common.collect.Collections2;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

public interface GeneralPropertyFunction extends PropertyFunction<Integer> {
  @NotNull Collection<Property<?>> except();

  static void updateExcepts(Collection<? extends PropertyFunction<?>> propertyFunctions) {
    for (PropertyFunction<?> propertyFunction : propertyFunctions) {
      if (propertyFunction instanceof GeneralPropertyFunction generalPropertyFunction) {
        generalPropertyFunction.except().clear();
        generalPropertyFunction.except().addAll(Collections2.filter(Collections2.transform(propertyFunctions, PropertyFunction::property), Objects::nonNull));
      }
    }
  }

  interface OfName extends PropertyNameFunction {
    @NotNull Collection<String> except();

    static void updateExcepts(Collection<? extends PropertyNameFunction> propertyNameFunctions) {
      for (PropertyNameFunction propertyNameFunction : propertyNameFunctions) {
        if (propertyNameFunction instanceof OfName ofName) {
          ofName.except().clear();
          ofName.except().addAll(Collections2.filter(Collections2.transform(propertyNameFunctions, PropertyNameFunction::propertyName), Objects::nonNull));
        }
      }
    }
  }
}
