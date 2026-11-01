package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Test;
import vn.edu.dlu.autograder.analyzer.WinnowingEngine;
import static org.junit.jupiter.api.Assertions.*;

class SimilarityRegressionTest {
    private final WinnowingEngine engine = new WinnowingEngine();

    @Test void identicalCodeHasFullSimilarity() {
        String code = "int main(){int value=3;return value+1;}";
        var fp = engine.generateFingerprints(code);
        assertFalse(fp.isEmpty());
        assertEquals(100.0, engine.calculateSimilarity(fp, engine.generateFingerprints(code)), 0.00001);
    }

    @Test void renamingIdentifiersKeepsNormalizedFingerprint() {
        String first = "int main(){int apple=9;return apple+3;}";
        String second = "int main(){int pear=9;return pear+3;}";
        assertEquals(engine.generateFingerprints(first), engine.generateFingerprints(second));
    }

    @Test void emptyCodeDoesNotCrash() {
        assertTrue(engine.generateFingerprints("").isEmpty());
        assertEquals(0.0, engine.calculateSimilarity(engine.generateFingerprints(""), engine.generateFingerprints("int main(){return 0;}")));
    }

    @Test void unrelatedCodeDoesNotHaveMoreSimilarityThanItself() {
        String first = "int main(){int a=0;for(int i=0;i<9;i++){a+=i;}return a;}";
        String other = "void foo(){while(false){}} int main(){return 7;}";
        var left = engine.generateFingerprints(first);
        assertTrue(engine.calculateSimilarity(left, engine.generateFingerprints(other)) < 100.0);
    }
}
