package phylo.tree.algorithm.flipcut.flipCutGraph;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 16:37
 */
public class SingleCutGraphCutter extends SimpleCutGraphCutter<FlipCutGraphSimpleWeight> {
    public SingleCutGraphCutter(CutGraphTypes type) {
        super(type);
    }

    public SingleCutGraphCutter(CutGraphTypes type, ExecutorService executorService, int threads) {
        super(type, executorService, threads);
    }

    @Override
    public List<FlipCutGraphSimpleWeight> cut(FlipCutGraphSimpleWeight source){
        LinkedHashSet<FlipCutNodeSimpleWeight> minCut = getMinCut(source);
        if (split == null) {
            split = (List<FlipCutGraphSimpleWeight>) source.split(minCut);
        }
        return split;
    }


}
