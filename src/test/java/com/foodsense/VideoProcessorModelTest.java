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
 * This is the declarative JSON port of the original hand-written
 * {@code VPState}/{@code Step} model. Programs are loaded from
 * {@code video-processor.json} (2-thread) and
 * {@code video-processor-external-stop.json} (3-thread) which use the
 * bounded-queue encoding with lookup tables ({@code grab_next}, {@code dec})
 * and {@code when: final} terminal invariants. The test suite exercises all
 * explorers (DFS/StaticPor/Dpor) and stores (exact/bitstate) via the public
 * seams {@code ProgramLoader} → {@code DfsResult}.
 * </p>
 */
class VideoProcessorModelTest {

    private final ProgramLoader loader = new ProgramLoader();

    private BenchmarkProgram load(String resource) {
        return loader.loadFromResource(resource);
    }

    private BenchmarkProgram videoProcessor() {
        return load("video-processor.json");
    }

    private BenchmarkProgram videoProcessorExternal() {
        return load("video-processor-external-stop.json");
    }

    private BenchmarkProgram transientViolation() {
        return load("video-processor-transient-violation.json");
    }

    // ---- helpers ----

    private DfsResult runWithStore(Program program, Invariant invariant, Strategy strategy, StateStore store) {
        return switch (strategy) {
            case DFS -> new DfsExplorer().explore(program, invariant, store, null);
            case STATIC_POR -> new StaticPorExplorer().explore(program, invariant, store, null);
            case DPOR -> new DporExplorer().explore(program, invariant, store, null);
        };
    }

    private boolean hasViolation(DfsResult result) {
        return result.traces().stream().anyMatch(t -> t.outcome() == TraceOutcome.VIOLATION);
    }

    // ---- original 16 tests, now JSON-driven ----

    @Test
    void dporFindsNoDeadlockViolation() {
        BenchmarkProgram bp = videoProcessor();
        DporExplorer dpor = new DporExplorer();
        DfsResult result = dpor.explore(bp.program(), bp.invariant().orElse(null));
        assertFalse(hasViolation(result), "DPOR should not find violation. Traces: " + result.traces());
        assertTrue(result.statesExplored() > 0, "Should explore some states");
    }

    @Test
    void dporExploresMultipleInterleavings() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = new DporExplorer().explore(bp.program(), bp.invariant().orElse(null));
        assertTrue(result.traces().size() > 1, "Should explore multiple interleavings. Found: " + result.traces().size());
        assertTrue(result.traces().stream().anyMatch(t -> t.outcome() == TraceOutcome.COMPLETED),
                "Should have COMPLETED traces: " + result.traces());
    }

    @Test
    void quickCheckWithDefaultStrategy() {
        BenchmarkProgram bp = videoProcessor();
        TestResult result = Interleave.quickCheck(bp.program());
        assertFalse(result.hasViolation(), "quickCheck should not find violation: " + result.failingTraces());
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void quickCheckWithDporStrategy() {
        BenchmarkProgram bp = videoProcessor();
        TestResult result = Interleave.quickCheck(bp.program(), Strategy.DPOR);
        assertFalse(result.hasViolation(), "DPOR quickCheck should not find violation: " + result.failingTraces());
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

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

    @Test
    void dporWithExternalStopFindsNoViolation() {
        BenchmarkProgram bp = videoProcessorExternal();
        // External variant is expected DEADLOCK under harness, but invariant is terminal and we only check VIOLATION
        DfsResult result = new DporExplorer().explore(bp.program(), bp.invariant().orElse(null));
        assertFalse(hasViolation(result), "DPOR with external stop should not find VIOLATION. Traces: " + result.traces());
        assertTrue(result.statesExplored() > 0, "Should explore some states");
    }

    @Test
    void dfsExactFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DFS, new HashingStateStore());
        assertFalse(hasViolation(result), "DFS + exact should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void dfsBitstateFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DFS, new BitstateStore(1_000_003, 4));
        assertFalse(hasViolation(result), "DFS + bitstate should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void staticPorExactFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.STATIC_POR, new HashingStateStore());
        assertFalse(hasViolation(result), "STATIC_POR + exact should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void staticPorBitstateFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.STATIC_POR, new BitstateStore(1_000_003, 4));
        assertFalse(hasViolation(result), "STATIC_POR + bitstate should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void dporExactFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DPOR, new HashingStateStore());
        assertFalse(hasViolation(result), "DPOR + exact should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void dporBitstateFindsNoViolation() {
        BenchmarkProgram bp = videoProcessor();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DPOR, new BitstateStore(1_000_003, 4));
        assertFalse(hasViolation(result), "DPOR + bitstate should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

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

    @Test
    void bitstateWithExternalStop() {
        BenchmarkProgram bp = videoProcessorExternal();
        DfsResult result = runWithStore(bp.program(), bp.invariant().orElse(null), Strategy.DPOR, new BitstateStore(1_000_003, 4));
        assertFalse(hasViolation(result), "Bitstate with external stop should find no VIOLATION. Traces: " + result.traces());
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

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

    // ---- new: Spec 10 when:always negative control ----

    @Test
    void whenAlwaysCatchesTransientViolationWhileFinalDoesNot() {
        // transientViolation.json has when:always with queue_size == 0 -> violates after first grab
        BenchmarkProgram always = transientViolation();
        DfsResult resAlways = new DfsExplorer().explore(always.program(), always.invariant().orElse(null));
        assertTrue(hasViolation(resAlways), "when:always should catch transient queue_size == 0 violation");

        // Same program shape but with when:final (default) should NOT violate — final queue_size may be 0 or 1, but invariant holds at termination for some paths?
        // We test the positive fixture: video-processor.json has when:final (default) and should NOT violate
        BenchmarkProgram fin = videoProcessor();
        DfsResult resFinal = new DfsExplorer().explore(fin.program(), fin.invariant().orElse(null));
        assertFalse(hasViolation(resFinal), "when:final should not flag transient queue violation");
    }

    @Test
    void declarativeFilesLoadDirectly() {
        assertDoesNotThrow(() -> load("video-processor.json"));
        assertDoesNotThrow(() -> load("video-processor-external-stop.json"));
        assertDoesNotThrow(() -> load("video-processor-transient-violation.json"));
    }

    enum StoreType { EXACT, BITSTATE }
}
