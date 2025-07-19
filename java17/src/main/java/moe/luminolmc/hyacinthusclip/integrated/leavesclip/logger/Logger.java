package moe.luminolmc.hyacinthusclip.integrated.leavesclip.logger;

import org.spongepowered.asm.logging.ILogger;

public abstract class Logger implements ILogger {
    public abstract void warn(Throwable t, String message, Object... params);

    public abstract void error(Throwable t, String message, Object... params);
}
