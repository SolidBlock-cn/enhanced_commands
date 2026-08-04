package pers.solid.ecmd.util.pack.problems;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface ValidationProblem {
  Component message();

  static ValidationProblem concentrateMultiple(List<ValidationProblem> problems) {
    if (problems.size() == 1) {
      return problems.get(0);
    } else {
      return new MultipleValidationProblems(problems);
    }
  }
}
