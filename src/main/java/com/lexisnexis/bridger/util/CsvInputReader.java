package com.lexisnexis.bridger.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CsvInputReader {

    private CsvInputReader() {
    }

    public static Reader open(String fileName) throws IOException {
        Path workingDirectoryFile = Path.of(fileName);
        if (Files.isRegularFile(workingDirectoryFile)) {
            return Files.newBufferedReader(workingDirectoryFile, StandardCharsets.UTF_8);
        }

        Path sourceResourceFile = Path.of("src", "main", "resources").resolve(fileName);
        if (Files.isRegularFile(sourceResourceFile)) {
            return Files.newBufferedReader(sourceResourceFile, StandardCharsets.UTF_8);
        }

        InputStream classpathResource = CsvInputReader.class.getClassLoader().getResourceAsStream(fileName);
        if (classpathResource != null) {
            return new InputStreamReader(classpathResource, StandardCharsets.UTF_8);
        }

        throw new FileNotFoundException("CSV file '" + fileName + "' was not found in the working directory, "
            + sourceResourceFile.toAbsolutePath() + ", or the application classpath");
    }
}
