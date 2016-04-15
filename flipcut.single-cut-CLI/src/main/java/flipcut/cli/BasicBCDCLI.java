package flipcut.cli;

import cli.EnumArrayOptionHandler;
import cli.Multithreaded;
import cli.Progressbar;
import flipcut.AbstractFlipCut;
import flipcut.costComputer.FlipCutWeights;
import flipcut.flipCutGraph.CutGraphCutter;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.ExplicitBooleanOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import phyloTree.SupertreeAlgortihmCLI;
import phyloTree.model.tree.Tree;
import scm.algorithm.AbstractSCMAlgorithm;
import scm.algorithm.GreedySCMAlgorithm;
import scm.algorithm.MultiGreedySCMAlgorithm;
import scm.algorithm.RandomizedSCMAlgorithm;
import scm.algorithm.treeSelector.TreeScorers;
import scm.cli.SCMCLI;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import static org.kohsuke.args4j.ExampleMode.ALL;
import static org.kohsuke.args4j.OptionHandlerFilter.PUBLIC;

/**
 * Created by fleisch on 20.11.15.
 */
public abstract class BasicBCDCLI<A extends AbstractFlipCut> extends SupertreeAlgortihmCLI<A> implements Multithreaded, Progressbar {
    public BasicBCDCLI(String appHomeParent, String appHomeFolderName, String logDir, int maxLogFileSize, int logRotation) {
        super(appHomeParent, appHomeFolderName, logDir, maxLogFileSize, logRotation);
    }

    public BasicBCDCLI(Path propertiesFile) {
        super(propertiesFile);
    }


    @Argument(usage = "Path of the file containing the guide tree", index = 1, required = false)
    private Path inputSCM = null;

    public Path getSCMInputFile() {
        if (inputSCM == null)
            return null;
        if(this.inputSCMFile == null) {
            this.inputSCMFile = this.workingPath.resolve(this.inputSCM);
        }
        return this.inputSCMFile;
    }

    private Path inputSCMFile = null;

    public enum Algorithm {BCD, FC}

    public enum SuppportedWeights {UNIT_WEIGHT, TREE_WEIGHT, BRANCH_LENGTH, BOOTSTRAP_VALUES, LEVEL, BRANCH_AND_LEVEL, BOOTSTRAP_AND_LEVEL}

    protected final EnumMap<SuppportedWeights, FlipCutWeights.Weights> weightMapping = initWheightMapping();

    protected abstract EnumMap<SuppportedWeights, FlipCutWeights.Weights> initWheightMapping();

    //##### flip cut parameter #####
    //cut graph type
    @Option(name = "-c", aliases = "--cutGraphImplementation", usage = "Choose the graph structure used for the mincut calculation", hidden = true)
    public abstract void setGraphType(CutGraphCutter.CutGraphTypes graphType);

    //FlipCut or BCD
    @Option(name = "-a", aliases = "--algorithm", usage = "The algorithm that is used to estimate the Supertree.\n BCD = BadCharacterDeletion supertrees\n FC = FlipCut supertrees.", hidden = true)
    public void setAlgorithm(Algorithm algo) {
        if (algo == Algorithm.FC)
            setGraphType(CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
        else
            setGraphType(CutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
    }//default value is true

    //Weighting
    @Option(name = "-w", aliases = "--weighting", usage = "Weighting strategy", forbids = "-W")
    public void setWeights(SuppportedWeights weighting) {
        if (weighting != null) {
            FlipCutWeights.Weights tmp = weightMapping.get(weighting);
            if (tmp != null) {
                setWeights(tmp);
            }
        }
    }

    @Option(name = "-W", aliases = "--internalWeighting", usage = "Weighting with internal code including nonofficial weightings", forbids = "-w", hidden = true)
    public void setWeights(FlipCutWeights.Weights weights) {
        this.weights = weights;
    }//default value is UW

    public FlipCutWeights.Weights getWeights() {
        return weights;
    }

    private FlipCutWeights.Weights weights = FlipCutWeights.Weights.UNIT_COST;

    @Option(name = "-b", aliases = "--bootstrapThreshold", usage = "Minimal bootstrap value of a tree-node to be considered during the supertree calculation")
    private int bootstrapThreshold = 0;

    public int getBootstrapThreshold() {
        return bootstrapThreshold;
    }


    //##### command line parameter #####
    @Option(name = "-s", aliases = "--scm", handler = ExplicitBooleanOptionHandler.class, usage = "Use SCM-tree as guide tree", forbids = {"-S", "-SR"})
    public boolean useSCM = true;  //default value has to be true because of -S

    @Option(name = "-S", aliases = "--scmScoring", usage = "Enable the usage of a GSCM-tree as guide tree and specify the scorings that will be used to construct this GSCM-tree", forbids = "-s", hidden = true, handler = SCMCLI.ScorerTypeArrayOptionHandler.class)
    private TreeScorers.ScorerType[] scorerTypes = new TreeScorers.ScorerType[]{TreeScorers.ScorerType.COLLISION_SUBTREES, TreeScorers.ScorerType.UNIQUE_CLADE_RATE, TreeScorers.ScorerType.UNIQUE_CLADES_LOST};
    public void setScorerTypes(TreeScorers.ScorerType[] scorerTypes) {
        this.scorerTypes = scorerTypes;
    }



    @Option(name = "-Sr", aliases = {"--scmRandomized"}, usage = "Enables randomization (standard iterations are numberOfTrees^2 per scoring)", forbids = {"-s", "-SR"}, hidden = true)
    private void setRandomized(boolean r) {
//        useSCM = true;
        if (r) {
            randomIterations = 0;
        } else {
            randomIterations = -1;
        }
    }

    @Option(name = "-SR", aliases = {"--scmRandomIterations"}, usage = "Enables randomization and specifies the number of iterations per scoring", forbids = {"-s", "-Sr"}, hidden = true)
    private void setRandomIterations(int iter) {
//        useSCM = true;
        if (iter < 0) {
            randomIterations = -1;
        } else {
            randomIterations = iter;
        }
    }

    private int randomIterations = -1;
    private final boolean supportTree =  false;
    public boolean useSupportTree() {
        return supportTree;
    }

    public AbstractSCMAlgorithm getSCMInstance() {
        AbstractSCMAlgorithm algo = null;
        if (randomIterations < 0) {
            //non random
            if (scorerTypes.length > 1) {
                //multi
                algo = new MultiGreedySCMAlgorithm(TreeScorers.getScorerArray(isMultiThreaded(), scorerTypes));
            } else {
                //standard
                algo = new GreedySCMAlgorithm(TreeScorers.getScorer(isMultiThreaded(), scorerTypes[0]));
            }
        } else {
            //randomized
            algo = new RandomizedSCMAlgorithm(randomIterations, TreeScorers.getScorerArray(isMultiThreaded(), scorerTypes));
        }
        algo.setThreads(numberOfThreads);
        return algo;
    }

   /* @Option(name = "-R", aliases = "--scmIterations", usage = "Enables randomization and specifies the number of iterations per scoring for gscm algorithm", hidden = true)
    public int scmiterations = 25;*/


    //pre and post procesing stuff
    @Option(name = "-n", aliases = "--ucr", usage = "Run unsupported clade reduction post-processing", hidden = true)
    public boolean unsupportedCladeReduction = false;

    @Option(name = "-u", aliases = "--uds", usage = "Run undisputed sibling pre-processing", hidden = true)
    public boolean removeUndisputedSiblings = false;

    @Option(name = "-r", aliases = "--noRooting", usage = "Do not optimize fake roots of unrooted input trees based on Guide tree", hidden = true)
    public boolean noRootingOptimization = false;

    @Option(name = "-i", aliases = "--insufficient", usage = "Skip if input trees have insufficient taxa overlap", hidden = true)
    public boolean skipInsufficientOverlapInstances = false; //todo warn if taxa have insufficient taxa overlap taxa overlap check

    private int numberOfThreads = 0;

    @Override
    public void setNumberOfThreads(int i) {
        numberOfThreads = i;
    }

    @Override
    public int getNumberOfThreads() {
        return numberOfThreads;
    }





    /*public void writeOutput(Tree primaryResult, List<Tree> multiTreeList) throws IOException {
        List<Tree> results;
        if (appendUnmerged && multiTreeList.size() > 1) {
            results = new ArrayList<>(multiTreeList.size() + 1);
            results.add(primaryResult);
            results.addAll(multiTreeList);
        } else {
            results = Arrays.asList(primaryResult);
        }
        writeOutput(results);
    }

    public AbstractSCMAlgorithm getAlgorithmInstance() {
        AbstractSCMAlgorithm algo = null;
        if (randomIterations < 0) {
            //non random
            if (scorerTypes.length > 1) {
                //multi
                algo = new MultiGreedySCMAlgorithm(TreeScorers.getScorerArray(isMultiThreaded(), scorerTypes));
            } else {
                //standard
                algo = new GreedySCMAlgorithm(TreeScorers.getScorer(isMultiThreaded(), scorerTypes[0]));
            }
        } else {
            //randomized
            algo = new RandomizedSCMAlgorithm(randomIterations, TreeScorers.getScorerArray(isMultiThreaded(), scorerTypes));
        }
        setParameters(algo);
        return algo;
    }

    @Override
    public void setParameters(A scmAlgorithm) {
        A.setThreads(getNumberOfThreads());
    }*/


}
