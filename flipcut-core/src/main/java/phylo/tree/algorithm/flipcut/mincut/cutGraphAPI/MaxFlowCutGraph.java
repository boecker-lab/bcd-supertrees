package phylo.tree.algorithm.flipcut.mincut.cutGraphAPI;

import core.utils.parallel.DefaultIterationCallable;
import core.utils.parallel.IterationCallableFactory;
import core.utils.parallel.ParallelUtils;
import phylo.tree.algorithm.flipcut.mincut.cutGraphAPI.bipartition.STCut;

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
    public List<STCut<V>> calculateMinSTCuts() {//todo palatalize if really used
        List<STCut<V>> stCuts = new LinkedList<>();
        final int max = stToCalculate.size();
        for (int i = 0; i < max; i++) {
            SS st = stToCalculate.get(i);
            stCuts.add(
                    calculateMinSTCut(st.source, st.sink));
        }
        return stCuts;
    }

    @Override
    public STCut<V> calculateMinCut() throws ExecutionException, InterruptedException {
        if (threads == 1 || executorService == null) {
            return calculatMinCutSingle();
        } else {
            return calculatMinCutParallel(); //minus 1 because it is daster if 1 thread is left for other things
        }
    }

    private STCut<V> calculatMinCutSingle() {
        STCut<V> cut = STCut.MAX_CUT_DUMMY;
        final int max = stToCalculate.size();
        for (int i = 0; i < max; i++) {
            SS st = stToCalculate.get(i);
            STCut<V> next = calculateMinSTCut(
                    st.source,
                    st.sink);
            if (next.minCutValue < cut.minCutValue())
                cut = next;
        }
        return cut;
    }

    private STCut<V> calculatMinCutParallel() throws ExecutionException, InterruptedException {

        final List<Future<List<STCut<V>>>> busyMaxFlow = ParallelUtils.parallelBucketForEach(executorService, getMaxFlowCallableFactory(), stToCalculate, threads);

        STCut<V> cut = STCut.MAX_CUT_DUMMY;
        for (Future<List<STCut<V>>> future : busyMaxFlow) {
            STCut<V> next = future.get().get(0);
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

    abstract class MaxFlowCallable extends DefaultIterationCallable<SS, STCut<V>> {
        MaxFlowCallable(List<SS> jobs) {
            super(jobs);
        }

        abstract void initGraph();


        @Override
        public List<STCut<V>> call() throws Exception {
            initGraph();
            STCut<V> best = STCut.MAX_CUT_DUMMY;

            for (SS job : jobs) {
                STCut<V> next = doJob(job);
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
