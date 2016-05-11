package phylo.tree.algorithm.flipcut.flipCutGraph;

import phylo.tree.algorithm.flipcut.model.Cut;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 19.04.13
 * Time: 15:19
 */
public interface MultiCutter {
    public Cut getNextCut();
    public CutGraphCutter.CutGraphTypes getType();
}
