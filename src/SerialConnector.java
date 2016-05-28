import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

import java.io.*;
import java.util.*;

public class SerialConnector implements SerialPortEventListener {

    private static final String PORT_NAME = "/dev/cu.pinkie-DevB";
    private static final String FILE_PATH_AVG = "/Users/martinosecchi/Documents/university/ITU/ITU 2nd semester/Pervasive Computing/mandatory 2/data_avg.csv";
    private static final String FILE_PATH_W1 = "/Users/martinosecchi/Documents/university/ITU/ITU 2nd semester/Pervasive Computing/mandatory 2/window.csv";
    private static final String FILE_PATH_TRAINSET = "/Users/martinosecchi/Documents/university/ITU/ITU 2nd semester/Pervasive Computing/mandatory 2/training_set.csv";
    private static final int DATA_RATE = 9600;
	private static final int TIME_OUT = 2000;
    private static final int SLEEP_TIME = 10000;

    private int NEIGHBORS = 10;
    private int WINDOW_SIZE = 50 * 6;
    private int WINDOW_OVERLAP = 10;
    private int instanceCounter = 0;
    private int countpred = 0;

    private FileWriter fileAvg;
	private BufferedReader input = null;
    private OutputStream output;
    private SerialPort serialPort;

    public LinkedList<OutputObj> data = new LinkedList<OutputObj>();
    public LinkedList window = new LinkedList();

    DataSource trainSrc;
    Instances train;
    Classifier classifier;
    Evaluation eval;
    Standardize filter = new Standardize();


   	public static void main(String[] args) {
   		SerialConnector main = new SerialConnector();

        main.initialize();

//        TesterOnFiles plot = new TesterOnFiles();
//        try { Thread.sleep(SLEEP_TIME);} catch (InterruptedException exn){}
        promptEnterKey();
        main.close();

        try { Thread.sleep(1000); } catch (InterruptedException exn){}

//        plot.gnuplotScriptCSVAll(FILE_PATH_AVG);
//        try { Thread.sleep(1000); } catch (InterruptedException exn){}
   	}

    private void sendGesture(String pred) throws IOException{

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
        System.out.println("predictions made: " + countpred);
//        try {
//            fileAvg.close();
////            fileRaw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

   	public void initialize() {
//        FileCreate();

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
            // weka stuff
            this.trainSrc = new DataSource(FILE_PATH_TRAINSET);
            this.train = trainSrc.getDataSet();
            if (train.classIndex() == -1)
                train.setClassIndex(train.numAttributes() - 1);
            this.classifier = new BayesNet();
            this.classifier.buildClassifier(train);
            this.eval = new Evaluation(train);
            this.filter.setInputFormat(train);
            this.train = Filter.useFilter(train, filter);

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

            fileAvg = new FileWriter(new File(FILE_PATH_AVG));
            fileAvg.append("#AccX,AccY,AccZ,GyrX,GyrY,GyrZ\n");

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

//                        fileAvg.append(getAvgs());

                        recognizeGesture(out);
                    }


                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
        }
    }

    private synchronized boolean updateCounter(){
        if (instanceCounter == WINDOW_OVERLAP){
            instanceCounter = 0;
            return true;
        } else{
            instanceCounter += 1;
            return false;
        }
    }

    private synchronized void recognizeGesture(OutputObj out){
        updateWindow(out);
//        until window size is not reached I don' do anything
        if (window.size() == WINDOW_SIZE) {
//            then, I do stuff every WINDOW_OVERLAP times
            if (updateCounter()) {
                countpred++;
                try {
                    FileWriter fileW1 = new FileWriter(new File(FILE_PATH_W1));
                    fileW1.append(getWindowHeader());
                    fileW1.append(getWindow());
                    fileW1.close();
                    DataSource src = new DataSource(FILE_PATH_W1);
                    final Instances test = src.getDataSet();
//                    test = Filter.useFilter(test, this.filter);

                    if (test.classIndex() == -1)
                        test.setClassIndex(test.numAttributes() - 1);

                    if (this.eval!=null && this.classifier!=null && test!=null) {

                        double[] predictions = eval.evaluateModel(classifier, test);
                        String pred = train.classAttribute().value((int) predictions[0]);
                        System.out.println(pred);
                        sendGesture(pred);

                    } else {
                        System.out.println("null pointers");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void updateWindow(OutputObj obj){
        for (int i = 0; i < obj.values.length; i++) {
            if (window.size() == WINDOW_SIZE )
                window.poll();
            window.add(obj.values[i]);
        }
    }

    private synchronized String getWindow(){
        String res = "";
        for (Object i : window){
            res+=""+i+",";
        }
        res+="?";
        return res;
    }

    private String getWindowHeader(){
        return "accx0,accy0,accz0,gyrx0,gyry0,gyrz0,accx1,accy1,accz1,gyrx1,gyry1,gyrz1,accx2,accy2,accz2,gyrx2,gyry2,gyrz2,accx3,accy3,accz3,gyrx3,gyry3,gyrz3,accx4,accy4,accz4,gyrx4,gyry4,gyrz4,accx5,accy5,accz5,gyrx5,gyry5,gyrz5,accx6,accy6,accz6,gyrx6,gyry6,gyrz6,accx7,accy7,accz7,gyrx7,gyry7,gyrz7,accx8,accy8,accz8,gyrx8,gyry8,gyrz8,accx9,accy9,accz9,gyrx9,gyry9,gyrz9,accx10,accy10,accz10,gyrx10,gyry10,gyrz10,accx11,accy11,accz11,gyrx11,gyry11,gyrz11,accx12,accy12,accz12,gyrx12,gyry12,gyrz12,accx13,accy13,accz13,gyrx13,gyry13,gyrz13,accx14,accy14,accz14,gyrx14,gyry14,gyrz14,accx15,accy15,accz15,gyrx15,gyry15,gyrz15,accx16,accy16,accz16,gyrx16,gyry16,gyrz16,accx17,accy17,accz17,gyrx17,gyry17,gyrz17,accx18,accy18,accz18,gyrx18,gyry18,gyrz18,accx19,accy19,accz19,gyrx19,gyry19,gyrz19,accx20,accy20,accz20,gyrx20,gyry20,gyrz20,accx21,accy21,accz21,gyrx21,gyry21,gyrz21,accx22,accy22,accz22,gyrx22,gyry22,gyrz22,accx23,accy23,accz23,gyrx23,gyry23,gyrz23,accx24,accy24,accz24,gyrx24,gyry24,gyrz24,accx25,accy25,accz25,gyrx25,gyry25,gyrz25,accx26,accy26,accz26,gyrx26,gyry26,gyrz26,accx27,accy27,accz27,gyrx27,gyry27,gyrz27,accx28,accy28,accz28,gyrx28,gyry28,gyrz28,accx29,accy29,accz29,gyrx29,gyry29,gyrz29,accx30,accy30,accz30,gyrx30,gyry30,gyrz30,accx31,accy31,accz31,gyrx31,gyry31,gyrz31,accx32,accy32,accz32,gyrx32,gyry32,gyrz32,accx33,accy33,accz33,gyrx33,gyry33,gyrz33,accx34,accy34,accz34,gyrx34,gyry34,gyrz34,accx35,accy35,accz35,gyrx35,gyry35,gyrz35,accx36,accy36,accz36,gyrx36,gyry36,gyrz36,accx37,accy37,accz37,gyrx37,gyry37,gyrz37,accx38,accy38,accz38,gyrx38,gyry38,gyrz38,accx39,accy39,accz39,gyrx39,gyry39,gyrz39,accx40,accy40,accz40,gyrx40,gyry40,gyrz40,accx41,accy41,accz41,gyrx41,gyry41,gyrz41,accx42,accy42,accz42,gyrx42,gyry42,gyrz42,accx43,accy43,accz43,gyrx43,gyry43,gyrz43,accx44,accy44,accz44,gyrx44,gyry44,gyrz44,accx45,accy45,accz45,gyrx45,gyry45,gyrz45,accx46,accy46,accz46,gyrx46,gyry46,gyrz46,accx47,accy47,accz47,gyrx47,gyry47,gyrz47,accx48,accy48,accz48,gyrx48,gyry48,gyrz48,accx49,accy49,accz49,gyrx49,gyry49,gyrz49,label\n";
    }

    private String getAvgs(){
        String res = "";
        int[] avgs = new int[6];
        for (OutputObj o:data){
            for (int i=0; i<o.values.length; i++){
                avgs[i]+=o.values[i];
            }
        }
        for (int i=0; i < avgs.length; i++){
            res+=  (avgs[i]/data.size());
            if (i< avgs.length-1)
                res+=",";
            else
                res +="\n";
        }
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
            for (int i=0; i<6; i++) {
                res += values[i];
                if (i < values.length - 1)
                    res+=",";
                else
                    res+="\n";
            }
        }
        return res;
    }
}