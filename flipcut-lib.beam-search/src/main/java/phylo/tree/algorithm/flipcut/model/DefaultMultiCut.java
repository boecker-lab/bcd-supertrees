package phylo.tree.algorithm.flipcut.model;


import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 *         Date: 18.01.13
 *         Time: 18:10
 */
public class DefaultMultiCut extends MultiCut<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
    private LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes;
    private List<List<FlipCutNodeSimpleWeight>> comp;

    private final long minCutValue;


    public DefaultMultiCut(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes, long minCutValue, FlipCutGraphMultiSimpleWeight sourceGraph) {
        super(sourceGraph);
        this.minCutValue = minCutValue;
        this.sinkNodes = sinkNodes;
        comp = null;
    }

    public DefaultMultiCut(List<List<FlipCutNodeSimpleWeight>> comp, FlipCutGraphMultiSimpleWeight graph) {
        super(graph);
        this.minCutValue = 0;
        sinkNodes = null;
        this.comp = comp;
    }

    @Override
    public long minCutValue() {
        return minCutValue;
    }

    public List<FlipCutGraphMultiSimpleWeight> getSplittedGraphs() {
        if (splittedGraphs == null) {
            if (comp != null) {
                splittedGraphs = sourceGraph.buildComponentGraphs(comp);
                comp = null;

            } else {
                splittedGraphs = (List<FlipCutGraphMultiSimpleWeight>) sourceGraph.split(sinkNodes);
                sinkNodes = null;
            }
        }
        return splittedGraphs;
    }
}
