package flipCut;

//import epos.model.tree.Tree;
import epos.model.tree.Tree;
import flipCut.costComputer.CostComputer;
import flipCut.costComputer.FlipCutWeights;
import flipCut.flipCutGraph.AbstractFlipCutGraph;
import flipCut.flipCutGraph.AbstractFlipCutNode;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:10
 */
public abstract class AbstractFilpCut<N extends AbstractFlipCutNode<N>,T extends AbstractFlipCutGraph<N>> {
    protected static final boolean DEBUG = false;
    //postprocess that deletes clade of the supertree without support from the input trees

    /**
     * Use edge weights
     */
    protected FlipCutWeights.Weights weights = FlipCutWeights.Weights.EDGE_WEIGHTS;
    /**
     * Logger
     */
    protected Logger log;
    /**
     * Verbose logs
     */
    protected boolean verbose = false;
    /**
     * Turn on/of merging of identical character nodes
     */
    protected boolean removeDuplets = false;
    /**
     * Set minimum bootstrap value of a clade to be part of the analysis ()
     */
    protected double bootstrapThreshold = 0d;
    /**
     * The Graph actual working on
     */
    protected T currentGraph;

    protected AbstractFilpCut() {
        this(Logger.getLogger(AbstractFilpCut.class));
    }

    /**
     * Create new instace with logger
     *
     * @param log the logger
     */
    protected AbstractFilpCut(Logger log) {
        this.log = log;
    }


    /**
     * Provide lazy access to the log
     * @return log the log
     */
    public Logger getLog(){
        if(log == null){
            log = Logger.getLogger(getClass());
        }
        return log;
    }

    public void setRemoveDuplets(boolean removeDuplets) {
        this.removeDuplets = removeDuplets;
    }

    /**
     * Activate verbose log output
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Sets the edge weights function
     *
     * @param weights the weighting scheme
     */
    public void setWeights(FlipCutWeights.Weights weights) {
        this.weights = weights;
    }

    public void setInputTrees(List<Tree> inputTrees) {
        setInputTrees(inputTrees,null);
    }

    public void setInputTrees(List<Tree> inputTrees, Tree scaffoldTree) {
        final CostComputer costs = initCosts(inputTrees, scaffoldTree);

        currentGraph = createInitGraph(costs, bootstrapThreshold);
    }

    public void setBootstrapThreshold(double bootstrapThreshold) {
        this.bootstrapThreshold = bootstrapThreshold;
    }

    //abstract stuff!
    public abstract List<Tree> getSupertrees();
    protected abstract T createInitGraph(CostComputer costsComputer, double bootstrapThreshold);
    protected abstract CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree);


}