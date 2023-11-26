package wikixmlsplit.output.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.io.NodeDeserializer;
import wikixmlsplit.io.PageIO;
import wikixmlsplit.util.InstantTypeAdapter;

public abstract class Outputbase {
	final static Logger logger = Logger.getLogger(Outputbase.class);
	final static int maxFiles = 200;

	@Parameter(names = "-input", description = "Input file")
	protected String inputFile;

	@Parameter(names = "-output", description = "Output path")
	protected String outputFile;

	@Parameter(names = "-limit1", description = "Limit 1")
	protected double limit1 = 0.99d;
	@Parameter(names = "-limit2", description = "Limit 2")
	protected double limit2 = 0.6d;
	@Parameter(names = "-limit3", description = "Limit 3")
	protected double limit3 = 0.4d;


	protected PageIO pageIO = new PageIO();

	protected final Gson g = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantTypeAdapter()).disableHtmlEscaping().create();

	Writer writer = null;
	String writerId = null;

	protected NodeDeserializer deserializer = new NodeDeserializer();

	private String fileExtension;
	private Path inputPath;

	protected Outputbase(String fileExtension) {
		this.fileExtension = fileExtension;
	}
	

	public void run() throws IOException {
		// we turn off logging because sweble spams the log quite badly
		LogManager.getLogger(org.sweble.wikitext.engine.output.HtmlRenderer.class).setLevel(Level.OFF);
		LogManager.getLogger(Outputbase.class).setLevel(Level.ALL);

		// parse bucket
		inputPath = Paths.get(inputFile);

		// if (outputFile != null) {
		// 	w = new OutputStreamWriter(new FileOutputStream(outputFile + "/" + inputPath.getFileName() + ".output.json"), StandardCharsets.UTF_8);
		// 	jw = g.newJsonWriter(w);
		// }

		if (inputFile.endsWith(fileExtension)) {
			// parse individual file
			processPath(inputPath);
		} else {
			long start = System.nanoTime();
			logger.trace("Processing Bucket " + inputPath.getFileName());
			processBucket(inputPath.toFile());
			logger.trace("Finished Bucket in total time (ms): " + (System.nanoTime() - start) / 1E6);
		}

		dispose();
	}

	private void processBucket(File bucket) {
		File[] files1 = bucket.listFiles((d, name) -> name.endsWith(fileExtension));
		List<File> files = Arrays.asList(files1);
		if (files == null) {
			System.err.println("No .parsed files found in the bucket");
			return;
		}

		for(int fileNr = 0; fileNr < files.size(); fileNr++) {
			System.out.println("Processing file " + (fileNr) + "/" + files.size());
			
			File f = files.get(fileNr);

			try {
				processPath(f.toPath());
				logger.trace("Success: " + f.getName());
			}
			catch (OutOfMemoryError e) {
				System.out.println("Ran out of mem on file: " + f.getName());
				e.printStackTrace();
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	abstract protected void processPath(Path inputPath) throws IOException;

	protected void addMetaData(MyPageType page, MyRevisionType r, Map<String, List<String>> parsed,
			Map<String, Object> map) {
		map.put("pageTitle", page.getTitle());
		map.put("pageID", page.getId());
		map.put("revisionId", r.getId());
		map.put("user", r.getContributor());
		map.put("validFrom", r.getInstant());
		if (r.getComment() != null) {
			map.put("comment", r.getComment().getValue());
		}
//		map.put("categories", parsed.get("CATEGORIES"));
//		map.put("templateNames", parsed.get("TEMPLATES"));
	}

	protected void write(Map<String, Object> map) {
		write(map, null);
	}
	protected void write(Map<String, Object> map, String identifier) {
		if(identifier == null || identifier.equals("")) identifier = inputPath.getFileName().toString();
		
		try {

			if(writer == null || !writerId.equals(identifier)) {
				dispose();
				String path = outputFile + File.separator + (identifier != null ? identifier : inputPath.getFileName()) + ".output.json";
				writer = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
				writerId = identifier;
			}

			String s = g.toJson(map, Map.class);
			writer.write(s + "\n");
		} 
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	protected void dispose() {
		if(writer != null) {
			try {
				writer.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		writer = null;
		writerId = null;		
	}
}
