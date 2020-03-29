package com.jad.dashboard.weather.math;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

public class LSFDeDiscretization {
    private final Iterator<Point2D> data;

    public LSFDeDiscretization(Iterator<Point2D> data) {
        this.data = data;
    }

    public List<Point2D> compute(double minLossFactor) {
        return compute(defaultMinLossCondition(minLossFactor));
    }

    public List<Point2D> compute(BiPredicate<LinearCoefficients, Double> stopCondition) {
        Objects.requireNonNull(stopCondition);
        final List<Point2D> res = new ArrayList<>();
        LinearLSFDouble lsf = new LinearLSFDouble();
        Point2D current;
        Point2D newPoint;
        if (data.hasNext()) {
            lsf.addPoint(current = data.next());
            res.add(lsf.getFirst());

            if (!data.hasNext()) {
                return res;
            }
            //lsf.addPoint(current = data.next());
            Double lastA = null;

            while (data.hasNext()) {
                lsf.addPoint(newPoint = data.next());
                final LinearCoefficients coefficients = lsf.calc(true, false);
                if (
                        1 == 0
                        || stopCondition.test(coefficients, lastA)
                        //|| coefficients.getLoss() > minLosFactor
                        //|| coefficients.getN() >= 3 && coefficients.getLoss() / coefficients.getN() > minLosFactor
                       // || (coefficients.getN() > 3 && lastA != null && Math.signum(coefficients.getA()) != Math.signum(lastA))
                       // || (coefficients.getN() > 3 && coefficients.getDistStatistic().getMax() > minLosFactor)
                       // || (coefficients.getN() > 2 && coefficients.getDistStatistic().getAverage() > minLosFactor)
                ) {
                    //res.add(current);
                    //res.add(newPoint);
                    res.add(new Point2D(current.getX(), coefficients.getA()*current.getX() + coefficients.getB()));
                    lsf = new LinearLSFDouble();
                    lsf.addPoint(current);
                    lsf.addPoint(newPoint);
                    lastA = lsf.calc(false).getA();
                } else {
                    current = newPoint;
                    lastA = coefficients.getA();
                }
            }
            if (!res.get(res.size() - 1).equals(lsf.getLast())) {
                res.add(lsf.getLast());
            }
        }
        return res;

    }



    public static BiPredicate<LinearCoefficients, Double> defaultMinLossCondition(double minLoss) {
        return (coefficients, lastA) -> coefficients.getLoss() > minLoss;
    }


}
