package wikixmlsplit.output.meta;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.io.BuilderBase;
import wikixmlsplit.io.NodeDeserializer;
import wikixmlsplit.io.PageIO;
import wikixmlsplit.lists.builder.WikiListBuilder;
import wikixmlsplit.wikitable.builder.TableBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class ExtractMetadata {
	@Parameter(names = "-input", description = "Input file")
	protected String inputFile;

	@Parameter(names = "-summary")
	protected String summaryFile;

	@Parameter(names = "-revisions")
	protected boolean revisionFiles;

	@Parameter(names = "-list")
	protected boolean targetList;

	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private PageIO reader;

	private BuilderBase<?> tableBuilder;

	public static void main(String[] args) throws IOException {

		ExtractMetadata main = new ExtractMetadata();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}

	public void run() throws IOException {

		File dir = new File(inputFile);
		File[] files = dir.listFiles((d, name) -> name.endsWith(".parsed"));

		if (files == null) {
			System.err.println("Input is no valid directory!");
			return;
		}

		tableBuilder = targetList ? new WikiListBuilder() : new TableBuilder(false);

		try (Writer summaryWriter = getSummaryWriter()) {
			reader = new PageIO();
			for (File dumpFile : files) {
				handleDumpFile(dumpFile, summaryWriter);
			}
		}

	}

	private void handleDumpFile(File dumpFile, Writer summaryWriter) throws IOException {
		System.out.println("Handling: " + dumpFile);
		MyPageType page = reader.read(dumpFile.toPath());

		AtomicInteger maxCount = new AtomicInteger();
		try (Writer revisionWriter = getRevisionWriter(dumpFile)) {
			new NodeDeserializer().deserialize(page.getRevisions(), (r, nodes) -> {

				int revTableCount = tableBuilder.getObjectCount(nodes);
				if (revisionFiles)
					writeRevision(revisionWriter, r, revTableCount);
				maxCount.getAndAccumulate(revTableCount, Math::max);
			});
		}

		if (summaryWriter != null) {
			summaryWriter.append(String.valueOf(page.getId())).append(",").append(String.valueOf(maxCount.get())).append(",").append(page.getTitle()).append("\n");
		}
	}

	private Writer getSummaryWriter() throws IOException {
		if (summaryFile == null)
			return null;

		Path outputFile = Paths.get(summaryFile);
		Files.createDirectories(outputFile.getParent());
		return Files.newBufferedWriter(outputFile);
	}

	private Writer getRevisionWriter(File dumpFile) throws IOException {
		Path outputFile = Paths.get(dumpFile.getAbsolutePath() + ".meta");
		return revisionFiles ? Files.newBufferedWriter(outputFile) : null;
	}

	private void writeRevision(Writer revisionWriter, MyRevisionType r, int revTableCount) {
		try {
			revisionWriter.append(String.valueOf(r.getId())).append(",").append(df.format(r.getInstant())).append(",")
					.append(String.valueOf(revTableCount)).append("\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
