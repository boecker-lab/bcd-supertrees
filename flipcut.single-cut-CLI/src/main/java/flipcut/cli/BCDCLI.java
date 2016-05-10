package flipcut.cli;

import flipcut.FlipCutSingleCutSimpleWeight;
import flipcut.costComputer.FlipCutWeights;
import flipcut.flipCutGraph.CutGraphCutter;
import phylo.tree.algorithm.consensus.Consensus;
import phylo.tree.io.TreeFileUtils;
import phylo.tree.model.tree.Tree;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;

/**
 * Created by fleisch on 03.02.16.
 */
public class BCDCLI extends BasicBCDCLI<FlipCutSingleCutSimpleWeight> {

    public BCDCLI(String appHomeParent, String appHomeFolderName, String logDir, int maxLogFileSize, int logRotation) {
        super(appHomeParent, appHomeFolderName, logDir, maxLogFileSize, logRotation);
    }

    public BCDCLI(Path propertiesFile) {
        super(propertiesFile);
    }

    static {
        initName("bcd");
        DEFAULT_PROPERTIES.setProperty("APP_HOME_PARENT", System.getProperty("user.home"));
    }


    //##### flip cut parameter #####
    //this is a simple mapping from experimental weighting names to public naming
    @Override
    protected final EnumMap<BasicBCDCLI.SuppportedWeights, FlipCutWeights.Weights> initWheightMapping() {
        final EnumMap<BasicBCDCLI.SuppportedWeights, FlipCutWeights.Weights> weightMapping = new EnumMap<>(BasicBCDCLI.SuppportedWeights.class);
        weightMapping.put(BasicBCDCLI.SuppportedWeights.UNIT_WEIGHT, FlipCutWeights.Weights.UNIT_COST);
        weightMapping.put(BasicBCDCLI.SuppportedWeights.TREE_WEIGHT, FlipCutWeights.Weights.TREE_WEIGHT);
        weightMapping.put(BasicBCDCLI.SuppportedWeights.BRANCH_LENGTH, FlipCutWeights.Weights.EDGE_WEIGHTS);
        weightMapping.put(BasicBCDCLI.SuppportedWeights.BOOTSTRAP_VALUES, FlipCutWeights.Weights.BOOTSTRAP_VALUES);
        weightMapping.put(BasicBCDCLI.SuppportedWeights.LEVEL, FlipCutWeights.Weights.NODE_LEVEL);
        weightMapping.put(BasicBCDCLI.SuppportedWeights.BRANCH_AND_LEVEL, FlipCutWeights.Weights.EDGE_AND_LEVEL);
        weightMapping.put(BasicBCDCLI.SuppportedWeights.BOOTSTRAP_AND_LEVEL, FlipCutWeights.Weights.BOOTSTRAP_AND_LEVEL);

        return weightMapping;
    }

    //cut graph type
    CutGraphCutter.CutGraphTypes graphType = CutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG;

    @Override
    public void setGraphType(CutGraphCutter.CutGraphTypes graphType) {
        this.graphType = graphType;
    } //deafault has to be null

    public CutGraphCutter.CutGraphTypes getGraphType() {
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
        stream.println("    The only required argument is the input tree file in newick format");
        stream.println();
        stream.println(" " + name() + " [options...] INPUT_TREE_FILE GUIDE_TREE_FILE");
        stream.println("    Additionally, a guide tree can be specified. Otherwise SCM tree gets calculated as default guide tree");
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
    public void setParameters(FlipCutSingleCutSimpleWeight algo) {
        algo.setNumberOfThreads(getNumberOfThreads());
        algo.setPrintProgress(isProgressBar());
        algo.setBootstrapThreshold(getBootstrapThreshold());
        algo.setCutter(getGraphType());
        algo.setWeights(getWeights());
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
