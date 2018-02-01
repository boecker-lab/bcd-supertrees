import gnu.trove.TIntCollection;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import mincut.cutGraphImpl.minCutKargerStein.CompressedKargerGraph;
import mincut.cutGraphImpl.minCutKargerStein.GraphUtils;
import mincut.cutGraphImpl.minCutKargerStein.KargerStein;
import org.junit.Test;

import java.util.*;

public class CompressedKargerTest {
    /*
        p max 8 11
        n 1 s
        n 8 t
        a 1 2 5
        a 2 3 5
        a 3 4 5
        a 3 5 2
        a 4 2 5
        a 4 7 2
        a 5 6 5
        a 6 7 5
        a 6 8 4
        a 7 5 5
        a 7 8 1
    */
    private static final List<TIntCollection> edges = createEdges(GraphUtils.getArray(CompressedKargerGraph.class.getResource("/kargerAdj.txt").getFile()));
    /*private static final List<TIntCollection> edges = Collections.unmodifiableList(Arrays.asList(
            new TIntArrayList(new int[]{1, 2})
            , new TIntArrayList(new int[]{2, 3})
            , new TIntArrayList(new int[]{3, 4})
            , new TIntArrayList(new int[]{3, 5})
            , new TIntArrayList(new int[]{4, 2})
            , new TIntArrayList(new int[]{4, 7})
            , new TIntArrayList(new int[]{5, 6})
            , new TIntArrayList(new int[]{6, 7})
            , new TIntArrayList(new int[]{6, 8})
            , new TIntArrayList(new int[]{7, 5})
            , new TIntArrayList(new int[]{7, 8})
    ));*/

    //    private static final List<Integer> weights = Collections.unmodifiableList(Arrays.asList(5, 5, 5, 2, 5, 2, 5, 5, 4, 5, 1));
    private static final List<? extends Number> weights = makeWeights();


    private static List<? extends Number> makeWeights() {
        Number[] weights = new Number[edges.size()];
        Arrays.fill(weights, 1d);
        return Arrays.asList(weights);
    }

    @Test
    public void simpleExampleTest() {
        CompressedKargerGraph cg = new CompressedKargerGraph(edges, weights);
        System.out.println();
    }


    public static List<TIntCollection> createEdges(int[][] arr) {
        final Set<TIntSet> edges = new HashSet<>();
        for (int i = 0; i < arr.length; i++) {
            for (int edgeTo : arr[i]) {
                edges.add(new TIntHashSet(new int[]{i, edgeTo}));
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(edges));
    }

    @Test
    public void coreTest() {
        System.out.println(Math.sqrt(2d));
        System.out.println(KargerStein.SQRT2);
        int f = 0;
        for (int i = 0; i < 1000; i++) {
            KargerStein<CompressedKargerGraph> cutter = new KargerStein<>();
            CompressedKargerGraph cg = new CompressedKargerGraph(edges, weights);
            CompressedKargerGraph cuttedGraph = cutter.getMinCut(cg, true);
            int comparison = Double.compare(3d, cuttedGraph.mincutValue());
            if (comparison != 0) {
                f++;
            }
            System.out.println((comparison == 0) + ": " + cuttedGraph.getTaxaCutSets() + " Score: " + cuttedGraph.mincutValue());
        }
        System.out.println((f / 1000d * 100) + "% wrong cuts!");
    }
}
