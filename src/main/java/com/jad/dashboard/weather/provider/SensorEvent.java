package com.jad.dashboard.weather.provider;

import lombok.Data;
import org.springframework.context.ApplicationEvent;


@Data
public class SensorEvent extends ApplicationEvent {
    private final String sensorName;
    private final Float sensorValue;

    public SensorEvent(Object source, String sensorName, Float sensorValue) {
        super(source);
        this.sensorName = sensorName;
        this.sensorValue = sensorValue;
    }
}
