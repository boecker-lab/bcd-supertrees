package flipcut.mincut;

/**
 * Created by fleisch on 15.04.15.
 */
public interface DirectedCutGraph<V> extends CutGraph<V> {
   public void calculate(V source, V sink);
}
