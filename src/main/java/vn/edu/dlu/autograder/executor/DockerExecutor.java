package vn.edu.dlu.autograder.executor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import vn.edu.dlu.autograder.model.ExecutionResult;
import vn.edu.dlu.autograder.model.TestCase;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class DockerExecutor implements IExecutor {

    private final DockerClient dockerClient;
    private final String imageName;

    public DockerExecutor() {
        this("gcc:latest");
    }

    public DockerExecutor(String imageName) {
        this.imageName = imageName;

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    @Override
    public ExecutionResult execute(Path sourceOrExePath, TestCase testCase) {
        if (sourceOrExePath == null || !Files.exists(sourceOrExePath)) {
            return new ExecutionResult(-1, "", "Lỗi Sandbox: File mã nguồn không tồn tại!", 0, false);
        }

        Path parentDir = sourceOrExePath.getParent().toAbsolutePath();
        long startTime = System.currentTimeMillis();
        long timeLimitSec = testCase.getTimeLimitSeconds() > 0 ? testCase.getTimeLimitSeconds() : 2;

        String containerId = null;
        Path inputFile = null;

        try {
            // 1. Ghi Testcase Input vào file input.txt tạm thời trong thư mục bài làm
            inputFile = parentDir.resolve("input.txt");
            String inputStr = testCase.getInput() != null 
                    ? testCase.getInput().replace("\r\n", "\n").replace("\r", "") 
                    : "";
            Files.writeString(inputFile, inputStr, StandardCharsets.UTF_8);

            // 2. Cấu hình Container
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(new Bind(parentDir.toString(), new Volume("/app")))
                    .withMemory(256 * 1024 * 1024L) // 256MB RAM
                    .withCpuQuota(50000L)           // 50% CPU
                    .withNetworkMode("none")         // Chặn Internet
                    .withAutoRemove(false);

            // 3. Chuỗi lệnh Shell:
            // - Tìm file .cpp
            // - Biên dịch thành /app/solution_exec
            // - Đọc dữ liệu từ /app/input.txt bằng dấu < truyền vào chương trình
            String shellCmd = "SRC_FILE=$(find /app -type f -name \"*.cpp\" | head -n 1) && " +
                             "if [ -z \"$SRC_FILE\" ]; then echo \"Không tìm thấy file .cpp trong /app\" >&2; exit 1; fi && " +
                             "g++ -O2 \"$SRC_FILE\" -o /app/solution_exec 2> /app/compile_dock.err && " +
                             "chmod +x /app/solution_exec && " +
                             "stdbuf -o0 -e0 /app/solution_exec < /app/input.txt";

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/app")
                    .withCmd("sh", "-c", shellCmd)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(false)
                    .exec();

            containerId = container.getId();

            // 4. Khởi chạy Container
            dockerClient.startContainerCmd(containerId).exec();

            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();

            // 5. Đính kèm Callback để bắt kết quả STDOUT & STDERR
            ResultCallback<Frame> attachCallback = dockerClient.attachContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            if (frame.getStreamType() == StreamType.STDOUT) {
                                stdoutStream.write(frame.getPayload(), 0, frame.getPayload().length);
                            } else if (frame.getStreamType() == StreamType.STDERR) {
                                stderrStream.write(frame.getPayload(), 0, frame.getPayload().length);
                            }
                        }
                    });

            // 6. Chờ thực thi
            WaitContainerResultCallback waitCallback = dockerClient.waitContainerCmd(containerId).start();
            boolean completed = waitCallback.awaitCompletion(timeLimitSec, TimeUnit.SECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            attachCallback.close();

            if (!completed) {
                forceKillContainer(containerId);
                return new ExecutionResult(-1, "", "Time Limit Exceeded (" + timeLimitSec + "s)", executionTime, true);
            }

            int exitCode = waitCallback.awaitStatusCode();
            String stdout = stdoutStream.toString(StandardCharsets.UTF_8);
            String stderr = stderrStream.toString(StandardCharsets.UTF_8);

            return new ExecutionResult(exitCode, stdout, stderr, executionTime, false);

        } catch (Exception e) {
            return new ExecutionResult(-1, "", "Lỗi Docker Sandbox: " + e.getMessage(), System.currentTimeMillis() - startTime, false);
        } finally {
            // Dọn dẹp file temp input.txt và Container
            if (inputFile != null) {
                try { Files.deleteIfExists(inputFile); } catch (Exception ignored) {}
            }
            if (containerId != null) {
                removeContainerQuietly(containerId);
            }
        }
    }

    private void forceKillContainer(String containerId) {
        try {
            dockerClient.killContainerCmd(containerId).exec();
        } catch (Exception ignored) {}
    }

    private void removeContainerQuietly(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception ignored) {}
    }
}