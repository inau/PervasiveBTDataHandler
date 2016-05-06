import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.*;
import java.util.*;

public class SerialConnector implements SerialPortEventListener {
	private static final String PORT_NAME = "/dev/cu.HC-06-DevB";
    private static final String FILE_PATH_RAW = "/Users/martinosecchi/Documents/university/ITU/ITU 2nd semester/Pervasive Computing/mandatory 2/data_raw.csv";
    private static final String FILE_PATH_AVG10 = "/Users/martinosecchi/Documents/university/ITU/ITU 2nd semester/Pervasive Computing/mandatory 2/data_avg10.csv";

	private static final int DATA_RATE = 9600;
	private static final int TIME_OUT = 2000;
    private static final int SLEEP_TIME = 10000;
    private int NEIGHBORS = 10;


    private FileWriter fileRaw;
    private FileWriter fileAvg10;

	private BufferedReader input = null;
    private OutputStream output;
    private SerialPort serialPort;
    LinkedList<OutputObj> data = new LinkedList<OutputObj>();


   	public static void main(String[] args) {
   		SerialConnector main = new SerialConnector();
        main.initialize();

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException exn){
        }
       // promptEnterKey();

        main.close();
   	}

    public static void promptEnterKey(){
        System.out.println("Press \"ENTER\" to continue...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    public synchronized void close() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

   	public void initialize() {
        FileCreate();

        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

        //initialize data queue for averages
        for (int i = 0; i < NEIGHBORS; i++) {
            data.add(new OutputObj(new int[6]));
        }

        //First, Find an instance of serial port as set in PORT_NAMES.
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            if (currPortId.getName().equals(PORT_NAME)) {
      	        portId = currPortId;
                break;
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
            fileRaw = new FileWriter(new File(FILE_PATH_RAW));
            fileRaw.append("#Raw data\n");
            fileRaw.append("#AccX, AccY, AccZ,GyrX, GyrY, GyrZ, timestamp\n");

            fileAvg10 = new FileWriter(new File(FILE_PATH_AVG10));
            fileAvg10.append("#averaged data - 10 closest values\n");
            fileAvg10.append("#AccX, AccY, AccZ,GyrX, GyrY, GyrZ, timestamp\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void serialEvent(SerialPortEvent oEvent) {
        {
            if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                try {

                    Gson gson = new Gson();
                    JsonParser parser = new JsonParser();
                    JsonElement root = parser.parse(input.readLine());
                    OutputObj out = null;
                    if (root.isJsonObject())
                        out = gson.fromJson(root, OutputObj.class);
                    if (out!=null){
                        data.poll(); //removes head
                        data.add(out); //append last -> constant size set in NEIGHBORS var

                        fileRaw.append(out.toString());
                        fileAvg10.append(getAvgs());
                    }


                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
        }
    }

    private String getAvgs(){
        String res = "";
        int[] avgs = new int[6];
        for (OutputObj o:data){
            for (int i=0; i<o.values.length; i++){
                avgs[i]+=o.values[i];
            }
        }
        for (int i: avgs){
            res+=  (i/data.size()) + "    ";
        }
        res += System.currentTimeMillis() + "   ";
        res += "\n";
        return res;
    }

}

class OutputObj {
    int[] values;

    public OutputObj(int[] values){
        this.values = values;
    }

    public String toString(){
        String res = "";
        if (values.length == 6) {
            for (int v : values) {
                res += v + "    ";
            }
        }
        //System.out.println(res);
        res += System.currentTimeMillis() + "   ";
        res += "\n";
        return res;
    }
}