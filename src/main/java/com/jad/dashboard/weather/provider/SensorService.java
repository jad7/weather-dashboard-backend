package com.jad.dashboard.weather.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Supplier;

@Configuration
public class SensorService {

    @Autowired
    private Map<String, Supplier<Float>> sensors;


}
