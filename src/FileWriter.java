import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class FileWriter {
	static final String extension = ".log";
	
	//file writers
	protected File outputFile;
	protected Writer outs;
	
	public FileWriter() throws IOException {
		outputFile = getFileHandle("out"+extension);
		outs = new OutputStreamWriter( new FileOutputStream(outputFile) );
	}
	
	protected File getFileHandle(String name) throws IOException {
		File f = new File(name);
		if( !f.exists() ) f.createNewFile();
		return f;
	}
	
	public void writeToFile(String line) throws IOException {
		outs.write(line);
	}
	
}
