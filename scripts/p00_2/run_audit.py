#!/usr/bin/env python3
"""P00.2 offline baseline audit (no dependencies beyond Python, JDK21, g++ and shaded CLI JAR)."""
import argparse
import datetime as dt
import json
import os
from pathlib import Path
import platform
import shutil
import signal
import subprocess
import sys
import tempfile
import time
import zipfile

ROOT = Path(__file__).resolve().parents[2]
PROBE = Path(__file__).with_name('P002Probe.java')
SCENARIOS = {
    'ZipExtractor': ['zip_valid','zip_nested','zip_slip','zip_large','zip_no_cpp'],
    'CompilerEngine': ['compile_single','compile_multi','compile_error','compile_missing','compile_stderr_flood','compile_timeout'],
    'NativeProcessExecutor': ['native_normal','native_timeout','native_stdout_big','native_stderr_big','native_input_big','native_input_early_exit','native_empty_input_eof'],
    'CppCodeAnalyzer': ['analyzer_simple','analyzer_comments','analyzer_strings','analyzer_multifile','analyzer_complex_declarators'],
    'SubmissionGrader': ['grade_correct','grade_wrong','grade_compilation','grade_runtime','grade_tle','grade_rubric'],
    'OutputChecker': ['output_exact','output_exact_spaces','output_trim','output_internal_spaces','output_float','output_wrong','output_cr_only'],
    'WinnowingEngine': ['similarity_identical','similarity_renamed','similarity_reordered','similarity_unrelated','similarity_empty'],
    'AutoGraderCommand': ['cli_help','cli_grade_help','cli_invalid_option','cli_missing_required','cli_missing_submission','cli_report_option'],
}
EXPECTED_LIMITATIONS = {
 'zip_large': 'No upper bound on uncompressed archive size/entry count.',
 'zip_no_cpp': 'Extractor intentionally returns destination directory even when no .cpp is found (compiler rejects later).',
 'compile_stderr_flood': 'Compiler stderr is drained before waitFor(timeout); unbounded logs or open stderr can block.',
 'compile_timeout': 'Compiler timeout begins only after the entire stderr stream has closed; delayed compiler completion can evade timeout.',
 'native_stdout_big': 'Native executor waits before reading stdout; child may block on full pipe.',
 'native_stderr_big': 'Native executor waits before reading stderr; child may block on full pipe.',
 'native_input_big': 'Verify delivery of a 2 MB stdin payload to a consumer before diagnosing pipe behavior.',
 'native_input_early_exit': 'Child exits before consuming large stdin; writer may get broken pipe and discard valid exit evidence.',
 'native_empty_input_eof': 'Empty stdin is not closed; programs reading to EOF can hang.',
 'analyzer_multifile': 'Analyzer processes only the specified file, not the whole multi-source project.',
 'analyzer_complex_declarators': 'Regex misses C++ trailing return type function declarations.',
 'output_float': 'No floating point comparison with tolerance is implemented.',
 'output_cr_only': 'Standalone carriage returns are deleted rather than normalized to newlines.',
 'grade_rubric': 'Rubric weights are ignored: grader hardcodes 60/15/15/10.',
 'cli_report_option': 'Grade CLI has no --output export option; only terminal summary.',
}


def run(cmd, timeout=12, cwd=None, env=None):
    start = time.monotonic()
    if os.name == 'nt':
        flags = getattr(subprocess, 'CREATE_NEW_PROCESS_GROUP', 0)
        process = subprocess.Popen(cmd, cwd=cwd, env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   text=True, encoding='utf-8', errors='replace', creationflags=flags)
    else:
        process = subprocess.Popen(cmd, cwd=cwd, env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   text=True, encoding='utf-8', errors='replace', start_new_session=True)
    try:
        out, err = process.communicate(timeout=timeout)
        return {'exit_code': process.returncode, 'stdout': out[-1600:], 'stderr': err[-1600:],
                'seconds': round(time.monotonic()-start, 3), 'watchdog': False}
    except subprocess.TimeoutExpired:
        if os.name == 'nt':
            subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'], capture_output=True, timeout=5)
        else:
            os.killpg(process.pid, signal.SIGKILL)
        out, err = process.communicate(timeout=5)
        return {'exit_code': None, 'stdout': out[-1600:], 'stderr': err[-1600:],
                'seconds': round(time.monotonic()-start, 3), 'watchdog': True}



def build_fake_compiler(real_gxx, shim, scenario):
    """Compile a harmless g++ replacement so timeout probes also run on native Windows.

    The replacement only simulates a stalled compiler; it never compiles user files.
    The real g++ is resolved before PATH is modified to avoid recursive compilation.
    """
    output = shim / ('g++.exe' if os.name == 'nt' else 'g++')
    source = shim / 'fake_compiler.cpp'
    flood = scenario == 'compile_stderr_flood'
    source.write_text(
        '#include <chrono>\n#include <iostream>\n#include <string>\n#include <thread>\n'
        'int main(){\n' +
        ("  std::cerr << std::string(500000, 'E'); std::cerr.flush();\n" if flood else '') +
        f'  std::this_thread::sleep_for(std::chrono::seconds({36 if flood else 32}));\n'
        '  return 0;\n}\n', encoding='utf-8')
    build = run([real_gxx, '-std=c++17', str(source), '-o', str(output)], timeout=25)
    if build['exit_code'] != 0 or build['watchdog']:
        return None, 'Unable to create controlled compiler simulator: ' + build['stderr'][:350]
    if os.name != 'nt': output.chmod(0o755)
    return output, ''


def notes(s):
    return EXPECTED_LIMITATIONS.get(s, '')


def markdown(report):
    lines = [
        '# P00.2 — Functional baseline audit', '',
        '> **Baseline observation, not a fix.** Production `src/main/java` and `pom.xml` were not modified.', '',
        f"Generated (UTC): {report['generated_utc']}", '',
        '## Environment', '',
        f"- OS: `{report['environment']['os']}`", f"- Java: `{report['environment']['java']}`",
        f"- C++ compiler: `{report['environment']['g++']}`", f"- Maven: `{report['environment']['maven']}`", f"- Runtime dependencies JAR: `{report['environment']['runtime_jar']}`",
        f"- Source Git revision: `{report['environment']['git_revision']}`", '',
        '## Aggregate', '',
        '| Status | Count |', '|---|---:|',
    ]
    for key, val in report['summary'].items(): lines.append(f'| {key} | {val} |')
    lines += ['', 'PASS = matched specified expectation; FAIL = confirmed mismatch; OBSERVED = documented behavior without a reliable pass/fail oracle; BLOCKED = unable to execute.',
              '', '## Results by component', '']
    for component, scenarios in SCENARIOS.items():
        lines += [f'### {component}', '', '| Case | Status | Evidence / finding |', '|---|---|---|']
        for name in scenarios:
            found = next((r for r in report['results'] if r['case']==name), None)
            if found is None: continue
            detail = (found.get('detail') or found.get('stderr') or found.get('stdout') or '').replace('|','\\|').replace('\n',' ').strip()
            detail = detail[:350] if detail else '-'
            suffix = notes(name)
            if suffix: detail += ' **Baseline risk:** ' + suffix
            lines.append(f"| `{name}` | **{found['status']}** | {detail} |")
        lines.append('')
    lines += [
        '## Baseline risk register', '',
        '| ID | Severity | Description | Recommended phase |', '|---|---|---|---|',
        '| B-01 | P0 | Native child stdout/stderr pipe can deadlock; stdin EOF not guaranteed | P02 |',
        '| B-02 | P0 | SubmissionGrader directly calls NativeProcessExecutor; Docker setting bypassed | P02 |',
        '| B-03 | P0 | Compiler runs g++ on host and stderr blocking can bypass 30s timeout | P02 |',
        '| B-04 | P1 | Archive extraction missing decompression size/count quotas | P02 |',
        '| B-05 | P1 | Rubric weights ignored; score uses hard-coded 60/15/15/10 | P09 |',
        '| B-06 | P1 | CppCodeAnalyzer uses Regex; misses many declarations and multi-file structure | P04 |',
        '| B-07 | P2 | OutputChecker lacks floating tolerance and standalone CR normalization | P03/P10 |',
        '| B-08 | P2 | Winnowing Jaccard requires calibrated benchmark to interpret similarity | P07 |',
        '| B-09 | P1 | SubmissionGrader thread pool not shut down; probe uses System.exit | P02 |',
        '| B-10 | P2 | CLI grade exit code 0 even for unsuccessful grading; no grade file exporter | P10 |',
        '', '## Reproduction', '',
        '1. Build the CLI shaded JAR with Maven and Java 21: `mvn clean verify` (record independently).',
        '2. Run `python scripts/p00_2/run_audit.py --runtime-jar target/autograder-1.0.0-cli.jar --output-dir p00-2-results`.',
        '3. Source is freshly compiled using javac with the shaded JAR as the classpath; synthetic fixtures run in temporary directories.',
        '4. Every Java/C++ probe has a process-group watchdog; a timeout is reported as FAIL, not PASS.',
        '', '## Limitations', '',
        '- These probes execute only synthetic, benign C++ fixtures. Baseline native execution is NOT secure for untrusted submissions.',
        '- Runtime JAR is a separate prebuilt artifact; this audit uses it for dependencies, not as proof of Maven package correctness.',
        '- Cross-platform results may differ; the normal-execution fixture now accepts CRLF. Fake compilers are built as native executables for the host platform; Windows behavior requires a Windows rerun.',
        '- Gradle/Maven dependency vulnerability audit, Docker isolation test, complete JUnit suite and GitHub CI are not covered by this standalone runner.',
        '- This P00.2 audit does not modify production behavior; confirmed defects remain as subsequent work items.',
        '',
    ]
    return '\n'.join(lines)


def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--runtime-jar', type=Path, required=True)
    ap.add_argument('--output-dir', type=Path, default=Path('p00-2-results'))
    ap.add_argument('--skip-slow', action='store_true')
    ap.add_argument('--case', action='append', choices=sorted({c for cases in SCENARIOS.values() for c in cases}),
                    help='Run one selected probe; may be repeated to choose multiple probes')
    args=ap.parse_args()
    jar=args.runtime_jar.resolve(); dest=args.output_dir.resolve(); dest.mkdir(parents=True,exist_ok=True)
    if not jar.is_file(): ap.error(f'Runtime JAR unavailable: {jar}')
    git=run(['git','rev-parse','--short','HEAD'],timeout=5,cwd=ROOT)
    env_info={'os':platform.platform(), 'runtime_jar':str(jar),
              'git_revision':git['stdout'].strip(),
              'java':run(['java','-version'],timeout=5)['stderr'].splitlines()[0],
              'g++':run(['g++','--version'],timeout=5)['stdout'].splitlines()[0] if shutil.which('g++') else 'NOT FOUND',
              'maven':shutil.which('mvn') or 'NOT INSTALLED'}
    results=[]
    with tempfile.TemporaryDirectory(prefix='autograder_p002_') as temp:
        temp=Path(temp); cls=temp/'classes'; cls.mkdir(); work=temp/'fixtures'; work.mkdir();
        java_files=sorted(str(f) for f in (ROOT/'src/main/java').rglob('*.java'))
        compilation=run(['javac','-encoding','UTF-8','-cp',str(jar),'-d',str(cls),*java_files,str(PROBE)],timeout=55,cwd=ROOT)
        if compilation['exit_code']!=0 or compilation['watchdog']:
            print('FATAL: current source compilation failed:',compilation, file=sys.stderr)
            return 3
        cp=os.pathsep.join((str(cls),str(jar)))
        for comp, scenarios in SCENARIOS.items():
            for scenario in scenarios:
                if args.case and scenario not in args.case: continue
                if args.skip_slow and scenario=='compile_timeout':
                    outcome={'case':scenario,'component':comp,'status':'BLOCKED','detail':'Skipped by --skip-slow'}
                    results.append(outcome); print(f'BLOCKED  {scenario}'); continue
                path=work/scenario; path.mkdir(parents=True,exist_ok=True)
                cmd=['java','-cp',cp, 'P002Probe',scenario,str(path)]
                env=os.environ.copy(); timeout=14
                if scenario in ('compile_timeout','compile_stderr_flood'):
                    shim=path/'shim'; shim.mkdir(exist_ok=True)
                    gxx=shutil.which('g++')
                    if not gxx:
                        outcome={'case':scenario,'component':comp,'status':'BLOCKED','detail':'g++ unavailable; compiler simulator cannot be built'}
                        results.append(outcome); print(f'BLOCKED  {scenario}'); continue
                    fake, error=build_fake_compiler(gxx, shim, scenario)
                    if not fake:
                        outcome={'case':scenario,'component':comp,'status':'BLOCKED','detail':error}
                        results.append(outcome); print(f'BLOCKED  {scenario} {error[:70]}'); continue
                    timeout=37 if scenario=='compile_timeout' else 8
                    env['PATH']=str(shim)+os.pathsep+env.get('PATH','')
                if scenario.startswith('cli_'):
                    cmd=['java','-cp',cp,'vn.edu.dlu.autograder.cli.AutoGraderCommand']
                    if scenario=='cli_help': cmd+=['--help']
                    if scenario=='cli_grade_help': cmd+=['grade','--help']
                    if scenario=='cli_invalid_option': cmd+=['--definitely-invalid']
                    if scenario=='cli_missing_required': cmd+=['grade']
                    if scenario=='cli_missing_submission': cmd+=['grade',str(path/'missing.zip'),'-t',str(path)]
                    if scenario=='cli_report_option': cmd+=['grade',str(path/'missing.zip'),'-t',str(path),'--output',str(path/'grade.json')]
                out=run(cmd,timeout=timeout,cwd=ROOT,env=env)
                if out['watchdog']:
                    status='FAIL'; detail=f'Process-group watchdog exceeded {timeout}s; possible deadlock/timeout bypass'
                elif scenario.startswith('cli_'):
                    all_output=out['stdout']+'\n'+out['stderr']
                    expectation={
                        'cli_help': out['exit_code']==0 and 'grade' in all_output and 'check-plagiarism' in all_output,
                        'cli_grade_help': out['exit_code']==0 and '--testcases' in all_output,
                        'cli_invalid_option': out['exit_code']!=0,
                        'cli_missing_required': out['exit_code']!=0,
                        'cli_missing_submission': out['exit_code']!=0,
                        'cli_report_option': out['exit_code']==0 and (path/'grade.json').exists(),
                    }[scenario]
                    status='PASS' if expectation else 'FAIL'
                    detail=f"exit={out['exit_code']}; stdout={out['stdout'][:190]}; stderr={out['stderr'][:190]}"
                else:
                    status='PASS' if out['exit_code']==0 and 'AUDIT_PASS ' in out['stdout'] else 'FAIL'
                    if scenario in ('similarity_reordered','zip_no_cpp') and status=='PASS':status='OBSERVED'
                    detail=(out['stdout'] if status in ('PASS','OBSERVED') else out['stderr'] or out['stdout']).strip()[:450]
                item={'case':scenario,'component':comp,'status':status,'detail':detail,
                      'seconds':out['seconds'],'exit_code':out['exit_code'],'watchdog':out['watchdog']}
                results.append(item)
                print(f"{status:<9}{comp:23} {scenario:28} {detail[:85]}",flush=True)
    counts={x:sum(r['status']==x for r in results) for x in ('PASS','FAIL','OBSERVED','BLOCKED')}
    report={'schema_version':1,'generated_utc':dt.datetime.now(dt.timezone.utc).isoformat(),
            'environment':env_info,'summary':counts,'results':results}
    (dest/'P00.2-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    (dest/'P00.2-functional-audit.md').write_text(markdown(report),encoding='utf-8')
    print('\nSUMMARY:',counts,'\nReports:',dest,flush=True)
    return 1 if counts['FAIL'] or counts['BLOCKED'] else 0

if __name__ == '__main__': sys.exit(main())
