package model;


import flipCutGraph.FlipCutGraphMultiSimpleWeight;
import flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.List;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 18.01.13
 * Time: 18:10
 */
public class Cut implements Comparable<Cut> {
    private List<FlipCutNodeSimpleWeight> sinkNodes;
    private List<List<FlipCutNodeSimpleWeight>> comp;
    private List<FlipCutGraphMultiSimpleWeight> splittedGraphs;
    final long minCutValue;
    final FlipCutGraphMultiSimpleWeight sourceGraph;

    public Cut(List<FlipCutNodeSimpleWeight> sinkNodes, long minCutValue, FlipCutGraphMultiSimpleWeight sourceGraph) {
        //splittedGraphs = (List<FlipCutGraphMultiSimpleWeight>) sourceGraph.split(sinkNodes);
        this.sinkNodes = sinkNodes;
        this.minCutValue = minCutValue;
        this.sourceGraph = sourceGraph;
        comp = null;
    }

    public Cut(List<List<FlipCutNodeSimpleWeight>> comp, FlipCutGraphMultiSimpleWeight graph) {
        sinkNodes = null;
        minCutValue = 0;
        sourceGraph = graph;
        this.comp = comp;
        //splittedGraphs = sourceGraph.buildComponentGraphs(comp);
    }

    public long getMinCutValue() {
        return minCutValue;
    }

    public int compareTo(Cut o) {
        return (minCutValue < o.minCutValue) ? -1 : ((minCutValue == o.minCutValue) ? 0 : 1);
    }

    /*public List<FlipCutGraphMultiSimpleWeight> getSplittedGraphs() {
        return new ArrayList<FlipCutGraphMultiSimpleWeight>(splittedGraphs); //todo new object needed? proof it!
    }*/

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
