package mincut.cutGraphImpl.minCutKargerStein;

import mincut.cutGraphAPI.bipartition.HashableCut;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public interface KargerGraph<G extends KargerGraph<G, S>, S> extends Comparable<KargerGraph>, Cloneable {
    default void contract() {
        contract(ThreadLocalRandom.current());
    }

    default G contractAndKeep() {
        G clone = clone();
        contract();
        return clone;
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

    HashableCut<S> asCut();


}
