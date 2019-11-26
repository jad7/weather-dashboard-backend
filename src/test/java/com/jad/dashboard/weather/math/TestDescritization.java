package com.jad.dashboard.weather.math;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class TestDescritization {
    private static final String POINTS = "points";

    public static void main(String[] args) {
        List<Point2D> points = readPoints();
        final LSFDeDescritisation res = new LSFDeDescritisation(points.iterator(), 1d);

        final XYChart chart = new XYChartBuilder().width(1400).height(900).title("Area Chart").xAxisTitle("X").yAxisTitle("Y").build();
        List<Double> xData = points.stream().map(Point2D::getX).collect(Collectors.toList());
        List<Double> yData = points.stream().map(Point2D::getY).collect(Collectors.toList());

        XYSeries pointsSeries = chart.addSeries(POINTS, xData, yData);
        pointsSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);

        final List<Point2D> compute = res.compute();
        System.out.println("From: " + points.size() + " to:" + compute.size());
        List<Double> xLine = compute.stream().map(Point2D::getX).collect(Collectors.toList());
        List<Double> yLine = compute.stream().map(Point2D::getY).collect(Collectors.toList());
        XYSeries lsf = chart.addSeries("LSF", xLine, yLine);
        lsf.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        SwingWrapper<XYChart> swingWrapper = new SwingWrapper<>(chart);
        swingWrapper.displayChart();


    }

    private static List<Point2D> readPoints() {
        ObjectMapper om = new ObjectMapper();
        try {
            //List<JPoint> points = om.readValue(new File("/Users/ikrokhmalyov/IdeaProjects/weather/src/test/resources/lsf/outside.json"), new TypeReference<List<JPoint>>() {});
            List<LinearLSFTest.JPoint> points = om.readValue(new File("/Users/ikrokhmalyov/IdeaProjects/weather/src/test/resources/lsf/dataLog.txt"), new TypeReference<List<LinearLSFTest.JPoint>>() {});
            long minDate = points.stream().map(LinearLSFTest.JPoint::getX).mapToLong(Date::getTime).min().getAsLong();
            return points.stream().map(p -> new Point2D(Math.ceil((p.getX().getTime() - minDate)/1000), p.getY())).collect(toList());
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
