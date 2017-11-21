package phylo.tree.algorithm.flipcut.clo;


import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.IntOptionHandler;
import phylo.tree.algorithm.flipcut.AbstractFlipCut;
import phylo.tree.algorithm.flipcut.FlipCutMultiCut;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDMultiCutGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedGraphFactory;
import phylo.tree.algorithm.flipcut.cli.BCDCLI;
import phylo.tree.algorithm.flipcut.costComputer.SimpleCosts;
import phylo.tree.algorithm.flipcut.cutter.MultiCutterFactory;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphTypes;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.model.Tree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by fleisch on 19.06.14.
 */
public class BCDBeamCLI extends BCDCLI<FlipCutMultiCut> {
    //todo maybe extend or overwrite algorithm enum
    public MultiCutterFactory.MultiCutterType multiType = null;

    @Option(name = "-x", aliases = "--beamSearch", usage = "Use beam search implementation")
    public void setMultiType(MultiCutterFactory.MultiCutterType multiType) {
        this.multiType = multiType;
    }

    public BCDBeamCLI(String appHomeParent, String appHomeFolderName, String logDir, int maxLogFileSize, int logRotation) {
        super(appHomeParent, appHomeFolderName, logDir, maxLogFileSize, logRotation);
    }

    public BCDBeamCLI(InputStream propertiesFile) {
        super(propertiesFile);
    }

    public int cutNumber = 25; //default is null an means auto.

    @Option(name = "-k", aliases = "--cutNumber", handler = IntOptionHandler.class, usage = "Number of suboptimal solutions")
    public void setCutNumber(int cutNumber) {
        this.cutNumber = Math.max(1, cutNumber);
    }


    @Override
    public AbstractFlipCut createAlgorithmInstance() {
        if (multiType != null) {
            FlipCutMultiCut algo = new FlipCutMultiCut(MultiCutterFactory.newInstance(multiType, getGraphType()));
            algo.setNumberOfCuts(cutNumber);
            setParameters(algo);
            return algo;
        } else {
            return super.createAlgorithmInstance();
        }
    }

    @Override
    public SourceTreeGraph createGraphInstance(List<Tree> source, Tree scaffold) {
        if (multiType != null) {
            MultiCutterFactory factory = MultiCutterFactory.newInstance(multiType, getGraphType());
            if (getGraphType() == CutGraphTypes.COMPRESSED_BCD_VIA_MAXFLOW_TARJAN_GOLDBERG) {
                return new CompressedBCDMultiCutGraph(
                        CompressedGraphFactory.createSourceGraph(SimpleCosts.newCostComputer(source, scaffold, getWeights()), getBootstrapThreshold(), false), cutNumber, factory);
            } else {
                return new FlipCutGraphMultiSimpleWeight(SimpleCosts.newCostComputer(source, scaffold, getWeights()), cutNumber, factory);
            }
        } else {
            return super.createGraphInstance(source, scaffold);
        }
    }

    @Override
    public void writeOutput(List<Tree> treesToWrite) throws IOException {
        super.writeOutput(treesToWrite);

        //write scoring file for beam search trees
        if (isFullOutput()) {
            String name = getOutputFile().toString();
            String[] ext = name.split("[.]");
            name = name.replace("." + ext[ext.length - 1], ".score");

            try (BufferedWriter wr = Files.newBufferedWriter(Paths.get(name))) {
                for (Tree tree : treesToWrite) {
                    String s = tree.getName();
                    wr.write(s == null ? "NaN" : s);
                    wr.write(System.lineSeparator());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
