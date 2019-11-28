package com.jad.dashboard.weather.config;

import com.jad.dashboard.weather.provider.FileValueReader;
import com.jad.dashboard.weather.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.file.Path;


@Slf4j
@Configuration
public class SensorsConfig {

    @Bean @Profile("prod")
    public Sensor outside(@Value("${sensors.files.outside}") Path pathToFile) {
        return getSensor(pathToFile, "outside");
    }

    @Bean @Profile("prod")
    public Sensor outside2(@Value("${sensors.files.outside2}") Path pathToFile) {
        return getSensor(pathToFile, "outside2");
    }

    @Bean @Profile("prod")
    public Sensor centerRoom(@Value("${sensors.files.centerRoom}") Path pathToFile) {
        return getSensor(pathToFile, "centerRoom");
    }

    @Bean @Profile("prod")
    public Sensor nearWindow(@Value("${sensors.files.nearWindow}") Path pathToFile) {
        return getSensor(pathToFile, "nearWindow");
    }

    @Bean @Profile("humidity")
    public Sensor humidity(@Value("${sensors.files.humidity}") Path pathToFile) {
        return getSensor(pathToFile, "humidity");
    }

    private Sensor getSensor(Path pathToFile, String name) {
        final FileValueReader fileValueReader = new FileValueReader(pathToFile);
        return () -> {
            try {
                return fileValueReader.readValue();
            } catch (Exception e) {
                log.warn("Can not read sensor {} value", name, e);
                return null;
            }
        };
    }


    @Bean("inside")
    @Profile("dev")
    public Sensor inside() {
        return () -> (float)Utils.roundD(Math.random() * 2 + 22);
    }

    @Bean("outside")
    @Profile("dev")
    public Sensor outside1() {
        return () -> (float)Utils.roundD(Math.random() * 6 - 3);
    }


}
