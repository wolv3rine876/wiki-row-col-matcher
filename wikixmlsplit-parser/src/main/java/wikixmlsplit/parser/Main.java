package wikixmlsplit.parser;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.sweble.wikitext.dumpreader.DumpReader;
import org.sweble.wikitext.dumpreader.export_0_10.PageType;
import org.sweble.wikitext.dumpreader.export_0_10.RevisionType;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.io.PageIO;
import wikixmlsplit.parser.targets.TargetType;
import wikixmlsplit.util.Util;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Parses XML input and splits a bucket into individual pages, only includes
 * revisions that contain target objects
 */
public class Main {

	@Parameter(names = "-input", description = "Input file")
	protected String inputFile;
	@Parameter(names = "-output", description = "Output folder")
	protected String outputFile;
	@Parameter(names = "-type", description = "Target type")
	protected List<TargetType> targetTypes = Arrays.asList(TargetType.values());
	@Parameter(names = "-additionalType", description = "Additional type")
	protected List<TargetType> additionalTypes = Collections.emptyList();
	@Parameter(names = "-namespaces", description = "Name spaces")
	protected List<BigInteger> namespaces = Arrays.asList(BigInteger.ZERO, BigInteger.valueOf(14));

	private static final Logger logger = Logger.getLogger(Main.class.getName());

	private PageIO pageIO = new PageIO();

	public static void main(String[] args) throws Exception {

		Main main = new Main();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}

	protected void run() throws Exception {
		InputStream is = System.in;
		if (inputFile != null) {
			Path inputPath = Paths.get(inputFile);
			is = Files.newInputStream(inputPath);
		}
		if (outputFile != null)
			Files.createDirectories(Paths.get(outputFile));

		try (DumpReader dumpReader = new DumpReader(is, StandardCharsets.UTF_8,
				inputFile != null ? inputFile : "SYSTEMIN", logger, false) {
			int count = 0;

			@Override
			protected void processPage(Object mediaWiki, Object page_) throws Exception {
				++count;
				if (count % 20 == 0) {
					System.gc();
					System.out.println("gc..");
				}
				handle((PageType) page_);
			}

			@Override
			protected boolean processRevision(Object page_, Object revision_) throws Exception {
				PageType page = (PageType) page_;
				if (!namespaces.contains(page.getNs()))
					return false;

				if (targetTypes.stream().noneMatch(t -> t.accepts(page))) {
					return false;
				}
				if (!(revision_ instanceof RevisionType)) {
					// System.out.println("Filtered not revision");
					return false;
				}
				RevisionType revision = (RevisionType) revision_;
				if (revision.getText() != null) {
					boolean accepted = revision.getText().getDeleted() == null
							&& targetTypes.stream().anyMatch(t -> t.accepts(page) && t.accepts(page, revision));
					if (!accepted) {
						revision.setText(null);
					}
				}

				return super.processRevision(page_, revision_);
			}
		}) {
			dumpReader.unmarshal();
		}

	}

	private void handle(PageType page) throws Exception {
		if (!namespaces.contains(page.getNs()))
			return;
		
		if (targetTypes.stream().noneMatch(t -> t.accepts(page)))
			return;

		if (page.getRevisionOrUpload().isEmpty())
			return;

		System.out.println(page.getTitle());

		MyPageType t = new MyPageType(page);
		int acceptedCount = addRevisions(page, t);

		// Do we have at least one revision that potentially contains a target object
		if (acceptedCount > 0) {
			t.sortRevisions();
			writePage(t);
		}
	}

	private int addRevisions(PageType page, MyPageType t) {
		int acceptedCount = 0;
		for (Object o : page.getRevisionOrUpload()) {
			RevisionType revision = (RevisionType) o;
			if (revision.getText() != null) {
				++acceptedCount;
			}
			t.addRevison(new MyRevisionType(revision));
		}
		return acceptedCount;
	}

	protected void writePage(MyPageType page) throws Exception {
		Path revisionO = Paths.get(outputFile)
				.resolve(page.getId().longValue() + "-" + Util.makeSafeFileName(page.getTitle()) + ".page");
		pageIO.write(revisionO, page);
	}

}
