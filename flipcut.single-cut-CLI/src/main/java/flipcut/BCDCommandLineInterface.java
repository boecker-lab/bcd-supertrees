package flipcut; /**
 * Created by martin-laptop on 25.02.14.
 */

import flipcut.clo.FlipCutCLO;
import epos.model.tree.Tree;
import epos.model.tree.io.SimpleNexus;
import flipcut.costComputer.FlipCutWeights;
import flipcut.flipCutGraph.CutGraphCutter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.EnumMap;

public class BCDCommandLineInterface extends FlipCutCLO<FlipCutSingleCutSimpleWeight> {

    @Override
    protected String initName() {
        return "bcd";
    }

    @Override
    protected FlipCutSingleCutSimpleWeight initAlgorithm() {
        return new FlipCutSingleCutSimpleWeight(null);
    }

    //##### flip cut parameter #####
    @Override
    protected EnumMap<SuppportedWeights, FlipCutWeights.Weights> initWheightMapping() {
        final EnumMap<SuppportedWeights, FlipCutWeights.Weights> weightMapping = new EnumMap<>(SuppportedWeights.class);
        weightMapping.put(SuppportedWeights.UNIT_WEIGHT, FlipCutWeights.Weights.UNIT_COST);
        weightMapping.put(SuppportedWeights.BRANCH_LENGTH, FlipCutWeights.Weights.EDGE_WEIGHTS);
        weightMapping.put(SuppportedWeights.BOOTSTRAP_VALUES, FlipCutWeights.Weights.BOOTSTRAP_VALUES);
        weightMapping.put(SuppportedWeights.LEVEL, FlipCutWeights.Weights.NODE_LEVEL);
        weightMapping.put(SuppportedWeights.BRANCH_AND_LEVEL, FlipCutWeights.Weights.EDGE_AND_LEVEL);
        weightMapping.put(SuppportedWeights.BOOTSTRAP_AND_LEVEL, FlipCutWeights.Weights.BOOTSTRAP_AND_LEVEL);

        return weightMapping;
    }

    //cut graph type
    @Override
    public void setGraphType(CutGraphCutter.CutGraphTypes graphType) {
        algorithm.setCutter(graphType);
    } //deafault is has to be null


    //##### Methods #####
    public void run() throws Exception {
        //todo checkInput as "check" method and output stuff outside this method
        //todo implement own scm tree implementation --> not used at the moment
    }

    private void checkForNexus() throws IOException, Exception {
        File file = new File(arguments.get(0));
        Tree[] inputTrees = SimpleNexus.getTreesFromFile(file);

        FlipCutSingleCutSimpleWeight n = new FlipCutSingleCutSimpleWeight(CutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW);
        n.setInputTrees(Arrays.asList(inputTrees));

        //TOdo: parameter aus nexus file auslesen und setzen
    }


    @Override
    protected void printUsage(PrintStream stream) {
        stream.println("Usage:");
        stream.println(" " + NAME + " [options...] NEWICK_TREE_FILE");
        stream.println("    The only required argument is the input tree file in newick format");
        stream.println();
        stream.println(" " + NAME + " [options...] NEWICK_TREE_FILE NEWICK_GUIDE_TREE_FILE");
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




}

