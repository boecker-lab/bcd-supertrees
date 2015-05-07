package flipcut.mincut;

/**
 * Created by fleisch on 07.05.15.
 */
public abstract class AbstractSingelCutGraph<V> implements CutGraph<V> {
    @Override
    public long getMinCutValue(){
        if (getMinCut() == null)
            return -1;
        return getMinCut().minCutValue;
    }

}
