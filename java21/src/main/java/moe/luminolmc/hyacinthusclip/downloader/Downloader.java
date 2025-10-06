package moe.luminolmc.hyacinthusclip.downloader;

import moe.luminolmc.hyacinthusclip.FileEntry;
import moe.luminolmc.hyacinthusclip.Hyacinthusclip;
import moe.luminolmc.hyacinthusclip.Util;
import moe.luminolmc.hyacinthusclip.integrated.leavesclip.logger.SimpleLogger;
import moe.luminolmc.hyacinthusclip.integrated.leavesclip.mixin.MixinURLClassLoader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.nio.file.StandardOpenOption.*;

public record Downloader(FileEntry entry, Path outputDir, Path outputFile, String baseDir, boolean useInternalIfFailed) {
    private static final SimpleLogger logger = new SimpleLogger("Hyacinthusclip");

    @Contract("_ -> new")
    public @NotNull CompletableFuture<Path> downloadOrLoad(Executor worker) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Try loading : " + this.entry.id());

            final RuntimeException failed = new RuntimeException("All maven repo download attempts has been failed for library " + this.entry.id() + "!");

            final String filePath = Util.endingSlash(this.baseDir) + this.entry.path();
            InputStream fileStream = MixinURLClassLoader.class.getResourceAsStream(filePath);
            if (fileStream != null && this.useInternalIfFailed) {
                try {
                    logger.info("Located target jar inside jar, loading.");
                    this.deleteIfInvalid();
                    this.write(fileStream);
                    logger.info("Loaded " + this.entry.id() + "from jar package locally.");
                    return this.outputFile;
                }catch (Exception ex) {
                    failed.addSuppressed(ex);
                }
            }

            logger.info("Missing: " + this.entry.id() + ", downloading from maven repo.");

            try {
                this.deleteIfInvalid();

                final MavenDependencyResolver resolver = new MavenDependencyResolver(
                        (List.of(Arrays.stream(Hyacinthusclip.ALL_MAVEN_REPO_LINK_BASE).map(url -> new MavenDependencyResolver.MavenRepository(String.valueOf(url.hashCode()), url)).toArray(MavenDependencyResolver.MavenRepository[]::new))),
                        this.outputDir
                );

                resolver.downloadTo(this.entry.id(), this.outputFile);
                return this.outputFile;
            }catch (Exception ex) {
                failed.addSuppressed(ex);
            }

            throw failed;
        }, worker);
    }

    private void deleteIfInvalid() throws IOException {
        if (!Files.isDirectory(this.outputFile.getParent())) {
            Files.createDirectories(this.outputFile.getParent());
        }

        Files.deleteIfExists(this.outputFile);
    }

    private void write(InputStream in) throws IOException {
        try (
                final InputStream stream = in;
                final ReadableByteChannel inputChannel = Channels.newChannel(stream);
                final FileChannel outputChannel = FileChannel.open(this.outputFile, CREATE, WRITE, TRUNCATE_EXISTING)
        ) {
            outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
        }
    }
}
