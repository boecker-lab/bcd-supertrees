package flipcut;

import flipcut.costComputer.FlipCutWeights;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineParser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by Florian on 22.04.2014.
 */


public class BCDCommandLineInterfaceTest {

    //Test if parameter gets translated into flipcut parameters in the right way
    @Test
    public void test_used_algo() throws Exception{
        String[] test_string ={"-a","FC"};
        final BCDCommandLineInterface test_interface = new BCDCommandLineInterface();
        final CmdLineParser parser= new CmdLineParser(test_interface);
        parser.parseArgument(test_string);
        test_interface.initAlgorithm();

        //assert

        //assertEquals("FC", test_interface.getAlgorithm().cutter.type); //TODO: Need access to cutter type? (protected)
    }
    @Test
    public void test_set_weights_unit_cost() throws Exception{           //TODO: Test every weight?
        String[] test_String ={"-w","UNIT_WEIGHT"};
        final BCDCommandLineInterface test_interface = new BCDCommandLineInterface();
        final CmdLineParser parser= new CmdLineParser(test_interface);
        parser.parseArgument(test_String);
        test_interface.initWheightMapping();

        //assert

        assertEquals("UNIT_COST", test_interface.getAlgorithm().weights.name());
    }

    @Test
    public void test_bst_threshold() throws Exception{
        String[] test_String ={"-b","33"};
        final BCDCommandLineInterface test_interface = new BCDCommandLineInterface();
        final CmdLineParser parser= new CmdLineParser(test_interface);
        parser.parseArgument(test_String);

        //assert

        assertEquals(33, test_interface.getAlgorithm().bootstrapThreshold);

    }

     //Test if CLI parameter input works as intended
    @Test
    public void test_SCMtree_usage() throws Exception{
        String[] test_String ={"-s"};
        final BCDCommandLineInterface test_interface = new BCDCommandLineInterface();
        final CmdLineParser parser= new CmdLineParser(test_interface);
        parser.parseArgument(test_String);

        //assert

        assertEquals(true, test_interface.useSCM);

    }

    @Test
    public void test_output_path() throws Exception{
        String[] test_String ={"-o","C:\\fictional.txt"};
        final BCDCommandLineInterface test_interface = new BCDCommandLineInterface();
        final CmdLineParser parser= new CmdLineParser(test_interface);
        parser.parseArgument(test_String);

        //assert

        assertEquals("C:\\fictional.txt", test_interface.output.getName());

    }

    @Test
    public void test_ucr_usage() throws Exception{
        String[] test_String ={"-u"};
        final BCDCommandLineInterface test_interface = new BCDCommandLineInterface();
        final CmdLineParser parser= new CmdLineParser(test_interface);
        parser.parseArgument(test_String);

        //assert

        assertEquals(true, test_interface.useSCM);

    }





    @Test
    public void testDoMainGiveTrees()  throws Exception{

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