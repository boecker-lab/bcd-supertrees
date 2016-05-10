package flipcut;

import org.junit.Test;
import phylo.tree.io.Newick;
import phylo.tree.io.SimpleNexus;
import phylo.tree.io.TreeFileUtils;
import phylo.tree.model.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by fleisch on 02.02.15.
 */
public class BCDSupertreesTest {
    public final static String newickInput = "flipcut/sm.11.sourceTrees_OptSCM-Rooting.tre";
    public final static String newickSCM = "flipcut/sm.11.sourceTrees.scmTree.tre";
    public final static String newickOut = "sm.11.sourceTrees_OptSCM-Rooting_bcd-supertree.tre";

    public final static String newickInput16 = "flipcut/sm.16.sourceTrees_OptSCM-Rooting.tre";
    public final static String newickInputNoRoot16 = "flipcut/sm.16.sourceTrees.tre";
    public final static String newickSCMNoRoot16 = "flipcut/sm.16.sourceTrees.scmTree.tre";

    public final static String nexusInput = "flipcut/sm.11.sourceTrees_OptSCM-Rooting.nex";
    public final static String nexusOut = "sm.11.sourceTrees_OptSCM-Rooting_bcd-supertree.nex";
    public final static String nexusSCM = "flipcut/sm.11.sourceTrees.scmTree.nex";


    //todo move this tests to phylotree lib or core lib
    //File Hndling tests
    //test auto detection of same input type
    @Test
    public void test_newick_scm_input() throws Exception {
        assertTrue(testAutoFileTypeDetection(newickInput, newickSCM, newickOut, null, null));
    }

    @Test
    public void test_nexus_scm_input() throws Exception {
        testAutoFileTypeDetection(nexusInput, nexusSCM, nexusOut, null, null);
    }

    //test auto detection without guide tree
    @Test
    public void test_newick_input() throws Exception {
        testAutoFileTypeDetection(newickInput, null, newickOut, null, null);
    }

    @Test
    public void test_nexus_input() throws Exception {
        testAutoFileTypeDetection(nexusInput, null, nexusOut, null, null);
    }

    //Test Autodetection of different input Type
    @Test
    public void test_newick_nexus_scm_input() throws Exception {
        testAutoFileTypeDetection(newickInput, nexusSCM, newickOut, null, null);
    }

    @Test
    public void test_nexus_newick_scm_input() throws Exception {
        testAutoFileTypeDetection(nexusInput, newickSCM, nexusOut, null, null);
    }

    //test specify file type
    @Test
    public void test_newick_scm_input_parameter() throws Exception {
        testAutoFileTypeDetection(newickInput, newickSCM, newickOut, TreeFileUtils.FileType.NEWICK, null);
    }

    @Test
    public void test_nexus_scm_input_parameter() throws Exception {
        testAutoFileTypeDetection(nexusInput, nexusSCM, nexusOut, TreeFileUtils.FileType.NEXUS, null);
    }

    @Test
    public void test_newick_scm_input_same_parameter() throws Exception {
        testAutoFileTypeDetection(newickInput, newickSCM, newickOut, TreeFileUtils.FileType.NEWICK, TreeFileUtils.FileType.NEWICK);
    }

    @Test
    public void test_nexus_scm_input_same_parameter() throws Exception {
        testAutoFileTypeDetection(nexusInput, nexusSCM, nexusOut, TreeFileUtils.FileType.NEXUS, TreeFileUtils.FileType.NEXUS);
    }

    @Test
    public void test_newick_scm_input_diff_parameter() throws Exception {
        testAutoFileTypeDetection(newickInput, newickSCM, nexusOut, null, TreeFileUtils.FileType.NEXUS);
    }

    @Test
    public void test_nexus_scm_input_diff_parameter() throws Exception {
        testAutoFileTypeDetection(nexusInput, nexusSCM, newickOut, null, TreeFileUtils.FileType.NEWICK);
    }

    @Test
    public void test_newick_scm_input_diff_parameter2() throws Exception {
        testAutoFileTypeDetection(newickInput, nexusSCM, nexusOut, null, TreeFileUtils.FileType.NEXUS);
    }

    @Test
    public void test_nexus_scm_input_diff_parameter2() throws Exception {
        testAutoFileTypeDetection(nexusInput, newickSCM, newickOut, null, TreeFileUtils.FileType.NEWICK);
    }


    /*@Test
    public void testRootOptimization() throws IOException {
        final Path tempDir = getTmpDir();
        final List<String> toargs =  new LinkedList<>();


        //add input file
        Path inputPath = Paths.get(getClass().getResource("/" + newickInput16).getFile());
        inputPath = Files.copy(inputPath, tempDir.resolve(inputPath.getFileName()));
        toargs.add(inputPath.toString());
        System.out.println(inputPath);

        String[] args =  new String[toargs.size()];
        args = toargs.toArray(args);
        System.out.println("Arguments:");
        System.out.println(Arrays.toString(args));
        BCDSupertrees.main(args);

    }
*/
    /*@Test
    public void debugTest() throws IOException {
        Path inputPath = Paths.get(getClass().getResource("/" + newickInput).getFile());
        final Path tempDir = getTmpDir();
        final List<String> toargs = new LinkedList<>();

        //set parameter

        toargs.add("-S");
        toargs.add("SUPPORT");
        toargs.add("-w");
        toargs.add("BOOTSTRAP_VALUES");

        //timeFile
        Path timeFile = tempDir.resolve("timeFile");
        toargs.add("-R");
        toargs.add(timeFile.toString());
        System.out.println(timeFile);

        //add input file
        inputPath = Files.copy(inputPath, tempDir.resolve(inputPath.getFileName()));
        toargs.add(inputPath.toString());
        System.out.println(inputPath);

        String[] args = new String[toargs.size()];
        args = toargs.toArray(args);
        System.out.println("Arguments:");
        System.out.println(Arrays.toString(args));
        BCDSupertrees.main(args);

    }*/


    public boolean testAutoFileTypeDetection(String in, String scm, String outPath, TreeFileUtils.FileType inputType, TreeFileUtils.FileType outputType) throws IOException {
        final Path tempDir = getTmpDir();
        final List<String> toargs = new LinkedList<>();

        //set parameter
        if (inputType != null) {
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

        String[] args = new String[toargs.size()];
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
        else {
            superTree = null;
            System.out.println("ERROR, file unknown");
        }
        assertNotNull(superTree);
        System.out.println(Newick.getStringFromTree(superTree));
        return superTree != null;
    }

    public Path getTmpDir() throws IOException {
        Random r = new Random();
        final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path t = Files.createTempDirectory(tmpDir, "flipCut-test-tmp-" + r.nextInt());

        return t;
    }
}
