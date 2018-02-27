package DStatComms;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.ScatterRenderer;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.fazecast.jSerialComm.SerialPort;


public class ResultsPlot {

    //passed from main GUI
    GUI window = null;

    private List<XYSeries> dataSeriesList = new ArrayList<>();
    public JFreeChart chart;
    public ChartPanel cp;

    private XYSeries defaultSeries = new XYSeries("empty");

    public XYSeriesCollection dataset;

    public ResultsPlot(GUI window){
        this.window = window;
    }

    public void createChart(){
        dataset = new XYSeriesCollection(defaultSeries);//changed from default

        chart = ChartFactory.createXYLineChart(" ", "Voltage (mV)", "Current (A)", dataset);
        cp = new ChartPanel(chart);

        //create custom shape
        final Shape[] shape = new Shape[1];
        shape[0] = new Ellipse2D.Double(-0.5, -0.5, 1.0, 1.0);
        //create drawing supplier with custom shape
        final DrawingSupplier supplier = new DefaultDrawingSupplier(
                DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                shape
        );

        //Set the drawing supplier
        final XYPlot plot = chart.getXYPlot();
        plot.setDrawingSupplier(supplier);

        //modify renderer, enable shapes, disable lines
        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseShapesVisible(true);
        renderer.setBaseLinesVisible(false);



        // customise the range axis...
       /* final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setUpperMargin(0.12);*/

        clearSeries();

    }

    public void clearSeries(){

        dataset.removeAllSeries();
        dataSeriesList.clear();

    }

    public int getSeriesCount(){
        return dataset.getSeriesCount();
    }



    //todo add more checks for correct series #
    public void addPoint(int series, double x, double y){
        try {

            if (dataset.getSeriesCount() == 0) { //New experiment / add first series
                dataSeriesList.clear();
                dataSeriesList.add(new XYSeries("Scan: " + (series + 1)));
                dataset.addSeries(dataSeriesList.get(series));


            } else if (dataset.getSeriesCount() <= series) {
                dataSeriesList.add(new XYSeries("Scan: " + (series + 1)));
                dataset.addSeries(dataSeriesList.get(series));

            }

            dataSeriesList.get(series).add(x, y);
            cp.repaint();

        } catch (Exception e){
            window.dbgOut("Exception occurred: (" + e.toString() + ")");
        }

    }

    public void setDomainAxis(Range range, boolean checkCurrent){
        final XYPlot plot = chart.getXYPlot();
        //Customize X-axis (domain axis)
        final NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        //Check if current range (upper/lower) is (greater/less) then new range, if so use previous (upper or lower)
        if (checkCurrent&&(xAxis.getRange().getLowerBound() < range.getLowerBound()))
            range = new Range(xAxis.getRange().getLowerBound(),range.getUpperBound());
        if (checkCurrent&&(xAxis.getRange().getUpperBound() > range.getUpperBound()))
            range = new Range(range.getLowerBound(),xAxis.getRange().getUpperBound());
        xAxis.setRange(range);
    }

    public boolean currDatasetToArray(XYSeriesCollection ds){
        int seriesCount = ds.getSeriesCount();
        List<List<Double>> datalist = new ArrayList<>();
        XYDataset dat;
        int maxSeriesLength = 0;
        window.infoOut("Series count: " + seriesCount,GUI.P_DBG);

        if (seriesCount == 0) { //no data in data set
            window.dbgOut("No data to convert");
            return false;
        }



        for (int i = 0; i < seriesCount; i++) {
            double seriesArr[][] = ds.getSeries(i).toArray();
            //data[i][] = seriesArr[0][];
            datalist.add(ds.getSeries(i).getItems());
            if (ds.getSeries(i).getItemCount() > maxSeriesLength)
                maxSeriesLength = ds.getSeries(i).getItemCount();

        }

        for (int r = 0; r < maxSeriesLength; r++) {

            for (List series: datalist) {

                if (series.size() > r)
                    System.out.printf("%.6e,%.6e,",(double)((XYDataItem) series.get(r)).getXValue(),(double)((XYDataItem) series.get(r)).getYValue());
                else
                    System.out.print("   ,   ,");
            }
            System.out.println();
        }

        return true;
    }
}
