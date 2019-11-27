package com.jad.dashboard.weather.config;

import java.util.function.Supplier;

@FunctionalInterface
public interface Sensor extends Supplier<Float> {
}
