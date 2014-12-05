package flipcut;

import epos.model.tree.Tree;
import epos.model.tree.io.Newick;
import epos.model.tree.treetools.SiblingReduction;
import epos.model.tree.treetools.UnsupportedCladeReduction;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by fleisch on 25.11.14.
 */
public class BCDSupertrees {
    private final static BCDCommandLineInterface bcdCLI = new BCDCommandLineInterface();

    public static void main(String[] args) throws Exception {
        final CmdLineParser parser = new CmdLineParser(bcdCLI);
        // if you have a wider console, you could increase the value;
        // here 80 is also the default
        parser.setUsageWidth(80);

        try {
            // parse the arguments.
            parser.parseArgument(args);

            // you can parse additional arguments if you want.
            // parser.parseArgument("more","args");

            // after parsing arguments, you should check
            // if enough arguments are given.
            if (bcdCLI.help || bcdCLI.fullHelp) {
                bcdCLI.printHelp(parser);
                return;
            }


            if (bcdCLI.arguments != null && !bcdCLI.arguments.isEmpty()) {
                final int argSize = bcdCLI.arguments.size();
                //todo preprocessing
                if (argSize <= 2) { //todo add nexus support //read Block convert it tor options and parse it an parse is with parser.
                    Tree[] inputTreesUntouched = Newick.getTreeFromFile(bcdCLI.arguments.get(0));
                    List<Tree> inputTrees;

                    Tree guideTree = null;
                    if (argSize >= 2) {
                        guideTree = Newick.getTreeFromFile(bcdCLI.arguments.get(1))[0];
                    } else if (bcdCLI.useSCM) {
                        guideTree = calculateSCM(inputTreesUntouched);
                    }

                    SiblingReduction reucer = null;
                    if (bcdCLI.removeUndisputedSiblings) {
                        inputTrees = new ArrayList<>(inputTreesUntouched.length + 1);
                        for (Tree tree : inputTreesUntouched) {
                            inputTrees.add(tree.cloneTree());
                        }
                        if (guideTree != null)
                            inputTrees.add(guideTree); //put guide tree temporary in input list

                        reucer = removeUndisputedSiblings(inputTrees);
                        inputTrees.remove(inputTrees.size() - 1); //remove guide tree again from input list

                    } else {
                        inputTrees = Arrays.asList(inputTreesUntouched);
                    }


                    bcdCLI.setInputTrees(inputTrees, guideTree);
                    Tree superTree = bcdCLI.getSupertree();

                    if (bcdCLI.removeUndisputedSiblings)
                        reucer.unmodify(Arrays.asList(superTree));

                    if (bcdCLI.unsupportedCladeReduction)
                        removeUnsupportedClades(inputTreesUntouched, superTree);


                    writeOutput(superTree);
                } else {
                    throw new CmdLineException("ERROR: Illegal number arguments! See help text for information about correct usage.");
                }
            } else {
                throw new CmdLineException(parser, "ERROR: No argument is given! See help text for information about correct usage.");
            }


        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println();
            bcdCLI.printHelp(parser, System.err);

            return;
        }

        //todo return some exit code and do some error messages (catch messages)


    }

    private static Tree calculateSCM(Tree[] inputTrees) {
        //todo calculate scm tree
        return null;
    }

    private static SiblingReduction removeUndisputedSiblings(List<Tree> inputTrees) {
        SiblingReduction reducer = new SiblingReduction(null, false);
        reducer.modify(inputTrees);
        return reducer;
    }

    private static void removeUnsupportedClades(Tree[] sourceTrees, Tree supertree) {
        UnsupportedCladeReduction reducer = new UnsupportedCladeReduction(Arrays.asList(sourceTrees));
        reducer.reduceUnsupportedClades(supertree);
    }

    private static void writeOutput(Tree supertree) {
        File out;
        if (bcdCLI.output != null)
            out = bcdCLI.output;
        else
            out = new File(bcdCLI.arguments.get(0) + "_supertree");

        Newick.tree2File(out, supertree);
    }


}
