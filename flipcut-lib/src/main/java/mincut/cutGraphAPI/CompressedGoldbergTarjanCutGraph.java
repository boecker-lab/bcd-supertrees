package mincut.cutGraphAPI;

import core.utils.parallel.DefaultIterationCallable;
import core.utils.parallel.IterationCallableFactory;
import mincut.cutGraphAPI.bipartition.STCut;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.CutGraphImpl;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.Node;

import java.util.LinkedHashSet;
import java.util.List;

public class CompressedGoldbergTarjanCutGraph extends MaxFlowCutGraph {
    private final CutGraphImpl hipri;
    //todo clone function for hipri to implement multithreding

    public CompressedGoldbergTarjanCutGraph(CutGraphImpl hipri) {
        this.hipri = hipri;
    }

    private CutGraphImpl cloneHipri() {
        return new CutGraphImpl(0, 0); //todo neede for paralelisation
    }

    @Override
    IterationCallableFactory<? extends DefaultIterationCallable<SS, STCut>, SS> getMaxFlowCallableFactory() {
        return null;//todo create class -> for parallel
    }

    /**
     * Does the mincut execution
     *
     * @param source the source
     * @param sink   the sink
     */
    @Override
    public STCut calculateMinSTCut(Object source, Object sink) {
        return calculateMinSTCut((Node) source, (Node) sink);
    }

    public STCut calculateMinSTCut(Node source, Node sink) {
        hipri.setSource(source);
        hipri.setSink(sink);

        List<LinkedHashSet<Object>> cutList = hipri.calculateMaxSTFlowFull(false);
        return new STCut(cutList.get(0), cutList.get(1), source, sink, hipri.getValue());
    }


}
