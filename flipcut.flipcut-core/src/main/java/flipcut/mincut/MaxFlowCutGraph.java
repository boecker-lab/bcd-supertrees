package flipcut.mincut;

import flipcut.mincut.bipartition.BasicCut;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by fleisch on 07.05.15.
 */
public abstract class MaxFlowCutGraph<V> implements DirectedCutGraph<V>{
    protected ExecutorService executorService;
    protected static int CORES_AVAILABLE =  Runtime.getRuntime().availableProcessors();
    protected int threads;

    protected Queue<Future<BasicCut<V>>> busyMaxFlow;
    protected Queue<SS> stToCalculate = new LinkedBlockingQueue<>();


    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
    public void setThreads(int threads) {
        this.threads = threads;
    }


    public void submitSTCutCalculation(V source, V sink) {
        stToCalculate.offer(new SS(source, sink));
    }

    @Override
    public List<BasicCut<V>> calculateMinSTCuts() {
        List<BasicCut<V>> stCuts = new LinkedList<>();
        while (!stToCalculate.isEmpty()) {
            SS st = stToCalculate.poll();
            stCuts.add(
                    calculateMinSTCut(st.source, st.sink));
        }
        return stCuts;
    }

    @Override
    public BasicCut<V> calculateMinCut() throws ExecutionException, InterruptedException {
        if (threads == 1 || CORES_AVAILABLE == 1) {
            return calculatMinCutSingle();
        } else if (threads == 0) {
            return calculatMinCutParallel((CORES_AVAILABLE / 2) - 1); // find a HT solution
        } else {
            return calculatMinCutParallel(threads - 1); //minus 1 because it is daster if 1 thread is left for other things
        }

    }

    protected BasicCut<V> calculatMinCutSingle() {
        BasicCut<V> cut = BasicCut.MAX_CUT_DUMMY;
        while (!stToCalculate.isEmpty()) {
            SS st = stToCalculate.poll();
            BasicCut<V> next = calculateMinSTCut(st.source, st.sink);
            if (next.minCutValue < cut.minCutValue)
                cut=next;
        }
        return cut;
    }

    protected BasicCut<V> calculatMinCutParallel(int threads) throws ExecutionException, InterruptedException {
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(threads);
        }

        busyMaxFlow = new ArrayBlockingQueue<>(stToCalculate.size());
        BasicCut<V> cut = BasicCut.MAX_CUT_DUMMY;

        int index = 0;
        while (index < threads && !stToCalculate.isEmpty()) {
            SS ss = stToCalculate.poll();
            MaxFlowCallable callable = createCallable();
            callable.setSourceAndSink(ss);
            busyMaxFlow.offer(executorService.submit(callable));
            index++;
        }

        while (!busyMaxFlow.isEmpty()) {
            Future<BasicCut<V>> f = busyMaxFlow.poll();
            BasicCut<V> next = f.get();
            if (next.minCutValue < cut.minCutValue)
                cut = next;
        }
        return cut;
    }


    protected abstract MaxFlowCallable createCallable();

    @Override
    public void clear() {
        stToCalculate.clear();
    }


    protected abstract class MaxFlowCallable implements Callable<BasicCut<V>> {
        protected V source;
        protected V sink;

        public void setSource(V source) {
            this.source = source;
        }

        public void setSink(V sink) {
            this.sink = sink;
        }

        public void setSourceAndSink(SS sourceAndSink) {
            this.source = sourceAndSink.source;
            this.sink = sourceAndSink.sink;
        }
    }

    public class SS {
        public final  V source;
        public final  V sink;

        public SS(V source, V sink) {
            this.source = source;
            this.sink = sink;
        }
    }

}
