package com.jad.dashboard.weather.math;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Point2D {
    private final double x,y;
    public Point2D copy() {return new Point2D(x,y);}
}
