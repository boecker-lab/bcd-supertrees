package flipcut.mincut;

/**
 * Created by fleisch on 15.04.15.
 */
public interface UndirectedCutGraph<V> extends CutGraph<V> {
    public void calculateMinCuts();
}
