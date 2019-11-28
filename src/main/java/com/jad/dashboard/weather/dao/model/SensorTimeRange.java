package com.jad.dashboard.weather.dao.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class SensorTimeRange {
    private String name;
    private long from, to;
}
