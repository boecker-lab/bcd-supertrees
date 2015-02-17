package flipcut;

import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import epos.model.tree.io.Newick;
import epos.model.tree.io.SimpleNexus;
import epos.model.tree.io.TreeFileUtils;
import flipcut.clo.FlipCutCLO;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by fleisch on 02.02.15.
 */
public class BCDSupertreesTest {
    public final static String newickInput = "sm.11.sourceTrees_OptSCM-Rooting.tre";
    public final static String newickSCM = "sm.11.sourceTrees.scmTree.tre";
    public final static String newickOut = "sm.11.sourceTrees_OptSCM-Rooting_bcd-supertree.tre";

    public final static String nexusInput = "sm.11.sourceTrees_OptSCM-Rooting.nex";
    public final static String nexusOut = "sm.11.sourceTrees_OptSCM-Rooting_bcd-supertree.nex";
    public final static String nexusSCM = "sm.11.sourceTrees.scmTree.nex";


    //File Hndling tests
    //test auto detection of same input type
    @Test
    public void test_newick_scm_input() throws Exception {
        testAutoFileTypeDetection(newickInput, newickSCM, newickOut,null,null);
    }

    @Test
    public void test_nexus_scm_input() throws Exception {
        testAutoFileTypeDetection(nexusInput, nexusSCM, nexusOut,null,null);
    }

    //test auto detection without guide tree
    @Test
    public void test_newick_input() throws Exception {
        testAutoFileTypeDetection(newickInput, null, newickOut,null,null);
    }

    @Test
    public void test_nexus_input() throws Exception {
        testAutoFileTypeDetection(nexusInput, null, nexusOut,null,null);
    }

    //Test Autodetection of different input Type
    @Test
    public void test_newick_nexus_scm_input() throws Exception {
        testAutoFileTypeDetection(newickInput, nexusSCM, newickOut,null,null);
    }

    @Test
    public void test_nexus_newick_scm_input() throws Exception {
        testAutoFileTypeDetection(nexusInput, newickSCM, nexusOut,null,null);
    }

    //test specify file type
    @Test
    public void test_newick_scm_input_parameter() throws Exception {
        testAutoFileTypeDetection(newickInput, newickSCM, newickOut, FlipCutCLO.FileType.NEWICK,null);
    }

    @Test
    public void test_nexus_scm_input_parameter() throws Exception {
        testAutoFileTypeDetection(nexusInput, nexusSCM, nexusOut,FlipCutCLO.FileType.NEXUS,null);
    }

    @Test
    public void test_newick_scm_input_same_parameter() throws Exception {
        testAutoFileTypeDetection(newickInput, newickSCM, newickOut, FlipCutCLO.FileType.NEWICK,FlipCutCLO.FileType.NEWICK);
    }

    @Test
    public void test_nexus_scm_input_same_parameter() throws Exception {
        testAutoFileTypeDetection(nexusInput, nexusSCM, nexusOut,FlipCutCLO.FileType.NEXUS,FlipCutCLO.FileType.NEXUS);
    }

    @Test
    public void test_newick_scm_input_diff_parameter() throws Exception {
        testAutoFileTypeDetection(newickInput, newickSCM, nexusOut, null,FlipCutCLO.FileType.NEXUS);
    }

    @Test
    public void test_nexus_scm_input_diff_parameter() throws Exception {
        testAutoFileTypeDetection(nexusInput, nexusSCM, newickOut,null,FlipCutCLO.FileType.NEWICK);
    }

    @Test
    public void test_newick_scm_input_diff_parameter2() throws Exception {
        testAutoFileTypeDetection(newickInput, nexusSCM, nexusOut, null,FlipCutCLO.FileType.NEXUS);
    }

    @Test
    public void test_nexus_scm_input_diff_parameter2() throws Exception {
        testAutoFileTypeDetection(nexusInput, newickSCM, newickOut,null,FlipCutCLO.FileType.NEWICK);
    }

    @Test
    public void debugTest(){
        Path inputPath = Paths.get(getClass().getResource("/" + newickInput).getFile());
        Tree[] trees = Newick.getTreeFromFile(inputPath.toFile());
        for (Tree tree : trees) {
            System.out.println("### new Tree ###");
            List<TreeNode> l = new ArrayList<>();
            for (TreeNode node : tree.getRoot().depthFirstIterator()) {
                System.out.println(node);
                l.add(node);
            }
            System.out.println();
            System.out.println();
        }
    }



    private void  testAutoFileTypeDetection(String in,String scm, String outPath, FlipCutCLO.FileType inputType, FlipCutCLO.FileType outputType) throws IOException {
        final Path tempDir = getTmpDir();
        final List<String> toargs =  new LinkedList<>();

        //set parameter
        if (inputType != null){
            toargs.add("-f");
            toargs.add(inputType.toString());
        }

        if (outputType != null) {
            toargs.add("-d");
            toargs.add(outputType.toString());
        }

        //add input file
        Path inputPath = Paths.get(getClass().getResource("/" + in).getFile());
        inputPath = Files.copy(inputPath, tempDir.resolve(inputPath.getFileName()));
        toargs.add(inputPath.toString());
        System.out.println(inputPath);

        if (scm != null) {
            Path scmPath = Paths.get(getClass().getResource("/" + scm).getFile());
            scmPath = Files.copy(scmPath, tempDir.resolve(scmPath.getFileName()));
            System.out.println(scmPath);

           toargs.add(scmPath.toString());
        }

        String[] args =  new String[toargs.size()];
        args = toargs.toArray(args);
        System.out.println("Arguments:");
        System.out.println(Arrays.toString(args));
        BCDSupertrees.main(args);

        //assert
        System.out.println(tempDir.toString());
        File out = (tempDir.resolve(outPath)).toFile();
        System.out.println(out.getAbsolutePath());
        assertTrue(out.exists());
        assertTrue(out.isFile());

        Tree superTree;
        if (TreeFileUtils.nexusMatcher.matches(out.toPath()))
            superTree = SimpleNexus.getTreesFromFile(out)[0];
        else if (TreeFileUtils.newickMatcher.matches(out.toPath()))
            superTree = Newick.getTreeFromFile(out)[0];
        else{
            superTree = null;
            System.out.println("ERROR, file unknown");
        }
        assertNotNull(superTree);
        System.out.println(Newick.getStringFromTree(superTree));
    }

    private Path getTmpDir() throws IOException {
        Random r = new Random();
        final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path t = Files.createTempDirectory(tmpDir, "flipCut-test-tmp-" + r.nextInt());

        return t;
    }
}
