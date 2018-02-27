package DStatComms;

import javax.swing.*;

/**
 * Created by DK on 2017-06-17.
 */
public class ExecuteWorker extends SwingWorker<Boolean, Integer>
{
    //Communicator object
    Communicator communicator = null;

    //File Handler
    FileHandler fileHandle = null;

    ResultsPlot dataPlot = null;

    boolean autoSaveEnabled;

    public ExecuteWorker(Communicator communicator, FileHandler fileHandle, ResultsPlot dataPlot, boolean autoSaveEnabled)
    {
        //this.window = window;
        this.communicator = communicator;
        this.fileHandle = fileHandle;
        this.dataPlot = dataPlot;
        this.autoSaveEnabled = autoSaveEnabled;
    }

    public Boolean doInBackground() throws Exception
    {
        communicator.setAborted(false);
        if(communicator.runExperiment() && autoSaveEnabled) {
            fileHandle.createFile("auto.csv");
            if(!fileHandle.plotToCSV(dataPlot.dataset))
                System.out.println("Auto-save failed to write plot data");
            fileHandle.closeFileW();
        }
        return true;
    }

    @Override
    public void done()
    {
        try
        {
            System.out.println("Execute worker done");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

