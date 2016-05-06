import java.io.*;
import java.util.LinkedList;

/**
 * Created by martinosecchi on 06/05/16.
 */
public class TesterOnFiles {

    private static final String FOLDER = "/Users/martinosecchi/Documents/university/ITU/ITU 2nd semester/Pervasive Computing/mandatory 2/";
    private static final String SAMPLES_FOLDER = FOLDER + "samples/";
    private static final String OUT_FOLDER = FOLDER + "out/";
    private static final String GRAPH_SCRIPT_PATH =  "plot.plt";

    LinkedList<String> buf10 = new LinkedList<String>();

    public void gnuplotScript(String inputFile){
        String content = "plot  \"" + inputFile +"\" using 1 title 'accx' with lines, " +
                         "\"" + inputFile + "\" using 2 title 'accy' with lines, " +
                         "\"" + inputFile +"\" using 3 title 'accz' with lines, " +
                         "\"" + inputFile +"\" using 4 title 'girx' with lines, " +
                         "\"" + inputFile +"\" using 5 title 'giry' with lines, " +
                         "\"" + inputFile +"\" using 6 title 'girz' with lines";
        try {
            FileWriter fw = new FileWriter(new File(GRAPH_SCRIPT_PATH));
            fw.append(content);
            fw.close();
            Process p = Runtime.getRuntime().exec("gnuplot " + GRAPH_SCRIPT_PATH + " --persist");
            p.waitFor();
            BufferedReader errs = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line = errs.readLine();
            while (line != null){
                System.out.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

//  plot "samples/data_raw_left_right.txt" using 1 title 'accx' with lines,\
// "samples/data_raw_left_right.txt" using 2 title 'accy' with lines,\
// "samples/data_raw_left_right.txt" using 3 title 'accz' with lines,\
// "samples/data_raw_left_right.txt" using 4 title 'girx' with lines,\
// "samples/data_raw_left_right.txt" using 5 title 'giry' with lines,\
// "samples/data_raw_left_right.txt" using 6 title 'girz' with lines

    public static void main(String[] args){
        TesterOnFiles t = new TesterOnFiles();

        t.initializeBufs();
        try {
            t.workOn("data_raw_left_right.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeBufs(){
        for (int i = 0; i < 10; i++) {
            buf10.add("0   0   0   0   0   0   0000");
        }
    }

    public String getAvgs(){
        String res = "";
        int[] avgs = new int[6];
        for (String s:buf10){
            String[] ss = s.split("    ");
            for (int i=0; i<ss.length-1; i++){ //skip timestamp
                avgs[i] += Integer.parseInt(ss[i]) ;
            }
        }
        for (int i: avgs){
            res+=  (i/buf10.size()) + "    ";
        }
        res += System.currentTimeMillis() + "   ";
        res += "\n";
        return res;
    }

    public void workOn(String inputFilePath) throws IOException {
        FileWriter out = null;
        BufferedReader br = null;
        try {
            out = new FileWriter(new File(OUT_FOLDER + inputFilePath));
            br = new BufferedReader(new FileReader(new File(SAMPLES_FOLDER + inputFilePath)));

            String line = br.readLine();
            while (line != null) {
                if (!line.startsWith("#")){
                    buf10.poll();
                    buf10.add(line);
                    out.append(getAvgs());
                }
                line = br.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out!=null){
                out.close();
                gnuplotScript(SAMPLES_FOLDER+inputFilePath);
                gnuplotScript(OUT_FOLDER + inputFilePath);

            }
            if (br!=null)
                br.close();
        }
    }

}
