package flipcut.clo;


import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import org.kohsuke.args4j.spi.IntOptionHandler;

/**
 * Created by fleisch on 19.06.14.
 */
public abstract class FlipCutBeamCLO extends FlipCutCLO {
    //todo maybe extend or overwrite algorithm enum
    @Option(name="-x", aliases = "--beamSearch", handler = BooleanOptionHandler.class, usage="Use beam search implementation")
    public boolean beamSearch = false;

    @Option(name="-k", aliases = "--cutNumber", handler = IntOptionHandler.class, usage="Number of suboptimal solutions")
    public abstract void setCutNumber(int cutNumber);

}
