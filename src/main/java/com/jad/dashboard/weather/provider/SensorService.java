package com.jad.dashboard.weather.provider;

import com.jad.dashboard.weather.dao.TimeserialDao;
import com.jad.dashboard.weather.dao.model.SensorPoint;
import com.jad.dashboard.weather.math.LSFDeDescritisation;
import com.jad.dashboard.weather.math.Point2D;
import com.jad.dashboard.weather.math.RingBufferTimeserial;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class SensorService {


    private final ApplicationEventPublisher applicationEventPublisher;

    private final Map<String, Supplier<Float>> sensors;
    private final int intervalSec;
    private final TimeserialDao timeserialDao;
    private final Map<String, RingBufferTimeserial> dataAggregators;

    private final ScheduledExecutorService executorService;

    public SensorService(ApplicationEventPublisher applicationEventPublisher,
                         Map<String, Supplier<Float>> sensors,
                         @Value("${sensors.read.interval.seconds}") int intervalSec,
                         TimeserialDao timeserialDao) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.sensors = sensors;
        this.intervalSec = intervalSec;
        this.timeserialDao = timeserialDao;
        int bufferSize = (int) Math.ceil(24d * 3600 / intervalSec * 1.05);
        this.dataAggregators = sensors.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new RingBufferTimeserial(bufferSize)));
        this.executorService = Executors.newScheduledThreadPool(2, new CustomizableThreadFactory("sensorLoader"));
        init();
    }

    public void init() {
        loadLastDay(true);
        executorService.scheduleAtFixedRate(this::loadPublishData, 0, intervalSec, TimeUnit.SECONDS); //TODO property
        executorService.scheduleAtFixedRate(() -> loadLastDay(false), 1, 1, TimeUnit.DAYS); //TODO property
    }

    private void storeHistory() {

    }

    private void loadLastDay(boolean loadToRb) {
        try {
            final Instant last24H = Instant.now().minus(1, ChronoUnit.DAYS);
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
        return sensors.keySet().stream().flatMap(name ->
                new LSFDeDescritisation(
                    forHistory.stream()
                    .filter(sp -> sp.getSensorName().equals(name))
                    .map(sp -> new Point2D(sp.getTime().toEpochMilli() - minValue, sp.getValue()))
                    .iterator() , 0.1)
                .compute().stream()
                .map(point2D -> new SensorPoint(name, (float) point2D.getY(), Instant.ofEpochMilli((long) (point2D.getX() + minValue))))
        ).collect(Collectors.toList());
    }

    @PreDestroy
    public void stop() {
        executorService.shutdownNow();
    }

    private void loadPublishData() {
        for (Map.Entry<String, Supplier<Float>> entry : sensors.entrySet()) {
            final Supplier<Float> provider = entry.getValue();
            try {
                final Float val = provider.get();
                if (val != null) {
                    dataAggregators.get(entry.getKey()).add(val);
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


}
