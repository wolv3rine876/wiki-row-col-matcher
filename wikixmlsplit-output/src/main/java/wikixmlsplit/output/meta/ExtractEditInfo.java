package wikixmlsplit.output.meta;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.sweble.wikitext.dumpreader.DumpReader;
import org.sweble.wikitext.dumpreader.export_0_10.PageType;
import org.sweble.wikitext.dumpreader.export_0_10.RevisionType;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

public class ExtractEditInfo {

	@Parameter(names = "-input", description = "Input file")
	protected String inputFile;
	@Parameter(names = "-output", description = "Output folder")
	protected String outputFile;

	protected Writer w;

	private static final Logger logger = Logger.getLogger(ExtractEditInfo.class.getName());

	public static void main(String[] args) throws Exception {

		ExtractEditInfo main = new ExtractEditInfo();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}

	protected void run() throws Exception {
		InputStream is = System.in;
		if (inputFile != null) {
			Path inputPath = Paths.get(inputFile);
			is = new FileInputStream(inputPath.toFile());
		}
		if (outputFile != null) {
			// Files.createDirectories(Paths.get(outputFile).toAbsolutePath().getParent());
			w = Files.newBufferedWriter(Paths.get(outputFile));
		}
		try (DumpReader dumpReader = new DumpReader(is, StandardCharsets.UTF_8,
				inputFile != null ? inputFile : "SYSTEMIN", logger, false) {

			@Override
			protected void processPage(Object mediaWiki, Object page_) throws Exception {
				handle((PageType) page_);
			}

			@Override
			protected boolean processRevision(Object page_, Object revision_) throws Exception {
				PageType page = (PageType) page_;
				if (!(page.getNs().equals(BigInteger.valueOf(0)))) {
					return false;
				}
				if (!(revision_ instanceof RevisionType)) {
					return false;
				}
				return super.processRevision(page_, revision_);
			}
		}) {
			dumpReader.unmarshal();
		} finally {
			w.close();
		}

	}

	private void handle(PageType page) throws Exception {
		// only handle real articles
		if (!page.getNs().equals(BigInteger.valueOf(0)))
			return;

		Set<RevisionType> revertedRevisions = new HashSet<>();
		ListIterator<Object> iter = page.getRevisionOrUpload().listIterator();
		String hash = null;
		while (iter.hasNext()) {
			RevisionType revision = (RevisionType) iter.next();
			long dLimit = revision.getTimestamp().normalize().toGregorianCalendar().getTime().getTime()
					+ 1000 * 60 * 60 * 24;
			boolean foundSameHash = false;
			for (int i = iter.nextIndex(); i < page.getRevisionOrUpload().size(); ++i) {
				RevisionType revision2 = (RevisionType) page.getRevisionOrUpload().get(i);
				if (revision2.getTimestamp().normalize().toGregorianCalendar().getTime().getTime() > dLimit)
					break;
				if (revision2.getSha1().equals(hash)) {
					foundSameHash = true;
					break;
				}
			}

			if (foundSameHash) {
				revertedRevisions.add(revision);
				for (int i = iter.nextIndex(); i < page.getRevisionOrUpload().size(); ++i) {
					RevisionType revision2 = (RevisionType) page.getRevisionOrUpload().get(i);
					if (revision2.getTimestamp().normalize().toGregorianCalendar().getTime().getTime() > dLimit)
						break;
					revertedRevisions.add(revision2);
					if (revision2.getSha1().equals(hash)) {
						break;
					}
				}
			}
			hash = revision.getSha1();
		}

		StringBuilder builder = new StringBuilder();
		for (Object o : page.getRevisionOrUpload()) {
			RevisionType revision = (RevisionType) o;

			if (revision.getContributor().getId() != null) {
				builder.append(revision.getContributor().getUsername().replaceAll("\t", " "));
				builder.append("\t");
			} else if (revision.getContributor().getIp() != null) {
				builder.append(revision.getContributor().getIp().replaceAll("\t", " "));
				builder.append("\t");
			} else {
				builder.append("UNKNOWN");
				builder.append("\t");
			}
			Timestamp date = new java.sql.Timestamp(revision.getTimestamp().toGregorianCalendar().getTime().getTime());
			builder.append(date.toString());
			builder.append("\t");
			builder.append(page.getTitle().replaceAll("\t", " "));
			builder.append("\t");
			builder.append(revertedRevisions.contains(revision));
			builder.append("\n");
		}
		w.write(builder.toString());
	}

}
