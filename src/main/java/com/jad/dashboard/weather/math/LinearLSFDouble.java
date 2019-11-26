package com.jad.dashboard.weather.math;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class LinearLSFDouble {

    @Getter private Point2D first;
    @Getter private Point2D last;
    private double sumX;
    private double sumY;
    private double sumX2;
    private double sumY2;
    private double sumXY;
    private int n = 0;
    private final List<Point2D> points = new ArrayList<>();

    public LinearLSFDouble copy() {
        LinearLSFDouble linearLSF = new LinearLSFDouble();
        linearLSF.sumX = this.sumX;
        linearLSF.sumY = this.sumY;
        linearLSF.sumX2 = this.sumX2;
        linearLSF.sumY2 = this.sumY2;
        linearLSF.sumXY = this.sumXY;
        linearLSF.n = this.n;
        linearLSF.first = this.first.copy();
        linearLSF.last = this.last.copy();
        linearLSF.points.addAll(this.points);
        return linearLSF;
    }

    public void addPoint(Point2D point2D) {
        addPoint(point2D.getX(), point2D.getY());
    }

    public void addPoint(double x, double y) {
        final Point2D point = new Point2D(x, y);
        if (first == null) {
            this.first = point;
        }
        last = point;
        sumX += x;
        sumY += y;
        sumX2 += x*x;
        sumXY += x*y;
        sumY2 += y*y;
        n++;
        points.add(point);
    }

    public LinearCoefficients calc() {
        return calc(false);
    }

    public LinearCoefficients calc(boolean withLoss) {
        return calc(withLoss, true);
    }

    public LinearCoefficients calc(boolean withLoss, boolean maxDistance) {
        final double a = (sumXY * n - sumX*sumY) / (n*sumX2 - sumX*sumX);

        final double b = (sumY - a * sumX) / n;
        LinearCoefficients lc = new LinearCoefficients(a, b);

        if (withLoss) {
            double loss = sumY2 + a*(a*sumX2-2*sumXY+2*b*sumX) + b*(n*b-2*sumY);
            lc.setLoss(loss);
            lc.setN(n);
        }
        if (maxDistance) {
            final double maxDist = points.stream().parallel()
                    .mapToDouble(p -> Math.abs(-a * p.getX() + p.getY() - b) / Math.sqrt(a * a + b * b))
                    .max().getAsDouble();
            lc.setMaxDist(maxDist);
        }
        return lc;
    }



}
