package phylo.tree.algorithm.flipcut;

import mincut.cutGraphAPI.bipartition.MultiCut;
import phylo.tree.algorithm.flipcut.flipCutGraph.MultiCutter;
import phylo.tree.algorithm.flipcut.flipCutGraph.MultiCutterFactory;

import java.util.Iterator;

public abstract class MultiCutGraph<C, G extends MultiCutGraph<C, G>> implements SourceTreeGraphMultiCut<C, G> {
    protected int maxCutNumber;
    protected int nextCutIndexToCalculate;

    protected MultiCut<C, G>[] cuts;
    protected MultiCutter<C, G> cutter = null;
    protected MultiCutterFactory<MultiCutter<C, G>, C, G> cutterFactory;


    public boolean containsCuts() {
        return nextCutIndexToCalculate > 0;
    }

    public int getK() {
        return cuts.length;
    }


    @Override
    public Iterator<MultiCut<C, G>> getCutIterator() {
        return new CutIterator();
    }

    private boolean calculateNextCut() {
        if (nextCutIndexToCalculate < maxCutNumber) {
            if (cutter == null)
                cutter = cutterFactory.newInstance((G) this);


            MultiCut<C, G> c = cutter.getNextCut();
            if (c != null) {
                cuts[nextCutIndexToCalculate++] = c;
                if (nextCutIndexToCalculate >= maxCutNumber) {
                    disableCutting();
                }
                return true;
            }
            maxCutNumber = nextCutIndexToCalculate;
        }
        disableCutting();
        return false;
    }

    private void disableCutting() {
        cutter = null;
    }

    protected abstract MultiCut<C, G> getCutFromCompenents();


    class CutIterator implements Iterator<MultiCut<C, G>> {
        CutIterator() {
            //check if graph is already disconnected
            if (nextCutIndexToCalculate == 0 && maxCutNumber == cuts.length) {
                if (!isConnected()) {//graph is already disconnected we have to build subgraph from parts
                    cuts[nextCutIndexToCalculate] = getCutFromCompenents();
                    nextCutIndexToCalculate++;
                    maxCutNumber = nextCutIndexToCalculate;
                }
            }
        }

        int index = 0;

        public boolean hasNext() {
            if (index >= nextCutIndexToCalculate) {
                if (nextCutIndexToCalculate < maxCutNumber) {
                    return calculateNextCut();
                } else {
                    return false;
                }
            }
            return true;
        }

        public MultiCut<C, G> next() {
            if (cuts[index] == null)
                calculateNextCut();
            return cuts[index++];

        }
    }


}
