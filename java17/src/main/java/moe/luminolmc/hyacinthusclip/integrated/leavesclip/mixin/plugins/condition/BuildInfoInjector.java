package moe.luminolmc.hyacinthusclip.integrated.leavesclip.mixin.plugins.condition;

import moe.luminolmc.hyacinthusclip.Hyacinthusclip;
import moe.luminolmc.hyacinthusclip.integrated.leavesclip.logger.Logger;
import moe.luminolmc.hyacinthusclip.integrated.leavesclip.logger.SimpleLogger;
import org.leavesmc.plugin.mixin.condition.BuildInfoProvider;
import org.leavesmc.plugin.mixin.condition.data.BuildInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BuildInfoInjector {
    private static final Logger logger = new SimpleLogger("BuildInfoInjector");

    public static void inject() {
        String buildInfoString;
        try (InputStream inputStream = Hyacinthusclip.class.getResourceAsStream("/META-INF/build-info")) {
            buildInfoString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read build info", e);
            throw new RuntimeException(e);
        }
        if (buildInfoString.endsWith("\tDEV")) {
            buildInfoString = buildInfoString.replaceFirst("\tDEV", "\t0");
        }
        BuildInfo buildInfo = BuildInfo.fromString(buildInfoString);
        logger.info(buildInfo.toString());
        BuildInfoProvider.INSTANCE.setBuildInfo(buildInfo);
    }
}
