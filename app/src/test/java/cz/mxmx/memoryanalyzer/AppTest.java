package cz.mxmx.memoryanalyzer;

import cz.mxmx.memoryanalyzer.app.App;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class AppTest {
    private final String heapDumpPath = "../sandbox/data/null-refs-10.hprof";

    @Test
    public void verboseTest() {
        App app = new App(new String[]{"-v", "-p", heapDumpPath, "-l"});
        Assert.assertTrue(app.isVerbose());
    }

    @Test
    public void fieldsTest() {
        App app = new App(new String[]{"-f", "-p", heapDumpPath, "-l"});
        Assert.assertTrue(app.isFields());
    }

    @Test
    public void csvTest() {
        App app = new App(new String[]{"-c", "-p", heapDumpPath, "-l"});
        Assert.assertTrue(app.isCsv());
    }
    @Test
    public void helpTest() {
        App app = new App(new String[]{"-h", "-p", "path"});
        Assert.assertTrue(app.isHelp());
    }

    @Test
    public void interactiveTest() {
        String stop = "stop";
        System.setIn(new ByteArrayInputStream(stop.getBytes()));
        App app = new App(new String[]{"-i", "-p", heapDumpPath});

        Assert.assertTrue(app.isInteractive());
    }

    @Test
    public void runAnalysisFromCLI() {
        App app = new App(new String[]{"-l", "-p", heapDumpPath });

        Assert.assertTrue(app.isHasRunAnalysis());
    }
}