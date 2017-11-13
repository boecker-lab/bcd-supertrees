package phylo.tree.algorithm.flipcut.flipCutGraph;

import com.google.common.collect.Sets;
import phylo.tree.algorithm.flipcut.flipCutGraph.blacklists.BlackList;
import phylo.tree.algorithm.flipcut.flipCutGraph.blacklists.GreedyBlackList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BlackListVertexMapping extends VertexMapping<FlipCutGraphMultiSimpleWeight> {

    protected int singleTaxonIndex = 0;

    @Override
    protected void clear(FlipCutGraphMultiSimpleWeight s) {
        super.clear(s);
        singleTaxonIndex = 0;
    }

    @Override
    public ArrayList<FlipCutNodeSimpleWeight> createMapping(final FlipCutGraphMultiSimpleWeight s) {
        return createMapping(s, new GreedyBlackList());
    }

    protected ArrayList<FlipCutNodeSimpleWeight> createMapping(final FlipCutGraphMultiSimpleWeight s, final BlackList blacklist) {
        clear(s);

        Set<Set<FlipCutNodeSimpleWeight>> activePartitions = new HashSet<>();
        for (FlipCutNodeSimpleWeight character : sourceGraph.activePartitions) {
            activePartitions.add(new HashSet<>(character.edges));
        }
        for (FlipCutNodeSimpleWeight character : blacklist) {
            activePartitions.add(new HashSet<>(character.edges));
        }

        //todo is there a less ugly way to do this?
        if (!activePartitions.isEmpty()) {
            Set<Set<FlipCutNodeSimpleWeight>> finalActivePartitions = new HashSet<>();
            while (!activePartitions.isEmpty()) {
                Set<FlipCutNodeSimpleWeight> merged = activePartitions.iterator().next();
                activePartitions.remove(merged);
                boolean changes = true;
                while (changes) {
                    Iterator<Set<FlipCutNodeSimpleWeight>> it = activePartitions.iterator();
                    changes = false;
                    while (it.hasNext()) {
                        Set<FlipCutNodeSimpleWeight> next = it.next();
                        if (Sets.intersection(next, merged).size() > 0) {
                            merged.addAll(next);
                            it.remove();
                            changes = true;
                        }
                    }
                }
                finalActivePartitions.add(merged);
            }
            activePartitions = finalActivePartitions;
        }

        ArrayList<FlipCutNodeSimpleWeight> taxa = new ArrayList<>();
        for (Set<FlipCutNodeSimpleWeight> scaffChar : activePartitions) {
            FlipCutNodeSimpleWeight mergeTaxon = new FlipCutNodeSimpleWeight("TaxonGroup_" + mergedTaxonIndex);
            taxa.add(mergeTaxon);
            for (FlipCutNodeSimpleWeight taxon : scaffChar) {
                taxonToDummy.put(taxon, mergeTaxon);
            }
            dummyToTaxa.put(mergeTaxon, scaffChar);
            mergedTaxonIndex++;
        }

        for (FlipCutNodeSimpleWeight taxon : sourceGraph.taxa) {
            if (!taxonToDummy.containsKey(taxon)) {
                taxonToDummy.put(taxon, taxon);
                taxa.add(taxon);
                singleTaxonIndex++;
            }
        }
        return taxa;
    }
}
