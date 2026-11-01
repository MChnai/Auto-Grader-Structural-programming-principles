// Standalone baseline probe: no JUnit dependency, intentionally does not change production code.
// Run ONLY with self-generated fixtures in a disposable workspace; this baseline invokes native executables.
import vn.edu.dlu.autograder.archive.ZipExtractor;
import vn.edu.dlu.autograder.executor.CompilerEngine;
import vn.edu.dlu.autograder.executor.NativeProcessExecutor;
import vn.edu.dlu.autograder.analyzer.CppCodeAnalyzer;
import vn.edu.dlu.autograder.analyzer.WinnowingEngine;
import vn.edu.dlu.autograder.checker.OutputChecker;
import vn.edu.dlu.autograder.service.SubmissionGrader;
import vn.edu.dlu.autograder.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class P002Probe {
    private static final String S = "#include <iostream>\nint main(){std::cout << 30 << '\\n';return 0;}\n";
    private static Path root;
    private static void require(boolean ok, String detail) { if (!ok) throw new AssertionError(detail); }
    private static void log(String data) { System.out.println("DETAIL " + data.replace('\n',' ').replace('\r',' ').replace('\t',' ')); }
    private static Path file(String name, String data) throws IOException {
        Path p=root.resolve(name); Files.createDirectories(p.getParent()); Files.writeString(p,data); return p;
    }
    private static Path zip(String name, Map<String, byte[]> entries) throws IOException {
        Path p=root.resolve(name); Files.createDirectories(p.getParent());
        try (ZipOutputStream os = new ZipOutputStream(Files.newOutputStream(p))) {
            for(var e:entries.entrySet()) { os.putNextEntry(new ZipEntry(e.getKey())); os.write(e.getValue()); os.closeEntry(); }
        }
        return p;
    }
    private static Map<String,byte[]> entry(String path,String text) { return Map.of(path,text.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private static void zipCase(String which) throws Exception {
        Path z, dest=root.resolve("unpacked"); ZipExtractor.ExtractResult r;
        switch(which) {
            case "zip_valid" -> { z=zip("valid.zip",entry("main.cpp",S)); r=ZipExtractor.extract(z,dest); require(r.isSuccess(),r.getMessage()); require(Files.exists(dest.resolve("main.cpp")),"missing main.cpp"); }
            case "zip_nested" -> { z=zip("nested.zip",entry("project/src/main.cpp",S)); r=ZipExtractor.extract(z,dest); require(r.isSuccess(),r.getMessage()); require(r.getSourceDir().endsWith(Path.of("project","src")),"sourceDir="+r.getSourceDir()); }
            case "zip_slip" -> { z=zip("slip.zip",entry("../escaped.cpp",S)); r=ZipExtractor.extract(z,dest); require(!r.isSuccess(),"Zip Slip accepted"); require(!Files.exists(root.resolve("escaped.cpp")),"escaped file created"); }
            case "zip_large" -> { byte[] bytes=new byte[6*1024*1024]; Arrays.fill(bytes,(byte)'A'); z=zip("big.zip",Map.of("main.cpp",bytes)); r=ZipExtractor.extract(z,dest); require(!r.isSuccess(),"oversized entry accepted without a size quota; extractedBytes="+Files.size(dest.resolve("main.cpp"))); }
            case "zip_no_cpp" -> { z=zip("no_cpp.zip",entry("README.txt","only text")); r=ZipExtractor.extract(z,dest); require(r.isSuccess() && dest.equals(r.getSourceDir()),"unexpected no-CPP fallback behavior"); }
            default -> throw new IllegalArgumentException(which);
        }
        log(which+" OK");
    }
    private static void compileCase(String which) throws Exception {
        Path in=root.resolve("src"),out=root.resolve("out"); Files.createDirectories(in);
        switch(which) {
            case "compile_single" -> file("src/main.cpp",S);
            case "compile_multi" -> { file("src/main.cpp","#include <iostream>\nint answer();int main(){std::cout<<answer()<<'\\n';}\n"); file("src/lib.cpp","int answer(){return 30;}\n"); }
            case "compile_error" -> file("src/main.cpp","int main( { syntax error");
            case "compile_timeout", "compile_stderr_flood" -> file("src/main.cpp",S);
            case "compile_missing" -> { var r = new CompilerEngine().compile(root.resolve("absent"),out); require(!r.isSuccess(),"missing source accepted"); log(r.getCompilerMessage()); return; }
            default -> throw new IllegalArgumentException(which);
        }
        CompilerEngine.CompileResult r=new CompilerEngine().compile(in,out);
        if (which.equals("compile_error")) { require(!r.isSuccess(),"malformed C++ compiled"); require(r.getCompilerMessage().contains("error:"),"compiler diagnostic missing"); }
        else if (which.equals("compile_timeout")) { require(!r.isSuccess() && r.getCompilerMessage().contains("Timeout"),"compiler timeout not reported: "+r.getCompilerMessage()); }
        else if (which.equals("compile_stderr_flood")) { require(!r.isSuccess(),"flood compiler unexpectedly succeeds"); }
        else { require(r.isSuccess(),"compile error: "+r.getCompilerMessage()); require(Files.exists(r.getExecutablePath()),"binary missing"); }
        log("success="+r.isSuccess()+" message="+r.getCompilerMessage().substring(0,Math.min(180,r.getCompilerMessage().length())));
    }
    private static void nativeCase(String which) throws Exception {
        Path in=root.resolve("src"),out=root.resolve("out"); Files.createDirectories(in);
        String code=switch(which) {
            case "native_normal" -> "#include <iostream>\nint main(){int a,b;std::cin>>a>>b;std::cout<<(a+b)<<'\\n';}\n";
            case "native_timeout" -> "int main(){for(;;){} }\n";
            case "native_stdout_big" -> "#include <iostream>\nint main(){for(int i=0;i<600000;i++)std::cout<<'A';}\n";
            case "native_stderr_big" -> "#include <iostream>\nint main(){for(int i=0;i<600000;i++)std::cerr<<'B';}\n";
            case "native_input_big" -> "#include <iostream>\nint main(){std::cin.sync_with_stdio(false); long long count=0;char c;while(std::cin.get(c))++count;std::cout<<count;}\n";
            case "native_input_early_exit" -> "#include <iostream>\nint main(){std::cout<<1;}\n";
            case "native_empty_input_eof" -> "#include <iostream>\nint main(){char c;while(std::cin.get(c)){} std::cout<<'E';}\n";
            default -> throw new IllegalArgumentException(which);
        };
        file("src/main.cpp",code);
        CompilerEngine.CompileResult c = new CompilerEngine().compile(in,out);
        require(c.isSuccess(),"fixture compilation failed: "+c.getCompilerMessage());
        String input=switch(which){case "native_normal" -> "10 20\n";case "native_input_big", "native_input_early_exit" -> "x".repeat(2_000_000);default -> "";};
        ExecutionResult r=NativeProcessExecutor.runWithTimeout(c.getExecutablePath(),input,1);
        if (which.equals("native_timeout")) require(r.isTimedOut(),"timeout not detected");
        else { require(!r.isTimedOut(),"unexpected timeout: "+r.getStderr()); require(r.getExitCode()==0,"exit="+r.getExitCode()+" stderr="+r.getStderr()); }
        if (which.equals("native_normal")) require(r.getStdout().replace("\r\n", "\n").equals("30\n"), "wrong stdout (escaped): " + r.getStdout().replace("\r", "\\r").replace("\n", "\\n"));
        if (which.equals("native_input_big")) require(r.getStdout().equals("2000000"), "wrong input byte count: " + r.getStdout());
        if (which.equals("native_input_early_exit")) require(r.getStdout().equals("1"), "unexpected early exit result: " + r.getStdout());
        if (which.equals("native_stdout_big")) require(r.getStdout().length()==600000,"truncated stdout="+r.getStdout().length());
        if (which.equals("native_stderr_big")) require(r.getStderr().length()==600000,"truncated stderr="+r.getStderr().length());
        if (which.equals("native_empty_input_eof")) require(r.getStdout().equals("E"),"failed to close stdin");
        log("exit="+r.getExitCode()+" timeout="+r.isTimedOut()+" stdoutLen="+r.getStdout().length()+" stderrLen="+r.getStderr().length()+" timeMs="+r.getExecutionTimeMs());
    }
    private static void analyzerCase(String which) throws Exception {
        String code=switch(which){
            case "analyzer_simple" -> "#include <iostream>\nint add(int a,int b){return a+b;}\nint main(){for(int i=0;i<2;i++){}return 0;}\n";
            case "analyzer_comments" -> "// for while do\n/* for while do */\nint main(){return 0;}\n";
            case "analyzer_strings" -> "int main(){const char* s=\"for while if\";return 0;}\n";
            case "analyzer_multifile" -> "int main(){return 0;}\n";
            case "analyzer_complex_declarators" -> "auto calc(int x) -> int {return x;}\nint main(){return calc(1);}\n";
            default -> throw new IllegalArgumentException(which);
        };
        Path src=file("src/main.cpp",code);
        if (which.equals("analyzer_multifile")) file("src/helper.cpp","int helper(){for(int i=0;i<2;i++){} return 1;}\n");
        AnalysisResult r=new CppCodeAnalyzer().analyze(src);
        switch(which) {
            case "analyzer_simple" -> {require(r.getLoopCount()==1,"loops="+r.getLoopCount());require(r.getFunctionCount()==2,"functions="+r.getFunctionCount());}
            case "analyzer_comments", "analyzer_strings" -> require(r.getLoopCount()==0,"loops="+r.getLoopCount());
            case "analyzer_multifile" -> require(r.getFunctionCount()==2 && r.getLoopCount()==1,"main-only analysis functions="+r.getFunctionCount()+" loops="+r.getLoopCount());
            case "analyzer_complex_declarators" -> require(r.getFunctionCount()==2,"trailing return type not counted, functions="+r.getFunctionCount());
        }
        log("valid="+r.isValid()+" loops="+r.getLoopCount()+" functions="+r.getFunctionCount()+" warnings="+r.getWarnings().size()+" violations="+r.getViolations().size());
    }
    private static void outputCase(String which){
        OutputChecker o=new OutputChecker(); boolean match=switch(which){
            case "output_exact" -> o.check("A\n","A\n",OutputChecker.MatchMode.EXACT).isMatched();
            case "output_exact_spaces" -> !o.check("A \n","A\n",OutputChecker.MatchMode.EXACT).isMatched();
            case "output_trim" -> o.check("A   \r\nB\n\n","A\nB\n",OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE).isMatched();
            case "output_internal_spaces" -> !o.check("A  B\n","A B\n",OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE).isMatched();
            case "output_float" -> o.check("0.30000000000000004","0.3",OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE).isMatched();
            case "output_wrong" -> !o.check("3","4",OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE).isMatched();
            case "output_cr_only" -> o.check("A\rB","A\nB",OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE).isMatched();
            default -> throw new IllegalArgumentException(which);
        }; require(match,"checker behavior does not meet expectation for "+which); log("matched expectation");
    }
    private static void similarityCase(String which){
        WinnowingEngine w=new WinnowingEngine();
        String a="int main(){int total=0;for(int i=0;i<10;i++){total+=i;}return total;}";
        String b=switch(which){
            case "similarity_identical" -> a;
            case "similarity_renamed" -> "int main(){int sum=0;for(int j=0;j<10;j++){sum+=j;}return sum;}";
            case "similarity_reordered" -> "int main(){int total=0;for(int i=9;i>=0;i--){total+=i;}return total;}";
            case "similarity_unrelated" -> "#include <iostream>\nint main(){std::cout<<\"hello world\";return 0;}";
            case "similarity_empty" -> "";
            default -> throw new IllegalArgumentException(which);
        };
        double pct=w.calculateSimilarity(w.generateFingerprints(a),w.generateFingerprints(b));
        if(which.equals("similarity_identical")) require(Math.abs(pct-100)<0.0001,"identical="+pct);
        if(which.equals("similarity_renamed")) require(pct>=70,"rename sensitive similarity="+pct);
        if(which.equals("similarity_unrelated")) require(pct<40,"false-positive risk similarity="+pct);
        if(which.equals("similarity_empty")) require(pct==0,"empty="+pct);
        // Structural reorder similarity is an observed metric; no ground-truth classification from V2.
        log("similarity="+String.format(java.util.Locale.ROOT,"%.2f",pct)+"%");
    }
    private static void gradeCase(String which) throws Exception {
        String code=switch(which){
            case "grade_compilation" -> "int main( syntax error";
            case "grade_runtime" -> "int main(){return 7;}";
            case "grade_tle" -> "int main(){for(;;){} }";
            default -> S;
        };
        Path zip=zip("submission.zip",entry("main.cpp",code));
        StudentSubmission submission=new StudentSubmission("AUDIT","Fixture",zip);
        List<TestCase> cases=List.of(new TestCase("T1","",(which.equals("grade_wrong")||which.equals("grade_rubric"))?"31\n":"30\n",1));
        GradingRubric rubric=switch(which){
            case "grade_rubric" -> new GradingRubric(10.0,1.0,0.0,200.0);
            default -> new GradingRubric(10.0,0.8,0.2,200.0);
        };
        GradingReport r=new SubmissionGrader(false).grade(submission,cases,rubric,root);
        log("compiled="+r.isCompileSuccess()+" score="+r.getFinalScore()+" passed="+r.getPassedTestCount()+" tests="+r.getTotalTestCount()+" statuses="+r.getTestCaseResults().stream().map(x->x.getStatus().name()).toList()+" summary="+r.getSummary());
        switch(which){
            case "grade_correct" -> require(r.getPassedTestCount()==1,"expected AC");
            case "grade_wrong" -> require(r.getTestCaseResults().get(0).getStatus()==TestCaseResult.Status.WRONG_ANSWER,"expected WA");
            case "grade_compilation" -> require(!r.isCompileSuccess(),"expected CE");
            case "grade_runtime" -> require(r.getTestCaseResults().get(0).getStatus()==TestCaseResult.Status.RUNTIME_ERROR,"expected RE");
            case "grade_tle" -> require(r.getTestCaseResults().get(0).getStatus()==TestCaseResult.Status.TIME_LIMIT_EXCEEDED,"expected TLE");
            case "grade_rubric" -> require(r.getFinalScore()==0.0,"100% correctness rubric ignored on failing test: score="+r.getFinalScore());
            default -> throw new IllegalArgumentException(which);
        }
    }
    public static void main(String[] args){
        int code=0;
        try {
            if(args.length!=2) throw new IllegalArgumentException("usage: P002Probe <scenario> <workspace>");
            root=Path.of(args[1]); Files.createDirectories(root);
            String name=args[0];
            if(name.startsWith("zip_")) zipCase(name);
            else if(name.startsWith("compile_")) compileCase(name);
            else if(name.startsWith("native_")) nativeCase(name);
            else if(name.startsWith("analyzer_")) analyzerCase(name);
            else if(name.startsWith("output_")) outputCase(name);
            else if(name.startsWith("similarity_")) similarityCase(name);
            else if(name.startsWith("grade_")) gradeCase(name);
            else throw new IllegalArgumentException(name);
            System.out.println("AUDIT_PASS " + name);
        } catch(Throwable t){
            code=1;
            System.err.println("AUDIT_FAIL " + (args.length>0?args[0]:"unknown") + " " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        // SubmissionGrader's existing ExecutorService has no shutdown() path, so explicit exit is necessary
        // for this bounded baseline probe. The missing shutdown is recorded as a production finding.
        System.exit(code);
    }
}
