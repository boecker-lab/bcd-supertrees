package phylo.tree.algorithm.flipcut;

import org.slf4j.Logger;
import phylo.tree.algorithm.SupertreeAlgorithm;
import phylo.tree.algorithm.flipcut.cutter.CutterFactory;
import phylo.tree.algorithm.flipcut.cutter.GraphCutter;
import phylo.tree.model.Tree;

import java.util.List;
import java.util.concurrent.ExecutorService;


/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:10
 */
public abstract class AbstractFlipCut<S, T extends SourceTreeGraph<S>, C extends GraphCutter<S>, F extends CutterFactory<C, S, T>> extends SupertreeAlgorithm {
    public static final boolean DEBUG = false;
    public static final int CORES_AVAILABLE = Runtime.getRuntime().availableProcessors();
    /**
     * number of thread that should be used 0 {@literal ->} automatic {@literal <} 0 massive parralel
     */
    protected int numberOfThreads = 0;

    /**
     * number of thread that should be used 0 {@literal ->} automatic
     */
    protected boolean printProgress = false;


    protected F type;
    /**
     * The Graph actual working on
     */
    protected T initialGraph;


    protected AbstractFlipCut() {
        super();
    }

    /**
     * Create new instace with logger
     */
    protected AbstractFlipCut(F type) {
        super();
        this.type = type;
    }

    /**
     * Create new instace with logger
     *
     * @param log the logger
     */
    protected AbstractFlipCut(Logger log, F type) {
        super(log);
        this.type = type;
    }

    /**
     * Create new instace with logger and global Executor service
     *
     * @param log the logger
     */
    protected AbstractFlipCut(Logger log, ExecutorService executorService, F type) {
        super(log, executorService);
        this.type = type;
    }

    public void setPrintProgress(boolean printProgress) {
        this.printProgress = printProgress;
    }

    public boolean isPrintProgress() {
        return printProgress;
    }


    public void setInput(T initGraph) {
        this.initialGraph = initGraph;
    }

    @Override
    public void setInput(List<Tree> input) {
        try {
            throw new NoSuchMethodException("Method not supported");
        } catch (NoSuchMethodException e) {
            LOGGER.warn(e.toString());
        }
    }

    public void setCutter(F type) {
        this.type = type;
    }

    public F getCutterType() {
        return type;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }


}
