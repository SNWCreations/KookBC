package snw.kookbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CLIOptions {
    private static final Logger logger = LoggerFactory.getLogger(CLIOptions.class);

    public static final boolean NO_BUCKET;

    static {
        NO_BUCKET = Boolean.getBoolean("kookbc.nobucket");
        if (NO_BUCKET) {
            logger.warn("您已启用 kookbc.nobucket 选项，我们将不会检查是否会超出速率限制！");
        }
    }


    private CLIOptions() {
    }
}
