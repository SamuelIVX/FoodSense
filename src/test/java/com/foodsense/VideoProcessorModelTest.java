package com.foodsense;

import dev.samhb.interleave.Interleave;
import dev.samhb.interleave.InterleaveRunner;
import dev.samhb.interleave.Strategy;
import dev.samhb.interleave.TestResult;
import dev.samhb.interleave.bugs.BenchmarkProgram;
import dev.samhb.interleave.core.Program;
import dev.samhb.interleave.dpor.DporExplorer;
import dev.samhb.interleave.format.ProgramLoader;
import dev.samhb.interleave.por.StaticPorExplorer;
import dev.samhb.interleave.search.*;
import dev.samhb.interleave.state.BitstateStore;
import dev.samhb.interleave.state.HashingStateStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Model checking test for VideoProcessor's producer/consumer pipeline.
 * <p>
 * Declarative JSON port of the original hand-written {@code VPState}/{@code Step}
 * model. Programs are loaded from {@code video-processor.json} (2-thread) and
 * {@code video-processor-external-stop.json} (3-thread) which encode the
 * bounded queue (capacity 2) with lookup tables ({@code grab_next}, {@code dec})
 * and {@code when: final} terminal invariants. An additional fixture
 * {@code video-processor-always.json} is identical except for
 * {@code when: always} and is used to demonstrate that timing alone changes the
 * verdict. The suite exercises all explorers (DFS/StaticPor/DPOR) and stores
 * (exact/bitstate) via the public seams {@code ProgramLoader} → {@code DfsResult}.
 * </p>
 */
class VideoProcessorModelTest {

    private final ProgramLoader loader = new ProgramLoader();

    /**
     * Loads a declarative program from a classpath test resource.
     *
     * @param resource resource name under {@code src/test/resources}
     * @return loaded benchmark program with optional invariant and expected verdict
     */
    private BenchmarkProgram load(String resource) {
        return loader.loadFromResource(resource);
    }

    /**
     * Returns the standard 2-thread video-processor program.
     *
     * @return benchmark program for the producer/consumer pipeline
     */
    private BenchmarkProgram videoProcessor() {
        return load("video-processor.json");
    }

    /**
     * Returns the 3-thread variant with an external window-close stop.
     *
     * @return benchmark program with external stop thread
     */
    private BenchmarkProgram videoProcessorExternal() {
        return load("video-processor-external-stop.json");
    }

    /**
     * Returns the when:always variant used as a negative control.
     * <p>
     * Identical to {@code video-processor.json} except for
     * {@code "when": "always"} on the same {@code all} invariant;
     * expected verdict is {@code VIOLATION} because {@code stop_called == 1}
     * is false in the initial state.
     * </p>
     *
     * @return benchmark program with per-state invariant
     */
    private BenchmarkProgram videoProcessorAlways() {
        return load("video-processor-always.json");
    }

    // ---- helpers ----

    /**
     * Explores a program with the given strategy and state store.
     *
     * @param program program to explore
     * @param invariant invariant or null
     * @param strategy exploration strategy
     * @param store state store
     * @return exploration result
     */
    private DfsResult runWithStore(Program program, Invariant invariant, Strategy strategy, StateStore store) {
        return switch (strategy) {
            case DFS -> new DfsExplorer().explore(program, invariant, store, null);
            case STATIC_POR -> new StaticPorExplorer().explore(program, invariant, store, null);
            case DPOR -> new DporExplorer().explore(program, invariant, store, null);
        };
    }

    /**
     * Checks whether any trace reports a violation.
     *
     * @param result exploration result
     * @return true if any trace has {@code VIOLATION} outcome
     */
    private boolean hasViolation(DfsResult result) {
        return result.traces().stream().anyMatch(t -> t.outcome() == TraceOutcome.VIOLATION);
    }

    /**
     * Checks whether any trace reports a deadlock.
     *
     * @param result exploration result
     * @return true if any trace has {@code DEADLOCK} outcome
     */
    private boolean hasDeadlock(DfsResult result) {
        return result.traces().stream().anyMatch(t -> t.outcome() == TraceOutcome.DEADLOCK);
    }

    /**
     * Derives the expected verdict string from traces, matching
     * {@code BenchmarkHarness.actualVerdict}: VIOLATION > DEADLOCK > PASS.
     *
     * @param result exploration result
     * @return verdict string
     */
    private String actualVerdict(DfsResult result) {
        if (hasViolation(result)) return "VIOLATION";
        if (hasDeadlock(result)) return "DEADLOCK";
        return "PASS";
    }

    // ---- original 16 tests, now JSON-driven ----

    /** Verifies the standard model has no invariant violation under DPOR. */
    @Test
    void dporFindsNoDeadlockViolation() {
        BenchmarkProgram bp = videoProcessor();
        DporExplorer dpor = new DporExplorer();
        DfsResult result = dpor.explore(bp.program(), bp.invariant().orElse(null));
        assertFalse(hasViolation(result), "DPOR should not find violation. Traces: " + result.traces());
        assertFalse(hasDeadlock(result), "Standard model must not deadlock: " + result.traces());
        assertEquals(bp.expectedVerdict(), actualVerdict(result), "Fixture expected_verdict should match actual");
        assertTrue(result.statesExplored() > 0, "Should explore some states");
    }

    /** Verifies multiple interleavings are explored and some complete. */
    @Test
    void dporExploresMultipleInterleavings() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = new DporExplorer().explore(bp.program(), bp.invariant().orElse(null));
        assertTrue(result.traces().size() > 1, "Should explore multiple interleavings. Found: " + result.traces().size());
        assertTrue(result.traces().stream().anyMatch(t -> t.outcome() == TraceOutcome.COMPLETED),
                "Should have COMPLETED traces: " + result.traces());
    }

    /** Verifies the default Interleave quickCheck finds no violation. */
    @Test
    void quickCheckWithDefaultStrategy() {
        BenchmarkProgram bp = videoProcessor();
        TestResult result = Interleave.quickCheck(bp.program());
        assertFalse(result.hasViolation(), "quickCheck should not find violation: " + result.failingTraces());
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies DPOR quickCheck finds no violation. */
    @Test
    void quickCheckWithDporStrategy() {
        BenchmarkProgram bp = videoProcessor();
        TestResult result = Interleave.quickCheck(bp.program(), Strategy.DPOR);
        assertFalse(result.hasViolation(), "DPOR quickCheck should not find violation: " + result.failingTraces());
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies the runner is reusable across multiple runs. */
    @Test
    void runnerReusableAcrossMultipleRuns() {
        BenchmarkProgram bp = videoProcessor();
        InterleaveRunner runner = InterleaveRunner.builder().strategy(Strategy.DPOR).maxStates(1000).build();
        TestResult r1 = runner.run(bp.program());
        TestResult r2 = runner.run(bp.program());
        assertFalse(r1.hasViolation(), "First run should not have violation");
        assertFalse(r2.hasViolation(), "Second run should not have violation");
        assertEquals(r1.statesExplored(), r2.statesExplored(), "Reusable runner should produce consistent results");
    }

    /** Verifies the external-stop variant has no violation but the expected deadlock. */
    @Test
    void dporWithExternalStopFindsNoViolation() {
        BenchmarkProgram bp = videoProcessorExternal();
        DfsResult result = new DporExplorer().explore(bp.program(), bp.invariant().orElse(null));
        assertFalse(hasViolation(result), "DPOR with external stop should not find VIOLATION. Traces: " + result.traces());
        assertTrue(hasDeadlock(result), "External-stop fixture expects DEADLOCK: " + result.traces());
        assertEquals(bp.expectedVerdict(), actualVerdict(result), "Fixture expected_verdict should match actual");
        assertTrue(result.statesExplored() > 0, "Should explore some states");
    }

    /** Verifies DFS with exact store finds no violation and no deadlock. */
    @Test
    void dfsExactFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DFS, new HashingStateStore());
        assertFalse(hasViolation(result), "DFS + exact should find no violation");
        assertFalse(hasDeadlock(result), "DFS + exact should find no deadlock");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies DFS with bitstate store finds no violation. */
    @Test
    void dfsBitstateFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DFS, new BitstateStore(1_000_003, 4));
        assertFalse(hasViolation(result), "DFS + bitstate should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies Static POR with exact store finds no violation. */
    @Test
    void staticPorExactFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.STATIC_POR, new HashingStateStore());
        assertFalse(hasViolation(result), "STATIC_POR + exact should find no violation");
        assertFalse(hasDeadlock(result), "STATIC_POR + exact should find no deadlock");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies Static POR with bitstate store finds no violation. */
    @Test
    void staticPorBitstateFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.STATIC_POR, new BitstateStore(1_000_003, 4));
        assertFalse(hasViolation(result), "STATIC_POR + bitstate should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies DPOR with exact store finds no violation. */
    @Test
    void dporExactFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DPOR, new HashingStateStore());
        assertFalse(hasViolation(result), "DPOR + exact should find no violation");
        assertFalse(hasDeadlock(result), "DPOR + exact should find no deadlock");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies DPOR with bitstate store finds no violation. */
    @Test
    void dporBitstateFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DPOR, new BitstateStore(1_000_003, 4));
        assertFalse(hasViolation(result), "DPOR + bitstate should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies bitstate store metrics are populated. */
    @Test
    void bitstateMetricsPopulated() {
        BenchmarkProgram bp = videoProcessor();
        BitstateStore store = new BitstateStore(1_000_003, 4);
        runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DPOR, store);
        double fpr = store.estimatedFalsePositiveRate();
        assertTrue(fpr >= 0.0 && fpr < 1e-6, "FPR should be near zero: " + fpr);
        assertTrue(store.bitCount() > 0, "bitCount should be positive: " + store.bitCount());
        assertTrue(store.bitDensity() > 0.0, "bitDensity should be positive: " + store.bitDensity());
    }

    /** Verifies the runner works with a bitstate store factory. */
    @Test
    void runnerWithBitstateStore() {
        BenchmarkProgram bp = videoProcessor();
        InterleaveRunner runner = InterleaveRunner.builder()
                .strategy(Strategy.DPOR)
                .stateStoreFactory(() -> new BitstateStore(1_000_003, 4))
                .maxStates(1000)
                .build();
        TestResult result = runner.run(bp.program());
        assertFalse(result.hasViolation(), "Runner with bitstate should find no violation: " + result.failingTraces());
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies bitstate with external stop has no violation but expected deadlock. */
    @Test
    void bitstateWithExternalStop() {
        BenchmarkProgram bp = videoProcessorExternal();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DPOR, new BitstateStore(1_000_003, 4));
        assertFalse(hasViolation(result), "Bitstate with external stop should find no VIOLATION. Traces: " + result.traces());
        assertTrue(hasDeadlock(result), "Bitstate external stop should have DEADLOCK: " + result.traces());
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    /** Verifies all strategies produce the same violation verdict per store type. */
    @Test
    void allStrategiesProduceSameVerdict() {
        BenchmarkProgram bp = videoProcessor();
        for (StoreType storeType : StoreType.values()) {
            StateStore store = storeType == StoreType.EXACT ? new HashingStateStore() : new BitstateStore(1_000_003, 4);
            boolean dfsViol = hasViolation(runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DFS, store));
            store = storeType == StoreType.EXACT ? new HashingStateStore() : new BitstateStore(1_000_003, 4);
            boolean porViol = hasViolation(runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.STATIC_POR, store));
            store = storeType == StoreType.EXACT ? new HashingStateStore() : new BitstateStore(1_000_003, 4);
            boolean dporViol = hasViolation(runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DPOR, store));
            assertEquals(dfsViol, porViol, "DFS and STATIC_POR should agree for " + storeType);
            assertEquals(dfsViol, dporViol, "DFS and DPOR should agree for " + storeType);
        }
    }

    // ---- Spec 10: same program and invariant, only when differs ----

    /**
     * Demonstrates that {@code when} alone changes the verdict.
     * <p>
     * Uses the same program and {@code all} invariant; only {@code when} differs.
     * With {@code when: final} (default, in {@code video-processor.json}) the
     * invariant {@code stop_called == 1} is enforced only at termination and no
     * violation is reported. With {@code when: always} (in
     * {@code video-processor-always.json}) the same invariant is checked at every
     * configuration — the initial state violates it and a VIOLATION is reported.
     * </p>
     */
    @Test
    void whenAlwaysCatchesTransientViolationWhileFinalDoesNot() {
        BenchmarkProgram fin = videoProcessor();
        DfsResult resFinal = new DfsExplorer().explore(fin.program(), fin.invariant().orElse(null));
        assertFalse(hasViolation(resFinal), "when:final should not flag transient violation");
        assertEquals(fin.expectedVerdict(), actualVerdict(resFinal), "final fixture verdict should match");

        BenchmarkProgram always = videoProcessorAlways();
        DfsResult resAlways = new DfsExplorer().explore(always.program(), always.invariant().orElse(null));
        assertTrue(hasViolation(resAlways), "when:always should catch transient violation");
        assertEquals(always.expectedVerdict(), actualVerdict(resAlways), "always fixture verdict should match");
    }

    /** Verifies all three declarative fixtures load directly via ProgramLoader. */
    @Test
    void declarativeFilesLoadDirectly() {
        assertDoesNotThrow(() -> load("video-processor.json"));
        assertDoesNotThrow(() -> load("video-processor-external-stop.json"));
        assertDoesNotThrow(() -> load("video-processor-transient-violation.json"));
        assertDoesNotThrow(() -> load("video-processor-always.json"));
    }

    /** Store type for parameterized store tests. */
    enum StoreType { EXACT, BITSTATE }
}
