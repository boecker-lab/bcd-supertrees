package mincut.cutGraphAPI.bipartition;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 28.02.17.
 */

import java.util.LinkedHashSet;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface CutFactory<S,C extends Cut<S>> {
    C newCutInstance(S cutTaxaSource, S cutTaxaSink, S cutEdges, long mincutValue);
}
