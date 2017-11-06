package mincut.cutGraphAPI.bipartition;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 15.02.17.
 */

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface Cut<S> extends Comparable<Cut<S>> {


    long minCutValue();

    S getCutSet();

    default int compareTo(Cut<S> o) {
        return Long.compare(minCutValue(), o.minCutValue());
    }
}
