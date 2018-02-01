package mincut.cutGraphImpl.minCutKargerStein;

import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public interface KargerGraph<G extends KargerGraph<G>> extends Comparable<KargerGraph>, Cloneable {
    default void contract() {
        contract(ThreadLocalRandom.current());
    }

    void contract(final Random random);

    default double mincutValue() {
        if (!isCutted())
            return Double.NaN;
        return getSumOfWeights();
    }

    boolean isCutted();

    double getSumOfWeights();

    int getNumberOfVertices();


    G clone();

    @Override
    default int compareTo(@NotNull KargerGraph o) {
        return Double.compare(mincutValue(), o.mincutValue());
    }
}
