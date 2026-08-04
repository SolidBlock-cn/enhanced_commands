package pers.solid.ecmd.util.pack.problems;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public record MultipleValidationProblems(List<ValidationProblem> problems) implements ValidationProblem {
  @Override
  public Component message() {
    final MutableComponent arg = Component.empty().append("[");
    for (int i = 0; i < problems.size(); i++) {
      final ValidationProblem problem = problems.get(i);
      arg.append(i + ". ").append(problem.message());
    }
    arg.append("]");
    return Component.translatable("enhanced_commands.registry.validation.multiple", arg);
  }
}
