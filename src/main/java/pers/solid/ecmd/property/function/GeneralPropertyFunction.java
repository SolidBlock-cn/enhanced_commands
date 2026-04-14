package pers.solid.ecmd.property.function;

import com.google.common.collect.Collections2;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public interface GeneralPropertyFunction extends PropertyFunction<Integer> {
  static void updateExcepts(Collection<? extends PropertyFunction<?>> propertyFunctions) {
    for (PropertyFunction<?> propertyFunction : propertyFunctions) {
      if (propertyFunction instanceof GeneralPropertyFunction generalPropertyFunction) {
        generalPropertyFunction.except().clear();
        generalPropertyFunction.except().addAll(Collections2.filter(Collections2.transform(propertyFunctions, PropertyFunction::property), Objects::nonNull));
      }
    }
  }

  Set<Property<?>> except();

  interface OfName extends PropertyNameFunction {
    static void updateExcepts(Collection<? extends PropertyNameFunction> propertyNameFunctions) {
      for (PropertyNameFunction propertyNameFunction : propertyNameFunctions) {
        if (propertyNameFunction instanceof OfName ofName) {
          ofName.except().clear();
          ofName.except().addAll(Collections2.filter(Collections2.transform(propertyNameFunctions, PropertyNameFunction::propertyName), Objects::nonNull));
        }
      }
    }

    Collection<String> except();
  }
}
