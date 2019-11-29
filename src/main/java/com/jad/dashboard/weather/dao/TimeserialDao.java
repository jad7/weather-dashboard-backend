package com.jad.dashboard.weather.dao;


import com.jad.dashboard.weather.dao.model.SensorPoint;
import com.jad.dashboard.weather.dao.model.SensorTimeRange;
import com.jad.dashboard.weather.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.*;

@Slf4j
@Repository
public class TimeserialDao {

    private static final String DELIMITER = "|";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Value("${storage.files.currentDate}")
    private Path currentDateData;
    @Value("${storage.files.history}")
    private Path history;

    public Stream<SensorPoint> loadLastDay() throws IOException {
        if (Files.isReadable(currentDateData)) {
            return Files.readAllLines(currentDateData).stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(this::fromString)
                    .filter(Objects::nonNull)
                    ;
        } else {
            return Stream.empty();
        }
    }


    private SensorPoint fromString(String s) {
        final SensorPoint sensorPoint = new SensorPoint();
        final String[] split = StringUtils.tokenizeToStringArray(s, DELIMITER);
        if (split != null && split.length == 3) {
            sensorPoint.setSensorName(split[0]);
            sensorPoint.setTime(Instant.parse(split[1]));
            sensorPoint.setValue(Float.parseFloat(split[2]));
            return sensorPoint;
        }
        log.error("Can not parse SensorPoint: \"{}\"", s);
        return null;
    }

    private String serializeToString(SensorPoint sensorPoint) {
        return sensorPoint.getSensorName()
                + DELIMITER + sensorPoint.getTime()
                + DELIMITER + Utils.round(sensorPoint.getValue())
                + LINE_SEPARATOR;
    }

    public void storePoint(String key, Float val) throws IOException {
        Files.write(currentDateData, serializeToString(new SensorPoint(key, val)).getBytes(), CREATE, APPEND);
    }

    public void storeHistory(List<SensorPoint> convertPoints) throws IOException {
        Files.write(history, convertPoints.stream().map(this::serializeToString).collect(Collectors.joining()).getBytes(),
                CREATE, APPEND);
    }

    public void recreateCurrentData(List<SensorPoint> last24HPoints) throws IOException {
        Files.write(currentDateData, last24HPoints.stream().map(this::serializeToString).collect(Collectors.joining()).getBytes(),
                TRUNCATE_EXISTING, WRITE);
    }

    public Stream<SensorPoint> loadHistory(List<SensorTimeRange> historySensTR) {
        final Map<String, SensorTimeRange> dataMap = historySensTR.stream()
                .collect(Collectors.toMap(SensorTimeRange::getName, Function.identity()));
        try {
            return Files.lines(history).map(this::fromString).filter(point -> {
                if (point == null) return false;
                final SensorTimeRange sensorTimeRange = dataMap.get(point.getSensorName());
                if (sensorTimeRange != null) {
                    final long time = point.getTime().toEpochMilli();
                    return time >= sensorTimeRange.getFrom() && time <= sensorTimeRange.getTo();
                }
                return false;
            });
        } catch (IOException e) {
            log.warn("Can not read history", e);
            return Stream.empty();
        }
    }
}
