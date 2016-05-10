package flipcut.mincut.cutGraphAPI;

import core.utils.parallel.DefaultIterationCallable;
import core.utils.parallel.IterationCallableFactory;
import core.utils.parallel.ParallelUtils;
import flipcut.mincut.cutGraphAPI.bipartition.BasicCut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by fleisch on 07.05.15.
 */
public abstract class MaxFlowCutGraph<V> implements DirectedCutGraph<V> {
    private ExecutorService executorService;
    private int threads;

    final ArrayList<SS> stToCalculate = new ArrayList<>();


    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void submitSTCutCalculation(V source, V sink) {
        stToCalculate.add(new SS(source, sink));
    }

    @Override
    public List<BasicCut<V>> calculateMinSTCuts() {//todo palatalize if really used
        List<BasicCut<V>> stCuts = new LinkedList<>();
        final int max = stToCalculate.size();
        for (int i = 0; i < max; i++) {
            SS st = stToCalculate.get(i);
            stCuts.add(
                    calculateMinSTCut(st.source, st.sink));
        }
        return stCuts;
    }

    @Override
    public BasicCut<V> calculateMinCut() throws ExecutionException, InterruptedException {
        if (threads == 1 || executorService == null) {
            return calculatMinCutSingle();
        } else {
            return calculatMinCutParallel(); //minus 1 because it is daster if 1 thread is left for other things
        }
    }

    private BasicCut<V> calculatMinCutSingle() {
        BasicCut<V> cut = BasicCut.MAX_CUT_DUMMY;
        final int max = stToCalculate.size();
        for (int i = 0; i < max; i++) {
            SS st = stToCalculate.get(i);
            BasicCut<V> next = calculateMinSTCut(
                    st.source,
                    st.sink);
            if (next.minCutValue < cut.minCutValue)
                cut = next;
        }
        return cut;
    }

    private BasicCut<V> calculatMinCutParallel() throws ExecutionException, InterruptedException {

        final List<Future<List<BasicCut<V>>>> busyMaxFlow = ParallelUtils.parallelBucketForEach(executorService, getMaxFlowCallableFactory(), stToCalculate, threads);

        BasicCut<V> cut = BasicCut.MAX_CUT_DUMMY;
        for (Future<List<BasicCut<V>>> future : busyMaxFlow) {
            BasicCut<V> next = future.get().get(0);
            if (next.minCutValue < cut.minCutValue)
                cut = next;
        }

        return cut;
    }

    @Override
    public void clear() {
        stToCalculate.clear();
    }

    abstract <T extends MaxFlowCallable> IterationCallableFactory<T, SS> getMaxFlowCallableFactory();

    abstract class MaxFlowCallable extends DefaultIterationCallable<SS, BasicCut<V>> {
        MaxFlowCallable(List<SS> jobs) {
            super(jobs);
        }

        abstract void initGraph();


        @Override
        public List<BasicCut<V>> call() throws Exception {
            initGraph();
            BasicCut<V> best = BasicCut.MAX_CUT_DUMMY;

            for (SS job : jobs) {
                BasicCut<V> next = doJob(job);
                if (next.minCutValue < best.minCutValue)
                    best = next;
            }
            return Arrays.asList(best);
        }
    }

    class SS {
        final V source;
        final V sink;

        public SS(V source, V sink) {
            this.source = source;
            this.sink = sink;
        }
    }
}
