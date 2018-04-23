package mincut.cutGraphImpl.minCutKargerStein;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 */

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.KargerSteinCutGraph;
import mincut.cutGraphAPI.bipartition.AbstractBipartition;
import mincut.cutGraphAPI.bipartition.DefaultBipartition;
import mincut.cutGraphAPI.bipartition.STCut;
import mincut.cutGraphAPI.bipartition.SimpleHashableCut;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class KargerSteinTest {
    @Test
    public void coreTest() throws ExecutionException, InterruptedException {
        System.out.println(Math.sqrt(2d));
        System.out.println(KargerStein.SQRT2);
        int f = 0;
        String testFile = getClass().getResource("/kargerAdj.txt").getFile();
        final int[][] arr = GraphUtils.getArray(testFile);
        for (int i = 0; i < 1000; i++) {
            KargerStein cutter = new KargerStein();
            SimpleHashableCut cuttedGraph = (SimpleHashableCut) cutter.getMinCut(arr, true);
            int comparison = Double.compare(3d, cuttedGraph.minCutValue());
            if (comparison != 0) {
                f++;
            }
            System.out.println((comparison == 0) + ": " + cuttedGraph);

        }
        System.out.println((f / 1000d * 100) + "% wrong cuts!");

    }

    @Test
    public void testAPI() throws ExecutionException, InterruptedException {
        String testFile = getClass().getResource("/kargerAdj.txt").getFile();
        int[][] arr = GraphUtils.getArray(testFile);


        Set<TIntSet> edges = new HashSet<>();
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                TIntSet s = new TIntHashSet(Arrays.asList(i, arr[i][j]));
                edges.add(s);
            }
        }


//        while (true) {
        long time = System.currentTimeMillis();
        GoldbergTarjanCutGraph<Integer> goldi = new GoldbergTarjanCutGraph<>();
        goldi.setThreads(1);

        for (int s = 0; s < arr.length - 1; s++) {
            for (int t = s + 1; t < arr.length; t++) {
                goldi.submitSTCutCalculation(s, t);
            }
        }

        for (TIntSet edge : edges) {
            int[] e = edge.toArray();
            goldi.addEdge(e[0], e[1], 1);
            goldi.addEdge(e[1], e[0], 1);
        }

        STCut<Integer> goldCut = goldi.calculateMinCut();
        int[] c = goldCut.getCutSet().stream().sorted((i, j) -> Integer.compare(j, i)).mapToInt(i -> ((int) i)).toArray();
        System.out.println("Gold Score: " + goldCut.minCutValue() + " Cutset: " + Arrays.toString(c));
        System.out.println("time" + (System.currentTimeMillis() - time) / 1000d);


        long time2 = System.currentTimeMillis();
        KargerSteinCutGraph<Integer, DefaultBipartition.Factory<Integer>> kargi = new KargerSteinCutGraph<>(new DefaultBipartition.Factory<Integer>());
        for (TIntSet edge : edges) {
            int[] e = edge.toArray();
            kargi.addEdge(e[0], e[1], 1);
        }

        List<AbstractBipartition<Integer>> randCuts = kargi.calculateMinCuts();
        System.out.println(randCuts.size());
        AbstractBipartition<Integer> randCut = randCuts.get(0);
        int[] rc = randCut.getCutSet().stream().sorted((i, j) -> Integer.compare(j, i)).mapToInt(i -> ((int) i)).toArray();
        System.out.println("Karger Score: " + randCut.minCutValue() + " Cutset: " + Arrays.toString(rc));
        System.out.println("time" + (System.currentTimeMillis() - time2) / 1000d);

        if (randCut.minCutValue() != 3) {
            System.out.println("fail");
        }
        assertEquals(goldCut.minCutValue(), randCut.minCutValue());
    }
//    }
}
