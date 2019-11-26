package com.jad.dashboard.weather.config;

import com.jad.dashboard.weather.provider.FileValueReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.util.function.Supplier;


@Configuration
public class SensorsConfig {

    @Bean
    @Profile(("prod"))
    public Supplier<Float> outside(@Value("${sensors.outside.file}") File pathToFile) {
        return new FileValueReader(pathToFile)::readValue;
    }


}
