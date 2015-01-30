package flipcut;

import epos.model.tree.Tree;
import epos.model.tree.io.Newick;
import epos.model.tree.io.SimpleNexus;
import epos.model.tree.treetools.SiblingReduction;
import epos.model.tree.treetools.UnsupportedCladeReduction;
import flipcut.clo.FlipCutCLO;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static flipcut.clo.FlipCutCLO.FileType.*;

/**
 * Created by fleisch on 25.11.14.
 */
public class BCDSupertrees {
    private static final BCDCommandLineInterface bcdCLI = new BCDCommandLineInterface();

    private static final String NEXUS_EXT = "nex,NEX,ne,NE,nexus,NEXUS";
    private static final String NEXUS_DEFAULT = ".nex";
    private static final String NEWICK_EXT = "tree,TREE,tre,TRE,phy,PHY";
    private static final String NEWICK_DEFAULT = ".tre";
    private static final String EXT_PATTERN = "([^\\s]+(\\.(?i)(nex|NEX|ne|NE|nexus|NEXUS|tree|TREE|tre|TRE|phy|PHY))$)";
    private static final PathMatcher nexusMatcher = FileSystems.getDefault().getPathMatcher("glob:**.{" + NEXUS_EXT + "}");
    private static final PathMatcher newickMatcher = FileSystems.getDefault().getPathMatcher("glob:**.{" + NEWICK_EXT + "}");
    private static FlipCutCLO.FileType INPUT_TYPE = AUTO;



    public static void main(String[] args){
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
                //todo add nexus support //read Block convert it to options and parse it an parse is with parser.

                if (!bcdCLI.inputFile.isAbsolute())
                    bcdCLI.inputFile = bcdCLI.workingPath.resolve(bcdCLI.inputFile);

                Tree[] inputTreesUntouched = parseFileToTrees(bcdCLI.inputFile, bcdCLI.inputType);

                List<Tree> inputTrees;
                Tree guideTree = null;

                if (bcdCLI.inputSCMFile != null) {
                    if (!bcdCLI.inputSCMFile.isAbsolute())
                        bcdCLI.inputSCMFile = bcdCLI.workingPath.resolve(bcdCLI.inputSCMFile);

                    guideTree = parseFileToTrees(bcdCLI.inputSCMFile,bcdCLI.inputType)[0];
                } else if (bcdCLI.useSCM) {
                    guideTree = calculateSCM(inputTreesUntouched);
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
        } catch (IOException e){
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println();
            //todo return some exit code and do some error messages
        }




    }

    private static Tree[] parseFileToTrees(Path path) throws IOException {
        return parseFileToTrees(path, AUTO);
    }

    private static Tree[] parseFileToTrees(Path path, FlipCutCLO.FileType type) throws IOException {
        if (type == null || type == AUTO) {
            if (newickMatcher.matches(path)) {
                INPUT_TYPE = NEWICK;
                return SimpleNexus.getTreesFromFile(path.toFile());
            } else if (nexusMatcher.matches(path)) {
                INPUT_TYPE = NEXUS;
                return SimpleNexus.getTreesFromFile(path.toFile());
            }
        } else if (type == NEXUS) {
            INPUT_TYPE = NEXUS;
            return SimpleNexus.getTreesFromFile(path.toFile());
        } else if (type == NEWICK) {
            INPUT_TYPE = NEWICK;
            return SimpleNexus.getTreesFromFile(path.toFile());
        }

        throw new FileNotFoundException("ERROR: Unknown input file extension. Please specify the input file type (--fileType) or use a typical file extension for NEWICK (" + NEWICK_EXT + ") or NEXUS (" + NEXUS_EXT + ")");
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

    private static void writeOutput(Tree supertree) throws IOException {
        //create output path
        if (bcdCLI.output != null) {
            if (!bcdCLI.output.isAbsolute())
                bcdCLI.output = bcdCLI.workingPath.resolve(bcdCLI.output);

        } else {
            Path outFolder = bcdCLI.inputFile.getParent();
            String filename = bcdCLI.inputFile.getFileName().toString();
            filename = removerExtensions(filename);
            filename = filename + "_bcd-supertree";
            bcdCLI.output = outFolder.resolve(filename);

        }

        //find out file format
        if (bcdCLI.outputType != AUTO) {
            bcdCLI.output = removerExtensions(bcdCLI.output);
            writeTreeToFile(bcdCLI.output, supertree, bcdCLI.outputType, true);
        } else if (bcdCLI.inputType != AUTO) {
            bcdCLI.output = removerExtensions(bcdCLI.output);
            writeTreeToFile(bcdCLI.output, supertree, bcdCLI.inputType, true);
        } else if (newickMatcher.matches(bcdCLI.output)) {
            writeTreeToFile(bcdCLI.output, supertree, NEWICK, false);
        } else if (nexusMatcher.matches(bcdCLI.output)) {
            writeTreeToFile(bcdCLI.output, supertree, NEXUS, false);
        } else {
            //extension remove not needed because already checked before
            writeTreeToFile(bcdCLI.output, supertree, INPUT_TYPE, true);
        }

    }

    private static void writeTreeToFile(Path path, Tree supertree, FlipCutCLO.FileType type, boolean appendExt) throws IOException {
        switch (type){
            case NEWICK:
                Newick.tree2File(path.toFile(),supertree);
                break;
            case NEXUS:
                SimpleNexus.tree2File(path.toFile(),supertree);
                break;
            default:
                throw new IOException("ERROR: Unknown output file Type. Please specify the file type (--fileType or --outFileType) or use a typical file extension for NEWICK (" + NEWICK_EXT + ") or NEXUS (" + NEXUS_EXT + ")");
        }


    }

    private static String removerExtensions(String filename) {
        return filename.replaceAll(EXT_PATTERN, "");
    }

    private static Path removerExtensions(Path path) {
        Path outFolder = path.getParent();
        String filename = path.getFileName().toString();
        filename = removerExtensions(filename);
        return outFolder.resolve(filename);
    }
}
