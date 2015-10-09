package flipcut.mincut.cutGraphAPI;

import flipcut.mincut.cutGraphAPI.bipartition.BasicCut;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by fleisch on 07.05.15.
 */
public abstract class MaxFlowCutGraph<V> implements DirectedCutGraph<V> {
    private ExecutorService executorService;
    private int threads;

    final ArrayList<V> sToCalculate = new ArrayList<>();
    final ArrayList<V> tToCalculate = new ArrayList<>();


    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void submitSTCutCalculation(V source, V sink) {
        sToCalculate.add(source);
        tToCalculate.add(sink);
    }

    @Override
    public List<BasicCut<V>> calculateMinSTCuts() {//todo parralelize if really used
        List<BasicCut<V>> stCuts = new LinkedList<>();
        final int max = sToCalculate.size();
        for (int i = 0; i < max; i++) {
            stCuts.add(
                    calculateMinSTCut(
                            sToCalculate.get(i),
                            tToCalculate.get(i)));
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
        final int max = sToCalculate.size();
        for (int i = 0; i < max; i++) {
            BasicCut<V> next = calculateMinSTCut(
                    sToCalculate.get(i),
                    tToCalculate.get(i));
            if (next.minCutValue < cut.minCutValue)
                cut = next;
        }
        return cut;
    }

    private BasicCut<V> calculatMinCutParallel() throws ExecutionException, InterruptedException {
        final ArrayList<Future<BasicCut<V>>> busyMaxFlow = new ArrayList<>(threads);

        final int size = sToCalculate.size();
        final int bigparts = size % threads;
        final int minSize =  size / threads;


        int start = 0;
        //submit worker
        for (int i = 0; i < bigparts; i++) {
            int stop = start + minSize + 1;
            MaxFlowCallable callable = createCallable(start,stop);
            busyMaxFlow.add(executorService.submit(callable));
            start = stop;
        }
        for (int i = bigparts; i < threads; i++) {
            int stop = start + minSize;
            MaxFlowCallable callable = createCallable(start,stop);
            busyMaxFlow.add(executorService.submit(callable));
            start = stop;
        }

        BasicCut<V> cut = BasicCut.MAX_CUT_DUMMY;
        for (Future<BasicCut<V>> future : busyMaxFlow) {
            BasicCut<V> next = future.get();
            if (next.minCutValue < cut.minCutValue)
                cut = next;
        }

        return cut;
    }

    abstract MaxFlowCallable createCallable(int start, int stop);

    @Override
    public void clear() {
        sToCalculate.clear();
        tToCalculate.clear();
    }

    abstract class MaxFlowCallable implements Callable<BasicCut<V>> {
        private final int start;
        private final int stop;

        MaxFlowCallable(int start, int stop) {
            this.start = start;
            this.stop = stop;
        }

        abstract void initGraph();
        abstract BasicCut<V> calculate(V source, V sink);

        @Override
        public BasicCut<V> call() throws Exception {
            initGraph();
            BasicCut<V> best = BasicCut.MAX_CUT_DUMMY;

            for (int i = start; i < stop; i++) {
                BasicCut<V> next = calculate(sToCalculate.get(i), tToCalculate.get(i));
                if (next.minCutValue < best.minCutValue)
                    best = next;
            }
            return best;
        }
    }
}
