package com.jad.dashboard.weather.provider;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.*;

@Slf4j
public class FileValueReader {
    private final Path file;

    public FileValueReader(Path file) {
        this.file = file;
    }

    public Float readValue() throws IOException {
        return Float.parseFloat(normalizeSpace(remove(Files.readString(file), '\n')));
    }



}
