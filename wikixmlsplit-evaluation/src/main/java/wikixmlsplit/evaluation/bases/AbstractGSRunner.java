package wikixmlsplit.evaluation.bases;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.io.PageIO;
import wikixmlsplit.util.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractGSRunner {
	private final PageIO pageIO = new PageIO();
	protected final Gson g = new GsonBuilder().setPrettyPrinting().create();

	protected FileWriter w;
	protected JsonWriter jw;

	@Parameter(names = "-input", description = "Input files")
	protected String inputFiles;

	@Parameter(names = "-inputGold", description = "Input gold standard")
	protected String inputGold;

	@Parameter(names = "-output", description = "Output files")
	protected String outputFile;

	protected boolean json = true;

	public void run() throws IOException {
		if (outputFile != null) {
			w = new FileWriter(outputFile);
			if(json)
				jw = new JsonWriter(w);
		}

		File dir = new File(inputFiles);
		if(dir.isDirectory()) {
			File[] files = dir.listFiles((d, name) -> name.endsWith(".parsed"));
			if (files == null) {
				System.err.println("input is no valid directory!");
				return;
			}
			for (File parsed : files) {
				run(parsed.toPath());
			}
		} else if (dir.toPath().toString().endsWith(".parsed")) {
			run(dir.toPath());
		} else {
			System.err.println("Invalid input!");
		}
		
		if (w != null) {
			if(json)
				jw.close();
			w.close();
		}
	}

	public void run(Path parsed) throws IOException {
		MyPageType page = pageIO.read(parsed);
		Path inputFolder = Paths.get(inputGold).resolve(Util.makeSafeFileName(page.getTitle()));

		if (!inputFolder.toFile().exists()) {
			System.err.println("Did not find gold standard for " + page.getTitle());
			return;
		}
		System.out.println("Handling: " + page.getTitle());

		evaluate( page, inputFolder);
	}
	
	abstract protected void evaluate(MyPageType page, Path inputFolder) throws IOException;
}
