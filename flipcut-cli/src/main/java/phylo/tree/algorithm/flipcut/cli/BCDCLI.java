package phylo.tree.algorithm.flipcut.cli;

import phylo.tree.algorithm.flipcut.AbstractFlipCut;
import phylo.tree.algorithm.flipcut.FlipCutSingleCutSimpleWeight;
import phylo.tree.algorithm.flipcut.costComputer.FlipCutWeights;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphCutter;
import phylo.tree.algorithm.consensus.Consensus;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.SimpleCutGraphCutter;
import phylo.tree.algorithm.flipcut.flipCutGraph.SimpleCutterFactories;
import phylo.tree.io.TreeFileUtils;
import phylo.tree.model.Tree;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;

/**
 * Created by fleisch on 03.02.16.
 */
public class BCDCLI<A extends AbstractFlipCut> extends BasicBCDCLI<A> {

    public BCDCLI(String appHomeParent, String appHomeFolderName, String logDir, int maxLogFileSize, int logRotation) {
        super(appHomeParent, appHomeFolderName, logDir, maxLogFileSize, logRotation);
    }

    public BCDCLI(InputStream propertiesFile) {
        super(propertiesFile);
    }

    static {
        initName("bcd");
        DEFAULT_PROPERTIES.setProperty("APP_HOME_PARENT", System.getProperty("user.home"));
    }


    //##### flip cut parameter #####
    //this is a simple mapping from experimental weighting names to public naming
    @Override
    protected final EnumMap<SuppportedWeights, FlipCutWeights.Weights> initWheightMapping() {
        final EnumMap<SuppportedWeights, FlipCutWeights.Weights> weightMapping = new EnumMap<>(BasicBCDCLI.SuppportedWeights.class);
        weightMapping.put(SuppportedWeights.UNIT_WEIGHT, FlipCutWeights.Weights.UNIT_COST);
        weightMapping.put(SuppportedWeights.TREE_WEIGHT, FlipCutWeights.Weights.TREE_WEIGHT);
        weightMapping.put(SuppportedWeights.BRANCH_LENGTH, FlipCutWeights.Weights.EDGE_WEIGHTS);
        weightMapping.put(SuppportedWeights.BOOTSTRAP_WEIGHT, FlipCutWeights.Weights.BOOTSTRAP_VALUES);
        weightMapping.put(SuppportedWeights.BOOTSTRAP_LOG, FlipCutWeights.Weights.BOOTSTRAP_LOG_PROPS);
        weightMapping.put(SuppportedWeights.LEVEL, FlipCutWeights.Weights.NODE_LEVEL);
        weightMapping.put(SuppportedWeights.BRANCH_AND_LEVEL, FlipCutWeights.Weights.EDGE_AND_LEVEL);
        weightMapping.put(SuppportedWeights.BOOTSTRAP_AND_LEVEL, FlipCutWeights.Weights.BOOTSTRAP_AND_LEVEL);

        return weightMapping;
    }

    //cut graph type
    SimpleCutGraphCutter.CutGraphTypes graphType = SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG;

    @Override
    public void setGraphType(SimpleCutGraphCutter.CutGraphTypes graphType) {
        this.graphType = graphType;
    } //deafault has to be null

    public SimpleCutGraphCutter.CutGraphTypes getGraphType() {
        return graphType;
    } //deafault has to be null


    //##### Methods ####
    private void checkForNexus() throws IOException {
        //TOdo: parse parameters from our own NEXUS block see below for usage
    }


    @Override
    protected void printUsage(PrintStream stream) {
        stream.println("Usage:");
        stream.println(" " + name() + " [options...] INPUT_TREE_FILE");
        stream.println("    The only required argument is the input tree file");
        stream.println();
        stream.println(" " + name() + " [options...] INPUT_TREE_FILE GUIDE_TREE_FILE");
        stream.println("    Additionally, a guide tree can be specified. Otherwise the GSCM tree will be calculated as default guide tree");
        stream.println();
//        stream.println(" " + NAME + " NEXUS_FILE");
//        stream.println("    Use nexus file for options and input data");
//        stream.println();
//        stream.println(" " + NAME + " NEXUS_FILE TREE_FILE");
//        stream.println("    Use nexus file for options and an additional input tree file");
//        stream.println();
//        stream.println(" " + NAME + " NEXUS_FILE TREE_FILE SCM_TREE_FILE");
//        stream.println("    Use nexus file for options, an additional input tree file and specify a scm tree");
    }

    @Override
    public void setParameters(A algo) {
        algo.setNumberOfThreads(getNumberOfThreads());
        algo.setPrintProgress(isProgressBar());
        algo.setBootstrapThreshold(getBootstrapThreshold());
        algo.setWeights(getWeights());
    }

    @Override
    public A createAlgorithmInstance() {
        A algo = (A) new FlipCutSingleCutSimpleWeight();
        algo.setCutter(SimpleCutterFactories.newInstance(getGraphType()));
        setParameters(algo);
        return algo;
    }



    public Tree parseSCM() throws IOException {
        if (getSCMInputFile() == null)
            return null;

        LOGGER.info("Reading guide tree input File...");
        Tree[] result = TreeFileUtils.parseFileToTrees(getSCMInputFile(), this.inputType);
        if (result == null) {
            throw new FileNotFoundException("ERROR: Unknown input file extension. Please specify the correct input file type (--fileType) or use a typical file extension for NEWICK (tree|TREE|tre|TRE|phy|PHY|nwk|NWK) or NEXUS (nex|NEX|ne|NE|nexus|NEXUS)");
        } else if (result.length < 1) {
            throw new IOException("ERROR: No Tree in input file or wrong file type detected. Please specify the correct input file type (--fileType) or use a typical file extension for NEWICK (tree|TREE|tre|TRE|phy|PHY|nwk|NWK) or NEXUS (nex|NEX|ne|NE|nexus|NEXUS)");
        } else {
            Tree scm;
            if (result.length > 1) {
                LOGGER.info("...more than 1 guide tree found. Calculating semi-strict consensus to merge them... ");
                scm = Consensus.getLoosConsensus(Arrays.asList(result));
            } else {
                scm = result[0];
            }
            LOGGER.info("...guide tree(s) successful parsed! ");
            return scm;
        }
    }

}
