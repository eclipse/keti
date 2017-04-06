package com.ge.predix.cloudfoundry.client;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class CloudFoundryUtilities {

    private CloudFoundryUtilities() {
        throw new AssertionError();
    }

    public static Path getPathOfFileMatchingPattern(final String directoryPath, final String filePattern)
            throws IOException {

        try (DirectoryStream<Path> directoryStream =
                Files.newDirectoryStream(Paths.get(directoryPath), filePattern)) {

            return directoryStream.iterator().next();
        }
    }
}
