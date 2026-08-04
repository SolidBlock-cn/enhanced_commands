package pers.solid.ecmd.util.pack;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.pack.names.ReferencingValidationName;
import pers.solid.ecmd.util.pack.names.ResourceKeyValidationName;
import pers.solid.ecmd.util.pack.names.ValidationName;
import pers.solid.ecmd.util.pack.problems.ValidationProblem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ValidationContext {
  private final HolderGetter.Provider resolver;
  private final @Unmodifiable Set<ResourceKey<?>> referencedElements;
  private final ValidationName name;
  private @Nullable List<ValidationProblem> problems;

  protected ValidationContext(ValidationName name, HolderGetter.Provider resolver, Set<ResourceKey<?>> referencedElements, @Nullable List<ValidationProblem> problems) {
    this.name = name;
    this.resolver = resolver;
    this.referencedElements = referencedElements;
    this.problems = problems;
  }

  public ValidationContext(ValidationName name, HolderGetter.Provider resolver) {
    this.name = name;
    this.resolver = resolver;
    this.referencedElements = Collections.emptySet();
  }

  public ValidationName getName() {
    return name;
  }

  public HolderGetter.Provider resolver() {
    return resolver;
  }

  public boolean isElementReferenced(ResourceKey<?> element) {
    return referencedElements.contains(element);
  }

  public ValidationContext forResource(ResourceKey<?> key) {
    return new ValidationContext(new ResourceKeyValidationName<>(key), resolver, ImmutableSet.<ResourceKey<?>>builder().addAll(referencedElements).add(key).build(), problems);
  }

  public ValidationContext forReferencedElement(ResourceKey<?> key) {
    return new ValidationContext(new ReferencingValidationName<>(this.name, key), resolver, ImmutableSet.<ResourceKey<?>>builder().addAll(referencedElements).add(key).build(), problems);
  }

  public void recordProblem(ValidationProblem problem) {
    if (problems == null) {
      problems = new ArrayList<>();
    }
    EnhancedCommands.LOGGER.warn("Enhanced Commands: Datapack content {} has problems: {}", name.asString(), problem.message().getString());
    problems.add(problem);
  }

  public @UnmodifiableView List<ValidationProblem> problems() {
    return problems == null ? Collections.emptyList() : Collections.unmodifiableList(problems);
  }

  public boolean hasProblems() {
    return problems != null;
  }
}
