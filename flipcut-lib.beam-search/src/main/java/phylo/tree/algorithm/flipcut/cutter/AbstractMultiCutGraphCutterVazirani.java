package phylo.tree.algorithm.flipcut.cutter;

import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.MultiCut;
import mincut.cutGraphAPI.bipartition.VaziraniCut;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.SourceTreeGraphMultiCut;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 14:05
 */

/**
 * VAZIRANI ALGORITHM
 */

public abstract class AbstractMultiCutGraphCutterVazirani<C, G extends SourceTreeGraphMultiCut<C, G>> implements MultiCutter<C, G> {

    protected PriorityQueue<VaziraniCut<C>> queueAscHEAP = null;
    protected VaziraniCut<C> currentNode = null;
    protected VaziraniCut<C>[] initCuts;

    protected final G source;//todo make reusable??

    public AbstractMultiCutGraphCutterVazirani(G graphToCut) {
        source = graphToCut;
    }


    protected abstract void initialCut();

    protected abstract List<VaziraniCut<C>> findCutsFromPartialCuts(VaziraniCut<C> sourceCut, VaziraniCut<C>[] initCuts);

    protected abstract MultiCut<C, G> buildOutputCut(VaziraniCut<C> currentNode);

    @Override
    public void clear() {
        queueAscHEAP = null;
        currentNode = null;
        initCuts = null;
    }

    public MultiCut<C, G> getNextCut() {
        if (queueAscHEAP != null && queueAscHEAP.isEmpty()) {
            // all mincut calculated
            return null;
        } else {
            if (queueAscHEAP == null) {
                initialCut();
            }
            nextCut();
            return buildOutputCut(currentNode);
        }
    }

    private void nextCut() {
        //Starting find subobtimal mincut mincut with vaziranis algo
        currentNode = queueAscHEAP.poll();
        //compute next cut candidates with vaziranis algo
        List<VaziraniCut<C>> toHeap = findCutsFromPartialCuts(currentNode, initCuts);
        for (VaziraniCut<C> node : toHeap) {
            queueAscHEAP.add(node);
        }
    }


    @Override
    public Cut<C> cut(SourceTreeGraph<C> source) {
        if (source.equals(this.source))
            return getMinCut();
        return null;
    }

    @Override
    public Cut<C> getMinCut() {
        return getNextCut();
    }

    @Override
    public boolean isBCD() {
        return true;
    }
}
