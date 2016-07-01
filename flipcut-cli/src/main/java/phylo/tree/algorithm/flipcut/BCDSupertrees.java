package phylo.tree.algorithm.flipcut;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.InterfaceCmdLineParser;
import phylo.tree.algorithm.flipcut.cli.BCDCLI;
import phylo.tree.algorithm.gscm.SCMAlgorithm;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeUtils;
import phylo.tree.treetools.ReductionModifier;
import phylo.tree.treetools.UnsupportedCladeReduction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;


/**
 * Created by fleisch on 25.11.14.
 */
public class BCDSupertrees {
    private static BCDCLI CLI;

    public static void main(String[] args) {
        CLI = new BCDCLI(BCDCLI.DEFAULT_PROPERTIES_FILE);
        double startTime = System.currentTimeMillis();
        CLI.LOGGER.info("Start calculation with following parameters: " + Arrays.toString(args));
        final CmdLineParser parser = new InterfaceCmdLineParser(CLI);

        try {
            // parse the arguments.
            parser.parseArgument(args);

            // check for help
            if (CLI.isHelp() || CLI.isFullHelp()) {
                CLI.printHelp(parser);
                System.exit(0);
            }

            // configure algorithm
            FlipCutSingleCutSimpleWeight algorithm = new FlipCutSingleCutSimpleWeight();
            CLI.setParameters(algorithm);

            //parse guide trees
            Tree guideTree = CLI.parseSCM();




            double scmRuntime = Double.NaN;

            Tree guideTreeToCut = null;
            if (CLI.useSCM) {
                if (guideTree == null) { //scm tree option is hidden because should be activated
                    CLI.LOGGER.info("Calculating SCM Guide Tree...");
                    scmRuntime = System.currentTimeMillis();
                    List<Tree> inputTrees = CLI.parseInput();
                    guideTree = calculateSCM(inputTrees.toArray(new Tree[inputTrees.size()]));
                    scmRuntime = ((double) System.currentTimeMillis() - scmRuntime) / 1000d;

                    CLI.LOGGER.info("...SCM Guide Tree calculation DONE in " + scmRuntime + "s");
                }
                guideTreeToCut = guideTree;
                guideTree = TreeUtils.deleteInnerLabels(guideTreeToCut);
            }

            Tree suppportTree = null;
            ReductionModifier reducer = null;
            List<Tree>inputTrees = CLI.parseInput();
            if (CLI.removeUndisputedSiblings) { //ATTENTION this is an Error prone method
                if (suppportTree != null)
                    inputTrees.add(suppportTree); //put support tree temporary in input list
                if (guideTreeToCut != null)
                    inputTrees.add(guideTreeToCut); //put guide tree temporary in input list
                reducer = removeUndisputedSiblings(inputTrees);
                if (guideTreeToCut != null)
                    inputTrees.remove(inputTrees.size() - 1); //remove guide tree again from input list
            } else {
                if (suppportTree != null)
                    inputTrees.add(suppportTree);
            }

            //set input trees
            algorithm.setInput(inputTrees, guideTreeToCut);
            //run bcd supertrees
            algorithm.run();
            //collect results
            Tree superTree = algorithm.getResult();

            //postprocess results if needed
            if (CLI.removeUndisputedSiblings)
                reducer.unmodify(Arrays.asList(superTree));

            if (CLI.unsupportedCladeReduction) {
                List<Tree>inputTreesUntouched = CLI.parseInput();
                removeUnsupportedClades(inputTreesUntouched.toArray(new Tree[inputTreesUntouched.size()]), superTree);
            }
            //write output file
            if (CLI.isFullOutput() && guideTree != null) {
                CLI.writeOutput(Arrays.asList(superTree, guideTree));
            } else {
                CLI.writeOutput(Arrays.asList(superTree));
            }

            //calculate runtime
            double calcTime = (System.currentTimeMillis() - startTime) / 1000d;


            if (!Double.isNaN(scmRuntime)) {
                CLI.LOGGER.info("...SCM runs in " + (scmRuntime) + "s");
                CLI.LOGGER.info("...FlipCut runs in " + (calcTime - scmRuntime) + "s");
                Path timeFile = CLI.getRuntimeFile();
                if (timeFile != null) {
                    Files.deleteIfExists(timeFile);
                    Files.write(timeFile, ("gscm " + Double.toString(scmRuntime) + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE_NEW);
                    Files.write(timeFile, ("bcd " + Double.toString(calcTime - scmRuntime) + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                    Files.write(timeFile, ("complete " + Double.toString(calcTime)).getBytes(), StandardOpenOption.APPEND);
                }
            }

            CLI.LOGGER.info("Supertree calculation Done in: " + calcTime + "s");
            algorithm.shutdown();
            System.exit(0);

        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            CLI.LOGGER.log(Level.SEVERE, e.getMessage(), e);
            System.err.println(e.getMessage());
            System.err.println();
            CLI.printHelp(parser, System.err);
            System.exit(1);
        } catch (IOException e) {
            CLI.LOGGER.log(Level.SEVERE, e.getMessage(), e);
            System.err.println(e.getMessage());
            System.err.println();
            CLI.printHelp(parser, System.err);
            System.exit(2);
        } catch (Exception e) {
            CLI.LOGGER.log(Level.SEVERE, e.getMessage(), e);
            System.err.println(e.getMessage());
            System.err.println();
            System.exit(666);
        }

        System.exit(888);
    }


    private static Tree calculateSCM(Tree[] inputTrees) {
        SCMAlgorithm algo = CLI.getSCMInstance();
        algo.setInput(Arrays.asList(inputTrees));
        algo.run();
        algo.shutdown();
        return algo.getResult();
    }

    private static ReductionModifier removeUndisputedSiblings(List<Tree> inputTrees) {
        ReductionModifier reducer = new ReductionModifier(null, false);
        reducer.modify(inputTrees);
        return reducer;
    }

    private static void removeUnsupportedClades(Tree[] sourceTrees, Tree supertree) {
        UnsupportedCladeReduction reducer = new UnsupportedCladeReduction(Arrays.asList(sourceTrees));
        reducer.reduceUnsupportedClades(supertree);
    }
}
