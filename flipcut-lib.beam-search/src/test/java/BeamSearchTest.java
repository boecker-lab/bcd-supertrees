/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 14.02.17.
 */

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import phylo.tree.algorithm.SupertreeAlgorithm;
import phylo.tree.algorithm.flipcut.FlipCutMultiCut;
import phylo.tree.algorithm.flipcut.FlipCutSingleCutSimpleWeight;
import phylo.tree.algorithm.flipcut.costComputer.FlipCutWeights;
import phylo.tree.algorithm.flipcut.flipCutGraph.MultiCutGrapCutterFactories;
import phylo.tree.algorithm.flipcut.flipCutGraph.SimpleCutGraphCutter;
import phylo.tree.algorithm.flipcut.flipCutGraph.SingleCutGraphCutter;
import phylo.tree.io.Newick;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeUtils;
import phylo.tree.treetools.RFDistance;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

@RunWith(Parameterized.class)
public class BeamSearchTest {




    public static final List<Tree> trivialSource = Arrays.asList(Newick.getTreeFromString("((A,B),C);"), Newick.getTreeFromString("((A,B),C);"));
    public static final List<Tree> caterpillar = Arrays.asList(
            Newick.getTreeFromString("(((a,b),c),d)"),
            Newick.getTreeFromString("(((a,c),e),d);"),
            Newick.getTreeFromString("(((a,b),c),e);")
    );

    public static final List<Tree> cut = Arrays.asList(
            Newick.getTreeFromString("(((a,b),c),d)"),
            Newick.getTreeFromString("((a,b),(c,d));"),
            Newick.getTreeFromString("((a,b),(c,d));"),
            Newick.getTreeFromString("((a,b),(c,d));"),
            Newick.getTreeFromString("(((a,c),d),e);")
    );

    public static final List<Tree> realExample0 =  Arrays.asList(Newick.getTreeFromFile(new File("/home/fleisch/IdeaWorkspace/flipcut/flipcut-lib.beam-search/src/test/resources/data-d50-k50-1_phybp-s0r.nwk")));
    public static final List<Tree> realExample1 =  Arrays.asList(Newick.getTreeFromFile(new File("/home/fleisch/IdeaWorkspace/flipcut/flipcut-lib/src/test/resources/phylo/tree/algorithm/flipcut/smo.9.sourceTrees.tre")));
    public static final List<Tree> realExample2 = Arrays.asList( Newick.getAllTreesFromString("((t35:0.85146524062023543067,(((t23:0.47003124418285213704,(t5:0.09104560672893391438,t47:0.04179156035105692502):0.13994670123497821024):0.29611816711478095021,t46:0.70933427422272499197):0.47063062059377464008,(((t15:0.62728206409847775404,t38:0.72954619512532503656):0.83950883257683028837,(((t30:1.94729726134513847491,(((t29:1.13787511021578335857,t12:0.53671071090965649653):0.40918414112027873264,t19:0.66911341941212443274):0.21714837475223883190,(t11:0.80905560603953796672,(((t40:0.07141213110858052326,t36:0.26572933462418912853):0.26707429128525256301,t18:0.77252085423838590117):0.06389147919857497693,(t17:1.21647206698414778181,(t3:0.28865084692449094472,(t9:0.82806078243768865832,(t4:0.46280676954474581075,t8:0.43652934202943088904):0.10336273343019289350):0.26893021979338904703):0.05060104664331761121):0.02790910558009082956):0.58820097920026737093):0.44937881701099796627):0.07908342036084407833):0.09634943382207676599,t28:1.13002290190867094033):0.01502407291200677855,(t1:0.20841244021400201492,(t7:0.12386548889110673977,t24:0.21248746101640469952):0.08873180655254590821):1.09680376343815710882):0.02311066927707334379):0.07718507485281657365,(((t6:0.91180767129541151483,((t10:0.41458253639984410377,t42:0.52776368190653100143):0.25474983941264028875,((t13:0.61493132809661443794,(t22:0.35659491512716351425,t45:0.31796567155721788822):0.04283137676714951492):0.15479047244759566726,t48:0.93079177326858586472):0.10434681518233306086):0.14704123113293063319):0.00000479629726420491,(t41:0.54880943361583989226,(t27:0.31946043616104985441,(t33:1.44245791186473759637,t34:0.50201410868819207334):0.10138775232093752421):0.22024695288969273199):0.03041627320836625542):0.19308466573247973219,t39:1.30164193876933986793):0.30094783551829151769):0.06759210768006641834):0.13494186680524700161):1.23141760208209216643,OUTGOUP:1.23141760208209216643);\n" +"((t11:0.86717823503118507400,((((t41:0.63031969903248963316,(((t10:0.70127354653896423198,(t44:0.63862469648028374891,t42:0.55617475387949710797):0.03500043408627830088):0.27660883838573269156,((t45:0.58630050019458734845,t22:0.45144338666552447092):0.17856756719241198561,(t13:0.88909461012232304800,(t48:0.12044913555723422982,t2:0.06313761145495554172):0.60425826179170172292):0.10234314229513341576):0.12257735120506510484):0.22952506126324614955,((t26:0.19141887395258844196,(t39:0.65575842442769871621,t16:0.04613602007753455497):1.35722672851125225613):0.12587507893867475040,(t33:1.71931487320503939564,(t34:0.59732614834576747143,t27:0.54168471803105111562):0.29805247076818952578):0.20455701082806823643):0.01273939115188061320):0.22293781990009009508):0.26365301425379600309,(t5:0.07612896956496777168,(t47:0.03200796704384752805,t14:0.06965131626518236696):0.04962838303892876113):0.96761113687106736769):0.09463273133962619987,((((t38:0.67363524487699855126,(t20:0.18671587123538588293,t15:0.44028163500763928262):0.20742491006429131239):1.10676019250562029939,((t4:0.53358269818007175722,((t8:0.04986860649059381145,t37:0.22258895998427663532):0.36498469107701070335,t9:0.76179657486802965227):0.10045783926397991448):0.29283040953727995648,(t17:1.56816160711101804104,t18:0.91875413603985200961):0.06733653667565009382):0.97436525524310324542):0.01471640601921861413,(t19:0.90988821103423600523,((t29:1.21785746208119793899,t12:0.52698644542611450259):0.65521159412936125221,t32:1.22101089574236998381):0.06502260073279146624):0.22495051453157186017):0.06443030486465468887,(t24:1.27435481075057932543,(t30:1.89965049827109266900,t28:1.00236925795587117349):0.30811795389853785521):0.11776309832319978299):0.07112766194188423208):0.15069852977130485105,t35:0.94601003321201437846):0.08245812276690349385):1.26164542208891972130,OUTGOUP:1.26164542208891972130);"));

    public static final List<Tree> checkRoot1Source = Arrays.asList(
//            Newick.getTreeFromString("((b,c),(x,y,z));"),
//            Newick.getTreeFromString("((b,d),(x,y,z));"),
            Newick.getTreeFromString("((e,f),(x,y,z));"),
            Newick.getTreeFromString("((e,g),(x,y,z));")
    );

    public static final List<Tree> checkRoot2Source = Arrays.asList(
//            Newick.getTreeFromString("((b,c),(x,y,z));"),
//            Newick.getTreeFromString("((b,d),(x,y,z));"),
            Newick.getTreeFromString("((e,f),x,y,z);"),
            Newick.getTreeFromString("((e,g),x,y,z);")
    );

    public static final List<Tree> bryant1aSource = Arrays.asList(
            Newick.getTreeFromString("((b,c),(x,y,z));"),
            Newick.getTreeFromString("((b,d),(x,y,z));"),
            Newick.getTreeFromString("((e,f),(x,y,z));"),
            Newick.getTreeFromString("((e,g),(x,y,z));")
    );

    public static final List<Tree> bryant1bSource = Arrays.asList(
            Newick.getTreeFromString("((b,c),x,y,z);"),
            Newick.getTreeFromString("((b,d),x,y,z);"),
            Newick.getTreeFromString("((e,f),x,y,z);"),
            Newick.getTreeFromString("((e,g),x,y,z);")
    );

    public static final List<Tree> bryant1cSource = Arrays.asList(
            Newick.getTreeFromString("(((b,c),x,y,z),m);"),
            Newick.getTreeFromString("(((b,d),x,y,z),n);"),
            Newick.getTreeFromString("(((e,f),x,y,z),o);"),
            Newick.getTreeFromString("(((e,g),x,y,z),p);")
    );

    public static final List<Tree> bryant1dSource = Arrays.asList(
            Newick.getTreeFromString("(((b,c),(x,y,z)),m);"),
            Newick.getTreeFromString("(((b,d),(x,y,z)),n);"),
            Newick.getTreeFromString("(((e,f),(x,y,z)),o);"),
            Newick.getTreeFromString("(((e,g),(x,y,z)),p);")
    );

    public static final List<Tree> bryant2Source = Arrays.asList(
            Newick.getTreeFromString("((b,c),a);"),
            Newick.getTreeFromString("((b,d),a);"),
            Newick.getTreeFromString("((e,f),a);"),
            Newick.getTreeFromString("((e,g),a);"),
            Newick.getTreeFromString("((x,y),a);"),
            Newick.getTreeFromString("((x,z),a);")
    );

    public static final List<Tree> bryant3Source = Arrays.asList(
            Newick.getTreeFromString("((b,c),a);"),
            Newick.getTreeFromString("((b,d),a);"),
            Newick.getTreeFromString("((e,f),a);"),
            Newick.getTreeFromString("((e,g),a);"),
            Newick.getTreeFromString("((x,y),a);"),
            Newick.getTreeFromString("((x,z),a);")
    );

    public static final List<Tree> bryant4Source = Arrays.asList(
            Newick.getTreeFromString("((b,c),a);"),
            Newick.getTreeFromString("((b,d),a);"),
            Newick.getTreeFromString("((e,f),v);"),
            Newick.getTreeFromString("((e,g),v);"),
            Newick.getTreeFromString("((x,y),w);"),
            Newick.getTreeFromString("((x,z),w);")
    );

    public static final List<Tree> bryant5Source = Arrays.asList(
            Newick.getTreeFromString("((b,c),a);"),
            Newick.getTreeFromString("((b,d),v);"),
            Newick.getTreeFromString("((e,f),a);"),
            Newick.getTreeFromString("((e,g),v);")
    );

    public static final List<Tree> bryant6Source = Arrays.asList(
            Newick.getTreeFromString("(((b1,b2),(c1,c2)),a);"),
            Newick.getTreeFromString("(((b1,b2),(d1,d2)),a);"),
            Newick.getTreeFromString("(((e1,e2),(f1,f2)),a);"),
            Newick.getTreeFromString("(((e1,e2),(g1,g2)),a);")
    );

    public static final List<Tree> bryant7Source = Arrays.asList(
            Newick.getTreeFromString("((b,c),a);"),
            Newick.getTreeFromString("((b,d),a);"),
            Newick.getTreeFromString("((e,f),v);"),
            Newick.getTreeFromString("((e,g),v);")
    );



    @Parameterized.Parameter(0)
    public Tree expected;

    @Parameterized.Parameter(1)
    public Tree guideExpected;

    @Parameterized.Parameter(2)
    public List<Tree> source;

   /* @Parameterized.Parameter(2)
    public Tree guide = null;*/


    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        Collection<Object[]> params = new ArrayList<>();
        //trivial example
//        params.add(new Object[]{Newick.getTreeFromString("((A,B),C);"), Newick.getTreeFromString("((A,B),C);"), trivialSource});
//        params.add(new Object[]{Newick.getTreeFromString("(((a,b),c),e),d);"), Newick.getTreeFromString("(((a,b),c),e),d);"), caterpillar});
//        params.add(new Object[]{null,null, cut});
//        params.add(new Object[]{null,null,realExample0});
//        params.add(new Object[]{null,null,realExample1});
        params.add(new Object[]{null,null,realExample2});

        //bryant example
//        params.add(new Object[]{Newick.getTreeFromString("((e,f,g),(x,y,z));"), Newick.getTreeFromString("((e,f,g),(x,y,z));"), checkRoot1Source});
//        params.add(new Object[]{Newick.getTreeFromString("((e,f,g),x,y,z);"), Newick.getTreeFromString("((e,f,g),x,y,z);"), checkRoot2Source});
//        params.add(new Object[]{Newick.getTreeFromString("((b,c,d),(e,f,g),(x,y,z));"), Newick.getTreeFromString("((b,c,d),(e,f,g),(x,y,z));"), bryant1aSource});
//        params.add(new Object[]{Newick.getTreeFromString("((b,c,d),(e,f,g),x,y,z);"), Newick.getTreeFromString("((b,c,d),(e,f,g),x,y,z);"), bryant1bSource});
//        params.add(new Object[]{Newick.getTreeFromString("(((b,c,d),(e,f,g),x,y,z),m,n,o,p);"), Newick.getTreeFromString("(((b,c,d),(e,f,g),x,y,z),m,n,o,p);"), bryant1cSource});
//        params.add(new Object[]{Newick.getTreeFromString("(((b,c,d),(e,f,g),(x,y,z)),m,n,o,p);"),Newick.getTreeFromString("((b,c,d),(e,f,g),(x,y,z),m,n,o,p);"), bryant1dSource});

        return params;
    }


    public List<Tree> calculateSupertrees(SupertreeAlgorithm fs, Tree expected){
        return calculateSupertrees(fs,expected,null);
    }

    public List<Tree> calculateSupertrees(SupertreeAlgorithm fs, Tree expected, Tree guide) {
        fs.run();
        Tree supertree = fs.getResult();
        assertNotNull(supertree);

        supertree = TreeUtils.deleteInnerLabels(supertree);
        ArrayList<String> s = new ArrayList<>(TreeUtils.getLeafLabels(supertree));

        if (guide != null) {
            guide = TreeUtils.deleteInnerLabels(guide);
            TreeUtils.sortTree(guide,s);
            System.out.println(Newick.getStringFromTree(guide,true,false) + " Guide");
        }
        TreeUtils.sortTree(supertree,s);
        System.out.println(Newick.getStringFromTree(supertree,true,false) + " Estimated");

        if (expected != null) {
            TreeUtils.sortTree(expected,s);
            System.out.println(Newick.getStringFromTree(expected,true,false) + " Expected");
            Assert.assertEquals(TreeUtils.getLeafCount(source), supertree.getLeaves().length);
            int rfdist = RFDistance.getDifference(supertree, expected, false);
            assertEquals(0, rfdist);
        }

        return fs.getResults();
    }



    @Test
    public void bcdNoGuideTests() {
        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(new SingleCutGraphCutter.Factory(SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG));
        fs.setWeights(FlipCutWeights.Weights.EDGE_WEIGHTS);
        fs.setInput(TreeUtils.cloneTrees(TreeUtils.cloneTrees(source)));
        Tree exp = calculateSupertrees(fs, null).get(0);


        System.out.println("MultiSingle");
        FlipCutMultiCut fcm = new FlipCutMultiCut(MultiCutGrapCutterFactories.newInstance(MultiCutGrapCutterFactories.MultiCutterType.VAZIRANI,SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG));
        fcm.setNumberOfCuts(1);
        fcm.setWeights(FlipCutWeights.Weights.EDGE_WEIGHTS);
        fcm.setInput(TreeUtils.cloneTrees(TreeUtils.cloneTrees(source)));
        calculateSupertrees(fcm, null);

        System.out.println("Vazi");
        FlipCutMultiCut fcmVaz = new FlipCutMultiCut(MultiCutGrapCutterFactories.newInstance(MultiCutGrapCutterFactories.MultiCutterType.VAZIRANI,SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG));
        fcmVaz.setNumberOfCuts(100);
        fcmVaz.setWeights(FlipCutWeights.Weights.EDGE_WEIGHTS);
        fcmVaz.setInput(TreeUtils.cloneTrees(TreeUtils.cloneTrees(source)));
        calculateSupertrees(fcmVaz, null);

        System.out.println("Greedy");
        FlipCutMultiCut fcmG = new FlipCutMultiCut(MultiCutGrapCutterFactories.newInstance(MultiCutGrapCutterFactories.MultiCutterType.GREEDY,SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG));
        fcmG.setNumberOfCuts(100);
        fcmG.setWeights(FlipCutWeights.Weights.EDGE_WEIGHTS);
        fcmG.setInput(TreeUtils.cloneTrees(TreeUtils.cloneTrees(source)));
        calculateSupertrees(fcmG, null);

        System.out.println("Greedy_Rand");
        FlipCutMultiCut fcmGR = new FlipCutMultiCut(MultiCutGrapCutterFactories.newInstance(MultiCutGrapCutterFactories.MultiCutterType.GREEDY_RAND,SimpleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG));
        fcmGR.setNumberOfCuts(100);
        fcmGR.setWeights(FlipCutWeights.Weights.EDGE_WEIGHTS);
        fcmGR.setInput(TreeUtils.cloneTrees(TreeUtils.cloneTrees(source)));
        calculateSupertrees(fcmGR, null);

       /* System.out.println("MC");
        FlipCutMultiCut fcmMC = new FlipCutMultiCut(MultiCutGrapCutterFactories.newInstance(MultiCutGrapCutterFactories.MultiCutterType.MC));
        fcmMC.setNumberOfCuts(10);
        fcmMC.setWeights(FlipCutWeights.Weights.EDGE_WEIGHTS);
        fcmMC.setInput(TreeUtils.cloneTrees(TreeUtils.cloneTrees(source)));
        calculateSupertrees(fcmMC, exp);*/

    }

   /* @Test
    public void bcdGuideTests() {
        GreedySCMAlgorithm prepro = new GreedySCMAlgorithm();
        prepro.setInput(TreeUtils.cloneTrees(source));
        prepro.setScorer(TreeScorers.getScorer(TreeScorers.ScorerType.OVERLAP));
        prepro.run();
        Tree guide = prepro.getResult();
        System.out.println(Newick.getStringFromTree(guide));

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setWeights(FlipCutWeights.Weights.UNIT_COST);

        fs.setInput(TreeUtils.cloneTrees(source));
        Tree sNoGuide = calculateSupertrees(fs, expected).get(0);

        fs.setInput(TreeUtils.cloneTrees(source), guide);
        Tree sGuide = calculateSupertrees(fs, expected,guide).get(0);
        //todo check difference?
    }*/

}
