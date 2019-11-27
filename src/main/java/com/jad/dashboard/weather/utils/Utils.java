package com.jad.dashboard.weather.utils;

public class Utils {

    public static String round(double value) {
        return String.format("%.2f", value);
    }

    public static double roundD(double value) {
        return (double) Math.round(value * 100) / 100;
    }
    public static float roundF(float value) {
        return (float) Math.round(value * 100) / 100;
    }
}
