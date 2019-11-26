package com.jad.dashboard.weather.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LinearLSFBigDecimal {

    private BigDecimal sumX = new BigDecimal(0);
    private BigDecimal sumY = new BigDecimal(0);
    private BigDecimal sumX2 = new BigDecimal(0);
    private BigDecimal sumXY = new BigDecimal(0);
    private int n = 0;

    public LinearLSFBigDecimal copy() {
        LinearLSFBigDecimal linearLSF = new LinearLSFBigDecimal();
        linearLSF.sumY = new BigDecimal(sumY.toString());
        linearLSF.sumX = new BigDecimal(sumX.toString());
        linearLSF.sumX2 = new BigDecimal(sumX2.toString());
        linearLSF.sumXY = new BigDecimal(sumXY.toString());
        linearLSF.n = n;
        return linearLSF;
    }

    public void addPoint(Point2D point2D) {
        addPoint(point2D.getX(), point2D.getY());
    }

    public void addPoint(double x, double y) {
        BigDecimal bdX = new BigDecimal(x);
        sumX = sumX.add(bdX);
        BigDecimal bdY = new BigDecimal(y);
        sumY = sumY.add(bdY);
        sumX2 = sumX2.add(bdX.pow(2));
        sumXY = sumXY.add(bdX.multiply(bdY));
        n++;
    }

    public LinearCoefficients calc() {
        BigDecimal bdN = new BigDecimal(n);
        BigDecimal bdA = sumXY.multiply(bdN).add(sumX.multiply(sumY).negate())
                .divide(bdN.multiply(sumX2).add(sumX.pow(2).negate()), 10, RoundingMode.CEILING);

        BigDecimal bdB = sumY.add(bdA.multiply(sumX).negate()).divide(bdN, 10, RoundingMode.CEILING);
        return new LinearCoefficients(bdA.doubleValue(), bdB.doubleValue());
    }

}
