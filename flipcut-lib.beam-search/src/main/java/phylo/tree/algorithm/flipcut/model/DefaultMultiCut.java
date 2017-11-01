package phylo.tree.algorithm.flipcut.model;


import mincut.cutGraphAPI.bipartition.Cut;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 18.01.13
 * Time: 18:10
 */
public class DefaultMultiCut extends MultiCut<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
    protected LinkedHashSet<FlipCutNodeSimpleWeight> cutSet;
    protected final long minCutValue;
    private List<List<FlipCutNodeSimpleWeight>> comp;

    private DefaultMultiCut(LinkedHashSet<FlipCutNodeSimpleWeight> cutSet, long minCutValue, List<List<FlipCutNodeSimpleWeight>> comp, FlipCutGraphMultiSimpleWeight sourceGraph) {
        super(sourceGraph);
        this.cutSet = cutSet;
        this.minCutValue = minCutValue;
        this.comp = comp;
        calculateHash();
    }

    public DefaultMultiCut(Cut<FlipCutNodeSimpleWeight> cut, FlipCutGraphMultiSimpleWeight sourceGraph) {
        this(cut.getCutSet(), cut.minCutValue(), null, sourceGraph);
    }

    public DefaultMultiCut(List<List<FlipCutNodeSimpleWeight>> comp, FlipCutGraphMultiSimpleWeight graph) {
        this(null, 0, comp, graph);
    }

    @Override
    public long minCutValue() {
        return minCutValue;
    }

    @Override
    public LinkedHashSet<FlipCutNodeSimpleWeight> getCutSet() {
        return cutSet;
    }

    public List<FlipCutGraphMultiSimpleWeight> getSplittedGraphs() {
        if (splittedGraphs == null) {
            if (comp != null) {
                splittedGraphs = sourceGraph.buildComponentGraphs(comp);
                comp = null;
            } else {
                splittedGraphs = (List<FlipCutGraphMultiSimpleWeight>) sourceGraph.split(getCutSet());
                cutSet = null;
            }
            sourceGraph.setCutSplitted(this);
            sourceGraph.close();
        }
        return splittedGraphs;
    }

    @Override
    protected List<List<FlipCutNodeSimpleWeight>> comp() {
        return comp;
    }
}
