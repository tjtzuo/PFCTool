package com.pfc.tool;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;

public class ChemistryChart extends JFrame {

    /**
     *
     * @param title  the frame title.
     */
    public ChemistryChart(String title) {
        super(title);
    }
    
    public void createChartPanel(XYDataset dataset) {
        JFreeChart chart = createChart(dataset);
        ChartPanel panel = new ChartPanel(chart, false);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(new java.awt.Dimension(500, 600));
        setContentPane(panel);
    }

    /**
     * Creates a chart.
     *
     * @param dataset  a dataset.
     *
     * @return A chart.
     */
    private static JFreeChart createChart(XYDataset dataset) {

        JFreeChart chart = ChartFactory.createXYLineChart(
            "OCV",  // title
            "%",    // x-axis label
            "mV",   // y-axis label
            dataset);

        chart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        
        NumberAxis axisY = (NumberAxis)plot.getRangeAxis();
//        Range range = axisY.getRange();
//        axisY.setRange(dataset.getYValue(0, 0), range.getUpperBound());
        axisY.setLowerBound(dataset.getYValue(0, 0));
        System.out.println(axisY.getRange());

        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setDefaultShapesVisible(true);
            renderer.setDefaultShapesFilled(true);
            renderer.setDrawSeriesLineAsPath(true);
        }
        
        NumberFormat formatter = DecimalFormat.getInstance();
        formatter.setGroupingUsed(false);
        r.setDefaultToolTipGenerator(new StandardXYToolTipGenerator("({1}, {2})", formatter, formatter));
//                StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT, formatter, formatter));
//        axisY.setNumberFormatOverride(formatter);

        return chart;
    }
}
