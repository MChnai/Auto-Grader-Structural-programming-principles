# P00.2 — Offline functional baseline probes

These non-JUnit Java probes run **the current production source unchanged** against disposable, self-generated C++ and ZIP fixtures. The runner enforces a wall-clock watchdog on each probe; Java/C++ test programs are benign synthetic fixtures, but the old native executor is **not a sandbox**. Run only in an isolated test environment, never on arbitrary student submissions.

With Maven and a working Java 21 toolchain, build the shaded CLI JAR in the project first (`mvn -DskipTests package`). Then run:

```powershell
python scripts/p00_2/run_audit.py --runtime-jar target/autograder-1.0.0-cli.jar --output-dir p00-2-results
```

The runner uses the JAR **only as a dependency classpath**, compiles fresh `src/main/java` source files into a temporary directory and puts those fresh classes first on the classpath. The runner does not modify production Java, `pom.xml`, or Git branches. It prints PASS, FAIL, OBSERVED, or BLOCKED with a machine-readable JSON and Markdown report. `--skip-slow` omits the ~30-second compiler timeout probe. A nonzero exit code means at least one expectation failed or required probe was blocked; this is useful for documenting baseline defects, not evidence that P00.2 was skipped.

For an independent Maven verification on your Windows machine, run `mvn clean verify` and include its exit status in `docs/baseline/P00.2-functional-audit.md`. The standalone probes are supplementary; **they do not replace JUnit tests or Maven**.

## P00.2 Windows audit correction (audit-only)

The first Windows run can incorrectly classify `native_normal` as a failure because Windows C++ console output commonly uses `\r\n`, while the original probe required `\n` exactly. The corrected probe normalizes CRLF only for the cross-platform native-execution assertion. It does not relax `OutputChecker` tests.

`native_input_big` now uses a consumer that reads all 2,000,000 bytes of input. A distinct `native_input_early_exit` scenario verifies what happens when the child exits before consuming input; the original executor can return an I/O system error instead of a successful execution result. These checks deliberately remain separate.

Compiler timeout simulations now build temporary fake `g++` executables using the installed real compiler. The Linux-only shell dependency was removed, so these scenarios can run on native Windows with a working C++ compiler. Their behavior on your machine must still be confirmed by rerunning the audit.

To rerun only the relevant probes in PowerShell:

```powershell
python scripts/p00_2/run_audit.py `
  --runtime-jar target/autograder-1.0.0-cli.jar `
  --output-dir p00-2-results-windows-corrected `
  --case native_normal `
  --case native_input_big `
  --case native_input_early_exit `
  --case compile_timeout `
  --case compile_stderr_flood
```

Remember that the audit returns a nonzero process exit code if one or more **intentional baseline defects** remain. This is expected and does not mean the script failed to record results.
