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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

    /**
     * Biên dịch mã nguồn C++ trong Container 1 lần duy nhất trước khi chạy chuỗi Testcases.
     */
    public boolean compile(Path sourcePath) {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            System.err.println("[DockerExecutor] File nguồn không tồn tại: " + sourcePath);
            return false;
        }

        // Lấy thư mục gốc chứa file/thư mục giải nén
        Path parentDir = Files.isRegularFile(sourcePath) ? sourcePath.getParent().toAbsolutePath() : sourcePath.toAbsolutePath(); 
        
        // Chuẩn hóa đường dẫn tương thích với Docker Mount trên Windows
        String hostPath = parentDir.toString().replace("\\", "/");
        String containerId = null;

        try {
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(new Bind(hostPath, new Volume("/app")))
                    .withAutoRemove(false);

            // Bỏ -maxdepth 1 để quét toàn bộ thư mục con sau khi un-zip
            String compileCmd = "SRC_FILE=$(find /app -type f -name \"*.cpp\" | head -n 1) && " +
                                "if [ -z \"$SRC_FILE\" ]; then echo \"ERROR: Khong tim thay file .cpp trong /app\" >&2; exit 1; fi && " +
                                "g++ -O2 \"$SRC_FILE\" -o /app/solution_exec 2>&1";

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/app")
                    .withCmd("sh", "-c", compileCmd)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            containerId = container.getId();

            ByteArrayOutputStream logStream = new ByteArrayOutputStream();
            ResultCallback<Frame> callback = dockerClient.attachContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            logStream.write(frame.getPayload(), 0, frame.getPayload().length);
                        }
                    });

            dockerClient.startContainerCmd(containerId).exec();

            WaitContainerResultCallback waitCallback = dockerClient.waitContainerCmd(containerId).start();
            waitCallback.awaitCompletion(30, TimeUnit.SECONDS);
            callback.close();

            int exitCode = waitCallback.awaitStatusCode();
            String compileLog = logStream.toString(StandardCharsets.UTF_8);

            if (exitCode != 0) {
                System.err.println("[Docker Compile Error]:\n" + compileLog);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.err.println("[Docker Compile Exception]: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (containerId != null) {
                removeContainerQuietly(containerId);
            }
        }
    }

    @Override
    public ExecutionResult execute(Path sourceOrExePath, TestCase testCase) {
        if (sourceOrExePath == null || !Files.exists(sourceOrExePath)) {
            return new ExecutionResult(-1, "", "Lỗi Sandbox: Path không tồn tại!", 0, false);
        }

        Path parentDir = sourceOrExePath.getParent().toAbsolutePath();
        
        // Tự động kiểm tra và biên dịch nếu file binary chưa tồn tại
        Path binaryPath = parentDir.resolve("solution_exec");
        if (!Files.exists(binaryPath)) {
            boolean compiled = compile(sourceOrExePath);
            if (!compiled) {
                return new ExecutionResult(-1, "", "Lỗi Biên Dịch trong Docker Sandbox", 0, false);
            }
        }

        long startTime = System.currentTimeMillis();
        long timeLimitSec = testCase.getTimeLimitSeconds() > 0 ? testCase.getTimeLimitSeconds() : 2;
        String containerId = null;

        try {
            // 1. Cấu hình Container cách ly
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(new Bind(parentDir.toString(), new Volume("/app")))
                    .withMemory(256 * 1024 * 1024L) // 256MB RAM
                    .withCpuQuota(50000L)           // 50% CPU
                    .withNetworkMode("none")         // Chặn Internet
                    .withAutoRemove(false);

            // 2. Chạy binary trực tiếp với stdbuf (không biên dịch lại, không dùng file input.txt)
            String shellCmd = "stdbuf -o0 -e0 /app/solution_exec";

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/app")
                    .withCmd("sh", "-c", shellCmd)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withStdInOnce(true)
                    .withTty(false)
                    .exec();

            containerId = container.getId();

            // Prepare Input Data từ TestCase
            String inputStr = testCase.getInput() != null 
                    ? testCase.getInput().replace("\r\n", "\n").replace("\r", "") 
                    : "";
            InputStream stdinStream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));

            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();

            // 3. Khởi chạy và Attach STDIN / STDOUT / STDERR
            ResultCallback<Frame> attachCallback = dockerClient.attachContainerCmd(containerId)
                    .withStdIn(stdinStream)
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

            dockerClient.startContainerCmd(containerId).exec();

            // 4. Chờ thực thi có Timeout
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