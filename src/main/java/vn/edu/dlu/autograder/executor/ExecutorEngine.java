package vn.edu.dlu.autograder.executor;

import vn.edu.dlu.autograder.model.ExecutionResult;
import vn.edu.dlu.autograder.model.TestCase;
import java.nio.file.Path;

public class ExecutorEngine implements IExecutor {
    private final IExecutor delegate;

    // Constructor mặc định: Chạy Local cho nhanh & an toàn
    public ExecutorEngine() {
        this(false);
    }

    // Nếu passes true -> Chạy Docker, false -> Chạy Local
    public ExecutorEngine(boolean useDocker) {
        if (useDocker) {
            this.delegate = new DockerExecutor("gcc:latest");
        } else {
            this.delegate = new LocalExecutor();
        }
    }

    @Override
    public ExecutionResult execute(Path exePath, TestCase testCase) {
        return delegate.execute(exePath, testCase);
    }
}