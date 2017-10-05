package phylo.tree.algorithm.flipcut.flipCutGraph;

import mincut.cutGraphAPI.bipartition.BasicCut;
import mincut.cutGraphAPI.bipartition.Cut;
import mincut.cutGraphAPI.bipartition.STCut;

import java.util.*;

public class VertexMapping<T extends AbstractFlipCutGraph<FlipCutNodeSimpleWeight>> {
    protected T sourceGraph = null;
    protected final Map<FlipCutNodeSimpleWeight, FlipCutNodeSimpleWeight> taxonToDummy = new HashMap<>();
    protected final Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToTaxa = new HashMap<>();
    protected final Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> trivialcharacters = new HashMap<>();

    protected int mergedTaxonIndex = 0;

    protected void clear(final T s) {
        sourceGraph = s;
        taxonToDummy.clear();
        dummyToTaxa.clear();
        trivialcharacters.clear();
        mergedTaxonIndex = 0;
    }

    protected void clear() {
        clear(null);
    }

    protected ArrayList<FlipCutNodeSimpleWeight> createMapping(final T s) {
        clear(s);
        //create mapping
        //todo optimize all these mapping stuff if it works well
        ArrayList<FlipCutNodeSimpleWeight> taxonGroups = new ArrayList<>();
        for (FlipCutNodeSimpleWeight scaffChar : sourceGraph.activePartitions) {
            FlipCutNodeSimpleWeight mergeTaxon = new FlipCutNodeSimpleWeight("TaxonGroup_" + mergedTaxonIndex);
            taxonGroups.add(mergeTaxon);
            for (FlipCutNodeSimpleWeight taxon : scaffChar.edges) {
                taxonToDummy.put(taxon, mergeTaxon);
            }
            dummyToTaxa.put(mergeTaxon, scaffChar.edges);
            mergedTaxonIndex++;
        }
        // add unmerged taxa to mapping
        for (FlipCutNodeSimpleWeight taxon : sourceGraph.taxa) {
            if (!taxonToDummy.containsKey(taxon)) {
                taxonToDummy.put(taxon, taxon);
                taxonGroups.add(taxon);
            }
        }
        return taxonGroups;
    }

    protected STCut<FlipCutNodeSimpleWeight> undoMapping(final STCut<FlipCutNodeSimpleWeight> newMinCut, final Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToMerged) {
        final LinkedHashSet<FlipCutNodeSimpleWeight> nuSset = undoMapping(newMinCut.getsSet(), dummyToMerged);
        final LinkedHashSet<FlipCutNodeSimpleWeight> nuTset = undoMapping(newMinCut.gettSet(), dummyToMerged);
        return new STCut<>(nuSset, nuTset, newMinCut.minCutValue());
    }

    protected BasicCut<FlipCutNodeSimpleWeight> undoMapping(final Cut<FlipCutNodeSimpleWeight> newMinCut, final Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToMerged) {
        final LinkedHashSet<FlipCutNodeSimpleWeight> cutSet = undoMapping(newMinCut.getCutSet(), dummyToMerged);
        return new BasicCut<>(cutSet, newMinCut.minCutValue());
    }

    protected LinkedHashSet<FlipCutNodeSimpleWeight> undoMapping(Set<FlipCutNodeSimpleWeight> cutSet, final Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToMerged) {
        final LinkedHashSet<FlipCutNodeSimpleWeight> mincut = new LinkedHashSet<>();
        for (FlipCutNodeSimpleWeight node : cutSet) {
            //undo taxa mapping
            if (node.isTaxon()) {
                Set<FlipCutNodeSimpleWeight> trivials = trivialcharacters.get(node);
                if (trivials != null) {
                    for (FlipCutNodeSimpleWeight trivial : trivials) {
                        mincut.addAll(sourceGraph.dummyToCharacters.get(trivial));
                    }
                }
                Set<FlipCutNodeSimpleWeight> realT = dummyToTaxa.get(node);
                if (realT != null)
                    mincut.addAll(realT);
                else
                    mincut.add(node);

                //undo character mapping
            } else {
                Set<FlipCutNodeSimpleWeight> realNodes = new HashSet<>(sourceGraph.characters.size());
                for (FlipCutNodeSimpleWeight realNode : dummyToMerged.get(node)) {
                    realNodes.addAll(sourceGraph.dummyToCharacters.get(realNode));
                }
                mincut.addAll(realNodes);
            }
        }
        return mincut;
    }

}