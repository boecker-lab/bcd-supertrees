package phylo.tree.algorithm.flipcut.mincut;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 10.02.17.
 */

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface Colorable {
    void setColor(EdgeColor color);
    EdgeColor getColor();
    default EdgeColor deleteColor(){
        EdgeColor color = getColor();
        if (color == null)
            return null;

        color.remove(this);
        return color;
    }
}
