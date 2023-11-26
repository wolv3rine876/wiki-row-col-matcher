package wikixmlsplit.parser;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngine;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.ParserConfigImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.parser.parser.LinkTargetException;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.datastructures.UniversalRevisionType;
import wikixmlsplit.io.PageIO;
import wikixmlsplit.parser.targets.TargetType;
import wikixmlsplit.parser.visitors.MyStripVisitor;
import wikixmlsplit.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Reads an input page file (created by wikixmlsplit.Main), parses the
 * revisions' content and extracts table nodes, writes those nodes to file.
 */
public class Reader {

	public static class Config {
		@Parameter(names = "-input", description = "Input file")
		protected String inputFile;
		@Parameter(names = "-type", description = "Content Type", required = true)
		protected List<TargetType> targetTypes;
		@Parameter(names = "-additionalType", description = "Additional type")
		protected List<TargetType> additionalTypes = Collections.emptyList();

		public void setInputFile(String inputFile) {
			this.inputFile = inputFile;
		}

		public void setTargetTypes(List<TargetType> targetType) {
			this.targetTypes = targetType;
		}

		public void setAdditionalTypes(List<TargetType> additionalTypes) {
			this.additionalTypes = additionalTypes;
		}
	}

	public static void main(String[] args) throws Exception {
		Config cfg = new Config();
		JCommander.newBuilder().addObject(cfg).build().parse(args);
		new Reader(cfg).run();
	}

	@Parameter(names = "-input", description = "Input file")
	protected String inputFile;

	@Parameter(names = "-redoExisting")
	private boolean redoExisting = false;

	private final WtEngine engine;
	private final WikiConfig config;
	private final Gson serializer = Util.getJsonSerializer();
	private final PageIO pageIO = new PageIO();

	private final MyStripVisitor stripVis = new MyStripVisitor(false, true, true);
	private final Cache<String, Map<String, List<String>>> nodes = CacheBuilder.newBuilder().maximumSize(20)
			.recordStats().build();

	private final List<TargetType> targetTypes;
	private final List<TargetType> additionalTypes;

	public Reader(Config cfg) {
		this.inputFile = cfg.inputFile;
		config = getConfig();
		engine = new WtEngineImpl(config);
		targetTypes = cfg.targetTypes;
		additionalTypes = cfg.additionalTypes;
	}

	protected void run() throws IOException {
		Path inputPath = Paths.get(inputFile);
		MyPageType page = pageIO.read(inputPath);
		handlePage(inputPath.getParent(), page);
	}

	public boolean handlePage(Path outputPath, MyPageType page) {
		Path revisionO = outputPath
				.resolve(page.getId().longValue() + "-" + Util.makeSafeFileName(page.getTitle()) + ".parsed");
		if (!redoExisting && Files.exists(revisionO)) {
			System.out.println("Skipping " + page.getTitle() + " (already exists)");
			return true;
		}

		PageTitle pageTitle;
		try {
			pageTitle = PageTitle.make(config, page.getTitle());
		} catch (LinkTargetException e1) {
			System.err.println("Error while creating page title: " + page.getTitle());
			e1.printStackTrace();
			return false;
		}

		nodes.invalidateAll();

		long time = System.nanoTime();
		boolean valid = true;
		List<UniversalRevisionType> revisions = new ArrayList<>(page.getRevisions().size());
		for (MyRevisionType o : page.getRevisions()) {
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("Interrupted " + page.getTitle());
				return false;
			}

			if (o.getText() == null) {
				revisions.add(new UniversalRevisionType(o));
				continue;
			}

			revisions.add(handleRevision(page, pageTitle, o));
		}
		page.setRevisions(revisions);
		System.out.println("Finished parsing " + page.getTitle() + ":" + (System.nanoTime() - time) / 1E9);

		try {
			writeParsedPage(outputPath, page);
		} catch (FileNotFoundException e) {
			System.err.println("Error while writing: " + page.getTitle());
			return false;
		}

		System.out.println(nodes.stats());
		return valid;
	}

	@SuppressWarnings("unchecked")
	private UniversalRevisionType handleRevision(MyPageType page, PageTitle pageTitle, MyRevisionType o2) {
		String revText = o2.getText().getValue();
		Map<String, List<String>> extractedProperties;
		try {
			extractedProperties = nodes.get(revText, () -> {
				Map<String, List<String>> result = new HashMap<>();
				Set<TargetType> accepted = new HashSet<>();
				for (TargetType t : targetTypes) {
					if (!t.accepts(page) || !t.accepts(revText))
						continue;
					accepted.add(t);
				}

				if (accepted.isEmpty())
					return result;

				for (TargetType t : additionalTypes) {
					if (!t.accepts(page) || !t.accepts(revText))
						continue;
					accepted.add(t);
				}

				EngProcessedPage node = engine.postprocess(new PageId(pageTitle, o2.getId().longValue()), revText,
						null);
				stripVis.visit(node);

				for (TargetType t : accepted) {
					result.put(t.name(), (List<String>) t.getVistor(config.getParserConfig(), serializer).go(node));
				}

				return result;
			});
		} catch (ExecutionException e) {
			e.printStackTrace();
			extractedProperties = Collections.singletonMap("exception", Collections.singletonList(e.toString()));
		}

		UniversalRevisionType o = new UniversalRevisionType(o2, extractedProperties);
		o.removeText();
		return o;
	}

	private void writeParsedPage(Path outputPath, MyPageType page) throws FileNotFoundException {
		Path revisionO = outputPath
				.resolve(page.getId().longValue() + "-" + Util.makeSafeFileName(page.getTitle()) + ".parsed");
		pageIO.write(revisionO, page);
	}

	private static WikiConfig getConfig() {
		final WikiConfig config = DefaultConfigEnWp.generate();
		ParserConfigImpl pc = (ParserConfigImpl) config.getParserConfig();
		pc.setGatherRtData(false);
		pc.setWarningsEnabled(false);
		return config;
	}

}
