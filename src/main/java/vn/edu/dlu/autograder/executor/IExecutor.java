package vn.edu.dlu.autograder.executor;

import vn.edu.dlu.autograder.model.ExecutionResult;
import vn.edu.dlu.autograder.model.TestCase;
import java.nio.file.Path;

public interface IExecutor {
    ExecutionResult execute(Path exePath, TestCase testCase);
}