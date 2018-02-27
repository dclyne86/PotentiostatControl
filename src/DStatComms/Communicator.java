package DStatComms;

import gnu.io.*;


import java.awt.*;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.List;


import static DStatComms.GUI.*;

// TODO: 2017-09-15 DPV experiment
// TODO: 2017-09-15 finalize valid parameters
// TODO: 2017-09-15 Experiment analysis
// TODO: 2017-09-15 Add experiment graphics
// TODO: 2017-09-20 Add gain trim 

public class Communicator
{
    //passed from main GUI
    GUI window = null;

    //ResultsPlot object
    ResultsPlot dataPlot = null;

    //Settings object
    Settings settings = null;

    //for containing the ports that will be found
    private Enumeration ports = null;

    //map the port names to CommPortIdentifiers
    private HashMap portMap = new HashMap();

    //this is the object that contains the opened port
    private CommPortIdentifier selectedPortIdentifier = null;
    private SerialPort serialPort = null;

    //input and output streams for sending and receiving data
    private BufferedInputStream input = null;
    private OutputStream output = null;

    //boolean flag indicating if serial connection has been made
    private boolean bConnected = false;

    private boolean bAborted = false;

    //Read buffer array
    private byte[] readBuffer = new byte[1024];

    List<String> readLines = new ArrayList<>();
    List<byte[]> readBytes = new ArrayList<>();
    List<Byte> byteList = new ArrayList<>();
    List<Character[]> readChars = new ArrayList<>();


    private String readString = "";
    private String message = "";

    //flag for communication status
    private int HSstatus = 0;

    final static int EXP_START = 33;
    final static int EXP_INPROC = 23;
    final static int EXP_DONE = 3;
    final static int EXP_CMD_SENT = 32;
    final static int EXP_CMD_INPROC = 22;
    final static int EXP_CMD_RECD = 2;
    final static int INIT_CMD_SENT = 31;
    final static int INIT_CMD_INPROC = 21;
    final static int INIT_CMD_RECD = 1;
    final static int NONE = 0;

    //store available bytes for use by other methods:
    private int availableBytes;

    //the timeout value for connecting with the port
    final static int TIMEOUT = 2000;

    //some ascii values for for certain things
    final static int SPACE_ASCII = 32;
    final static int C_ASCII = 67;
    final static int B_ASCII = 66;
    final static int NL_ASCII = 10;
    final static int CR_ASCII = 13;

    //a string for recording what goes on in the program
    //this string is written to the GUI
    String logText = "";

    public Communicator(GUI window, Settings settings, ResultsPlot dataPlot)
    {
        this.window = window;
        this.settings = settings;
        this.dataPlot = dataPlot;
    }

    //search for all the serial ports
    //pre: none
    //post: adds all the found ports to a combo box on the GUI
    public void searchForPorts()
    {
        ports = CommPortIdentifier.getPortIdentifiers();
        window.cboxPorts.removeAllItems();
        while (ports.hasMoreElements())
        {
            CommPortIdentifier curPort = (CommPortIdentifier)ports.nextElement();

            //get only serial ports
            if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL)
            {
                window.cboxPorts.addItem(curPort.getName());
                portMap.put(curPort.getName(), curPort);
            }
        }
    }

    //connect to the selected port in the combo box
    //pre: ports are already found by using the searchForPorts method
    //post: the connected comm port is stored in commPort, otherwise,
    //an exception is generated
    public boolean connect()
    {
        boolean successful = false;
        int baudRate = 19200;
        String selectedPort = (String)window.cboxPorts.getSelectedItem();
        selectedPortIdentifier = (CommPortIdentifier)portMap.get(selectedPort);

        CommPort commPort = null;

        try
        {
            //the method below returns an object of type CommPort
            commPort = selectedPortIdentifier.open("Communicator", TIMEOUT);
            //the CommPort object can be casted to a SerialPort object
            serialPort = (SerialPort)commPort;

            window.infoOut("Connected to port: " + commPort, GUI.P_LOW);

        serialPort.setSerialPortParams(
                        baudRate,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

            window.infoOut("Baud Rate:"+serialPort.getBaudRate(),P_LOW);
            window.infoOut("Data Bits:"+serialPort.getDataBits(),P_LOW);
            window.infoOut("Stop Bits:"+serialPort.getStopBits(),P_LOW);
            window.infoOut("Parity:"+serialPort.getParity(),P_LOW);
            window.infoOut("Flow Control:"+serialPort.getFlowControlMode(),P_LOW);

            successful = true;
            return successful;
        }
        catch (PortInUseException e)
        {
            window.dbgOut(selectedPort + " is in use. (" + e.toString() + ")");
            return successful;
        }
        catch (Exception e)
        {
            window.dbgOut("Failed to open " + selectedPort + "(" + e.toString() + ")");
            return successful;
        }
    }

    //open the input and output streams
    //pre: an open port
    //post: initialized input and output streams for use to communicate data
    public boolean initIOStream()
    {
        //return value for whether opening the streams is successful or not
        boolean successful = false;
        try {
            input = new BufferedInputStream(serialPort.getInputStream());
            output = serialPort.getOutputStream();

            successful = true;
            return successful;
        }
        catch (IOException e) {
            logText = "I/O Streams failed to open. (" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
            return successful;
        }

    }

    //starts the event listener that knows whenever data is available to be read
    //pre: an open serial port
    //post: an event listener for the serial port that knows when data is received
/*    public boolean initListener()
    {
        //return value for whether initializing listeners is successful or not
        boolean successful = false;
        try
        {
            //serialPort.addEventListener(this);
            //serialPort.notifyOnDataAvailable(true);

            successful = true;
            return successful;
        }
        catch (TooManyListenersException e)
        {
            window.dbgOut("Too many listeners. (" + e.toString() + ")");
            return successful;
        }
    }*/

    //disconnect the serial port
    //pre: an open serial port
    //post: closed serial port
    public void disconnect()
    {
        //close the serial port
        try
        {
            serialPort.removeEventListener();
            serialPort.close();
            input.close();
            output.close();
            setConnected(false);
            window.toggleControls();
            window.infoOut("Disconnected",GUI.P_HIGH);
            window.txtLog.setForeground(Color.gray);
        }
        catch (Exception e)
        {
            window.dbgOut("Failed to close port:" + "(" + e.toString() + ")");
        }
    }

    final public boolean getConnected()
    {
        return bConnected;
    }

    public void setConnected(boolean bConnected)
    {
        this.bConnected = bConnected;
    }

    //convert Byte[] object to primitive byte[];
    byte[] toPrimitives(Byte[] oBytes)
    {
        byte[] bytes = new byte[oBytes.length];

        for(int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }

        return bytes;
    }

    //convert byte[] to String
    String bytesToStr(byte[] bytes)
    {
        String s = new String(bytes);
        return s;
    }

    //convert List<byte[] to string for printing>
    String blToStr(List<byte[]> bl)
    {
        String s = "";
        for (byte[] b : bl){
            s = s + bytesToStr(b) + '\n';
        }
        return s;
    }

    //convert List<String> to string for printing>
    String slToStr(List<String> sl)
    {
        String s = "";
        for (String str : sl){
            s = str + '\n';
        }
        return s;
    }

    //Clear read buffers
    public void clrReadBuffers(){
        readBytes.clear();
        readLines.clear();
        byteList.clear();
    }

    public boolean runExperiment(){
        long time = System.currentTimeMillis();
        window.infoOut("Starting experiment",GUI.P_MED);
        String[] cmd = new String[3];
        boolean success = false;
        boolean retain = window.cbRetainResults.getState();

        //Clear plot dataset if retain option not selected:
        if (!retain)
            dataPlot.clearSeries();

        for (int i = 0; i <= 2; i++) {
            cmd[i] = settings.getCommand(i + 1);
            if (cmd[i] == null || cmd[i].isEmpty()){
                success = false;
                break;
            }
            //Set domain axis, run only once
            if (i==2)
                dataPlot.setDomainAxis(settings.getRangeFromParams(),retain);

            //Valid command array formed, clear read data and send cmd
            clrReadData();
            settings.printParams();
            sendCommand(cmd[i]);

            if (inputHandler(2,dataPlot.getSeriesCount())){
                success = true;
            } else {
                success = false;
                break;
            }
        }
        if (bAborted) {
            window.dbgOut("Experiment aborted!");
            bAborted = false;
        } else if (success)
            window.infoOut("Experiment completed successfully",GUI.P_MED);
        else
            window.infoOut("Experiment failed",GUI.P_MED);

        return success;
    }

    //temporary method to obtain string from parameters
    public boolean getString(){
        String[] cmd = new String[3];
        boolean success = true;

        for (int i = 0; i <= 2; i++) {
            cmd[i] = settings.getCommand(i + 1);
            if (cmd[i] == null || cmd[i].isEmpty()){
                success = false;
                break;
            }

        }

        return success;
    }

    //Handles incoming data during experiment
    //Discards supplied number of initial measurements
    //Method returns true when "no" received, else runs indefinitely
    private boolean inputHandler(int discards, int scans){

        while(true){
            try {
                String line = formReadLine();
                byte databytes[] = new byte[10];
                MeasData dataPnt = new MeasData(3000);
                int dataType = settings.getDataType();

                //check if full line formed
                if (line == null) {

                } else if(line.equals("EXCEPTION")){ //Exception occurred in formReadLine method
                    return false;
                } else if(line.equals("a")){ //experiment aborted
                    window.dbgOut("Experiment aborted");
                } else if (line.equals("B")) { //data line follows
                    if (dataType == 1) {
                        input.read(databytes, 0, 6);
                        dataPnt = parseData(databytes, dataType, 3000);// TODO: 2017-09-20 add variable gain

                    } else if (dataType == 2) {
                        input.read(databytes, 0, 10);
                        dataPnt = parseData(databytes, dataType, 3000);// TODO: 2017-09-20 add variable gain

                    }
                    if ((dataType == 1)||(dataType == 2)){

                        if (discards > 0)
                            discards--;
                        else
                            dataPlot.addPoint(scans,dataPnt.voltage, dataPnt.current);
                            window.infoOut(dataPnt.toString(), GUI.P_LOW);
                    }
                } else if (line.equals("no")) { //end of experiment data
                    window.infoOut(line, GUI.P_LOW);
                    return true;
                } else if (line.equals("S")) { //new scan/series
                    scans++;
                    window.infoOut("Scans: " + scans, GUI.P_LOW);

                } else {
                    //System.out.println("line: " + line + "  length: " + line.length());
                }
            } catch(Exception e) {
                window.dbgOut("Input Handler exception occurred: (" + e.toString() + ")");
                return false;
            }

        }
        
    }

    public MeasData parseData(byte[] bytes, int dataType, int gain){
        MeasData d = new MeasData(gain);
        if (dataType == 1) {
            d.setData(uint16(bytes, 0), int32(bytes, 2));
        } else if (dataType == 2) {
            d.setData(uint16(bytes, 0), int32(bytes, 2) - int32(bytes, 6)); //current = forward - reverse
        }
        return d;
    }

    private int uint16(byte[] arr, int start){
        int result, result2;
        int a;
        int b;
        if ((arr.length - start)>=2){
/*            a = (int)arr[start];
            a = a << 8;
            b = (int)arr[start+1];
            result = a + b;*/
            result = (arr[start+1] & 0xFF) << 8 | (arr[start] & 0xFF);
            //System.out.println(result);
        } else {
            result = -1;
        }
        return result;
    }

    private int int32(byte[] arr, int start){
        int result;

        if ((arr.length - start)>=4){
            result = arr[start+3] << 24 | (arr[start + 2] & 0xFF) << 16 | (arr[start + 1] & 0xFF) << 8 | (arr[start] & 0xFF);
        } else {
            result = 0;
        }
        return result;
    }
    //Returns next serial line
    //If no complete line return null
    //Treats consecutive CR/NL as one
    private String formReadLine(){
        String s = "";
        boolean lineformed = false;
        byte b;
        try{
            input.mark(input.available());
            while (input.available()>0 && !lineformed) {
                b = (byte)input.read();
                if ((b == NL_ASCII) || (b == CR_ASCII)){
                    //input.mark(2);
                    //if next character is not \n or \r reset to mark
                    //b = (byte)input.read();
                    //if ((b != NL_ASCII) && (b != CR_ASCII))
                       //input.reset();
                    lineformed = true;

                } else {
                    s += (char)b;
                }
            }
            if (!lineformed){
                s = null;
                input.reset();
            }
            return s;
        } catch (Exception e){
            window.dbgOut("Failed to form read line. (" + e.toString() + ")");
            return "EXCEPTION";
        }

    }

    public void sendCommand(String cmd){
        if (sendInit()) {
            window.infoOut("Sending command: " + cmd, GUI.P_DBG);

            sendString(cmd);


        }
    }

    public boolean sendInit()
    {
        try
        {
            int i = 0;
            byte b;
            window.infoOut("sendInit(), writing initial !", GUI.P_DBG);

            //clear any data in input stream
            clrReadData();

            //send initialize cmd
            output.write('!');
            output.flush();

            // wait until data is rec'd
            Thread.sleep(20);

            while (((b = readByte()) != C_ASCII) && (i<=10)){
                window.infoOut("sendInit(), writing !, i = " + i, GUI.P_DBG);
                clrReadData();
                output.write('!');
                Thread.sleep(200);
                i++;
            }

            //Check if reply received or if max attempts exceeded
            if (b == C_ASCII){
                window.infoOut("sendInit True", GUI.P_DBG);
                clrReadData();
                return true;
            }
            else{
                window.infoOut("sendInit False", GUI.P_DBG);
                clrReadData();
                return false;
            }
        }
        catch (Exception e)
        {
            window.dbgOut("Failed to write data. (" + e.toString() + ")");
            return false;
        }
    }
    //read all bytes left in input stream
    public void clrReadData(){
        try {
            int i = input.available();
            byte[] b = new byte[i];
            input.read(b, 0, i);
        }catch (IOException e) {
            window.dbgOut("Failed to read data. (" + e.toString() + ")");
        }
    }

    //read single byte from serial inputStream
    public byte readByte(){
        byte b = 0;
        try {
            b = (byte)input.read();
            return b;
        } catch (IOException e) {
            window.dbgOut("Failed to read data. (" + e.toString() + ")");
            return b;
        }
    }
    //wait until availableBytes == 0 then set amount of time
    public void waitRead(int delay)
    {
        try
        {
            while(input.available()>0);
            Thread.sleep(delay);
        }
        catch (Exception e)
        {
            window.dbgOut("Exception occurred: (" + e.toString() + ")");
        }
    }

    //wait for specified amount of time (ms)
    public void waitTime(int delay)
    {
        try
        {
            Thread.sleep(delay);
        }
        catch (Exception e)
        {
            window.dbgOut("Exception occurred: (" + e.toString() + ")");
        }
    }
    public void sendString(String strOutput)
    {
        try
        {
            window.infoOut("sendString():" + strOutput, GUI.P_DBG);
            for (int i = 0; i < strOutput.length(); i++) {
                output.write(strOutput.charAt(i));
                output.flush();
                Thread.sleep(1);
                }
        }
        catch (Exception e)
        {
            logText = "Failed to write data. (" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
        }
    }

    void setAborted(boolean b){
        bAborted = b;
    }
    boolean getAborted(){
        return bAborted;
    }
}
