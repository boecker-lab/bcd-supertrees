package phylo.tree.algorithm.flipcut.bcdGraph;

import mincut.cutGraphAPI.bipartition.CompressedBCDMultiCut;
import mincut.cutGraphAPI.bipartition.MultiCut;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.MultiCutGraph;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.cutter.GraphCutter;
import phylo.tree.algorithm.flipcut.cutter.MultiCutter;
import phylo.tree.algorithm.flipcut.cutter.MultiCutterFactory;

import java.util.ArrayList;
import java.util.List;

public class CompressedBCDMultiCutGraph extends MultiCutGraph<RoaringBitmap, CompressedBCDMultiCutGraph> {
    private final CompressedBCDGraph source;

    public RoaringBitmap getTaxa() {
        return source.taxa;
    }

    public CompressedBCDMultiCutGraph(CompressedBCDGraph source, int k, MultiCutterFactory<MultiCutter<RoaringBitmap, CompressedBCDMultiCutGraph>, RoaringBitmap, CompressedBCDMultiCutGraph> cutterFactory) {
        super(k, cutterFactory);
        this.source = source;
    }

    public CompressedBCDGraph getSource() {
        return source;
    }

    //delegates from source
    @Override
    public void deleteSemiUniversals() {
        source.deleteSemiUniversals();
    }

    @Override
    public List<? extends SourceTreeGraph<RoaringBitmap>> getPartitions(GraphCutter<RoaringBitmap> c) {
        return (List<? extends SourceTreeGraph<RoaringBitmap>>) source.getPartitions(c);
    }

    @Override
    public Iterable<String> taxaLabels() {
        return source.taxaLabels();
    }

    @Override
    public int numTaxa() {
        return source.numTaxa();
    }

    @Override
    public int numCharacter() {
        return source.numCharacter();
    }

    @Override
    public boolean isConnected() {
        return source.isConnected();
    }


    //Multi cutter methods
    @Override
    protected MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph> getCutFromCompenents() {
        return new CompressedBCDMultiCut(this);
    }

    @Override
    public void close() {
        //todo do we need something her for memory saving
    }


    public List<CompressedBCDMultiCutGraph> split(RoaringBitmap toDelete) {
        CompressedBCDGraph cutted = CompressedBCDGraph.cloneAndDeleteCharacters(toDelete, this.source);
        List<CompressedBCDGraph> splitSource = cutted.split();
        List<CompressedBCDMultiCutGraph> splitGraphs = new ArrayList<>(splitSource.size());
        for (CompressedBCDGraph bcdGraph : splitSource) {
            splitGraphs.add(new CompressedBCDMultiCutGraph(bcdGraph, maxCutNumber, cutterFactory));
        }
        return splitGraphs;
    }
}
