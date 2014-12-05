package flipcut.flipCutGraph;

import java.util.List;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 16:37
 */

//todo useful comments how to use and extend this class
public class SingleCutGraphCutter extends SimpleCutGraphCutter<FlipCutGraphSimpleWeight> {
    public SingleCutGraphCutter(CutGraphTypes type) {
        super(type);
    }

    @Override
    public List<FlipCutGraphSimpleWeight> cut(FlipCutGraphSimpleWeight source){
        List<FlipCutNodeSimpleWeight> minCut = getMinCut(source);
        if (split == null) {
            split = (List<FlipCutGraphSimpleWeight>) source.split(minCut);
        }
        return split;
    }


}
