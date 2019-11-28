package com.jad.dashboard.weather.controller.dto;

import lombok.Data;

@Data
public class NamedSensorValue extends SensorValue {
    private String sensorName;
}
