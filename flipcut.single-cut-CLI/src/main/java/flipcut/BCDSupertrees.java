package flipcut;

import epos.algo.consensus.nconsensus.NConsensus;
import epos.model.tree.Tree;
import epos.model.tree.io.Newick;
import epos.model.tree.io.SimpleNexus;
import epos.model.tree.io.TreeFileUtils;
import epos.model.tree.treetools.ReductionModifier;
import epos.model.tree.treetools.TreeUtilsBasic;
import epos.model.tree.treetools.UnsupportedCladeReduction;
import flipcut.clo.FlipCutCLO;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import scmAlgorithm.AbstractSCMAlgorithm;
import scmAlgorithm.GreedySCMAlgorithm;
import scmAlgorithm.RandomizedSCMAlgorithm;
import scmAlgorithm.treeScorer.ConsensusResolutionScorer;
import scmAlgorithm.treeScorer.OverlapScorer;
import scmAlgorithm.treeScorer.TreeScorer;
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

            if (bcdCLI.inputFile != null) {
                //todo add nexus block support //read Block convert it to options and parse it an parse is with parser.

                if (!bcdCLI.inputFile.isAbsolute())
                    bcdCLI.inputFile = bcdCLI.workingPath.resolve(bcdCLI.inputFile);

                Tree[] inputTreesUntouched = parseFileToTrees(bcdCLI.inputFile, bcdCLI.inputType);

                List<Tree> inputTrees;
                Tree guideTree = null;
                Tree suppportTree = null;

                if (bcdCLI.inputSCMFile != null) {
                    if (!bcdCLI.inputSCMFile.isAbsolute())
                        bcdCLI.inputSCMFile = bcdCLI.workingPath.resolve(bcdCLI.inputSCMFile);

                    guideTree = parseFileToTrees(bcdCLI.inputSCMFile, bcdCLI.inputType)[0];
                } else if (bcdCLI.useSCM) { //scm tree option is hidden because should be activated
                    System.out.println("Calculating SCM Guide Tree...");
                    long t =  System.currentTimeMillis();
                    if (bcdCLI.scmMethod != FlipCutCLO.SCM.SUPPORT) {
                        guideTree = calculateSCM(TreeUtilsBasic.cloneTrees(TreeUtilsBasic.cloneTrees(inputTreesUntouched)), bcdCLI.scmMethod);
                    } else {
                        guideTree = calculateSCM(TreeUtilsBasic.cloneTrees(TreeUtilsBasic.cloneTrees(inputTreesUntouched)), FlipCutCLO.SCM.OVERLAP);
                        suppportTree = calculateSCMSupportTree(TreeUtilsBasic.cloneTrees(inputTreesUntouched));
                    }
                    System.out.println("...DONE in " + (double)(System.currentTimeMillis() - t)/1000d + "s");
                    System.out.println(Newick.getStringFromTree(guideTree));
                }

                ReductionModifier reducer = null;
                if (bcdCLI.removeUndisputedSiblings) { //ATTENTION this is an Error prone method
                    inputTrees = new ArrayList<>(inputTreesUntouched.length + 2);
                    for (Tree tree : inputTreesUntouched) {
                        inputTrees.add(tree.cloneTree());
                    }
                    if (suppportTree != null)
                        inputTrees.add(suppportTree); //put support tree temporary in input list
                    if (guideTree != null)
                        inputTrees.add(guideTree); //put guide tree temporary in input list

                    reducer = removeUndisputedSiblings(inputTrees);

                    if (guideTree != null)
                       inputTrees.remove(inputTrees.size() - 1); //remove guide tree again from input list
                } else {
                    inputTrees = new ArrayList<>(inputTreesUntouched.length +1);
                    inputTrees.addAll(Arrays.asList(inputTreesUntouched));
                    if (suppportTree != null)
                        inputTrees.add(suppportTree);
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
            bcdCLI.printHelp(parser, System.out);

            return;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println();
            bcdCLI.printHelp(parser, System.out);
        }


    }

//    private static void optimizeRooting(Tree inputTrees[], Tree guideTree) {
//        Set<String> all =  new HashSet<>(guideTree.vertexCount());
//        Map<TreeNode,Set<String>> guideLCAToLabels = new THashMap<>(guideTree.vertexCount());
//        Map<String,TreeNode> guideLabelToLeaf =  new HashMap<>(guideTree.vertexCount());
//
//        for (TreeNode node : guideTree.getRoot().depthFirstIterator()) {
//            if (!node.equals(guideTree.getRoot())) {
//                TreeNode p = node.getParent();
//
//
//                if (!guideLCAToLabels.containsKey(p))
//                    guideLCAToLabels.put(p,new HashSet<String>());
//
//                if (node.isLeaf()){
//                    String l =  node.getLabel();
//                    guideLCAToLabels.get(p).add(l);
//                    guideLabelToLeaf.put(l, node);
//                    all.add(l);
//
//                }else{
//                    guideLCAToLabels.get(p).addAll(guideLCAToLabels.get(node));
//
//                }
//            }
//        }
//
//        for (Tree tree : inputTrees) {
//            if (tree.getRoot().childCount() > 2){ //check if true is not already rooted
//                Set<String> allLeafLabels = TreeUtilsBasic.getLeafLabels(tree.getRoot());
//                List<TreeNode> allGuideLeafes = new ArrayList<>(tree.vertexCount());
//
//                BiMap<TreeNode,Set<String>> lCAToLabels = HashBiMap.create(tree.vertexCount());
//                BiMap<TreeNode,Set<Set<String>>> nodeToSplits = HashBiMap.create(tree.vertexCount());
//                Map<TreeNode,Set<String>> nodeToParentSplit =  new HashMap<>(tree.vertexCount());
//
//                for (TreeNode node : tree.getRoot().depthFirstIterator()) {
//                    if (!node.equals(tree.getRoot())) {
//                        TreeNode p = node.getParent();
//
//                        if (!nodeToParentSplit.containsKey(p))
//                            nodeToParentSplit.put(p,new HashSet<String>());
//                        if (!lCAToLabels.containsKey(p))
//                            lCAToLabels.put(p,new HashSet<String>());
//                        if (!nodeToSplits.containsKey(p))
//                            nodeToSplits.put(p,new HashSet<Set<String>>());
//
//                        if (node.isLeaf()){
//                            String l =  node.getLabel();
//                            lCAToLabels.get(p).add(l);
//                            allGuideLeafes.add(guideLabelToLeaf.get(l));
//
//                            Set<String> s1 =  new HashSet<>(1);
//                            s1.add(l);
//                            nodeToSplits.get(p).add(s1);
//
//                            Set<String> s2 =  new HashSet<>(allLeafLabels);
//                            s2.remove(l);
//                            nodeToSplits.get(p).add(s2);
//
//                            //add s1 to parentsplit
//                            nodeToParentSplit.get(p).addAll(s1);
//
//                        }else{
//                            lCAToLabels.get(p).addAll(lCAToLabels.get(node));
//
//                            Set<String> s1 =  new HashSet<>(lCAToLabels.get(node));
//                            nodeToSplits.get(p).add(s1);
//
//                            Set<String> s2 =  new HashSet<>(allLeafLabels);
//                            s2.removeAll(s1);
//                            nodeToSplits.get(p).add(s2);
//
//                            //add s1 to parentsplit
//                            nodeToParentSplit.get(p).addAll(s1);
//                            //add parent split off current node dot bimap
//                            Set<String> parentSplit =  nodeToParentSplit.get(node);
//                            if (parentSplit.equals(allLeafLabels))
//                                System.out.println("Something goes wrong");
//                            Set<String> parentSplit2 =  new HashSet<>(allLeafLabels);
//                            parentSplit2.removeAll(parentSplit);
//                            nodeToSplits.get(node).add(parentSplit);
//                            nodeToSplits.get(node).add(parentSplit2);
//
//                        }
//                    }
//                }
//
//                //build lca splits and search it in tree
//                TreeNode guideLCA =  guideTree.findLeastCommonAncestor(allGuideLeafes);
//                Set<Set<String>> splits = new HashSet<>(guideLCA.childCount());
////                Set<String> parentSplit = new HashSet<>(allGuideLeafes.size());
//
//                for (TreeNode child : guideLCA.children()) {
//                    Set<String> s1;
//                    Set<String> s2;
//                    if (child.isInnerNode()) {
//                        s1 = new HashSet<>(guideLCAToLabels.get(child));
//                    } else {
//                        s1 = new HashSet<>(1);
//                        s1.add(child.getLabel());
//                    }
//                    s1.retainAll(allLeafLabels);
//
//                    if (!s1.isEmpty()) {
//                        splits.add(s1);
////                        parentSplit.addAll(s1);
//
//                        s2 = new HashSet<>(allLeafLabels);
//                        s2.removeAll(s1);
//                        splits.add(s2);
//                    }
//                }
//
//                TreeNode lca = nodeToSplits.inverse().get(splits);
//                if (lca != null){
//                    TreeUtilsBasic.moveFakeRoot(tree,lca);
//                }else{
//                    System.err.println("NOT possible with strict consensus guide tree");
//                }
//            }
//        }
//    }

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


    private static Tree calculateSCM(Tree[] inputTrees, final FlipCutCLO.SCM method) {
        AbstractSCMAlgorithm algo;
        switch (method){
            case OVERLAP:
                algo = new GreedySCMAlgorithm(new GreedyTreeSelector(new OverlapScorer(TreeScorer.ConsensusMethods.STRICT),inputTrees));
                break;
            case RESOLUTION:
                algo = new GreedySCMAlgorithm(new GreedyTreeSelector(new ConsensusResolutionScorer(TreeScorer.ConsensusMethods.STRICT),inputTrees));
                break;
            case RANDOMIZED:
                algo = new RandomizedSCMAlgorithm(bcdCLI.scmiterations,inputTrees,new OverlapScorer(TreeScorer.ConsensusMethods.STRICT));
                break;
            default:
                return null;
        }
        return algo.getSupertree();
    }

    private static Tree calculateSCMSupportTree(Tree[] inputTrees){
        AbstractSCMAlgorithm scmSupportAlgorithm = new RandomizedSCMAlgorithm(bcdCLI.scmiterations,inputTrees,new OverlapScorer(TreeScorer.ConsensusMethods.SEMI_STRICT));
        List<Tree> temp =  scmSupportAlgorithm.getSupertrees();
        NConsensus c =  new NConsensus();
        c.setMethod(NConsensus.METHOD_MAJORITY);
        Tree scmSupportTree = c.getConsensusTree(temp.toArray(new Tree[temp.size()]));
        scmSupportTree.getRoot().setLabel(Double.toString((double)inputTrees.length/2d));
        return scmSupportTree;
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
