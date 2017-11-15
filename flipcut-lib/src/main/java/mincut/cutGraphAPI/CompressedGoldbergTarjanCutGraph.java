package mincut.cutGraphAPI;

import core.utils.parallel.IterationCallableFactory;
import mincut.cutGraphAPI.bipartition.STCut;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.Arc;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.CutGraphImpl;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.Node;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class CompressedGoldbergTarjanCutGraph extends MaxFlowCutGraph<Object> {
    private final CutGraphImpl hipri;

    private HipriCallableFactory factory = null;

    public CompressedGoldbergTarjanCutGraph(CutGraphImpl hipri) {
        this.hipri = hipri;
    }


    @Override
    HipriCallableFactory getMaxFlowCallableFactory() {
        if (factory == null)
            factory = new HipriCallableFactory();
        return factory;
    }


    /**
     * Does the mincut execution
     *
     * @param source the source
     * @param sink   the sink
     */
    @Override
    public STCut<Object> calculateMinSTCut(Object source, Object sink) {
        return calculateMinSTCut((Node) source, (Node) sink);
    }

    public STCut<Object> calculateMinSTCut(Node source, Node sink) {
        return calculateMinSTCut(source, sink, hipri);
    }

    public STCut<Object> calculateMinSTCut(final Object source, final Object sink, final CutGraphImpl hipri) {
        return calculateMinSTCut((Node) source, (Node) sink, hipri);
    }

    public STCut<Object> calculateMinSTCut(final Node source, final Node sink, final CutGraphImpl hipri) {
        hipri.setSource(source);
        hipri.setSink(sink);

        List<LinkedHashSet<Object>> cutList = hipri.calculateMaxSTFlowFull(false);
        return new STCut(cutList.get(0), cutList.get(1), source, sink, hipri.getValue());
    }


    private class HipriCallable extends MaxFlowCallable {
        private CutGraphImpl h;
        Map<Node, Node> nodeMapping;

        public HipriCallable(List<MaxFlowCutGraph<Object>.SS> jobs) {
            super(jobs);
        }

        @Override
        void initGraph() {
            if (h == null) {
                createHipriFromSource(hipri);
            }
        }

        private void createHipriFromSource(CutGraphImpl hipri) {
            nodeMapping = new HashMap<>(hipri.n);
            h = new CutGraphImpl(hipri.n, hipri.m);
            // iterate over all nodes and create clones
            for (Node node : hipri.getNodes()) {
                nodeMapping.put(node, h.createNode(node.name, node.getArcs().length));
            }


            //iterate over all arcs an insert them into cloned graph
            for (Node node : hipri.getNodes()) {
                for (Arc arc : node.getArcs()) {
                    if (arc.getCap() > 0)
                        h.addEdge(nodeMapping.get(arc.getSource()), nodeMapping.get(arc.getTarget()), arc.getCap());
                }
            }
        }

        @Override
        public STCut<Object> doJob(SS ss) {
            return calculateMinSTCut(nodeMapping.get((Node) ss.source), nodeMapping.get((Node) ss.sink), h);
        }
    }


    private class HipriCallableFactory implements IterationCallableFactory<HipriCallable, SS> {
        @Override
        public HipriCallable newIterationCallable(List<SS> list) {
            return new HipriCallable(list);
        }
    }


}
