package com.foodsense;

import dev.samhb.interleave.Interleave;
import dev.samhb.interleave.InterleaveRunner;
import dev.samhb.interleave.TestResult;
import dev.samhb.interleave.core.*;
import dev.samhb.interleave.dpor.DporExplorer;
import dev.samhb.interleave.por.StaticPorExplorer;
import dev.samhb.interleave.search.*;
import dev.samhb.interleave.state.BitstateStore;
import dev.samhb.interleave.state.HashingStateStore;
import dev.samhb.interleave.Strategy;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Model checking test for VideoProcessor's producer/consumer pipeline.
 * Uses interleave to exhaustively explore interleavings and verify
 * no deadlocks or liveness violations exist in the bounded queue protocol.
 */
class VideoProcessorModelTest {

    /**
     * Shared state modeling the VideoProcessor's bounded queue and control flags.
     * Queue capacity is 2 (matching VideoProcessor.frameQueue).
     */
    static final class VPState implements SharedState {
        // Queue states: 0=empty, 1=one frame, 2=full (capacity 2)
        private int queueSize = 0;
        private boolean producerRunning = true;
        private boolean consumerRunning = true;
        private boolean stopCalled = false;

        public VPState() {}

        public int queueSize() { return queueSize; }
        public boolean producerRunning() { return producerRunning; }
        public boolean consumerRunning() { return consumerRunning; }
        public boolean stopCalled() { return stopCalled; }

        public void setQueueSize(int size) { this.queueSize = size; }
        public void setProducerRunning(boolean v) { this.producerRunning = v; }
        public void setConsumerRunning(boolean v) { this.consumerRunning = v; }
        public void setStopCalled(boolean v) { this.stopCalled = v; }

        @Override
        public SharedState deepCopy() {
            VPState copy = new VPState();
            copy.queueSize = this.queueSize;
            copy.producerRunning = this.producerRunning;
            copy.consumerRunning = this.consumerRunning;
            copy.stopCalled = this.stopCalled;
            return copy;
        }

        @Override
        public void encodeTo(java.io.DataOutput out) throws java.io.IOException {
            out.writeInt(queueSize);
            out.writeBoolean(producerRunning);
            out.writeBoolean(consumerRunning);
            out.writeBoolean(stopCalled);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VPState that)) return false;
            return queueSize == that.queueSize &&
                   producerRunning == that.producerRunning &&
                   consumerRunning == that.consumerRunning &&
                   stopCalled == that.stopCalled;
        }

        @Override
        public int hashCode() {
            return Objects.hash(queueSize, producerRunning, consumerRunning, stopCalled);
        }

        @Override
        public String toString() {
            return String.format("VPState{queue=%d, prod=%b, cons=%b, stop=%b}",
                    queueSize, producerRunning, consumerRunning, stopCalled);
        }
    }

    /**
     * Producer step: grab frame and offer to queue.
     * Models the ProducerTask.run() logic in VideoProcessor.
     * If queue is full (size 2), it polls one then offers (overflow handling).
     * Enabled while producer is running. When stopCalled, becomes a no-op to allow progression to stop step.
     */
    static final class ProducerGrabStep implements Step {
        private final int threadId;

        public ProducerGrabStep(int threadId) { this.threadId = threadId; }

        @Override public Set<MemoryLocation> reads() {
            return Set.of(
                MemoryLocation.of("producerRunning"),
                MemoryLocation.of("queueSize"),
                MemoryLocation.of("stopCalled")
            );
        }

        @Override public Set<MemoryLocation> writes() {
            return Set.of(MemoryLocation.of("queueSize"));
        }

        @Override public boolean enabled(SharedState state) {
            if (!(state instanceof VPState vp)) return false;
            return vp.producerRunning();
        }

        @Override public StepOutcome execute(SharedState state) {
            VPState vp = (VPState) state;
            if (!vp.stopCalled()) {
                // Normal operation: grab frame, clone, offer to queue
                if (vp.queueSize() >= 2) {
                    vp.setQueueSize(2); // overflow: poll then offer
                } else {
                    vp.setQueueSize(vp.queueSize() + 1);
                }
            }
            // If stopCalled, this is a no-op - just advance PC to reach stop step
            return StepOutcome.ADVANCED;
        }

        @Override public boolean equals(Object o) {
            return o instanceof ProducerGrabStep that && threadId == that.threadId;
        }

        @Override public int hashCode() { return Objects.hash(threadId); }
    }

    /**
     * Producer step: stop when stopCalled becomes true.
     */
    static final class ProducerStopStep implements Step {
        private final int threadId;

        public ProducerStopStep(int threadId) { this.threadId = threadId; }

        @Override public Set<MemoryLocation> reads() {
            return Set.of(MemoryLocation.of("producerRunning"), MemoryLocation.of("stopCalled"));
        }

        @Override public Set<MemoryLocation> writes() {
            return Set.of(MemoryLocation.of("producerRunning"));
        }

        @Override public boolean enabled(SharedState state) {
            return state instanceof VPState vp && vp.producerRunning() && vp.stopCalled();
        }

        @Override public StepOutcome execute(SharedState state) {
            VPState vp = (VPState) state;
            vp.setProducerRunning(false);
            return StepOutcome.ADVANCED;
        }

        @Override public boolean equals(Object o) {
            return o instanceof ProducerStopStep that && threadId == that.threadId;
        }

        @Override public int hashCode() { return Objects.hash(threadId); }
    }

    /**
     * Consumer step: poll frame from queue with timeout.
     * Enabled while consumer running, stop not called, and (queue has frames OR producer stopped).
     */
    static final class ConsumerPollStep implements Step {
        private final int threadId;

        public ConsumerPollStep(int threadId) { this.threadId = threadId; }

        @Override public Set<MemoryLocation> reads() {
            return Set.of(
                MemoryLocation.of("consumerRunning"),
                MemoryLocation.of("queueSize"),
                MemoryLocation.of("producerRunning"),
                MemoryLocation.of("stopCalled")
            );
        }

        @Override public Set<MemoryLocation> writes() {
            return Set.of(MemoryLocation.of("queueSize"));
        }

        @Override public boolean enabled(SharedState state) {
            if (!(state instanceof VPState vp)) return false;
            return vp.consumerRunning() && !vp.stopCalled() &&
                   (vp.queueSize() > 0 || !vp.producerRunning());
        }

        @Override public StepOutcome execute(SharedState state) {
            VPState vp = (VPState) state;
            if (vp.queueSize() > 0) {
                vp.setQueueSize(vp.queueSize() - 1);
            }
            return StepOutcome.ADVANCED;
        }

        @Override public boolean equals(Object o) {
            return o instanceof ConsumerPollStep that && threadId == that.threadId;
        }

        @Override public int hashCode() { return Objects.hash(threadId); }
    }

    /**
     * Consumer step: process frame and stop (models: decode, draw box, notify, sleep 1s, stop).
     * In real code, this happens once when a barcode is found, then stop() is called.
     */
    static final class ConsumerProcessAndStopStep implements Step {
        private final int threadId;

        public ConsumerProcessAndStopStep(int threadId) { this.threadId = threadId; }

        @Override public Set<MemoryLocation> reads() {
            return Set.of(MemoryLocation.of("consumerRunning"), MemoryLocation.of("stopCalled"));
        }

        @Override public Set<MemoryLocation> writes() {
            return Set.of(
                MemoryLocation.of("consumerRunning"),
                MemoryLocation.of("stopCalled")
            );
        }

        @Override public boolean enabled(SharedState state) {
            return state instanceof VPState vp && vp.consumerRunning() && !vp.stopCalled();
        }

        @Override public StepOutcome execute(SharedState state) {
            VPState vp = (VPState) state;
            vp.setConsumerRunning(false);
            vp.setStopCalled(true); // This triggers producer to stop
            return StepOutcome.ADVANCED;
        }

        @Override public boolean equals(Object o) {
            return o instanceof ConsumerProcessAndStopStep that && threadId == that.threadId;
        }

        @Override public int hashCode() { return Objects.hash(threadId); }
    }

    /**
     * Consumer step: final exit/no-op to allow thread completion after stop.
     * In real code, after stop() the consumer thread exits its while loop.
     */
    static final class ConsumerExitStep implements Step {
        private final int threadId;

        public ConsumerExitStep(int threadId) { this.threadId = threadId; }

        @Override public Set<MemoryLocation> reads() {
            return Set.of(MemoryLocation.of("consumerRunning"), MemoryLocation.of("stopCalled"));
        }

        @Override public Set<MemoryLocation> writes() {
            return Collections.emptySet();
        }

        @Override public boolean enabled(SharedState state) {
            if (!(state instanceof VPState vp)) return false;
            // Enabled when consumer has finished work (not running or stopCalled)
            return !vp.consumerRunning() || vp.stopCalled();
        }

        @Override public StepOutcome execute(SharedState state) {
            // No-op - just advance PC to completion
            return StepOutcome.ADVANCED;
        }

        @Override public boolean equals(Object o) {
            return o instanceof ConsumerExitStep that && threadId == that.threadId;
        }

        @Override public int hashCode() { return Objects.hash(threadId); }
    }

    /**
     * External stop() call - signals both threads to exit and clears queue.
     * Models VideoProcessor.stop() called from windowClosing listener.
     */
    static final class ExternalStopStep implements Step {
        private final int threadId;

        public ExternalStopStep(int threadId) { this.threadId = threadId; }

        @Override public Set<MemoryLocation> reads() {
            return Set.of(
                MemoryLocation.of("producerRunning"),
                MemoryLocation.of("consumerRunning"),
                MemoryLocation.of("queueSize"),
                MemoryLocation.of("stopCalled")
            );
        }

        @Override public Set<MemoryLocation> writes() {
            return Set.of(
                MemoryLocation.of("producerRunning"),
                MemoryLocation.of("consumerRunning"),
                MemoryLocation.of("queueSize"),
                MemoryLocation.of("stopCalled")
            );
        }

        @Override public boolean enabled(SharedState state) {
            if (!(state instanceof VPState vp)) return false;
            return (vp.producerRunning() || vp.consumerRunning()) && !vp.stopCalled();
        }

        @Override public StepOutcome execute(SharedState state) {
            VPState vp = (VPState) state;
            vp.setProducerRunning(false);
            vp.setConsumerRunning(false);
            vp.setQueueSize(0);
            vp.setStopCalled(true);
            return StepOutcome.ADVANCED;
        }

        @Override public boolean equals(Object o) {
            return o instanceof ExternalStopStep that && threadId == that.threadId;
        }

        @Override public int hashCode() { return Objects.hash(threadId); }
    }

    /**
     * Invariant: No deadlock - at least one thread can always make progress
     * unless both have legitimately terminated (stopCalled && !producerRunning && !consumerRunning).
     * Checks program counters to avoid flagging transient states where a thread has enabled steps.
     * Handles both 2-thread (producer+consumer) and 3-thread (with external stop) programs.
     */
    static final Invariant noDeadlockInvariant = (state, config) -> {
        VPState vp = (VPState) state;
        List<Integer> pcs = config.programCounters();

        // Normal termination: stopCalled and both threads stopped
        if (vp.stopCalled() && !vp.producerRunning() && !vp.consumerRunning()) {
            return true;
        }

        // Check if producer has enabled steps
        // Producer PCs: 0,1,2 = grab steps (enabled when producerRunning)
        //             3 = stop step (enabled when producerRunning && stopCalled)
        int producerPc = pcs.get(0);
        boolean producerHasEnabledStep = false;
        if (producerPc <= 2) { // grab steps - now always enabled when producerRunning
            producerHasEnabledStep = vp.producerRunning();
        } else if (producerPc == 3) { // stop step
            producerHasEnabledStep = vp.producerRunning() && vp.stopCalled();
        }

        // Check if consumer has enabled steps
        // Consumer PCs: 0 = poll (enabled when consumerRunning && !stopCalled && (queue>0 || !producerRunning))
        //             1 = process+stop (enabled when consumerRunning && !stopCalled)
        //             2 = exit (enabled when !consumerRunning || stopCalled)
        int consumerPc = pcs.get(1);
        boolean consumerHasEnabledStep = false;
        if (consumerPc == 0) { // poll step
            consumerHasEnabledStep = vp.consumerRunning() && !vp.stopCalled() &&
                                     (vp.queueSize() > 0 || !vp.producerRunning());
        } else if (consumerPc == 1) { // process+stop step
            consumerHasEnabledStep = vp.consumerRunning() && !vp.stopCalled();
        } else if (consumerPc == 2) { // exit step
            consumerHasEnabledStep = !vp.consumerRunning() || vp.stopCalled();
        }

        // Check if external has enabled steps (only in 3-thread program)
        boolean externalHasEnabledStep = false;
        if (pcs.size() > 2) {
            int externalPc = pcs.get(2);
            externalHasEnabledStep = externalPc == 0 &&
                                     (vp.producerRunning() || vp.consumerRunning()) && !vp.stopCalled();
        }

        // Deadlock: no thread has an enabled step, but not all terminated
        boolean anyEnabled = producerHasEnabledStep || consumerHasEnabledStep || externalHasEnabledStep;
        boolean allTerminated = !vp.producerRunning() && !vp.consumerRunning() && vp.stopCalled();

        return anyEnabled || allTerminated;
    };

private Program createProgram() {
        VPState initial = new VPState();

        // Thread 0: Producer - unrolled loop: 3 grab iterations, then stop
        // Models: while(running && !stopCalled) { grab; offer; } if(stopCalled) stop();
        List<Step> producerSteps = List.of(
            new ProducerGrabStep(0),   // PC 0: iteration 1
            new ProducerGrabStep(0),   // PC 1: iteration 2
            new ProducerGrabStep(0),   // PC 2: iteration 3 (overflow)
            new ProducerStopStep(0)    // PC 3: stop when stopCalled
        );

        // Thread 1: Consumer - unrolled: poll (loop), process+stop, exit
        // Models: while(running && !stopCalled && (queue>0 || producerRunning)) { poll; if(got frame) { process; stop(); } } exit;
        List<Step> consumerSteps = List.of(
            new ConsumerPollStep(1),                    // PC 0: poll frame (loop)
            new ConsumerProcessAndStopStep(1),          // PC 1: process frame, call stop()
            new ConsumerExitStep(1)                     // PC 2: exit (no-op, allows completion)
        );

        ModelThread producer = new ModelThread(0, producerSteps);
        ModelThread consumer = new ModelThread(1, consumerSteps);

        return new Program(initial, List.of(producer, consumer));
    }

    // Program with external stop for testing window-close interaction
    private Program createProgramWithExternalStop() {
        VPState initial = new VPState();

        List<Step> producerSteps = List.of(
            new ProducerGrabStep(0),
            new ProducerGrabStep(0),
            new ProducerGrabStep(0),
            new ProducerStopStep(0)
        );

        List<Step> consumerSteps = List.of(
            new ConsumerPollStep(1),
            new ConsumerProcessAndStopStep(1),
            new ConsumerExitStep(1)
        );

        // Thread 2: External stop (window close) - can happen at any time
        List<Step> externalStopSteps = List.of(
            new ExternalStopStep(2)       // PC 0: call stop()
        );

        ModelThread producer = new ModelThread(0, producerSteps);
        ModelThread consumer = new ModelThread(1, consumerSteps);
        ModelThread external = new ModelThread(2, externalStopSteps);

        return new Program(initial, List.of(producer, consumer, external));
    }

    @Test
    void dporFindsNoDeadlockViolation() {
        Program program = createProgram();

        DporExplorer dpor = new DporExplorer();
        DfsResult result = dpor.explore(program, noDeadlockInvariant);

        // Should find no VIOLATION traces (no invariant violation = no deadlock)
        boolean hasViolation = result.traces().stream()
                .anyMatch(t -> t.outcome() == TraceOutcome.VIOLATION);

        assertFalse(hasViolation,
            "DPOR should not find deadlock violations in VideoProcessor model. " +
            "Found traces: " + result.traces());

        // Should explore some states
        assertTrue(result.statesExplored() > 0, "Should explore some states");
    }

    @Test
    void dporExploresMultipleInterleavings() {
        Program program = createProgram();

        DporExplorer dpor = new DporExplorer();
        DfsResult result = dpor.explore(program, noDeadlockInvariant);

        // Verify we explored multiple interleavings
        assertTrue(result.traces().size() > 1,
            "Should explore multiple interleavings. Found: " + result.traces().size());

        // Verify some traces reach completion (both threads terminate normally)
        boolean hasCompleted = result.traces().stream()
                .anyMatch(t -> t.outcome() == TraceOutcome.COMPLETED);
        assertTrue(hasCompleted, "Should have COMPLETED traces: " + result.traces());
    }

    @Test
    void quickCheckWithDefaultStrategy() {
        Program program = createProgram();

        TestResult result = Interleave.quickCheck(program);

        // Should complete without violation
        assertFalse(result.hasViolation(),
            "quickCheck should not find violation: " + result.failingTraces());

        // Should have explored states
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void quickCheckWithDporStrategy() {
        Program program = createProgram();

        TestResult result = Interleave.quickCheck(program, Strategy.DPOR);

        // Should complete without violation
        assertFalse(result.hasViolation(),
            "DPOR quickCheck should not find violation: " + result.failingTraces());

        // Should have explored states
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void runnerReusableAcrossMultipleRuns() {
        Program program = createProgram();

        InterleaveRunner runner = InterleaveRunner.builder()
            .strategy(Strategy.DPOR)
            .maxStates(1000)
            .build();

        TestResult result1 = runner.run(program);
        TestResult result2 = runner.run(program);

        // Both runs should succeed
        assertFalse(result1.hasViolation(), "First run should not have violation");
        assertFalse(result2.hasViolation(), "Second run should not have violation");

        // Results should be consistent
        assertEquals(result1.statesExplored(), result2.statesExplored(),
            "Reusable runner should produce consistent results");
    }

    @Test
    void dporWithExternalStopFindsNoViolation() {
        Program program = createProgramWithExternalStop();

        DporExplorer dpor = new DporExplorer();
        DfsResult result = dpor.explore(program, noDeadlockInvariant);

        // Should find no VIOLATION traces
        boolean hasViolation = result.traces().stream()
                .anyMatch(t -> t.outcome() == TraceOutcome.VIOLATION);

        assertFalse(hasViolation,
            "DPOR with external stop should not find violations. " +
            "Found traces: " + result.traces());

        assertTrue(result.statesExplored() > 0, "Should explore some states");
    }

    // --- Bitstate + Strategy Tests ---

    private DfsResult runWithStore(Program program, Strategy strategy, StateStore store) {
        return switch (strategy) {
            case DFS -> new DfsExplorer().explore(program, noDeadlockInvariant, store, null);
            case STATIC_POR -> new StaticPorExplorer().explore(program, noDeadlockInvariant, store, null);
            case DPOR -> new DporExplorer().explore(program, noDeadlockInvariant, store, null);
        };
    }

    private boolean hasViolation(DfsResult result) {
        return result.traces().stream()
            .anyMatch(t -> t.outcome() == TraceOutcome.VIOLATION);
    }

    @Test
    void dfsExactFindsNoViolation() {
        Program program = createProgram();
        DfsResult result = runWithStore(program, Strategy.DFS, new HashingStateStore());

        assertFalse(hasViolation(result), "DFS + exact should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void dfsBitstateFindsNoViolation() {
        Program program = createProgram();
        DfsResult result = runWithStore(program, Strategy.DFS, new BitstateStore(1_000_003, 4));

        assertFalse(hasViolation(result), "DFS + bitstate should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void staticPorExactFindsNoViolation() {
        Program program = createProgram();
        DfsResult result = runWithStore(program, Strategy.STATIC_POR, new HashingStateStore());

        assertFalse(hasViolation(result), "STATIC_POR + exact should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void staticPorBitstateFindsNoViolation() {
        Program program = createProgram();
        DfsResult result = runWithStore(program, Strategy.STATIC_POR, new BitstateStore(1_000_003, 4));

        assertFalse(hasViolation(result), "STATIC_POR + bitstate should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void dporExactFindsNoViolation() {
        Program program = createProgram();
        DfsResult result = runWithStore(program, Strategy.DPOR, new HashingStateStore());

        assertFalse(hasViolation(result), "DPOR + exact should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void dporBitstateFindsNoViolation() {
        Program program = createProgram();
        DfsResult result = runWithStore(program, Strategy.DPOR, new BitstateStore(1_000_003, 4));

        assertFalse(hasViolation(result), "DPOR + bitstate should find no violation");
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void bitstateMetricsPopulated() {
        Program program = createProgram();
        BitstateStore store = new BitstateStore(1_000_003, 4);

        runWithStore(program, Strategy.DPOR, store);

        double fpr = store.estimatedFalsePositiveRate();
        assertTrue(fpr >= 0.0 && fpr < 1e-6,
            "FPR should be near zero for a small state space: " + fpr);
        assertTrue(store.bitCount() > 0,
            "bitCount should be positive: " + store.bitCount());
        assertTrue(store.bitDensity() > 0.0,
            "bitDensity should be positive: " + store.bitDensity());
    }

    @Test
    void runnerWithBitstateStore() {
        Program program = createProgram();

        InterleaveRunner runner = InterleaveRunner.builder()
            .strategy(Strategy.DPOR)
            .stateStoreFactory(() -> new BitstateStore(1_000_003, 4))
            .maxStates(1000)
            .build();

        TestResult result = runner.run(program);

        assertFalse(result.hasViolation(),
            "Runner with bitstate should find no violation: " + result.failingTraces());
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void bitstateWithExternalStop() {
        Program program = createProgramWithExternalStop();
        DfsResult result = runWithStore(program, Strategy.DPOR, new BitstateStore(1_000_003, 4));

        assertFalse(hasViolation(result),
            "Bitstate with external stop should find no violation. " +
            "Found traces: " + result.traces());
        assertTrue(result.statesExplored() > 0, "Should explore states");
    }

    @Test
    void allStrategiesProduceSameVerdict() {
        Program program = createProgram();

        for (StoreType storeType : StoreType.values()) {
            StateStore store = storeType == StoreType.EXACT
                ? new HashingStateStore()
                : new BitstateStore(1_000_003, 4);

            boolean dfsViol = hasViolation(runWithStore(program, Strategy.DFS, store));
            store = storeType == StoreType.EXACT ? new HashingStateStore() : new BitstateStore(1_000_003, 4);
            boolean porViol = hasViolation(runWithStore(program, Strategy.STATIC_POR, store));
            store = storeType == StoreType.EXACT ? new HashingStateStore() : new BitstateStore(1_000_003, 4);
            boolean dporViol = hasViolation(runWithStore(program, Strategy.DPOR, store));

            assertEquals(dfsViol, porViol,
                "DFS and STATIC_POR should agree on verdict for " + storeType);
            assertEquals(dfsViol, dporViol,
                "DFS and DPOR should agree on verdict for " + storeType);
        }
    }

    enum StoreType { EXACT, BITSTATE }
}