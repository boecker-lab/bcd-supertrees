package phylo.tree.algorithm.flipcut.mincut;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 10.02.17.
 */

import java.util.Iterator;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface Colorable {
    default void clearColors() {
        Iterator<EdgeColor> it = colorIterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    Iterator<EdgeColor> colorIterator();
    boolean add(EdgeColor color);
}
