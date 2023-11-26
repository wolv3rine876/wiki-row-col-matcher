package wikixmlsplit.webtable.io;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multiset;
import com.google.gson.JsonSyntaxException;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;
import wikixmlsplit.webtable.Webtable;
import wikixmlsplit.webtable.position.WebtablePosition;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Parser {
	@Parameter(names = "-input", description = "Input files")
	protected String inputFiles;

	@Parameter(names = "-output", description = "Output files")
	protected String outputFile;

	public static void main(String[] args) throws IOException, ParseException {
		Parser main = new Parser();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.run();
	}

	Writer overviewWriter;

	public void run() throws IOException, ParseException {

		File dir = new File(inputFiles);
		if (dir.isDirectory()) {
			File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
			if (files == null) {
				System.err.println("input is no valid directory!");
				return;
			}
			overviewWriter = Files.newBufferedWriter(Paths.get(outputFile + "/" + "pages.html"));
			overviewWriter.write(
					"<html><table><tr><th>URL</th><th>Revision count</th><th>First date</th><th>Last date</th><th>Link</th></tr>");
			for (File parsed : files) {
				try {
					run(parsed.toPath());
				} catch (JsonSyntaxException ex) {
					System.err.println("failed to parse: " + parsed);
				}
			}
			System.out.println(counts);
			System.out.println(counts.size());
			System.out.println(totalChangeSize);
			overviewWriter.write("</table></html>");
			overviewWriter.close();
		} else if (dir.toPath().toString().endsWith(".json")) {
			run(dir.toPath());
		} else {
			System.err.println("Invalid input!");
		}
	}

	Multiset<Integer> counts = HashMultiset.create();

	public void run(Path parsed) throws IOException, ParseException {

		List<Revision> revisions = WebpageIO.readWebpageRevisions(parsed);
		if (revisions.size() > 1) {
			Matching<WebtablePosition> matching = new Matching<>();
			ObjectStore<Webtable> store = getObjectStore(0.99d, 0.8d, 0.4d, 0.95d);

			for (Revision r : revisions) {
				List<Webtable> webtables = WebpageIO.constructWebTables(r);

				WebpageRevision wr = new WebpageRevision(r.getMetadata().get(2), new BigInteger(r.getMetadata().get(1)),
						new SimpleDateFormat("yyyyMMddHHmmss").parse(r.getMetadata().get(1)).toInstant());

				BigInteger revId = new BigInteger(r.getMetadata().get(1));
				store.handleNewRevision(webtables, (tracked) -> {
					if (!tracked.isActive()) {
						return;
					}
					matching.add(tracked.getIdentifier(), revId, (WebtablePosition) tracked.getCurrentPosition(),
							tracked.getPrevObject() != null
									&& !Objects.equal(tracked.getPrevObject(), tracked.getObject()));

				}, wr, WebtablePosition.DEFAULT_MAPPER);

			}

			output(parsed, revisions, matching);

			if (overviewWriter != null) {
				overviewWriter.write("<tr><td>" + revisions.get(0).getMetadata().get(2) + "</td><td>" + revisions.size()
						+ "</td><td>" + revisions.get(0).getMetadata().get(1) + "</td><td>"
						+ revisions.get(revisions.size() - 1).getMetadata().get(1) + "</td><td><a href='"
						+ parsed.getFileName() + "/clusters.html'>Link</a></td></tr>");
			}
		}

	}



	private void output(Path parsed, List<Revision> revisions, Matching<WebtablePosition> matching)
			throws IOException, ParseException {
		Path outputFolder = Paths.get(outputFile).resolve(parsed.getFileName() + "/");
		Files.createDirectories(outputFolder);
		final DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

		HashMap<String, Multiset<String>> captions = new HashMap<>();
		HashMap<String, Multiset<String>> sections = new HashMap<>();

		try (Writer revisionWriter = Files.newBufferedWriter(outputFolder.resolve("revisions.csv"))) {

			ImmutableListMultimap<BigInteger, Match<WebtablePosition>> revMatching = matching.getRevisions();

			for (Revision r : revisions) {
				WebpageRevision wr = new WebpageRevision(r.getMetadata().get(2), new BigInteger(r.getMetadata().get(1)),
						new SimpleDateFormat("yyyyMMddHHmmss").parse(r.getMetadata().get(1)).toInstant());
				List<Match<WebtablePosition>> matches = revMatching.get(wr.getId());

				try {
					revisionWriter.append(String.valueOf(wr.getId())).append(",").append(String.valueOf(wr.getInstant()))
							.append(",").append(String.valueOf(matches.size())).append("\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				List<Webtable> webtables = WebpageIO.constructWebTables(r);
				for (Match<WebtablePosition> match : matches) {
					Webtable tab = webtables.get(match.getPosition().getIndex());

					String html = tab.getElement().outerHtml();
					Path folder = outputFolder.resolve(match.getClusterIdentifier() + "/");
					Files.createDirectories(folder);
					Writer w = Files.newBufferedWriter(folder.resolve(df.format(wr.getInstant()) + "-"
							+ wr.getId().toString() + "-" + match.getPosition().getPositionString() + ".html"));

					if (tab.getCaption() != null && !tab.getCaption().isEmpty())
						w.write("<p>Caption:" + tab.getCaption() + "</p>");
					if (tab.getHeadings() != null && !tab.getHeadings().isEmpty())
						w.write("<p>Headings:" + tab.getHeadings() + "</p>");
					w.write(html);
					w.close();

					if (tab.getCaption() != null && !tab.getCaption().isEmpty()) {
						captions.computeIfAbsent(match.getClusterIdentifier(), (a) -> HashMultiset.create())
								.add(tab.getCaption());
					}

					if (tab.getHeadings() != null && !tab.getHeadings().isEmpty()) {
						sections.computeIfAbsent(match.getClusterIdentifier(), (a) -> HashMultiset.create())
								.add(tab.getHeadings());
					}
				}
			}
		}

		rename(outputFolder, captions, sections);

		totalChangeSize += matching.getObjectChangeCount();
		System.out.println(matching.getObjectChangeCount() + "/" + matching.getDecisionSize());
		counts.add(revisions.size());
	}

	private void rename(Path outputFolder, HashMap<String, Multiset<String>> captions,
			HashMap<String, Multiset<String>> sections) {
		HashSet<String> keys = new HashSet<>(captions.keySet());
		keys.addAll(sections.keySet());

		for (String i : keys) {
			String maxCaption = getMostCommonElement(captions.get(i));
			String maxName = getMostCommonElement(sections.get(i));

			String newName = maxCaption + (maxCaption.isEmpty() || maxName.isEmpty() ? "" : " - ") + maxName;

			newName = newName.substring(0, Math.min(60, newName.length()));
			String fileName = Util.makeSafeFileName(i + " -" + newName);
			File dir = outputFolder.resolve(i + "/").toFile();

			if (!dir.isDirectory())
				continue;
			File newDir = new File(dir.getParent() + "/" + fileName);
			dir.renameTo(newDir);
		}

	}

	private String getMostCommonElement(Multiset<String> set) {
		int maxCount = 0;
		String maxCaption = "";
		if (set != null) {
			for (Multiset.Entry<String> e : set.entrySet()) {
				if (e.getCount() >= maxCount) {
					maxCaption = e.getElement();
					maxCount = e.getCount();
				}
			}
		}
		return maxCaption;
	}

	private int totalChangeSize = 0;

	private static MatchingObjectBoWSimilarityWeighted sim = new MatchingObjectBoWSimilarityWeighted(true);
	private static MatchingObjectBoWSimilarityWeighted sim2 = new MatchingObjectBoWSimilarityWeighted(false);

	private ObjectStore<Webtable> getObjectStore(double limit1, double limit2, double limit3, double relaxLimit) {
		return ObjectStore.createDefault(sim, sim2, limit1, limit2, limit3, relaxLimit, false);
	}
}
