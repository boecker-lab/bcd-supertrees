package phylo.tree.algorithm.flipcut;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.InterfaceCmdLineParser;
import phylo.tree.algorithm.flipcut.cli.BCDCLI;
import phylo.tree.algorithm.flipcut.utils.Utils;
import phylo.tree.algorithm.gscm.SCMAlgorithm;
import phylo.tree.algorithm.gscm.treeMerger.TreeScorers;
import phylo.tree.io.Newick;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeUtils;
import phylo.tree.treetools.ReductionModifier;
import phylo.tree.treetools.TreetoolUtils;
import phylo.tree.treetools.UnsupportedCladeReduction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.*;


/**
 * Created by fleisch on 25.11.14.
 */
public class BCDSupertrees {
    protected static BCDCLI CLI;

    public static void main(String[] args) {
        CLI = new BCDCLI(BCDCLI.DEFAULT_PROPERTIES_FILE);
        run(args);
    }

    public static void run(final String[] args) {
        double startTime = System.currentTimeMillis();
        CLI.LOGGER_WRAPPER.info("Start calculation with following parameters: " + Arrays.toString(args));
        final CmdLineParser parser = new InterfaceCmdLineParser(CLI);

        try {
            // parse the arguments.
            parser.parseArgument(args);

            // check for help
            if (CLI.isHelp() || CLI.isFullHelp()) {
                CLI.printHelp(parser);
                System.exit(0);
            }


            //parse guide trees
            Tree guideTree = CLI.parseSCM();


            double scmRuntime = Double.NaN;

            Tree guideTreeToCut = null;
            if (CLI.useSCM) {
                if (guideTree == null) { //scm tree option is hidden because should be activated
                    CLI.LOGGER_WRAPPER.info("Calculating SCM Guide Tree...");
                    scmRuntime = System.currentTimeMillis();
                    SCMAlgorithm algo;
                    {

                        List<Tree> input = TreetoolUtils.removeDuplicates(CLI.parseInput());
                        if (input.size() > 100){
                            CLI.scorerTypes = new TreeScorers.ScorerType[]{TreeScorers.ScorerType.UNIQUE_TAXA};
                            CLI.LOGGER_WRAPPER.warn("More than 100 input trees, using GSCM with Unique-Taxa scoring instead Unique-Clades-Lost scoring as default postprocessing.");
                        }
                        algo = CLI.getSCMInstance();
                        algo.setInput(input);
                    }
                    algo.call();
                    algo.shutdown();
                    guideTree = algo.getResult();
                    scmRuntime = ((double) System.currentTimeMillis() - scmRuntime) / 1000d;
                    CLI.LOGGER_WRAPPER.info("...SCM Guide Tree calculation DONE in " + scmRuntime + "s");
                    if (CLI.isVerbose())
                        CLI.LOGGER_WRAPPER.debug("GuideTree:" + System.lineSeparator() + Newick.getStringFromTree(guideTree));
                } else {

                }
                guideTreeToCut = guideTree;
                guideTree = TreeUtils.deleteInnerLabels(guideTreeToCut);
                CLI.LOGGER_WRAPPER.info("Clades in guide tree: " + (guideTree.vertexCount() - guideTree.getNumTaxa()));
            }


            Tree suppportTree = null;
            ReductionModifier reducer = null;
            List<Tree> inputTrees = CLI.parseInput();
            if (CLI.isVerbose())
                CLI.LOGGER_WRAPPER.debug("Tree sizes: " + Arrays.toString(inputTrees.stream().mapToInt(Tree::getNumTaxa).sorted().toArray()));

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

            Logger.getLogger("org.jline").setLevel(Level.OFF);

            // configure algorithm
            AbstractFlipCut algorithm = CLI.createAlgorithmInstance();

            //set input trees
            algorithm.setInput(CLI.createGraphInstance(inputTrees, guideTreeToCut));

            //run bcd supertrees
            algorithm.run();
            //collect results
            List<Tree> superTrees = algorithm.getResults();

            //postprocess results if needed
            if (CLI.removeUndisputedSiblings)
                reducer.unmodify(superTrees);

            if (CLI.unsupportedCladeReduction) {
                List<Tree> inputTreesUntouched = CLI.parseInput();
                for (Tree superTree : superTrees) {
                    removeUnsupportedClades(inputTreesUntouched.toArray(new Tree[inputTreesUntouched.size()]), superTree);
                }
            }
            // calc support values
            if (CLI.supportValues) {
                Utils.addCladewiseSplitFit(inputTrees, CLI.getWeights(), superTrees);
            }
            inputTrees = null;

            //write output file
            if (CLI.isFullOutput() && guideTree != null) {
                List<Tree> withSCM = new LinkedList(superTrees);
                withSCM.add(guideTree);
                CLI.writeOutput(withSCM);
            } else {
                CLI.writeOutput(superTrees);
            }

            //calculate runtime
            double calcTime = (System.currentTimeMillis() - startTime) / 1000d;

            if (CLI.isVerbose()) {
                int i = 1;
                for (Tree supertree : superTrees) {
                    CLI.LOGGER_WRAPPER.debug("SuperTree " + i++ +" with Score: " + supertree.getName()
                            + System.lineSeparator()
                            + Newick.getStringFromTree(supertree));
                }
            }

            if (!Double.isNaN(scmRuntime)) {
                CLI.LOGGER_WRAPPER.info("...GSCM run in " + (scmRuntime) + "s");
                CLI.LOGGER_WRAPPER.info("...BCD run in " + (calcTime - scmRuntime) + "s");
            }

            //todo move this to write output???
            Path timeFile = CLI.getRuntimeFile();
            if (timeFile != null) {
                Files.deleteIfExists(timeFile);
                if (!Double.isNaN(scmRuntime)) {
                    Files.write(timeFile, ("gscm=" + Double.toString(scmRuntime) + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE_NEW);
                    Files.write(timeFile, ("bcd=" + Double.toString(calcTime - scmRuntime) + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                    Files.write(timeFile, ("complete=" + Double.toString(calcTime)).getBytes(), StandardOpenOption.APPEND);
                } else {
                    Files.write(timeFile, ("bcd=" + Double.toString(calcTime)).getBytes(), StandardOpenOption.CREATE_NEW);
                }


            }

            CLI.LOGGER_WRAPPER.info("Supertree calculation Done in: " + calcTime + "s");
            algorithm.shutdown();
            System.exit(0);

        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            CLI.LOGGER_WRAPPER.error(e.getMessage(), e);
            System.err.println(e.getMessage());
            System.err.println();
            CLI.printHelp(parser, System.err);
            System.exit(1);
        } catch (IOException e) {
            CLI.LOGGER_WRAPPER.error(e.getMessage(), e);
            System.err.println(e.getMessage());
            System.err.println();
            CLI.printHelp(parser, System.err);
            System.exit(2);
        } catch (Exception e) {
            CLI.LOGGER_WRAPPER.error(e.getMessage(), e);
            System.err.println(e.getMessage());
            System.err.println();
            System.exit(666);
        }

        System.exit(888);
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
