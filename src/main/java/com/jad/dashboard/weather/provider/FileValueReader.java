package com.jad.dashboard.weather.provider;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.commons.lang3.StringUtils.remove;

@Slf4j
public class FileValueReader {
    private final Path file;

    public FileValueReader(Path file) {
        this.file = file;
    }

    public Float readValue() throws IOException {
        final String result = String.join("", Files.readAllLines(file));
        return Float.parseFloat(normalizeSpace(remove(result, '\n')));
    }



}
