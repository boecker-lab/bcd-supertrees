package flipcut;

import epos.model.tree.Tree;
import epos.model.tree.io.Newick;
import epos.model.tree.io.SimpleNexus;
import epos.model.tree.io.TreeFileUtils;
import epos.model.tree.treetools.SiblingReduction;
import epos.model.tree.treetools.UnsupportedCladeReduction;
import flipcut.clo.FlipCutCLO;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import scmAlgorithm.GreedySCMAlgorithm;
import scmAlgorithm.treeScorer.AbstractOverlapScorer;
import scmAlgorithm.treeSelector.GreedyTreeSelector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static flipcut.clo.FlipCutCLO.FileType.*;

/**
 * Created by fleisch on 25.11.14.
 */
public class BCDSupertrees {
    private static BCDCommandLineInterface bcdCLI;


    private static FlipCutCLO.FileType INPUT_TYPE;


    public static void main(String[] args) {
        INPUT_TYPE = AUTO;
        bcdCLI = new BCDCommandLineInterface();
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

            //todo implement own scm tree implementation --> not used at the moment


            if (bcdCLI.inputFile != null) {
                //todo preprocessing
                //todo add nexus block support //read Block convert it to options and parse it an parse is with parser.

                if (!bcdCLI.inputFile.isAbsolute())
                    bcdCLI.inputFile = bcdCLI.workingPath.resolve(bcdCLI.inputFile);

                Tree[] inputTreesUntouched = parseFileToTrees(bcdCLI.inputFile, bcdCLI.inputType);

                List<Tree> inputTrees;
                Tree guideTree = null;

                if (bcdCLI.inputSCMFile != null) {
                    if (!bcdCLI.inputSCMFile.isAbsolute())
                        bcdCLI.inputSCMFile = bcdCLI.workingPath.resolve(bcdCLI.inputSCMFile);

                    guideTree = parseFileToTrees(bcdCLI.inputSCMFile, bcdCLI.inputType)[0];
                } else if (bcdCLI.useSCM) {
                    System.out.println("Calculating SCM Guide Tree...");
                    long t =  System.currentTimeMillis();
                    guideTree = calculateSCM(inputTreesUntouched);

                    System.out.println("...DONE in " + (double)(System.currentTimeMillis() - t)/1000d + "s");
                }

                SiblingReduction reducer = null;
                if (bcdCLI.removeUndisputedSiblings) {
                    inputTrees = new ArrayList<>(inputTreesUntouched.length + 1);
                    for (Tree tree : inputTreesUntouched) {
                        inputTrees.add(tree.cloneTree());
                    }
                    if (guideTree != null)
                        inputTrees.add(guideTree); //put guide tree temporary in input list

                    reducer = removeUndisputedSiblings(inputTrees);
                    inputTrees.remove(inputTrees.size() - 1); //remove guide tree again from input list

                } else {
                    inputTrees = Arrays.asList(inputTreesUntouched);
                }


                bcdCLI.setInputTrees(inputTrees, guideTree);
                Tree superTree = bcdCLI.getSupertree();

                if (bcdCLI.removeUndisputedSiblings)
                    reducer.unmodify(Arrays.asList(superTree));

                if (bcdCLI.unsupportedCladeReduction)
                    removeUnsupportedClades(inputTreesUntouched, superTree);


                writeOutput(superTree);

            } else {
                throw new CmdLineException(parser, "ERROR: No input file is given! See help text for information about correct usage.");
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
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println();
            bcdCLI.printHelp(parser, System.err);
        }


    }

    private static Tree[] parseFileToTrees(Path path) throws IOException {
        return parseFileToTrees(path, AUTO);
    }

    private static Tree[] parseFileToTrees(Path path, FlipCutCLO.FileType type) throws IOException {
        Tree[] t = null;
        FlipCutCLO.FileType toInputType = AUTO;
        if (type == null || type == AUTO) {
            if (TreeFileUtils.newickMatcher.matches(path)) {
                toInputType = NEWICK;
                t = Newick.getAllTrees(new FileReader(path.toFile()));
            } else if (TreeFileUtils.nexusMatcher.matches(path)) {
                toInputType = NEXUS;
                t = SimpleNexus.getTreesFromFile(path.toFile());
            } else {
                throw new FileNotFoundException("ERROR: Unknown input file extension. Please specify the correct input file type (--fileType) or use a typical file extension for NEWICK (" + TreeFileUtils.NEWICK_EXT + ") or NEXUS (" + TreeFileUtils.NEXUS_EXT + ")");
            }
        } else if (type == NEXUS) {
            toInputType = NEXUS;
            t = SimpleNexus.getTreesFromFile(path.toFile());
        } else if (type == NEWICK) {
            toInputType = NEWICK;
            t = Newick.getAllTrees(new FileReader(path.toFile()));
        } else {
            throw new FileNotFoundException("ERROR: Unknown input file extension. Please specify the correct input file type (--fileType) or use a typical file extension for NEWICK (" + TreeFileUtils.NEWICK_EXT + ") or NEXUS (" + TreeFileUtils.NEXUS_EXT + ")");
        }

        if (t == null || t.length < 1)
            throw new IOException("ERROR: No Tree in input file or wrong file type detected. Please specify the correct input file type (--fileType) or use a typical file extension for NEWICK (" + TreeFileUtils.NEWICK_EXT + ") or NEXUS (" + TreeFileUtils.NEXUS_EXT + ")");

        if (INPUT_TYPE == null || INPUT_TYPE == AUTO )
            INPUT_TYPE = toInputType;

        return t;
    }


    private static Tree calculateSCM(Tree[] inputTrees) {
        GreedySCMAlgorithm algo = new GreedySCMAlgorithm(new GreedyTreeSelector.GTSMapPQ(new AbstractOverlapScorer.OverlapScorerTroveObject(),inputTrees));
        return algo.getSupertree();
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

    private static void writeOutput(Tree supertree) throws IOException {
        //create output path
        if (bcdCLI.output != null) {
            if (!bcdCLI.output.isAbsolute())
                bcdCLI.output = bcdCLI.workingPath.resolve(bcdCLI.output);

        } else {
            Path outFolder = bcdCLI.inputFile.getParent();
            String filename = bcdCLI.inputFile.getFileName().toString();
            filename = filename.replaceFirst(TreeFileUtils.EXT_PATTERN, "_bcd-supertree");
            bcdCLI.output = outFolder.resolve(filename);

        }

        //find out file format
        if (bcdCLI.outputType != AUTO) {
            bcdCLI.output = removeExtension(bcdCLI.output);
            writeTreeToFile(bcdCLI.output, supertree, bcdCLI.outputType, true);
        } else if (bcdCLI.inputType != AUTO) {
            bcdCLI.output = removeExtension(bcdCLI.output);
            writeTreeToFile(bcdCLI.output, supertree, bcdCLI.inputType, true);
        } else if (TreeFileUtils.newickMatcher.matches(bcdCLI.output)) {
            writeTreeToFile(bcdCLI.output, supertree, NEWICK, false);
        } else if (TreeFileUtils.nexusMatcher.matches(bcdCLI.output)) {
            writeTreeToFile(bcdCLI.output, supertree, NEXUS, false);
        } else {
            //extension remove not needed because already checked before
            writeTreeToFile(bcdCLI.output, supertree, INPUT_TYPE, true);
        }

    }

    private static void writeTreeToFile(Path path, Tree supertree, FlipCutCLO.FileType type, boolean appendExt) throws IOException {
        switch (type) {
            case NEWICK:
                Newick.tree2File(new File(path.toString() + TreeFileUtils.NEWICK_DEFAULT), supertree);
                break;
            case NEXUS:
                SimpleNexus.tree2File(new File(path.toString() + TreeFileUtils.NEXUS_DEFAULT), supertree);
                break;
            default:
                throw new IOException("ERROR: Unknown output file Type. Please specify the file type (--fileType or --outFileType) or use a typical file extension for NEWICK (" + TreeFileUtils.NEWICK_EXT + ") or NEXUS (" + TreeFileUtils.NEXUS_EXT + ")");
        }


    }

    private static Path removeExtension(Path path) {
        Path outFolder = path.getParent();
        String filename = path.getFileName().toString();
        filename = filename.replaceFirst(TreeFileUtils.EXT_PATTERN, "");
        return outFolder.resolve(filename);
    }
}
