package moe.luminolmc.hyacinthusclip.downloader;

import moe.luminolmc.hyacinthusclip.FileEntry;
import moe.luminolmc.hyacinthusclip.Hyacinthusclip;
import moe.luminolmc.hyacinthusclip.Util;
import moe.luminolmc.hyacinthusclip.integrated.leavesclip.logger.SimpleLogger;
import moe.luminolmc.hyacinthusclip.integrated.leavesclip.mixin.MixinURLClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.nio.file.StandardOpenOption.*;

public class Downloader {
    private static final SimpleLogger logger = new SimpleLogger("Hyacinthusclip");

    private final FileEntry entry;
    private final String baseDir;
    private final Path outputFile;
    private final boolean useInternalIfFailed;

    public Downloader(FileEntry entry, Path outputFile, String baseDir, boolean useInternalIfFailed) {
        this.entry = entry;
        this.outputFile = outputFile;
        this.baseDir = baseDir;

        this.useInternalIfFailed = useInternalIfFailed;
    }

    public CompletableFuture<Void> download(Executor worker) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Downloading: " + this.entry.id());

            final RuntimeException failed = new RuntimeException("All maven repo download attempts has been failed for library " + this.entry.id() + "!");

            final String filePath = Util.endingSlash(this.baseDir) + this.entry.path();
            InputStream fileStream = MixinURLClassLoader.class.getResourceAsStream(filePath);
            if (fileStream != null) {
                try {
                    this.write(fileStream);
                }catch (Exception ex) {
                    failed.addSuppressed(ex);
                }

                logger.info("Loaded " + this.entry.id() + "from jar package locally.");
                return;
            }

            for (int i = 0; i < Hyacinthusclip.ALIYUN_MAVEN_REPO_LINK_BASE.length; i++) {
                final String url = Hyacinthusclip.ALIYUN_MAVEN_REPO_LINK_BASE[i] + "/" + this.entry.path();

                try {
                    final URL urlObj = new URL(url);
                    final HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setRequestMethod("GET");

                    this.write(connection.getInputStream());

                    logger.info("Downloaded: " + url);
                    return;
                }catch (Exception ex) {
                    failed.addSuppressed(ex);
                }
            }

            throw failed;
        }, worker);
    }

    private void write(InputStream in) throws IOException {
        if (!Files.isDirectory(this.outputFile.getParent())) {
            Files.createDirectories(this.outputFile.getParent());
        }

        Files.deleteIfExists(this.outputFile);

        try (
                final InputStream stream = in;
                final ReadableByteChannel inputChannel = Channels.newChannel(stream);
                final FileChannel outputChannel = FileChannel.open(outputFile, CREATE, WRITE, TRUNCATE_EXISTING)
        ) {
            outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
        }
    }
}
