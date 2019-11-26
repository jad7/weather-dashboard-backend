package com.jad.dashboard.weather.dao.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor
@AllArgsConstructor
public class SensorPoint {
    private String sensorName;
    private float value;
    private Instant time;



    public SensorPoint(String sensorName, Float val) {
        this.sensorName = sensorName;
        this.value = val;
        this.time = Instant.now();
    }
}
