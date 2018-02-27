package DStatComms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


//XY chart:
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class GUI {
    public JButton btnConnect;
    public JComboBox cboxPorts;
    public JButton btnDisconnect;
    public JTextArea txtRawData;
    private JPanel pnlMain;
    private JPanel pnlSouth;
    private JLabel labelSrlPrt;
    private JPanel pnlWest;
    private JPanel pnlADCSettings;
    private JPanel pnlExperimentCards;
    private JPanel pnlPotSettings;
    private JComboBox cboxGain;
    private JLabel lblGain;
    private JLabel lblExperiment;
    private JPanel pnlEast;
    private JTabbedPane tabbedPane1;
    private JPanel tabpnlPlot;
    private JLabel lblPlot;
    private JPanel tabpnlRawData;
    private JLabel lblPGASet;
    private JLabel lbl2ElectrodeMode;
    private JLabel lblInputBuffer;
    private JLabel lblSampRate;
    private JComboBox cboxPGASet;
    private JButton btnRefresh;
    private JComboBox cboxSampRate;
    private JCheckBox chkboxInputBuffer;
    private JCheckBox chkbox2ElectrodeMode;
    private JPanel pnlExpSelect;
    private JComboBox cboxExperimentSelect;
    private JPanel pnlCardExpLSV;
    public JTextArea txtLog;
    private JPanel pnlLSV1;
    private JTextField fieldLSVCleaningPot;
    private JTextField fieldLSVDepPot;
    private JTextField fieldLSVCleaningTime;
    private JTextField fieldLSVDepTime;
    private JPanel pnlLSV2;
    private JScrollPane scrlPane1;
    private JTextField fieldLSVStop;
    private JTextField fieldLSVStart;
    private JTextField fieldLSVSlope;
    private JPanel pnlManualCmd;
    private JPanel pnlCommands;
    private JButton btnExecute;
    private JButton btnStop;
    private JLabel lblManualCmd;
    private JTextField fieldManualCmd;
    private JPanel pnlCardExpDPV;
    private JPanel pnlCardExpCA;
    private JLabel CA;
    private JPanel pnlCardExpCV;
    private JPanel pnlCardExpSWV;
    private JLabel SWV;
    private JPanel pnlCardExpACV;
    private JLabel ACV;
    private JLabel PD;
    private JPanel pnlCardExpPD;
    private JPanel pnlCardExpPot;
    private JPanel pnlCV1;
    private JPanel pnlCV2;
    private JTextField fieldCVCleaningPot;
    private JTextField fieldCVDepPot;
    private JTextField fieldCVDepTime;
    private JTextField fieldCVCleaningTime;
    private JTextField fieldCVStart;
    private JTextField fieldCVvertex1;
    private JTextField fieldCVvertex2;
    private JTextField fieldCVSlope;
    private JTextField fieldCVScans;
    private JPanel pnlCardExpOffCal;
    private JScrollPane tabpnlLog;
    private JTextField txtStatus;
    private JTextField fieldDPVCleaningPot;
    private JTextField fieldDPVDepPot;
    private JTextField fieldDPVCleaningTime;
    private JTextField fieldDPVDepTime;
    private JTextField fieldDPVStart;
    private JTextField fieldDPVStop;
    private JTextField fieldDPVStepSize;
    private JTextField fieldDPVPHeight;
    private JTextField fieldDPVPWidth;
    private JTextField fieldDPVPPeriod;

    JMenuBar menuBar;
    JCheckBoxMenuItem cbRetainResults;

    //Communicator object
    Communicator communicator = null;

    //Settings object
    Settings settings = null;

    //ResultsPlot object
    ResultsPlot dataPlot = null;

    //FileHandler object
    FileHandler fileHandle = null;

    //SwingWorkers
    ExecuteWorker executeWrk = null;

    //Determines priority level of messages to be displayed, 0 = display all
    private final static int MIN_DISPLAY_PRIORITY = 0;
    final static int P_DBG = 0;
    final static int P_LOW = 1;
    final static int P_MED = 2;
    final static int P_HIGH = 3;
    final static int P_USER = 4;

    /** Creates new form GUI */

    public GUI() {

        createObjects();
        dataPlot.createChart();
        tabpnlPlot.add(dataPlot.cp,BorderLayout.CENTER);
        communicator.searchForPorts();
        setParamsFromFields();
        toggleControls();


        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean successful = false;
                String s = (String)cboxPorts.getSelectedItem();
                //infoOut(Integer.toString(s.length()),P_USER);
                //Check port is selected
                if (s != null) {
                    if (communicator.connect()) {
                        if (communicator.initIOStream()) {
                             communicator.waitTime(100);
                             communicator.clrReadData();
                             if (communicator.sendInit()) {
                                //for controlling GUI elements
                                communicator.setConnected(true);
                                toggleControls();

                                infoOut("Connected to board successfully", P_USER);
                                //TODO check version
                                communicator.sendString("V");
                                //TODO start OCP
                                successful = true;
                             }
                        }
                    }
                    if (!successful) {
                        dbgOut("Could not connect to board; command not acknowledged");
                        communicator.disconnect();
                    }

                } else {
                    dbgOut("Could not connect, no port selected");
                }

            }
        });

        //----Disconnect button pressed
        btnDisconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                communicator.disconnect();

            }
        });
/*
        Detect Experiment select ComboBox item and display correct card
        Cards:
        1   Chronoamperometry                   pnlCardExpCA
        2   Linear Sweep Voltammetry            pnlCardExpLSV
        3   Cyclic Voltammetry                  pnlCardExpCV
        4   Square Wave Voltammetry             pnlCardExpSWV
        5   Differential Pulse Voltammetry      pnlCardExpDPV
        6   AC Voltammetry                      pnlCardExpACV
        7   Photodiode                          pnlCardExpPD
        8   Potentiometry                       pnlCardExpPot
        9   Offset Calibration                  pnlCardExpOffsetCal
*/

        cboxExperimentSelect.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                CardLayout cl = (CardLayout)(pnlExperimentCards.getLayout());

                if (e.getStateChange() == 1){
                    cl.show(pnlExperimentCards, (String)e.getItem());
                }

            }
        });

        //----Execute button action performed
        btnExecute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String manualCmd = fieldManualCmd.getText();

                    //clear status bar
                    txtStatus.setText("");

                    if (fieldManualCmd.isEnabled() && manualCmd.length() > 0) {
                        infoOut("Sending manual command: " + manualCmd, P_MED);
                        communicator.sendString(manualCmd);
                        fieldManualCmd.setText("");
                    } else if (!executeWrk.getState().equals(SwingWorker.StateValue.STARTED)){
                        (executeWrk = new ExecuteWorker(communicator,fileHandle,dataPlot,true)).execute();

                    }
                } catch (Exception exc){
                    dbgOut("Exception occurred: (" + exc.toString() + ")");
                    exc.printStackTrace();
                }
            }
        });

        //----Stop button action performed
        btnStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                communicator.setAborted(true);
                communicator.sendString("a");

            }
        });

        //----Refresh button action performed
        btnRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                communicator.searchForPorts();
            }
        });


    }

    private void createObjects() {
        settings = new Settings(this);
        dataPlot = new ResultsPlot(this);
        fileHandle = new FileHandler(this, dataPlot, settings);
        communicator = new Communicator(this, settings, dataPlot);
        executeWrk = new ExecuteWorker(communicator,fileHandle,dataPlot,false);
    }

    private void createChart(XYSeries series){

        XYSeriesCollection dataset = null;// = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(" ", "Voltage (mV)", "Current (A)", dataset);
        tabpnlPlot.add(new ChartPanel(chart),BorderLayout.CENTER);
        chart.removeLegend();

    }

    public void dbgOut (String output){
        System.out.println("ERR: " + output);
        txtLog.append("ERR: " + output + "\n");
        txtLog.setForeground(Color.red);

        txtStatus.setText(output);
        txtStatus.setForeground(Color.red);
    }
    //Display information test based on priority, higher value = higher priority
    public void infoOut (String output, int priority){
        if (priority >= MIN_DISPLAY_PRIORITY) {
            System.out.println(output);
            txtLog.append(output + "\n");
            txtLog.setForeground(Color.black);
            if (priority == P_USER) {
                txtStatus.setForeground(Color.black);
                txtStatus.setText(output);
            }
        }
    }

    //Toggle enable/disable of controls based on connected/disconnected status
    public void toggleControls()
    {
        if (communicator.getConnected() == true)
        {
            //enable when connected / disable when disconnected:
            btnDisconnect.setEnabled(true);
            btnExecute.setEnabled(true);
            btnStop.setEnabled(true);

            //disable when connected / enable when disconnected:
            btnConnect.setEnabled(false);
            btnRefresh.setEnabled(false);
            cboxPorts.setEnabled(false);
        }
        else
        {
            //enable when connected / disable when disconnected:
            btnDisconnect.setEnabled(false);
            btnExecute.setEnabled(false);
            btnStop.setEnabled(false);

            //disable when connected / enable when disconnected:
            btnConnect.setEnabled(true);
            btnRefresh.setEnabled(true);
            cboxPorts.setEnabled(true);

        }
    }

    public void setParamsFromFields (){

        settings.params.put("PGA Setting",(String)cboxPGASet.getSelectedItem());
        settings.params.put("Sample Rate",(String)cboxSampRate.getSelectedItem());
        settings.params.put("Input Buffer",(chkboxInputBuffer.isSelected()) ? "True":"False");
        settings.params.put("2 Electrode",(chkbox2ElectrodeMode.isSelected()) ? "True":"False");
        settings.params.put("Gain",(String)cboxGain.getSelectedItem());
        settings.params.put("Experiment",(String)cboxExperimentSelect.getSelectedItem());

        settings.params.put(Settings.CVPARAM[0],fieldCVCleaningPot.getText());
        settings.params.put(Settings.CVPARAM[1],fieldCVCleaningTime.getText());
        settings.params.put(Settings.CVPARAM[2],fieldCVDepPot.getText());
        settings.params.put(Settings.CVPARAM[3],fieldCVDepTime.getText());
        settings.params.put(Settings.CVPARAM[4],fieldCVvertex1.getText());
        settings.params.put(Settings.CVPARAM[5],fieldCVvertex2.getText());
        settings.params.put(Settings.CVPARAM[6],fieldCVStart.getText());
        settings.params.put(Settings.CVPARAM[7],fieldCVScans.getText());
        settings.params.put(Settings.CVPARAM[8],fieldCVSlope.getText());

        settings.params.put(Settings.LSVPARAM[0],fieldLSVCleaningPot.getText());
        settings.params.put(Settings.LSVPARAM[1],fieldLSVCleaningTime.getText());
        settings.params.put(Settings.LSVPARAM[2],fieldLSVDepPot.getText());
        settings.params.put(Settings.LSVPARAM[3],fieldLSVDepTime.getText());
        settings.params.put(Settings.LSVPARAM[4],fieldLSVStart.getText());
        settings.params.put(Settings.LSVPARAM[5],fieldLSVStop.getText());
        settings.params.put(Settings.LSVPARAM[6],fieldLSVSlope.getText());

        settings.params.put(Settings.DPVPARAM[0],fieldDPVCleaningPot.getText());
        settings.params.put(Settings.DPVPARAM[1],fieldDPVCleaningTime.getText());
        settings.params.put(Settings.DPVPARAM[2],fieldDPVDepPot.getText());
        settings.params.put(Settings.DPVPARAM[3],fieldDPVDepTime.getText());
        settings.params.put(Settings.DPVPARAM[4],fieldDPVStart.getText());
        settings.params.put(Settings.DPVPARAM[5],fieldDPVStop.getText());
        settings.params.put(Settings.DPVPARAM[6],fieldDPVStepSize.getText());
        settings.params.put(Settings.DPVPARAM[7],fieldDPVPHeight.getText());
        settings.params.put(Settings.DPVPARAM[8],fieldDPVPPeriod.getText());
        settings.params.put(Settings.DPVPARAM[9],fieldDPVPWidth.getText());
    }

    public void setFieldsFromParams (){

        cboxPGASet.setSelectedItem(settings.params.get("PGA Setting"));
        cboxSampRate.setSelectedItem(settings.params.get("Sample Rate"));
        chkboxInputBuffer.setSelected(settings.params.get("Input Buffer").equals("True"));
        chkbox2ElectrodeMode.setSelected(settings.params.get("2 Electrode").equals("True"));
        cboxGain.setSelectedItem(settings.params.get("Gain"));
        cboxExperimentSelect.setSelectedItem(settings.params.get("Experiment"));

        
        fieldCVCleaningPot.setText(settings.params.get(Settings.CVPARAM[0]));
        fieldCVCleaningTime.setText(settings.params.get(Settings.CVPARAM[1]));
        fieldCVDepPot.setText(settings.params.get(Settings.CVPARAM[2]));
        fieldCVDepTime.setText(settings.params.get(Settings.CVPARAM[3]));
        fieldCVvertex1.setText(settings.params.get(Settings.CVPARAM[4]));
        fieldCVvertex2.setText(settings.params.get(Settings.CVPARAM[5]));
        fieldCVStart.setText(settings.params.get(Settings.CVPARAM[6]));
        fieldCVScans.setText(settings.params.get(Settings.CVPARAM[7]));
        fieldCVSlope.setText(settings.params.get(Settings.CVPARAM[8]));

        fieldLSVCleaningPot.setText(settings.params.get(Settings.LSVPARAM[0]));
        fieldLSVCleaningTime.setText(settings.params.get(Settings.LSVPARAM[1]));
        fieldLSVDepPot.setText(settings.params.get(Settings.LSVPARAM[2]));
        fieldLSVDepTime.setText(settings.params.get(Settings.LSVPARAM[3]));
        fieldLSVStart.setText(settings.params.get(Settings.LSVPARAM[4]));
        fieldLSVStop.setText(settings.params.get(Settings.LSVPARAM[5]));
        fieldLSVSlope.setText(settings.params.get(Settings.LSVPARAM[6]));

        fieldDPVCleaningPot.setText(settings.params.get(Settings.DPVPARAM[0]));
        fieldDPVCleaningTime.setText(settings.params.get(Settings.DPVPARAM[1]));
        fieldDPVDepPot.setText(settings.params.get(Settings.DPVPARAM[2]));
        fieldDPVDepTime.setText(settings.params.get(Settings.DPVPARAM[3]));
        fieldDPVStart.setText(settings.params.get(Settings.DPVPARAM[4]));
        fieldDPVStop.setText(settings.params.get(Settings.DPVPARAM[5]));
        fieldDPVStepSize.setText(settings.params.get(Settings.DPVPARAM[6]));
        fieldDPVPHeight.setText(settings.params.get(Settings.DPVPARAM[7]));
        fieldDPVPPeriod.setText(settings.params.get(Settings.DPVPARAM[8]));
        fieldDPVPWidth.setText(settings.params.get(Settings.DPVPARAM[9]));


    }


    public static void main(String args[]) {

        JFrame frame = new JFrame("GUI");
        frame.setMinimumSize(new Dimension(800, 600));
        GUI window = new GUI();
        frame.setContentPane(window.pnlMain);

        window.menuBar = window.createMenuBar();
        frame.setJMenuBar(window.menuBar);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);
    }

    private JMenuBar createMenuBar(){
        //Where the GUI is created:
        JMenuBar menuBar;
        JMenu menu, submenu;
        JMenuItem menuItem;
        JRadioButtonMenuItem rbMenuItem;
        JCheckBoxMenuItem cbMenuItem;

        //Create the menu bar.
        menuBar = new JMenuBar();

        //Build the File menu.
        menu = new JMenu("File");

        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);

        //JMenuItems

        //Save Parameters
        menuItem = new JMenuItem("Save Parameters");
        menuItem.setMnemonic(KeyEvent.VK_P);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setParamsFromFields();
                fileHandle.saveParamsFromDialog();
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Save Plots");
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileHandle.savePlotFromDialog();
            }
        });
        menu.add(menuItem);

        menu.addSeparator();

        //Load Parameters
        menuItem = new JMenuItem("Load Parameters");
        menuItem.setMnemonic(KeyEvent.VK_A);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileHandle.loadParamsFromDialog();
                setFieldsFromParams();
            }
        });
        menu.add(menuItem);

        //Load Plots
        menuItem = new JMenuItem("Load Plots");
        menuItem.setMnemonic(KeyEvent.VK_L);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_L, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileHandle.loadPlotFromDialog();
            }
        });
        menu.add(menuItem);

        menu.addSeparator();

        //Exit
        menuItem = new JMenuItem("Exit");
        menuItem.setMnemonic(KeyEvent.VK_X);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                communicator.disconnect();
                System.exit(0);
            }
        });
        menu.add(menuItem);

        //Build the Experiment menu.
        menu = new JMenu("Experiment");
        menu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(menu);

        cbRetainResults = new JCheckBoxMenuItem("Retain previous plot data");
        menu.add(cbRetainResults);

        //Get String
        menuItem = new JMenuItem("Get Command String");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                communicator.getString();
            }
        });
        menu.add(menuItem);



        //Build the help menu
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(menu);

        menuItem = new JMenuItem("About");
        menuItem.setMnemonic(KeyEvent.VK_A);
        menu.add(menuItem);

        return menuBar;
    }

}
