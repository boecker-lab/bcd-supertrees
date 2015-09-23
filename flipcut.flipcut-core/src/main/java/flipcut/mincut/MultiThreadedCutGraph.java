package flipcut.mincut;

import flipcut.mincut.bipartition.BasicCut;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Created by fleisch on 07.05.15.
 */
public abstract class MultiThreadedCutGraph<V> implements CutGraph<V>{
    protected ExecutorService executorService;
    protected static int CORES_AVAILABLE =  Runtime.getRuntime().availableProcessors();
    protected int threads;


    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }




}
