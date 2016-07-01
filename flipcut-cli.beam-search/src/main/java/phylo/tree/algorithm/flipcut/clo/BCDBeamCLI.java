package phylo.tree.algorithm.flipcut.clo;


import phylo.tree.algorithm.flipcut.cli.BCDCLI;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import org.kohsuke.args4j.spi.IntOptionHandler;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Created by fleisch on 19.06.14.
 */
public abstract class BCDBeamCLI extends BCDCLI {
    //todo maybe extend or overwrite algorithm enum
    @Option(name="-x", aliases = "--beamSearch", handler = BooleanOptionHandler.class, usage="Use beam search implementation")
    public boolean beamSearch = false;

    public BCDBeamCLI(String appHomeParent, String appHomeFolderName, String logDir, int maxLogFileSize, int logRotation) {
        super(appHomeParent, appHomeFolderName, logDir, maxLogFileSize, logRotation);
    }

    public BCDBeamCLI(InputStream propertiesFile) {
        super(propertiesFile);
    }

    @Option(name="-k", aliases = "--cutNumber", handler = IntOptionHandler.class, usage="Number of suboptimal solutions")
    public abstract void setCutNumber(int cutNumber);

}
