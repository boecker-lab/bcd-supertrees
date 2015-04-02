package flipcut;

import flipcut.clo.FlipCutCLO;
import flipcut.flipCutGraph.CutGraphCutter;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineParser;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * Created by Florian on 22.04.2014.
 */


public class BCDCommandLineInterfaceTest extends BCDCommandLineInterface { //extending from class to test to get protected access
    public final String requiredInputPath = "/home/user/inputTrees.tre";

    //Test if parameter gets translated into flipcut parameters in the right way
    @Test
    public void test_used_algo() throws Exception {
        final CmdLineParser parser = new CmdLineParser(this);

        String[] test_string = new String[]{"-a", Algorithm.BCD.name(), requiredInputPath};
        parser.parseArgument(test_string);
        //assert
        assertEquals(CutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW, getAlgorithm().cutter.getType());

        test_string = new String[]{"-a", Algorithm.FC.name(), requiredInputPath};
        parser.parseArgument(test_string);
        //assert
        assertEquals(CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG, getAlgorithm().cutter.getType());
    }

    @Test
    public void test_set_weights_unit_cost() throws Exception {           //TODO: Test every weight? --> not for the internal notation but for the official see below
        String[] test_String = {"-w", "UNIT_WEIGHT", requiredInputPath};
        final BCDCommandLineInterface test_interface = new BCDCommandLineInterface();
        final CmdLineParser parser = new CmdLineParser(test_interface);
        parser.parseArgument(test_String);
        test_interface.initWheightMapping();

        //assert
        assertEquals("UNIT_COST", test_interface.getAlgorithm().weights.name());
    }

    @Test
    public void test_supported_weights() throws Exception {           //TODO: Test every weight? --> you can iterate over the common weights
        final BCDCommandLineInterface test_interface = new BCDCommandLineInterface();
        final CmdLineParser parser = new CmdLineParser(test_interface);

        for (FlipCutCLO.SuppportedWeights weight : SuppportedWeights.values()) {
            String[] test_String = {"-w", weight.name(), requiredInputPath};
            parser.parseArgument(test_String);

            //assert
            assertEquals(weightMapping.get(weight), test_interface.getAlgorithm().weights);

        }
    }

    @Test
    public void test_bst_threshold() throws Exception {
        String[] test_String = {"-b", "33", requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert

        assertEquals(33, getAlgorithm().bootstrapThreshold);

    }

    //Test if CLI parameter input works as intended
    @Test
    public void test_SCMtree_usage() throws Exception {
        String[] test_String = {"-s", "1", requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert

        assertEquals(true, useSCM);

    }

    @Test
    public void test_output_path() throws Exception {
        final String path = "C:\\fictional.txt";
        String[] test_String = {"-o", path, requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert

        assertEquals(path, output.toString());

    }


    @Test
    public void test_ucr_usage() throws Exception {
        String[] test_String = {"-n", requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(true, unsupportedCladeReduction);

    }

    @Test
    public void test_uds_usage() throws Exception {
        String[] test_String = {"-u", requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(true, removeUndisputedSiblings);
    }

    @Test
    public void test_noRooting() throws Exception{
        String[] test_String = {"-r", requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(true, noRootingOptimization);

    }

    @Test
    public void test_verbose() throws Exception{
        String[] test_String = {"-v", requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(true, verbose);

    }

    @Test
    public void test_insufficient() throws Exception{
        String[] test_String = {"-i", requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(true, skipInsufficientOverlapInstances);

    }

    @Test
    public void test_in_file_type() throws Exception{
        final FileType file_type=FileType.AUTO;
        String[] test_String = {"-f", file_type.name(), requiredInputPath};

        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(file_type, inputType);

    }

    @Test
    public void test_out_file_type() throws Exception{
        final FileType file_type=FileType.AUTO;
        String[] test_String = {"-d",file_type.name(), requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(file_type, outputType);

    }

    @Test
    public void test_work_dir() throws Exception{
        final Path work_dir_path = Paths.get("/fictional/path");
        String[] test_String = {"-p", work_dir_path.toString(),requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(work_dir_path, workingPath);

    }

    @Test
    public void test_help() throws Exception{
        String[] test_String = {"-h", requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(true, help);

    }

    @Test
    public void test_full_help() throws Exception{
        String[] test_String = {"-H", requiredInputPath};
        final CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(test_String);

        //assert
        assertEquals(true, fullHelp);

    }






    @Test
    public void testDoMainGiveTrees() throws Exception {

        /*//arrange

        String[] test = {"-w","BRANCH_AND_LEVEL",this.getClass().getResource("/mincut_sourcetrees.nwk").getPath(),this.getClass().getResource("/mincut_sourcetrees.nwk").getPath()};

        //more realistic example, but much to slow (more than 300s)
        // String[] test = {"-el",this.getClass().getResource("/mcmahon.source_trees").getPath(),this.getClass().getResource("/mincut_sourcetrees.nwk").getPath()};
        final BCDCommandLineInterface test2 =  new BCDCommandLineInterface();
        final CmdLineParser parser = new CmdLineParser(test2);
        parser.parseArgument(test);
        test2.run();


        //todo in this test we should test if the cli works fine... checcking if the Flipcut stuff work has to be done in the flipcut test

        //assert
        //bootstrapThreshold
        assertEquals(0,test2.getAlgorithm().bootstrapThreshold);

        //UNIT_COST
        assertNotEquals(test2.getAlgorithm().weights, FlipCutWeights.Weights.UNIT_COST);
        //EDGE_AND_LEVEL
        assertEquals(test2.getAlgorithm().weights,FlipCutWeights.Weights.EDGE_AND_LEVEL);
        //BOOTSTRAP_VALUES
        assertNotEquals(test2.getAlgorithm().weights, FlipCutWeights.Weights.BOOTSTRAP_VALUES);
        //BOOTSTRAP_AND_LEVEL
        assertNotEquals(test2.getAlgorithm().weights, FlipCutWeights.Weights.BOOTSTRAP_AND_LEVEL);
*/


    }

    //todo convert
  /*  @Test
    public void testDoMainGiveTreesAndTresh() throws Exception{

        //arrange

        String[] test = {"-w","BOOTSTRAP_VALUES",this.getClass().getResource("/mincut_sourcetrees.nwk").getPath(),this.getClass().getResource("/mincut_sourcetrees.nwk").getPath(), "-b", "1"};


        BCDCommandLineInterface test2 = new BCDCommandLineInterface();
        BCDCommandLineInterface control = new BCDCommandLineInterface();
        test2.run(test);
        control.main(test);

        //act



        control.getBCDInstance().setWeights(FlipCutWeights.Weights.UNIT_COST);
        boolean unitCost = control.getBCDInstance().weights.equals(test2.getBCDInstance().weights);
        control.getBCDInstance().setWeights(FlipCutWeights.Weights.EDGE_AND_LEVEL);
        boolean edgeAndLevel = control.getBCDInstance().weights.equals(test2.getBCDInstance().weights);
        control.getBCDInstance().setWeights(FlipCutWeights.Weights.BOOTSTRAP_VALUES);
        boolean bootstrapValue = control.getBCDInstance().weights.equals(test2.getBCDInstance().weights);
        control.getBCDInstance().setWeights(FlipCutWeights.Weights.BOOTSTRAP_AND_LEVEL);
        boolean bootstrapAndLevel = control.getBCDInstance().weights.equals(test2.getBCDInstance().weights);


        //assert

        //bootstrapThreshold
        assertTrue(1.0,test2.getBCDInstance().bootstrapThreshold);

        //UNIT_COST
        assertEquals(false, FlipCutWeights.Weights.UNIT_COST);
        //EDGE_AND_LEVEL
        assertEquals(false, edgeAndLevel);
        //BOOTSTRAP_VALUES
        assertEquals(true, bootstrapValue);
        //BOOTSTRAP_AND_LEVEL
        assertEquals(false,bootstrapAndLevel);



    }


    @Test
    public void testDoMainEmpty() throws Exception {

        //arrange

        //save streams as ByteArrayOutoutStreams
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        ByteArrayOutputStream compareContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));
        System.setOut(new PrintStream(compareContent));

        // initialise
        String[] test = {};

        //create expected String
        CmdLineParser parser = new CmdLineParser(this);
        System.out.println("No argument is given");
        System.out.println("java CLI [options...] arguments...");
        parser.printUsage(System.out);
        System.out.println();
        System.out.println("  Example: java CLI" + parser.printExample(ExampleMode.ALL));

        //act
        BCDCommandLineInterface test2 = new BCDCommandLineInterface();
        test2.main(test);
       // System.setErr(null);
        //System.setOut(null);

        //assert

        assertEquals(compareContent.toString(), errContent.toString());


    }*/


}