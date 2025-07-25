/*
 * Paperclip - Paper Minecraft launcher
 *
 * Copyright (c) 2019 Kyle Wood (DemonWav)
 * https://github.com/PaperMC/Paperclip
 *
 * MIT License
 */

package moe.luminolmc.hyacinthusclip;

import java.lang.reflect.Method;

public final class Main {

    public static void main(final String[] args) {
        if (getJavaVersion() < 21) {
            System.err.println("Minecraft 1.20.6+ requires running the server with Java 21 or above. " +
                "For information on how to update Java, see https://docs.papermc.io/misc/java-install");
            System.exit(1);
        }

        try {
            final Class<?> hyacinthusclipClazz = Class.forName("moe.luminolmc.hyacinthusclip.Hyacinthusclip");
            final Method mainMethod = hyacinthusclipClazz.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static int getJavaVersion() {
        final String version = System.getProperty("java.specification.version");
        final String[] parts = version.split("\\.");

        final String errorMsg = "Could not determine version of the current JVM";
        if (parts.length == 0) {
            throw new IllegalStateException(errorMsg);
        }

        if (parts[0].equals("1")) {
            if (parts.length < 2) {
                throw new IllegalStateException(errorMsg);
            }
            return Integer.parseInt(parts[1]);
        } else {
            return Integer.parseInt(parts[0]);
        }
    }
}
