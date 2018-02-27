package DStatComms;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.*;
/**
 * Created by DK on 2017-06-19.
 */
public class FileHandler {
    private Formatter fileW;
    private Scanner fileR;

    //Header information for plot files
    private final static String[] HEADER = new String[]{
            "Potentiostat Plot Data",
            "Series Count:",
            "",
            "data:"
    };
    //Header information for parameter files
    private final static String[] PARAM_HEADER = new String[]{
            "Potentiostat Parameters",
            "Parameter,Value",
    };

    //passed from main GUI
    GUI window = null;
    ResultsPlot dataPlot = null;
    Settings settings = null;

    FileHandler(GUI window, ResultsPlot dataPlot, Settings settings)
    {
        this.window = window;
        this.dataPlot = dataPlot;
        this.settings = settings;
    }

    //Saves plot file using file select dialog
    //If valid filename chosen plot data is saved to selected file path
    // using createFile(), plotToCSV(), and closeFileW() methods
    void savePlotFromDialog(){
        JFileChooser c = new JFileChooser();
        File f;
        String filepath;
        FileNameExtensionFilter ff = new FileNameExtensionFilter("Plot Files (*.csv)", new String[] { "csv" });
        c.setAcceptAllFileFilterUsed(false);
        c.addChoosableFileFilter(ff);

        // Open Save dialog:
        int rVal = c.showSaveDialog(window.menuBar);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            //selected file from dialog
            f = c.getSelectedFile();
            //path of file selected
            filepath = f.getAbsolutePath();
            //check if path does not contain extension, apply csv if ext blank
            if (!filepath.contains(".")) {
                filepath = filepath + ".csv";
                f = new File(filepath);
            }
            if (ff.accept(f)) {
                createFile(filepath);
                if(!plotToCSV(dataPlot.dataset))
                    window.dbgOut("Failed to save plot data");
                closeFileW();
                window.infoOut("Plot saved: " + filepath,GUI.P_USER);
            }
            else{
                window.infoOut("Plot not saved, invalid extension!",GUI.P_USER);
            }
        }
    }
    void loadPlotFromDialog(){
        JFileChooser c = new JFileChooser();
        File f;
        String filepath;
        FileNameExtensionFilter ff = new FileNameExtensionFilter("Plot Files (*.csv)", new String[] { "csv" });
        c.setAcceptAllFileFilterUsed(false);
        c.addChoosableFileFilter(ff);

        // Open Load dialog:
        int rVal = c.showOpenDialog(window.menuBar);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            //selected file from dialog
            f = c.getSelectedFile();
            //path of file selected
            filepath = f.getAbsolutePath();

            //check if file is acceptable (ie has .csv ext)
            if (ff.accept(f)) {
                openFileR(filepath);
                if(readFile())
                    window.infoOut("Plot loaded: " + filepath,GUI.P_USER);
                closeFileR();

            }
            else{
                window.infoOut("Could not load plot, invalid extension!",GUI.P_USER);
            }
        }
    }
    void saveParamsFromDialog(){
        JFileChooser c = new JFileChooser();
        File f;
        String filepath;
        FileNameExtensionFilter ff = new FileNameExtensionFilter("Parameter Files (*.param)", "param");
        c.setAcceptAllFileFilterUsed(false);
        c.addChoosableFileFilter(ff);

        // Open Save dialog:
        int rVal = c.showSaveDialog(window.menuBar);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            //selected file from dialog
            f = c.getSelectedFile();
            //path of file selected
            filepath = f.getAbsolutePath();
            //check if path does not contain extension, apply csv if ext blank
            if (!filepath.contains(".")) {
                filepath = filepath + ".param";
                f = new File(filepath);
            }
            if (ff.accept(f)) {
                createFile(filepath);
                writeParams();
                closeFileW();
                window.infoOut("Parameters saved: " + filepath,GUI.P_USER);
            }
            else{
                window.infoOut("Parameters not saved, invalid extension!",GUI.P_USER);
            }
        }
    }
    void loadParamsFromDialog(){
        JFileChooser c = new JFileChooser();
        File f;
        String filepath;
        FileNameExtensionFilter ff = new FileNameExtensionFilter("Parameter Files (*.param)", "param");
        c.setAcceptAllFileFilterUsed(false);
        c.addChoosableFileFilter(ff);

        // Open Load dialog:
        int rVal = c.showOpenDialog(window.menuBar);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            //selected file from dialog
            f = c.getSelectedFile();
            //path of file selected
            filepath = f.getAbsolutePath();

            //check if file is acceptable (ie has .csv ext)
            if (ff.accept(f)) {
                openFileR(filepath);
                if(readParamFile())
                    window.infoOut("Parameters loaded: " + filepath,GUI.P_USER);
                closeFileR();

            }
            else{
                window.infoOut("Could not load parameters, invalid extension!",GUI.P_USER);
            }
        }
    }
    private javax.swing.filechooser.FileFilter newFileFilter(final String desc, final String[] allowed_extensions) {
        return new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                int pos = f.getName().lastIndexOf('.');
                if (pos == -1) {
                    return false;
                } else {
                    String extension = f.getName().substring(pos + 1);
                    for (String allowed_extension : allowed_extensions) {
                        if (extension.equalsIgnoreCase(allowed_extension)) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return desc;
            }
        };
    }

    void createFile(String filename){
        try {
            fileW = new Formatter(filename);
            window.infoOut("File created successfully: " + filename,GUI.P_DBG);
        }
        catch (Exception e){
            window.dbgOut("Cannot create file, exception: " + e.toString());
        }
    }

    boolean plotToCSV(XYSeriesCollection ds){
        int seriesCount = ds.getSeriesCount();
        List<List<Double>> datalist = new ArrayList<>();

        int maxSeriesLength = 0;
        window.infoOut("Series count: " + seriesCount,GUI.P_DBG);

        if (seriesCount == 0) { //no data in data set
            window.dbgOut("Cannot convert to CSV: No data to convert");
            return false;
        }

        //Write file header, and series count
        fileW.format("%s\n",HEADER[0]);
        fileW.format("%s\n%d\n",HEADER[1],seriesCount);
        fileW.format("%s\n",HEADER[3]);

        //convert collection into list and get maximum series length
        for (int i = 0; i < seriesCount; i++) {
            datalist.add(ds.getSeries(i).getItems());
            if (ds.getSeries(i).getItemCount() > maxSeriesLength)
                maxSeriesLength = ds.getSeries(i).getItemCount();

        }

        for (int r = 0; r < maxSeriesLength; r++) { //iterate through each row

            for (List series : datalist) { //iterate through each series (pair of columns)

                if (series.size() > r){
                    System.out.printf("%.6e,%.6e,", (double) ((XYDataItem) series.get(r)).getXValue(), (double) ((XYDataItem) series.get(r)).getYValue());
                    fileW.format("%.6e,%.6e,",(double)((XYDataItem) series.get(r)).getXValue(),(double)((XYDataItem) series.get(r)).getYValue());
                }else{
                    System.out.print("   ,   ,");
                    fileW.format("   ,   ,");
                }
            }
            System.out.println();
            fileW.format("\n");
        }

        return true;
    }

    //Writes parameters to open file
    //File to write must be open before calling
    //Applies header information and writes params HashMap to file
    //order of writing undefined
    private void writeParams(){
        //Write file header
        fileW.format("%s\n", PARAM_HEADER[0]);
        fileW.format("%s\n", PARAM_HEADER[1]);

        //write key for each setting followed by ,, then value for each setting
        //order not essential/defined
        settings.params.forEach((k,v)-> fileW.format("%s,,%s\n",k,v));

    }

    //Opens file to read of supplied filepath
    private void openFileR(String path){
        try {
            fileR = new Scanner(new File(path));
        }
        catch(Exception e){
            window.dbgOut("Cannot open file, exception: " + e.toString());
        }

    }
    //Reads open plot file, plot data stored in dataPlot series using
    // dataPlot.addPoint (class:ResultsPlot) method
    //File to read must be opened before calling
    //Returns valid if file header information valid
    private boolean readFile(){
        boolean valid = true;
        int numOfSeries = 0;
        String header[] = new String[4];

        for (int i = 0; i <= 3; i++) {
            header[i] = fileR.nextLine();
            //Check headers for correct data
            if (i != 2) { //Headers should contain matching text
                if (!header[i].equals(HEADER[i])) {
                    valid = false;
                    window.dbgOut("Invalid file header, cannot open file");
                    break; //stop for loop execution
                }
            } else if (i == 2){ //Header should contain number of series
                try {
                    numOfSeries = Integer.parseInt(header[i]);
                    if (!(numOfSeries > 0)){
                        valid = false;
                        window.dbgOut("Invalid number of series, cannot open file");
                        break; //stop for loop execution
                    }
                }catch (Exception e){
                    window.dbgOut("Exception: " + e.toString());
                }
            }
        }
        //Header verified, read data
        if (valid){
            dataPlot.clearSeries();
            String nextLine;
            while (fileR.hasNext()){ // loop over data
                nextLine = fileR.nextLine();
                List<String> strList = new ArrayList<String>(Arrays.asList(nextLine.split(",")));
                // loop over each series
                String xStr, yStr = "";
                for (int i = 0; i < numOfSeries; i++) {
                    xStr = strList.get(2*i);
                    yStr = strList.get(2*i + 1);
                    if(!isNotNum(xStr)&& !isNotNum(yStr))
                        try {
                            dataPlot.addPoint(i,Double.parseDouble(xStr),Double.parseDouble(yStr));
                        }
                        catch(Exception e){}
                }
            }

        }

        return valid;
    }
    //Reads open file, parameters read are stored in settings
    //File to read must be opened before calling
    //Returns valid if file header information valid
    private boolean readParamFile(){
        boolean valid = true;
        String header[] = new String[2];

        for (int i = 0; i <= 1; i++) {
            header[i] = fileR.nextLine();
            //Check headers for correct data
                if (!header[i].equals(PARAM_HEADER[i])) {
                    valid = false;
                    window.dbgOut("Invalid file header, cannot open file");
                    break; //stop for loop execution
                }

        }
        //Header verified, read data
        if (valid){
            dataPlot.clearSeries();
            String nextLine;
            while (fileR.hasNext()){ // loop over data
                nextLine = fileR.nextLine();
                List<String> strList = new ArrayList<String>(Arrays.asList(nextLine.split(",,")));
                settings.params.put(strList.get(0),strList.get(1));
            }

        }

        return valid;
    }
    //Checks if string contains a space or is empty
    //Will return a string is not a number if it has space padding eg. "  99"
    private boolean isNotNum(String s){
        return (s.isEmpty()||s.contains(" "));
    }

    //Closes open read file
    private void closeFileR(){
        fileR.close();

        window.infoOut("File closed",GUI.P_DBG);
    }
    //Closes open write file
    public void closeFileW(){
        fileW.close();

        window.infoOut("File closed",GUI.P_DBG);
    }

}
