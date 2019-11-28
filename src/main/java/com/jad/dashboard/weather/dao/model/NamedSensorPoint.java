package com.jad.dashboard.weather.dao.model;

import lombok.Data;

@Data
public class NamedSensorPoint extends SensorPoint {
    private String sensorName;
}
