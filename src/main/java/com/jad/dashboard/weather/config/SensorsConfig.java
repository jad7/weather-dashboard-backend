package com.jad.dashboard.weather.config;

import com.jad.dashboard.weather.provider.FileValueReader;
import com.jad.dashboard.weather.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.util.function.Supplier;


@Configuration
public class SensorsConfig {

    @Bean
    @Profile("prod")
    public Sensor outside(@Value("${sensors.outside.file}") File pathToFile) {
        return new FileValueReader(pathToFile)::readValue;
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
