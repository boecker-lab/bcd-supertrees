package phylo.tree.algorithm.flipcut.cutter;

import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.STCut;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedCut;
import phylo.tree.algorithm.flipcut.bcdGraph.Hyperedge;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;

public class CompressedSingleCutter implements GraphCutter<RoaringBitmap> {
    @Override
    public void clear() {

    }

    @Override
    public Cut<RoaringBitmap> cut(SourceTreeGraph<RoaringBitmap> source) {
        return cut((CompressedBCDGraph) source);
    }

    public Cut<RoaringBitmap> cut(final CompressedBCDGraph source) {
        final GoldbergTarjanCutGraph cutGraph = new GoldbergTarjanCutGraph();
        if (source.hasGuideEdges()) {
            //create cutgraph with merged taxa
            //todo get guide edges
            //todo check intersection
            //todo check subset

        } else {
            // create cutgraph without merged taxa
            source.characters.forEach((IntConsumer) edgeIndex -> {
                Hyperedge edge = source.getEdge(edgeIndex);
                cutGraph.addEdge(edgeIndex, edge, edge.getWeight());
                edge.ones.forEach((IntConsumer) taxonIndex -> {
                    final String t = source.getTaxon(taxonIndex);
                    cutGraph.addEdge(t, edgeIndex, CutGraphCutter.getInfinity());
                    cutGraph.addEdge(edge, t, CutGraphCutter.getInfinity());
                });
            });

            Iterator<String> taxit = source.taxaLabels().iterator();
            String s = taxit.next();
            while (taxit.hasNext()) {
                cutGraph.submitSTCutCalculation(s, taxit.next());
            }
        }

        //we do not have to map merged taxa back, hence we need only the hyperedges we have to delete
        try {
            STCut cut = cutGraph.calculateMinCut();
            RoaringBitmap toRemove = new RoaringBitmap();
            LinkedHashSet cs = cut.getsSet();
            for (Object o : cs) {
                if (o instanceof Integer) {
                    int index = (int) o;
                    if (!cs.contains(source.getEdge(index))) {
                        toRemove.add(index);
                    }
                }
            }
            return new CompressedCut(toRemove, cut.minCutValue());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace(); //todo logging
            return null;
        }


    }


    @Override
    public Cut<RoaringBitmap> getMinCut() {
        return null;
    }

    @Override
    public boolean isBCD() {
        return true;
    }
}
