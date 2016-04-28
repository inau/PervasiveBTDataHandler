import java.io.*;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import com.google.common.collect.EvictingQueue;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.event.*;


public class SerialTest implements SerialPortEventListener {

    private static final String PORT_NAMES[] = {
            //"/dev/ttyUSB0",
            "/dev/ttyS98" //SymLink to Bluetooth using prefix the library can understand
            //"/dev/rfcomm1/" //Linux Bluetooth
    };
    /**
     * A BufferedReader which will be fed by a InputStreamReader
     * converting the bytes into characters
     * making the displayed results codepage independent--
     */

    private BufferedReader input;
    private OutputStream output;
    /**
     * The output stream to the port
     */
    private static final int TIME_OUT = 2000;
    /**
     * Milliseconds to block while waiting for port open
     */
    private static final int DATA_RATE = 9600;
    /**
     * Default bits per second for COM port.
     */
    SerialPort serialPort;/**The port we're normally going to use.*/


    /**
     * Three things to change according to how we want it to behave and on what machine - should be user defined in GUI
     */
    private int windowLength = 5;
    private FileWriter file;
    private String saveFilePath = "/home/aunk05/Documents/data.csv";


    private int ACCX = 0, ACCY = 1, ACCZ = 2, GYRX = 3, GYRY = 4, GYRZ = 5;
    private EvictingQueue<String> dataQueue = EvictingQueue.create(windowLength);

    public static void main(String[] args) throws Exception {

        SerialTest main = new SerialTest();
        main.prepareGUI();
        main.displayGUI();
        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000000);
                } catch (InterruptedException ie) {
                }
            }
        };
        t.start();
        System.out.println("Started");
    }

    public void initialize() {
        FileCreate();
        initializeQueue();

        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

        //First, Find an instance of serial port as set in PORT_NAMES.
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            for (String portName : PORT_NAMES) {
                if (currPortId.getName().equals(portName)) {
                    portId = currPortId;
                    break;
                }
            }
        }
        if (portId == null) {
            System.out.println("Could not find COM port.");
            return;
        }

        try {
            // open serial port, and use class name for the appName.
            serialPort = (SerialPort) portId.open(this.getClass().getName(),
                    TIME_OUT);

            // set port parameters
            serialPort.setSerialPortParams(DATA_RATE,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            // open the streams
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();

            // add event listeners
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    public void FileCreate() {

        try {
            file = new FileWriter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void close() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

     /** Handle an event on the serial port. Reads each column of fields across the 'depth'
     * of the queue, aggregate susing 'getAverage' for each data element, and composes a .csv file line.*/

    public synchronized void serialEvent(SerialPortEvent oEvent) {
        {
            if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                try {

                    dataQueue.add(input.readLine());
                    file.writeToFile(
                            System.currentTimeMillis() + "," +
                                    getAverage(ACCX) + "," +
                                    getAverage(ACCY) + "," +
                                    getAverage(ACCZ) + "," +
                                    getAverage(GYRX) + "," +
                                    getAverage(GYRY) + "," +
                                    getAverage(GYRZ) + "\n");


                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
        }
    }


    private String getAverage(int elementNo) {

        Object[] values = dataQueue.toArray(); /**changes current EvictionQueue into array of [window length] elements*/

        int sum = 0;
        for (Object k : values) { /** takes each array element and into  aggregates the int value of the */
            String[] line = ((String) k).split(","); /**splits it into an array of Strings, the aggregates the */
            sum += Integer.valueOf(line[elementNo]);/** [element no]th item across the consequent array of arrays*/
        }
        return Integer.toString(sum / windowLength);/** to produce an average for each datum*/

    }

    /**Initialises [window length] number of queue items, each a group of the six
     * sensor fields,  so that we can start aggregating immediately. The first few
     * averages will therefore be lower than the rest.*/

    private void initializeQueue() {
        for (int i = 0; i < windowLength; i++)
            dataQueue.add("0,0,0,0,0,0");
    }

    /** Everything below is GUI, not interesting. */

    private Frame mainFrame;
    private Label splash;
    private Panel controlPanel;




    private void prepareGUI() {
        mainFrame = new Frame("This is SPARTA");
        mainFrame.setSize(200, 100);
        mainFrame.setLayout(new GridLayout(2, 1));
        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });
        splash = new Label();

        controlPanel = new Panel();
        controlPanel.setLayout(new FlowLayout());

        mainFrame.add(splash);
        mainFrame.add(controlPanel);
        mainFrame.setVisible(true);
    }

    private void displayGUI() {
        splash.setText("Sampling" + "\n" + "Preprocessing\nAcceleration\nRotation and\nTranslation\nAnalysis");

        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");

        startButton.setActionCommand("Start");
        stopButton.setActionCommand("Stop");

        startButton.addActionListener(new ButtonClickListener());
        stopButton.addActionListener(new ButtonClickListener());

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        mainFrame.setVisible(true);
    }

    private class ButtonClickListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            if (command.equals("Start")) {
                initialize();
            } else if (command.equals("Stop")) {
                close();
                System.exit(0);
            }
        }
    }
}
