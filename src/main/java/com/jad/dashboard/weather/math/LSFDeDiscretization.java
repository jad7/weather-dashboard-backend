package com.jad.dashboard.weather.math;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LSFDeDiscretization implements Iterable<Point2D> {
    private final Iterator<Point2D> data;
    private final double minLosFactor;

    public LSFDeDiscretization(Iterator<Point2D> data, double minLosFactor) {
        this.data = data;
        this.minLosFactor = minLosFactor;
    }

    public List<Point2D> compute() {
        final List<Point2D> res = new ArrayList<>();
        LinearLSFDouble lsf = new LinearLSFDouble();
        if (data.hasNext()) {
            lsf.addPoint(data.next());
            res.add(lsf.getFirst());
            Point2D current;
            Point2D newPoint;
            if (!data.hasNext()) {
                return res;
            }
            lsf.addPoint(current = data.next());
            Double lastA = null;

            while (data.hasNext()) {
                lsf.addPoint(newPoint = data.next());
                final LinearCoefficients coefficients = lsf.calc(true, true);
                if (
                        1 == 0
                        || coefficients.getLoss() > minLosFactor
                        //|| coefficients.getLoss() / coefficients.getN() > minLosFactor
                        //|| (coefficients.getN() > 20 && lastA != null && Math.signum(coefficients.getA()) != Math.signum(lastA))
                       // || (coefficients.getN() > 3 && coefficients.getDistStatistic().getMax() > minLosFactor)
                       // || (coefficients.getN() > 2 && coefficients.getDistStatistic().getAverage() > minLosFactor)
                ) {
                    //res.add(current);
                    //res.add(newPoint);
                    res.add(new Point2D(current.getX(), coefficients.getA()*current.getX() + coefficients.getB()));
                    lsf = new LinearLSFDouble();
                    lastA = null;
                    lsf.addPoint(newPoint);
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

    @Override
    public Iterator<Point2D> iterator() {
        return compute().iterator();
    }


}
