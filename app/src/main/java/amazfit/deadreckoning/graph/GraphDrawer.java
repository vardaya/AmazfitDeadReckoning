package amazfit.deadreckoning.graph;

import android.content.Context;
import android.graphics.Color;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;

public class GraphDrawer {

    private String seriesName;
    private ArrayList<Double> xList;
    private ArrayList<Double> yList;

    public GraphDrawer(String seriesName) {
        this.seriesName = seriesName;
        xList = new ArrayList<>();
        yList = new ArrayList<>();
    }

    public GraphicalView getGraphView(Context context) {

        XYSeries mySeries;
        XYSeriesRenderer seriesRenderer;
        XYMultipleSeriesDataset seriesDataset;
        XYMultipleSeriesRenderer multipleSeriesRenderer;

        double[] xSet = new double[xList.size()];
        for (int i = 0; i < xList.size(); i++)
            xSet[i] = xList.get(i);

        double[] ySet = new double[yList.size()];
        for (int i = 0; i < yList.size(); i++)
            ySet[i] = yList.get(i);

        mySeries = new XYSeries(seriesName);
        for (int i = 0; i < xSet.length; i++)
            mySeries.add(xSet[i], ySet[i]);

        seriesRenderer = new XYSeriesRenderer();
        seriesRenderer.setFillPoints(true);
        seriesRenderer.setPointStyle(PointStyle.CIRCLE);
        seriesRenderer.setColor(Color.parseColor("#99009911"));

        seriesDataset = new XYMultipleSeriesDataset();
        seriesDataset.addSeries(mySeries);

        multipleSeriesRenderer = new XYMultipleSeriesRenderer();
        multipleSeriesRenderer.addSeriesRenderer(seriesRenderer);

        multipleSeriesRenderer.setPointSize(10); //size of scatter plot points
        multipleSeriesRenderer.setShowLegend(false); //hide legend

        int[] chartMargins = {100, 100, 25, 100};
        multipleSeriesRenderer.setMargins(chartMargins);
        multipleSeriesRenderer.setYLabelsPadding(50);
        multipleSeriesRenderer.setXLabelsPadding(10);

        multipleSeriesRenderer.setChartTitle("Position");
        multipleSeriesRenderer.setChartTitleTextSize(75);
        multipleSeriesRenderer.setLabelsTextSize(40);

        double bound = getMaxBound();
        multipleSeriesRenderer.setXAxisMin(-bound);
        multipleSeriesRenderer.setXAxisMax(bound);
        multipleSeriesRenderer.setYAxisMin(-bound);
        multipleSeriesRenderer.setYAxisMax(bound);

        return ChartFactory.getScatterChartView(context, seriesDataset, multipleSeriesRenderer);
    }

    public void addPoint(double x, double y) {
        xList.add(x);
        yList.add(y);
    }

    public float getLastXPoint() {
        double x = xList.get(xList.size() - 1);
        return (float)x;
    }

    public float getLastYPoint() {
        double y = yList.get(yList.size() - 1);
        return (float)y;
    }

    private double getMaxBound() {
        double max = 0;
        for (double num : xList)
            if (max < Math.abs(num))
                max = num;
        for (double num : yList)
            if (max < Math.abs(num))
                max = num;
        return (Math.abs(max) / 100) * 100 + 100;
    }
}
