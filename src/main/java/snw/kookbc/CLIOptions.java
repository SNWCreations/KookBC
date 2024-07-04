package snw.kookbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CLIOptions {
    private static final Logger logger = LoggerFactory.getLogger(CLIOptions.class);

    public static final boolean NO_BUCKET;

    static {
        NO_BUCKET = Boolean.getBoolean("kookbc.nobucket");
        if (NO_BUCKET) {
            logger.warn("You've used kookbc.nobucket option, we won't check if you're going to be out of rate limit!");
        }
    }


    private CLIOptions() {
    }
}
