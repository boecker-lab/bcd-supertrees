package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 */

import gnu.trove.set.TIntSet;
import org.junit.Test;

import java.util.Map;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class KargerSteinMastaPTest {

    @Test
    public void basicTest(){
        String testFile = getClass().getResource("/kargerAdj.txt").getFile();
        int[][] arr = GraphUtils.getArray(testFile);

        long time =  System.currentTimeMillis();
        KargerSteinMastaP cutter = new KargerSteinMastaP();
        Map<Integer, TIntSet> statistics = cutter.getMinCut(arr);
        System.out.println("time" +  (System.currentTimeMillis()-time)/1000d);
    }
}
