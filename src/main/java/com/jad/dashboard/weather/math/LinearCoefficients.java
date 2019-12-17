package com.jad.dashboard.weather.math;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.DoubleSummaryStatistics;

@RequiredArgsConstructor
@Data
public class LinearCoefficients {
    private final double a, b;
    private Double loss;
    private int n;
    private DoubleSummaryStatistics distStatistic;
}
