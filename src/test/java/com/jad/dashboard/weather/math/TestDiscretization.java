package com.jad.dashboard.weather.math;

import com.jad.dashboard.weather.dao.TimeserialDao;
import com.jad.dashboard.weather.dao.model.SensorPoint;
import lombok.Data;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestDiscretization {
    private static final String POINTS = "points";

    public static void main(String[] args) {
        List<Point2D> points = readPoints();
        final LSFDeDiscretization res = new LSFDeDiscretization(points.iterator());

        final XYChart chart = new XYChartBuilder().width(1400).height(900).title("Area Chart").xAxisTitle("X").yAxisTitle("Y").build();
        List<Double> xData = points.stream().map(Point2D::getX).collect(Collectors.toList());
        List<Double> yData = points.stream().map(Point2D::getY).collect(Collectors.toList());

        XYSeries pointsSeries = chart.addSeries(POINTS, xData, yData);
        pointsSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);

        final List<Point2D> compute = res.compute(0.1d);
        LSFDeDiscretization res2 = new LSFDeDiscretization(compute.iterator());
        List<Point2D> compute2 = res2.compute(LSFDeDiscretization.defaultMinLossCondition(0.5d).or((coef,lastA) -> lastA != null && Math.abs(Math.atan(coef.getA() - lastA)) < Math.PI/36));
        System.out.println("1: From: " + points.size() + " to:" + compute.size());
        System.out.println("2: From: " + compute.size() + " to:" + compute2.size());
        List<Double> xLine = compute.stream().map(Point2D::getX).collect(Collectors.toList());
        List<Double> xLine2 = compute2.stream().map(Point2D::getX).collect(Collectors.toList());
        List<Double> yLine = compute.stream().map(Point2D::getY).collect(Collectors.toList());
        List<Double> yLine2 = compute2.stream().map(Point2D::getY).collect(Collectors.toList());
        XYSeries lsf = chart.addSeries("LSF", xLine, yLine);
        XYSeries lsf2 = chart.addSeries("LSF2", xLine2, yLine2);
        lsf.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        lsf2.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        SwingWrapper<XYChart> swingWrapper = new SwingWrapper<>(chart);
        swingWrapper.displayChart();


    }

    /*private static List<Point2D> readPoints() {
        ObjectMapper om = new ObjectMapper();
        try {
            //List<JPoint> points = om.readValue(new File("/Users/ikrokhmalyov/IdeaProjects/weather/src/test/resources/lsf/outside.json"), new TypeReference<List<JPoint>>() {});
            List<LinearLSFTest.JPoint> points = om.readValue(new File("/Users/ikrokhmalyov/IdeaProjects/weather/src/test/resources/lsf/dataLog.txt"), new TypeReference<List<LinearLSFTest.JPoint>>() {});
            long minDate = points.stream().map(LinearLSFTest.JPoint::getX).mapToLong(Date::getTime).min().getAsLong();
            return points.stream().map(p -> new Point2D(Math.ceil((p.getX().getTime() - minDate)/1000), p.getY())).collect(toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/

    private static List<Point2D> readPoints() {
        //final String fileName = "history.log";
        final String fileName = "current.log";
        final TimeserialDao timeserialDao = new TimeserialDao(Path.of("/Users/ikrokhmalyov/IdeaProjects/weather", fileName), null);
        long[] first = new long[1];
        try {
            final Stream<SensorPoint> stream = timeserialDao.loadLastDay();
            return stream
                    .filter(sp -> sp.getSensorName().equals("outside"))
                    .map(sp -> {
                if (first[0] == 0) {
                    first[0] = sp.getTime().getEpochSecond();
                }
                return new Point2D(sp.getTime().toEpochMilli() - first[0], sp.getValue());
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    public static class JPoint {
        private Date x; private float y;
    }

    private static double[][] buildLine(LinearCoefficients calc, double xmin, double xmax) {
        return new double[][]{
                new double[]{xmin, xmax},
                new double[]{calc.getA() * xmin + calc.getB(), calc.getA() * xmax + calc.getB()}
        };
    }
}
