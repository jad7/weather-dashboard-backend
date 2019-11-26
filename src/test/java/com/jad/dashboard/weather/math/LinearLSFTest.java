package com.jad.dashboard.weather.math;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.knowm.xchart.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

class LinearLSFTest {



    private static final String POINTS = "points";

    @Test
    public void test1() {
        LinearLSFBigDecimal linearLSF = new LinearLSFBigDecimal();
        linearLSF.addPoint(tp(27, 18, -7.9));
        linearLSF.addPoint(tp(27, 43, -7.87));
        linearLSF.addPoint(tp(28, 7, -7.9));
        List<Point2D> points = asList(
                tp(27, 18, -7.9),
                tp(27, 43, -7.87),
                tp(28, 7, -7.9),
                tp(28, 32, -7.9),
                tp(28, 56, -7.94),
                tp(29, 21, -7.94),
                tp(30, 34, -7.97),
                tp(31, 23, -7.94),
                tp(46, 19, -8)
        );

        LinearCoefficients calc = linearLSF.calc();
        System.out.println("First res:" + calc);

        for (Point2D point : points) {
            linearLSF.addPoint(point);
            LinearCoefficients calc1 = linearLSF.calc();
            System.out.println("New point res:" + calc1
                    + "; Dif A:" + (Math.abs(calc.getA() - calc1.getA()))
                    + "; Dif B:" + (Math.abs(calc.getB() - calc1.getB()))
            );
            calc = calc1;
        }

    }


    public static Point2D tp(int min, int sec, double y) {
        return p((min-27) * 60 + sec, y);
    }

    public static Point2D p(double x, double y) {
        return new Point2D(x, y);
    }


    public static void main(String[] args) throws InterruptedException {
        //System.out.println(((999999999999999999999999999999999999999999d * 99999999999999999999999d) - 1) / Math.random());
        //LinearLSF linearLSF = new LinearLSF();
        LinearLSFDouble linearLSF = new LinearLSFDouble();
        List<Point2D> points = readPoints();/*asList(
                tp(27, 18, -7.9),
                tp(27, 43, -7.87),
                tp(28, 7, -7.9),
                tp(28, 32, -7.9),
                tp(28, 56, -7.94),
                tp(29, 21, -7.94),
                tp(30, 34, -7.97),
                tp(31, 23, -7.94),
                tp(46, 19, -8),
                tp(51, 17, -8.6),
                tp(53, 18, -8.2),
                tp(54, 17, -8.7)
        );*/

        final XYChart chart = new XYChartBuilder().width(600).height(400).title("Area Chart").xAxisTitle("X").yAxisTitle("Y").build();
        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Point2D point = points.get(i);
            xData.add(point.getX());
            yData.add(point.getY());
            linearLSF.addPoint(point);
        }
        XYSeries pointsSeries = chart.addSeries(POINTS, xData, yData);
        pointsSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);


        LinearCoefficients calc = linearLSF.calc(true);
        System.out.println("First res:" + calc);

        double[][] data = buildLine(calc, xData.get(0), xData.get(xData.size() - 1));
        XYSeries lsf = chart.addSeries("LSF", data[0], data[1]);
        lsf.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        SwingWrapper<XYChart> swingWrapper = new SwingWrapper<>(chart);
        swingWrapper.displayChart();


        for (int i = 3; i < points.size(); i++) {
            Thread.sleep(500);
            Point2D point = points.get(i);
            xData.add(point.getX());
            yData.add(point.getY());


            linearLSF.addPoint(point);
            LinearCoefficients calc1 = linearLSF.calc(true);
            System.out.println("New point res:" + calc1
                    + "; Dif A:" + (Math.abs(calc.getA() - calc1.getA()))
                    + "; Dif B:" + (Math.abs(calc.getB() - calc1.getB()))
                    + "; Loss:" + calc1.getLoss()/calc1.getN()
            );

            data = buildLine(calc1, xData.get(0), xData.get(xData.size() - 1));
            double[][] finalData = data;
            int finalI = i;
            javax.swing.SwingUtilities.invokeLater(() -> {
                chart.updateXYSeries(POINTS, xData, yData, null);
                chart.addSeries("LSF" + finalI, finalData[0], finalData[1]).setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
                swingWrapper.repaintChart();
            });
            //lsf = chart.addSeries("LSF" + i, data[0], data[1]);
            //lsf.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);


            calc = calc1;
        }




// Show it

    }

    private static List<Point2D> readPoints() {
        ObjectMapper om = new ObjectMapper();
        try {
            //List<JPoint> points = om.readValue(new File("/Users/ikrokhmalyov/IdeaProjects/weather/src/test/resources/lsf/outside.json"), new TypeReference<List<JPoint>>() {});
            List<JPoint> points = om.readValue(new File("/Users/ikrokhmalyov/IdeaProjects/weather/src/test/resources/lsf/dataLog.txt"), new TypeReference<List<JPoint>>() {});
            long minDate = points.stream().map(JPoint::getX).mapToLong(Date::getTime).min().getAsLong();
            return points.stream().map(p -> new Point2D(Math.ceil((p.getX().getTime() - minDate)/1000), p.getY())).collect(toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Data public static class JPoint {
        private Date x; private float y;
    }

    private static double[][] buildLine(LinearCoefficients calc, double xmin, double xmax) {
        return new double[][]{
                new double[]{xmin, xmax},
                new double[]{calc.getA() * xmin + calc.getB(), calc.getA() * xmax + calc.getB()}
        };
    }


}