package wikixmlsplit.output.meta;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MetaDataSummary {
	@Parameter(names = "-input", description = "Input folder")
	protected String inputFile;

	@Parameter(names = "-output", description = "Output file")
	protected String outputFile;
	
	private BufferedWriter w;
	private int revisions;
	private int maxTables;
	private int lastTables;

	public static void main(String[] args) throws IOException {
		MetaDataSummary main = new MetaDataSummary();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.run();
	}

	private void run() throws IOException {
		w = Files.newBufferedWriter(Paths.get(outputFile));
		Files.walk(Paths.get(inputFile)).filter(Files::isRegularFile).forEach(this::handleFile);
		w.close();
	}

	private void handleFile(Path p) {
		if (!p.toString().endsWith(".meta"))
			return;

		revisions = 0;
		maxTables = 0;
		lastTables = 0;
		try (BufferedReader reader = Files.newBufferedReader(p)) {
			reader.lines().forEach(line -> {
				++revisions;
				int tableCount = Integer.parseInt(line.split(",")[2]);
				lastTables = tableCount;
				maxTables = Math.max(maxTables, tableCount);
			});
			
			if (maxTables > 0)
				w.append(p.getFileName().toString().split("\\.")[0]).append(",").append(String.valueOf(revisions)).append(",").append(String.valueOf(maxTables)).append(",").append(String.valueOf(lastTables)).append("\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
