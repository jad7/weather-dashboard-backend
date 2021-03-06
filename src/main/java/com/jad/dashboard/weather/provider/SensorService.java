package com.jad.dashboard.weather.provider;

import com.jad.dashboard.weather.config.Sensor;
import com.jad.dashboard.weather.dao.TimeserialDao;
import com.jad.dashboard.weather.dao.model.SensorPoint;
import com.jad.dashboard.weather.dao.model.SensorTimeRange;
import com.jad.dashboard.weather.math.LSFDeDiscretization;
import com.jad.dashboard.weather.math.Point2D;
import com.jad.dashboard.weather.math.RingBufferTimeserial;
import com.jad.dashboard.weather.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Configuration
public class SensorService {
    private static final RingBufferTimeserial empty = new RingBufferTimeserial(0);

    private final ApplicationEventPublisher applicationEventPublisher;

    private final Map<String, Sensor> sensors;
    private final int intervalSec;
    private final int intervalHistorySec;
    private final double minLossFactor;
    private final TimeserialDao timeserialDao;
    private final Map<String, RingBufferTimeserial> dataAggregators;

    private final ScheduledExecutorService executorService;

    public SensorService(ApplicationEventPublisher applicationEventPublisher,
                         Map<String, Sensor> sensors,
                         @Value("${sensors.read.interval.seconds:10}") int intervalSec,
                         @Value("${sensors.history.interval.seconds:86400}") int intervalHistorySec,
                         @Value("${sensors.history.lsf.minLos:0.1}") double minLossFactor,
                         TimeserialDao timeserialDao) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.sensors = sensors;
        this.intervalSec = intervalSec;
        this.intervalHistorySec = intervalHistorySec;
        this.minLossFactor = minLossFactor;
        this.timeserialDao = timeserialDao;
        int bufferSize = (int) Math.ceil((double) intervalHistorySec / intervalSec * 1.05);
        this.dataAggregators = sensors.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new RingBufferTimeserial(bufferSize)));
        this.executorService = Executors.newScheduledThreadPool(1, new CustomizableThreadFactory("sensors-loader-"));
        init();
    }

    public void init() {
        loadLastDay(true);
        executorService.scheduleAtFixedRate(this::loadPublishData, 0, intervalSec, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(() -> loadLastDay(false), intervalHistorySec, intervalHistorySec, TimeUnit.SECONDS);
    }


    private void loadLastDay(boolean loadToRb) {
        log.debug("Start compaction");
        try {
            final Instant last24H = Instant.now().minus(intervalHistorySec, ChronoUnit.SECONDS);
            List<SensorPoint> forHistory = new ArrayList<>();
            List<SensorPoint> last24HPoints = new ArrayList<>();
            timeserialDao.loadLastDay() //Maybe sort
                .forEach(sp -> {
                    if (sp.getTime().isBefore(last24H)) {
                        forHistory.add(sp);
                    } else {
                        last24HPoints.add(sp);
                        if (loadToRb) {
                            final RingBufferTimeserial ringBufferTimeserial = dataAggregators.get(sp.getSensorName());
                            if (ringBufferTimeserial != null) {
                                final long time = sp.getTime().toEpochMilli();
                                ringBufferTimeserial.add(time, sp.getValue());
                            }
                        }
                    }
                });
            if (!forHistory.isEmpty()) {
                timeserialDao.storeHistory(convertPoints(forHistory));
            }
            if (!last24HPoints.isEmpty()) {
                timeserialDao.recreateCurrentData(last24HPoints);
            }
        } catch (IOException e) {
            log.warn("Can not load data for a last day:");
        }
    }

    private List<SensorPoint> convertPoints(List<SensorPoint> forHistory) {
        final long minValue = forHistory.stream().map(SensorPoint::getTime).mapToLong(Instant::toEpochMilli).min().getAsLong();
        final List<SensorPoint> collect = sensors.keySet().stream().flatMap(name ->
                new LSFDeDiscretization(
                        forHistory.stream()
                                .filter(sp -> sp.getSensorName().equals(name))
                                .map(sp -> new Point2D(sp.getTime().toEpochMilli() - minValue, sp.getValue()))
                                .iterator(), minLossFactor)
                        .compute().stream()
                        .map(point2D -> new SensorPoint(name, (float) point2D.getY(), Instant.ofEpochMilli((long) (point2D.getX() + minValue))))
        ).collect(Collectors.toList());
        log.debug("Compacted {} points to history {} points", forHistory.size(), collect.size());
        return collect;
    }

    @PreDestroy
    public void stop() {
        executorService.shutdownNow();
    }

    private void loadPublishData() {
        for (Map.Entry<String, Sensor> entry : sensors.entrySet()) {
            final Supplier<Float> provider = entry.getValue();
            try {
                final Float val = provider.get();
                if (val != null) {
                    log.debug("Sensor {} val: {}", entry.getKey(),  Utils.round(val));
                    dataAggregators.get(entry.getKey()).add(Instant.now().toEpochMilli(), val);
                    try {
                        timeserialDao.storePoint(entry.getKey(), val);
                    } catch (IOException e) {
                        log.warn("Can not store point", e); //TODO event to UI
                    }
                    applicationEventPublisher.publishEvent(new SensorEvent(this, entry.getKey(), val));
                } else {
                    log.warn("Sensor {} return NULL", entry.getKey());
                }
            } catch (RuntimeException e) {
                log.warn("Exception on reading data from sensor", e);
                //continue
            }
        }
    }


    public Stream<SensorPoint> getFromBuffer(List<String> sensorsList, Instant from) {
        Stream<SensorPoint> builder = Stream.empty();
        for (String s : (sensorsList == null ? sensors.keySet() : sensorsList)) {
            builder = Stream.concat(builder, dataAggregators.getOrDefault(s, empty).getStreamFrom(from.toEpochMilli())
                    .map(tp -> new SensorPoint(s, (float) tp.getValue(), Instant.ofEpochMilli(tp.getTime())))
            );
        }
        return builder;
    }

    public Stream<SensorPoint> getDataStream(List<String> sensorsList, long fromEpochMillis, Long toEpochMillis) {
        final Collection<String> sensors = sensorsList == null ? this.sensors.keySet() : sensorsList;
        final Map<String, RingBufferTimeserial.MinMax<Long>> minMax = sensors.stream()
                .collect(Collectors.toMap(Function.identity(), s -> dataAggregators.get(s).timeMinMax()));
        Stream<SensorPoint> builder = Stream.empty();

        List<SensorTimeRange> historySensTR = new ArrayList<>(this.sensors.size());
        List<SensorTimeRange> bufferSensTR = new ArrayList<>(this.sensors.size());
        for (String sensorName : sensors) {
            final RingBufferTimeserial.MinMax<Long> longMinMax = minMax.get(sensorName);
            if (longMinMax != null) {
                if (toEpochMillis > longMinMax.getMin()) {
                    if (fromEpochMillis < longMinMax.getMin()) {
                        historySensTR.add(new SensorTimeRange(sensorName, fromEpochMillis, longMinMax.getMin()));
                        bufferSensTR.add(new SensorTimeRange(sensorName, longMinMax.getMin(), toEpochMillis));
                    } else {
                        bufferSensTR.add(new SensorTimeRange(sensorName, fromEpochMillis, toEpochMillis));
                    }
                } else {
                    historySensTR.add(new SensorTimeRange(sensorName, fromEpochMillis, toEpochMillis));
                }
            }
        }

        builder = Stream.concat(builder, timeserialDao.loadHistory(historySensTR));
        for (SensorTimeRange sensorTimeRange : bufferSensTR) {
            builder = Stream.concat(builder, dataAggregators.getOrDefault(sensorTimeRange.getName(), empty)
                    .getStreamFromTo(sensorTimeRange.getFrom(), sensorTimeRange.getTo())
                    .map(tp -> new SensorPoint(sensorTimeRange.getName(), (float) tp.getValue(), Instant.ofEpochMilli(tp.getTime())))
            );
        }

        return builder;
    }
}
