package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Test;
import vn.edu.dlu.autograder.checker.OutputChecker;
import static org.junit.jupiter.api.Assertions.*;

class OutputRegressionTest {
    private final OutputChecker checker = new OutputChecker();

    @Test void exactDoesNotIgnoreWhitespace() {
        assertFalse(checker.check("30 \n", "30\n", OutputChecker.MatchMode.EXACT).isMatched());
    }

    @Test void trailingModeAcceptsWindowsAndLinuxNewlines() {
        assertTrue(checker.check("30  \r\n", "30\n", OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE).isMatched());
    }

    @Test void trailingModePreservesMeaningfulInternalWhitespace() {
        assertFalse(checker.check("1  2\n", "1 2\n", OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE).isMatched());
    }

    @Test void incorrectValuesReturnWrongAnswerDetail() {
        var result = checker.check("31", "30", OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE);
        assertFalse(result.isMatched());
        assertTrue(result.getDiffMessage().contains("Wrong Answer"));
    }

    @Test void nullAndEmptyAreEquivalentInTrimmedMode() {
        assertTrue(checker.check(null, "  \r\n", OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE).isMatched());
    }
}
