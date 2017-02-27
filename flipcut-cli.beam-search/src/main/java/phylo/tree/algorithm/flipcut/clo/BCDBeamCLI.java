package phylo.tree.algorithm.flipcut.clo;


import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.IntOptionHandler;
import phylo.tree.algorithm.flipcut.FlipCutMultiCut;
import phylo.tree.algorithm.flipcut.cli.BCDCLI;
import phylo.tree.algorithm.flipcut.flipCutGraph.MultiCutGrapCutterFactories;
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
    public MultiCutGrapCutterFactories.MultiCutterType multiType = null;

    @Option(name = "-x", aliases = "--beamSearch", usage = "Use beam search implementation")
    public void setMultiType(MultiCutGrapCutterFactories.MultiCutterType multiType) {
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

    ;


    @Override
    public FlipCutMultiCut createAlgorithmInstance() {
        FlipCutMultiCut algo = new FlipCutMultiCut(MultiCutGrapCutterFactories.newInstance(multiType, getGraphType()));
        algo.setNumberOfCuts(cutNumber);
        setParameters(algo);
        return algo;
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
                    wr.write(s==null?"NaN":s);
                    wr.write(System.lineSeparator());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
