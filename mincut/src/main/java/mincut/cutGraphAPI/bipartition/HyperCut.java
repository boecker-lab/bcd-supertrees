package mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 15.02.17.
 */

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class HyperCut<V> extends AbstractBipartition<V> {


    public HyperCut(long minCutValue, LinkedHashSet<V> sSet, LinkedHashSet<V> tSet) {
        super(minCutValue, sSet, tSet);
    }





}
