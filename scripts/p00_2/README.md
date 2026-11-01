# P00.2 — Offline functional baseline probes

These non-JUnit Java probes run **the current production source unchanged** against disposable, self-generated C++ and ZIP fixtures. The runner enforces a wall-clock watchdog on each probe; Java/C++ test programs are benign synthetic fixtures, but the old native executor is **not a sandbox**. Run only in an isolated test environment, never on arbitrary student submissions.

With Maven and a working Java 21 toolchain, build the shaded CLI JAR in the project first (`mvn -DskipTests package`). Then run:

```powershell
python scripts/p00_2/run_audit.py --runtime-jar target/autograder-1.0.0-cli.jar --output-dir p00-2-results
```

The runner uses the JAR **only as a dependency classpath**, compiles fresh `src/main/java` source files into a temporary directory and puts those fresh classes first on the classpath. The runner does not modify production Java, `pom.xml`, or Git branches. It prints PASS, FAIL, OBSERVED, or BLOCKED with a machine-readable JSON and Markdown report. `--skip-slow` omits the ~30-second compiler timeout probe. A nonzero exit code means at least one expectation failed or required probe was blocked; this is useful for documenting baseline defects, not evidence that P00.2 was skipped.

For an independent Maven verification on your Windows machine, run `mvn clean verify` and include its exit status in `docs/baseline/P00.2-functional-audit.md`. The standalone probes are supplementary; **they do not replace JUnit tests or Maven**.
