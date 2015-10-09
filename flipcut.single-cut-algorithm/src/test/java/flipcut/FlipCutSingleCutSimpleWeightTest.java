package flipcut;

import epos.model.tree.Tree;
import epos.model.tree.io.Newick;
import epos.model.tree.treetools.RFDistance;
import epos.model.tree.treetools.TreeSorter;
import epos.model.tree.treetools.UnsupportedCladeReduction;
import flipcut.costComputer.FlipCutWeights;
import flipcut.flipCutGraph.CutGraphCutter;
import flipcut.flipCutGraph.SingleCutGraphCutter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 13.12.12
 * Time: 12:29
 */
public class FlipCutSingleCutSimpleWeightTest {

    private static final String t1String = "((((((t41:0.51127305213250950899,(((t10:0.48018958596953098539,t44:0.51766121345282511967):0.37820800065158260983,((t2:0.00660106631918401174,t48:0.17901438694030952226):0.70069290697404085222,((t22:0.30608032081197839025,t45:0.32910726217930208470):0.13907138931720755548,t13:0.62789722173030759755):0.03783312466452365408):0.07573942024422802843):0.24348427124218707807,((t33:1.33857077555221026799,t34:0.81864697245480877452):0.08302957632262716881,(t6:0.67556512379037569893,t26:0.11416850776113425525):0.11285620720599905120):0.01269845244828135024):0.18148015183588295240):0.28669353968909555563,t35:0.99780919637598053384):0.02105833720180345578,(t46:0.88804532403322122835,(t5:0.10720249642171776616,t47:0.01618975705603504026):0.44723029560503752000):0.54641703682636777462):0.14885030353660033686,((t15:0.38881653114071035304,t38:0.67610437039192539110):0.86911725442375309392,((t32:1.22047097150285499545,(t29:1.14954243502324104931,t12:0.77920252832655489961):0.52560846837640073925):0.00000554872119392131,(t19:0.06164572518812583973,t31:0.16194276841398644318):0.93228146111935794593):0.27711156885639709468):0.05322190115640378777):0.00220218528042856682,(((((t40:0.22419064499467949991,t36:0.32852964025943387494):0.27509704658405398359,((t4:0.47075940401174559691,((t8:0.21578610695362226979,t37:0.12369789172827899693):0.29415648915142633690,t9:0.45639359231824716545):0.01195645149457751077):0.06231642973057911761,t18:0.89459654150627965219):0.09238968056475078960):0.52051074407778008535,t11:0.75643709739575382134):0.44310312146190178328,(t28:0.09705344675146689026,t25:0.09581362862889745979):0.98563758006327650829):0.10030868980958286552,(t30:2.02661657844797815642,(t7:0.20943807439325906605,t24:0.16850223084845714072):0.68724964545983480146):0.11336248408125269849):0.03623679306498845537):1.24245817089725174576,OUTGOUP:1.24245817089725174576);";
    private static final String t2String = "(((t19:0.76851186613957689353,((t29:1.06511293584646438148,t12:0.57551667214169643927):0.48146775966657029411,t32:1.27456629785236663643):0.10871501349219915566):0.19905266068350777053,(((t30:1.73686687040379927893,(t25:0.09308655740498866793,t28:0.11714121416339394466):0.31593918290617012401):0.28861579427440231527,(((t7:0.11381254553479662472,t24:0.09919280566902602403):0.06693334548668478101,t1:0.43777283635603053691):0.01926286910012516007,t43:0.27900191452324607511):0.83123201010036917857):0.01056317505101037751,((((t48:0.13818818295665824536,t2:0.02172251892758649688):0.46030015647880745400,(((t22:0.30170653775239181238,t45:0.46210860694036692831):0.04998708374295702389,t13:0.50093349419156196767):0.18688716729956442175,((t10:0.47606526165174345433,t42:0.64463156797583576196):0.26969597717720450447,((t27:0.43693562626233761748,t34:0.50467398072554769950):0.11323892175753487443,(t16:1.07372540293411966594,t33:1.48415037187143106223):0.11239244135045381445):0.12443418609919322526):0.00000439759789274463):0.07014822425269182093):0.35706437472627405860,(t46:0.65477710857634952024,(t23:0.26688025947077614886,(t5:0.14986974493162311117,(t47:0.03578300174512620557,t14:0.08046779586736808876):0.01744560126236641140):0.23829753409763010374):0.32088584435621120283):0.60899763979712817630):0.16076004800453957966,(((t36:0.65093963650537556287,t18:0.74924744525920916605):0.04412271886537624116,((t9:0.58325891521505202064,(t4:0.33692049974871485229,t8:0.52673158689223520401):0.04074981656869958169):0.32179059900046791398,t3:0.19899561058090420307):0.00231658272502263587):0.70764766351057650784,((t20:0.16793658301978156566,t15:0.40945167238803842347):0.18412785296032174731,t38:0.64021773270992554661):0.51919154824647950619):0.04788728470109086116):0.09182422225671280080):0.02595483928734209841):1.41955781856683360687,OUTGOUP:1.41955781856683360687);";

//    @Test
    public void testSimpleExample(){
        Tree t1 = Newick.getTreeFromString("((A,B),C);");
        Tree t2 = Newick.getTreeFromString("((A,B),C);");

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
        fs.setInput(Arrays.asList(t1, t2));
        fs.run();
        Tree supertree = fs.getResult();
        assertNotNull(supertree);
        Assert.assertEquals(5, supertree.vertexCount());
        Assert.assertEquals(3, supertree.getLeaves().length);
        int rfdist = RFDistance.getDifference(supertree, Newick.getTreeFromString("((A,B),C);"), false);
        assertEquals(0, rfdist);

        System.out.println(Newick.getStringFromTree(supertree));
    }

    @Test
    public void testBryantSample1(){
        Tree t1 = Newick.getTreeFromString("((b,c),a);");
        Tree t2 = Newick.getTreeFromString("((b,d),a);");
        Tree t3 = Newick.getTreeFromString("((e,f),a);");
        Tree t4 = Newick.getTreeFromString("((e,g),a);");
        List<Tree> input = Arrays.asList(t1, t2, t3, t4);

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setWeights(FlipCutWeights.Weights.UNIT_COST);
        fs.setInput(input);
        UnsupportedCladeReduction r = new UnsupportedCladeReduction(input);

        fs.run();
        Tree supertree = fs.getResult();
        Tree toReduce = supertree.cloneTree();
        r.reduceUnsupportedClades(toReduce);

        assertNotNull(supertree);
        assertNotNull(toReduce);
        System.out.println(Newick.getStringFromTree(supertree));
        System.out.println(Newick.getStringFromTree(toReduce));
    }

    @Test
    public void testBryantSample2(){
        Tree t1 = Newick.getTreeFromString("((b,c),a);");
        Tree t2 = Newick.getTreeFromString("((b,d),a);");
        Tree t3 = Newick.getTreeFromString("((e,f),v);");
        Tree t4 = Newick.getTreeFromString("((e,g),v);");
        List<Tree> input = Arrays.asList(t1, t2, t3, t4);

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setWeights(FlipCutWeights.Weights.UNIT_COST);
        fs.setInput(input);
        UnsupportedCladeReduction r = new UnsupportedCladeReduction(input);

        fs.run();
        Tree supertree = fs.getResult();
        Tree toReduce = supertree.cloneTree();
        r.reduceUnsupportedClades(toReduce);

        assertNotNull(supertree);
        assertNotNull(toReduce);
        System.out.println(Newick.getStringFromTree(supertree));
        System.out.println(Newick.getStringFromTree(toReduce));
    }

    @Test
    public void testBryantSample3(){
        Tree t1 = Newick.getTreeFromString("((b,c),a);");
        Tree t2 = Newick.getTreeFromString("((b,d),a);");
        Tree t3 = Newick.getTreeFromString("((e,f),a);");
        Tree t4 = Newick.getTreeFromString("((e,g),a);");
        Tree t5 = Newick.getTreeFromString("((x,y),a);");
        Tree t6 = Newick.getTreeFromString("((x,z),a);");
        List<Tree> input = Arrays.asList(t1, t2, t3, t4,t5,t6);

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setWeights(FlipCutWeights.Weights.UNIT_COST);
        fs.setInput(input);
        UnsupportedCladeReduction r = new UnsupportedCladeReduction(input);

        fs.run();
        Tree supertree = fs.getResult();
        Tree toReduce = supertree.cloneTree();
        r.reduceUnsupportedClades(toReduce);

        assertNotNull(supertree);
        assertNotNull(toReduce);
        System.out.println(Newick.getStringFromTree(supertree));
        System.out.println(Newick.getStringFromTree(toReduce));
    }

    @Test
    public void testBryantSample4(){
        Tree t1 = Newick.getTreeFromString("((b,c),a);");
        Tree t2 = Newick.getTreeFromString("((b,d),a);");
        Tree t3 = Newick.getTreeFromString("((e,f),v);");
        Tree t4 = Newick.getTreeFromString("((e,g),v);");
        Tree t5 = Newick.getTreeFromString("((x,y),w);");
        Tree t6 = Newick.getTreeFromString("((x,z),w);");
        List<Tree> input = Arrays.asList(t1, t2, t3, t4,t5,t6);

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setWeights(FlipCutWeights.Weights.UNIT_COST);
        fs.setInput(input);
        UnsupportedCladeReduction r = new UnsupportedCladeReduction(input);

        fs.run();
        Tree supertree = fs.getResult();
        Tree toReduce = supertree.cloneTree();
        r.reduceUnsupportedClades(toReduce);

        assertNotNull(supertree);
        assertNotNull(toReduce);
        System.out.println(Newick.getStringFromTree(supertree));
        System.out.println(Newick.getStringFromTree(toReduce));
    }

    @Test
    public void testBryantSample5(){
        Tree t1 = Newick.getTreeFromString("((b,c),a);");
        Tree t2 = Newick.getTreeFromString("((b,d),v);");
        Tree t3 = Newick.getTreeFromString("((e,f),a);");
        Tree t4 = Newick.getTreeFromString("((e,g),v);");
        List<Tree> input = Arrays.asList(t1, t2, t3, t4);

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setWeights(FlipCutWeights.Weights.UNIT_COST);
        fs.setInput(input);
        UnsupportedCladeReduction r = new UnsupportedCladeReduction(input);

        fs.run();
        Tree supertree = fs.getResult();
        Tree toReduce = supertree.cloneTree();
        r.reduceUnsupportedClades(toReduce);

        assertNotNull(supertree);
        assertNotNull(toReduce);
        System.out.println(Newick.getStringFromTree(supertree));
        System.out.println(Newick.getStringFromTree(toReduce));
    }

    @Test
    public void testBryantSample6(){
        Tree t1 = Newick.getTreeFromString("(((b1,b2),(c1,c2)),a);");
        Tree t2 = Newick.getTreeFromString("(((b1,b2),(d1,d2)),a);");
        Tree t3 = Newick.getTreeFromString("(((e1,e2),(f1,f2)),a);");
        Tree t4 = Newick.getTreeFromString("(((e1,e2),(g1,g2)),a);");
        List<Tree> input = Arrays.asList(t1, t2, t3, t4);

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setWeights(FlipCutWeights.Weights.UNIT_COST);
        fs.setInput(input);
        UnsupportedCladeReduction r = new UnsupportedCladeReduction(input);

        fs.run();
        Tree supertree = fs.getResult();
        Tree toReduce = supertree.cloneTree();
        r.reduceUnsupportedClades(toReduce);

        assertNotNull(supertree);
        assertNotNull(toReduce);
        System.out.println(Newick.getStringFromTree(supertree));
        System.out.println(Newick.getStringFromTree(toReduce));
    }

    @Test
    public void testBryantSample7Negative(){
        Tree t1 = Newick.getTreeFromString("((b,c),a);");
        Tree t2 = Newick.getTreeFromString("((b,d),a);");
        Tree t3 = Newick.getTreeFromString("((e,f),a);");
        Tree t4 = Newick.getTreeFromString("((e,g),a);");
        Tree t5 = Newick.getTreeFromString("(((f,g,e),(c,d,b)),v);");
        List<Tree> input = Arrays.asList(t1, t2, t3, t4,t5);

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setWeights(FlipCutWeights.Weights.UNIT_COST);
        fs.setInput(input);
        UnsupportedCladeReduction r = new UnsupportedCladeReduction(input);

        fs.run();
        Tree supertree = fs.getResult();
        Tree toReduce = supertree.cloneTree();
        r.reduceUnsupportedClades(toReduce);

        assertNotNull(supertree);
        assertNotNull(toReduce);
        System.out.println(Newick.getStringFromTree(supertree));
        System.out.println(Newick.getStringFromTree(toReduce));
    }



    @Test
    public void testMalteExample(){
        /*
        (((a,b),c),d);
        (((a,c),e),d);
        (((a,b),c),e);
         */
        Tree t1 = Newick.getTreeFromString("(((a,b),c),d)");
        Tree t2 = Newick.getTreeFromString("(((a,c),e),d);");
        Tree t3 = Newick.getTreeFromString("(((a,b),c),e);");

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
        fs.setInput(Arrays.asList(t1, t2, t3));
        fs.run();
        Tree supertree = fs.getResult();
        System.out.println(Newick.getStringFromTree(supertree));
    }

    @Test
    public void scaffoldFail(){
        Tree t1 = Newick.getTreeFromString("(((c,b),a),(d,e,f));");
        Tree t2 = Newick.getTreeFromString("((((a,b),y),(x,c)),(d,e,f));");
//        Tree t3 = Newick.getTreeFromString("(((a,b),c),e);");

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setInput(Arrays.asList(t1, t2));
        fs.run();
        Tree supertree = fs.getResult();
        System.out.println(Newick.getStringFromTree(supertree));
    }

    @Test
    public void scaffoldFailBS(){
        Tree t1 = Newick.getTreeFromString("(((c,b):90,a):66,(d,e,f):88);");
        Tree t2 = Newick.getTreeFromString("((((a,b):50,y):50,(x,c):55):77,(d,e,f):88);");
//        Tree t3 = Newick.getTreeFromString("(((a,b),c),e);");

        FlipCutSingleCutSimpleWeight fs = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        fs.setInput(Arrays.asList(t1, t2));
        fs.run();
        Tree supertree = fs.getResult();
        System.out.println(Newick.getStringFromTree(supertree));
    }

    @Test
    public void testSupertree2000_25(){
        Tree[] trees = Newick.getAllTreesFromString("((t35:0.85146524062023543067,(((t23:0.47003124418285213704,(t5:0.09104560672893391438,t47:0.04179156035105692502):0.13994670123497821024):0.29611816711478095021,t46:0.70933427422272499197):0.47063062059377464008,(((t15:0.62728206409847775404,t38:0.72954619512532503656):0.83950883257683028837,(((t30:1.94729726134513847491,(((t29:1.13787511021578335857,t12:0.53671071090965649653):0.40918414112027873264,t19:0.66911341941212443274):0.21714837475223883190,(t11:0.80905560603953796672,(((t40:0.07141213110858052326,t36:0.26572933462418912853):0.26707429128525256301,t18:0.77252085423838590117):0.06389147919857497693,(t17:1.21647206698414778181,(t3:0.28865084692449094472,(t9:0.82806078243768865832,(t4:0.46280676954474581075,t8:0.43652934202943088904):0.10336273343019289350):0.26893021979338904703):0.05060104664331761121):0.02790910558009082956):0.58820097920026737093):0.44937881701099796627):0.07908342036084407833):0.09634943382207676599,t28:1.13002290190867094033):0.01502407291200677855,(t1:0.20841244021400201492,(t7:0.12386548889110673977,t24:0.21248746101640469952):0.08873180655254590821):1.09680376343815710882):0.02311066927707334379):0.07718507485281657365,(((t6:0.91180767129541151483,((t10:0.41458253639984410377,t42:0.52776368190653100143):0.25474983941264028875,((t13:0.61493132809661443794,(t22:0.35659491512716351425,t45:0.31796567155721788822):0.04283137676714951492):0.15479047244759566726,t48:0.93079177326858586472):0.10434681518233306086):0.14704123113293063319):0.00000479629726420491,(t41:0.54880943361583989226,(t27:0.31946043616104985441,(t33:1.44245791186473759637,t34:0.50201410868819207334):0.10138775232093752421):0.22024695288969273199):0.03041627320836625542):0.19308466573247973219,t39:1.30164193876933986793):0.30094783551829151769):0.06759210768006641834):0.13494186680524700161):1.23141760208209216643,OUTGOUP:1.23141760208209216643);\n" +
                "((t11:0.86717823503118507400,((((t41:0.63031969903248963316,(((t10:0.70127354653896423198,(t44:0.63862469648028374891,t42:0.55617475387949710797):0.03500043408627830088):0.27660883838573269156,((t45:0.58630050019458734845,t22:0.45144338666552447092):0.17856756719241198561,(t13:0.88909461012232304800,(t48:0.12044913555723422982,t2:0.06313761145495554172):0.60425826179170172292):0.10234314229513341576):0.12257735120506510484):0.22952506126324614955,((t26:0.19141887395258844196,(t39:0.65575842442769871621,t16:0.04613602007753455497):1.35722672851125225613):0.12587507893867475040,(t33:1.71931487320503939564,(t34:0.59732614834576747143,t27:0.54168471803105111562):0.29805247076818952578):0.20455701082806823643):0.01273939115188061320):0.22293781990009009508):0.26365301425379600309,(t5:0.07612896956496777168,(t47:0.03200796704384752805,t14:0.06965131626518236696):0.04962838303892876113):0.96761113687106736769):0.09463273133962619987,((((t38:0.67363524487699855126,(t20:0.18671587123538588293,t15:0.44028163500763928262):0.20742491006429131239):1.10676019250562029939,((t4:0.53358269818007175722,((t8:0.04986860649059381145,t37:0.22258895998427663532):0.36498469107701070335,t9:0.76179657486802965227):0.10045783926397991448):0.29283040953727995648,(t17:1.56816160711101804104,t18:0.91875413603985200961):0.06733653667565009382):0.97436525524310324542):0.01471640601921861413,(t19:0.90988821103423600523,((t29:1.21785746208119793899,t12:0.52698644542611450259):0.65521159412936125221,t32:1.22101089574236998381):0.06502260073279146624):0.22495051453157186017):0.06443030486465468887,(t24:1.27435481075057932543,(t30:1.89965049827109266900,t28:1.00236925795587117349):0.30811795389853785521):0.11776309832319978299):0.07112766194188423208):0.15069852977130485105,t35:0.94601003321201437846):0.08245812276690349385):1.26164542208891972130,OUTGOUP:1.26164542208891972130);");
//        FlipCutSingleCutSimpleWeight a = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
        FlipCutSingleCutSimpleWeight a = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
        a.setWeights(FlipCutWeights.Weights.EDGE_AND_LEVEL);
        long t = System.currentTimeMillis();
        a.setInput(Arrays.asList(trees[0], trees[1]));
        a.run();
        Tree tree = a.getResult();
        System.out.println("time : " + (System.currentTimeMillis()-t));


        String mt = "((((t30:1.290659,(t21:0.354582,(t28:0.113505,t25:0.106899):0.235366):0.257865):0.206891,(((((t40:0.162831,t36:0.261645):0.268882,((((t37:0.099151,t8:0.121348):0.222927,t4:0.326142):0.049862,t9:0.470160):0.131571,(t18:0.610145,t3:0.213747):0.006927):0.036167):0.015578,t17:0.768445):0.401205,t11:0.526053):0.209854,((t19:0.077338,t31:0.147930):0.592102,((t29:0.765377,t12:0.318797):0.404620,t32:0.992713):0.010507):0.135329):0.075242):0.018362,(((t38:0.418386,(t15:0.319527,t20:0.092976):0.099825):0.594375,(t35:0.633074,((((t5:0.140725,(t47:0.044152,t14:0.075033):0.002275):0.099881,t23:0.262932):0.229256,t46:0.581066):0.481380,(t41:0.343125,((t33:1.143012,(t34:0.355399,t27:0.328268):0.187727):0.049112,(((t16:0.069209,t39:0.292382):0.709816,(t26:0.098316,t6:0.515534):0.067670):0.017594,(((t13:0.456708,(t45:0.335605,t22:0.224024):0.046379):0.045893,(t48:0.123697,t2:0.057706):0.430220):0.036544,((t42:0.342213,t10:0.392808):0.008420,t44:0.358730):0.241165):0.122041):0.005728):0.071820):0.243179):0.040400):0.056510):0.004441,(t43:0.241675,(t1:0.226737,(t24:0.149040,t7:0.147785):0.055858):0.052196):0.616819):0.015087):0.5,OUTGOUP:1.5);";
        Tree mtt = Newick.getTreeFromString(mt);
        TreeSorter.sortTree(mtt);
        TreeSorter.sortTree(tree);

        System.out.println("F:"+ Newick.getStringFromTree(tree));
        System.out.println("M:"+ Newick.getStringFromTree(mtt));

        System.out.println(RFDistance.getDifference(tree, mtt, true));
    }

    @Test
    public void testManyInputTrees(){
        File inputFile =  new File(getClass().getResource("/flipcut/omm.source.Trees.tre").getFile());
//        File inputFile =  new File(getClass().getResource("/flipcut/mcmahon.source_trees").getFile());
//        File inputFile =  new File(getClass().getResource("/flipcut/berrysemple-sourcetrees.tre").getFile());
//        File inputFile =  new File(getClass().getResource("/flipcut/smo.9.sourceTrees.tre").getFile());
//        File inputFile =  new File(getClass().getResource("/flipcut/sm.9.sourceTrees_OptSCM-Rooting.tre").getFile());
//        File inputFile =  new File(getClass().getResource("/flipcut/smo.8.sourceTrees.tre").getFile());
        Tree[] trees =  Newick.getTreeFromFile(inputFile);

        long t = System.currentTimeMillis();
//        GreedySCMAlgorithm algo = new GreedySCMAlgorithm(new GreedyTreeSelector(new ConsensusResolutionScorer(TreeScorer.ConsensusMethods.STRICT), TreeUtilsBasic.cloneTrees(trees)));
//        Tree guideTree = algo.getSupertree();
        System.out.println("SCM time : " + (System.currentTimeMillis()-t));

        t = System.currentTimeMillis();
        FlipCutSingleCutSimpleWeight a = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG);
//        FlipCutSingleCutSimpleWeight a = new FlipCutSingleCutSimpleWeight(SingleCutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_AHOJI_ORLIN);
        a.setWeights(FlipCutWeights.Weights.UNIT_COST);
//        a.setInput(Arrays.asList(trees),guideTree);
        a.setInput(Arrays.asList(trees));
        a.setNumberOfThreads(2);
        a.run();
        Tree sTree =  a.getResult();
        System.out.println("time : " + (System.currentTimeMillis()-t));
        assertNotNull(sTree);
        System.out.println(Newick.getStringFromTree(sTree));
    }

}
