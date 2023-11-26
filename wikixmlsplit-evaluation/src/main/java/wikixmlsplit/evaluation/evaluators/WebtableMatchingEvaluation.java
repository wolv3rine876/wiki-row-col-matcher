package wikixmlsplit.evaluation.evaluators;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import wikixmlsplit.evaluation.bases.ObjectEvaluation;
import wikixmlsplit.evaluation.bases.PairEvaluation;
import wikixmlsplit.evaluation.data.PairEvaluationResult;
import wikixmlsplit.evaluation.gsreader.GSReaderWebtable;
import wikixmlsplit.matching.Matcher;
import wikixmlsplit.matching.MatcherBuilder;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.ranking.SimilarityRankingNoHistory;
import wikixmlsplit.matching.similarity.CachingSimilarity;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;
import wikixmlsplit.matching.similarity.SchemaObjectSchemaBoWSimilarity;
import wikixmlsplit.webtable.Webtable;
import wikixmlsplit.webtable.io.Revision;
import wikixmlsplit.webtable.io.WebpageIO;
import wikixmlsplit.webtable.io.WebpageRevision;
import wikixmlsplit.webtable.position.WebtablePosition;
import wikixmlsplit.webtable.ranking.HeaderRanking;
import wikixmlsplit.webtable.ranking.SchemaRanking;
import wikixmlsplit.webtable.similarity.WebtableAnchoredSimilarity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WebtableMatchingEvaluation {
	protected final Gson g = new GsonBuilder().setPrettyPrinting().create();

	protected FileWriter w;
	protected JsonWriter jw;


	@Parameter(names = "-input", description = "Input files")
	protected String inputFiles;

	@Parameter(names = "-inputGold", description = "Input gold standard")
	protected String inputGold;

	@Parameter(names = "-output", description = "Output files")
	protected String outputFile;
	
	@Parameter(names = "-inputSubjectColumn", description = "Input subject column")
	private String subjectColumnInput = "subjectColumn/outputWT.json";

	@Parameter(names = "-best")
	private boolean best;

	private CachingSimilarity<Webtable> simAnchored;


	public void run() throws IOException, ParseException {
		if (outputFile != null) {
			w = new FileWriter(outputFile);
			jw = new JsonWriter(w);
		}

		Map<String, Integer> subjectColumns = SubjectColumnLoader.loadSubjectColumns(Paths.get(subjectColumnInput));
		System.out.println("loaded " + subjectColumns.size() + " subject columns");
		
		simAnchored = new CachingSimilarity<>(new WebtableAnchoredSimilarity(subjectColumns),
				"simAnchoredCached");

		File dir = new File(inputFiles);
		if (dir.isDirectory()) {
			File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
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
			jw.close();
			w.close();
		}
	}

	public void run(Path parsed) throws IOException, ParseException {
		Path inputFolder = Paths.get(inputGold).resolve(parsed.getFileName());

		if (!inputFolder.toFile().exists()) {
			System.err.println("Did not find gold standard for " + parsed.getFileName());
			return;
		}
		System.out.println("Handling: " + parsed.getFileName().toString());

		evaluate(parsed, inputFolder);
	}

	public static void main(String[] args) throws IOException, ParseException {

		WebtableMatchingEvaluation main = new WebtableMatchingEvaluation();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}

	protected void evaluate(Path page, Path inputFolder) throws ParseException, IOException {
		String pageTitle = page.getFileName().toString();
		List<Revision> revisions = WebpageIO.readWebpageRevisions(page);

		Matching<WebtablePosition> matchingGold = GSReaderWebtable.loadMatching(inputFolder, revisions);
		List<ObjectStore<Webtable>> stores = new ArrayList<>();

		List<String> configs = new ArrayList<>();

		createConfigs(stores, configs);
		List<Matching<WebtablePosition>> matchings = new ArrayList<>();
		for (int i = 0; i < stores.size() ; ++i)
			matchings.add(new Matching<>());

		AtomicInteger revisionsCount = new AtomicInteger(0);
		AtomicInteger tableRevisions = new AtomicInteger(0);
		AtomicInteger obviousMatches = new AtomicInteger(0);
		AtomicInteger maximumObjectCount = new AtomicInteger(0);
		List<Webtable> prev = new ArrayList<>();
		for (Revision r : revisions) {
			WebpageRevision wr = new WebpageRevision(r.getMetadata().get(2), new BigInteger(r.getMetadata().get(1)),
					new SimpleDateFormat("yyyyMMddHHmmss").parse(r.getMetadata().get(1)).toInstant());
			List<Webtable> tables = WebpageIO.constructWebTables(r);
			revisionsCount.incrementAndGet();
			tableRevisions.addAndGet(tables.size());
			maximumObjectCount.getAndAccumulate(tables.size(), Math::max);

			int equalCount = 0;
			for (int pos = 0; pos < tables.size(); ++pos) {
				boolean equal = prev.size() > pos && Objects.equal(prev.get(pos), tables.get(pos));
				if (equal)
					++equalCount;
			}
			if (equalCount >= tables.size() - 1) {
				obviousMatches.addAndGet(equalCount);
			}

			for (int i = 0; i < stores.size() - 1; ++i) {
				final Matching<WebtablePosition> m = matchings.get(i);
				stores.get(i).handleNewRevision(tables, (tracked) -> {
					if (!tracked.isActive()) {
						return;
					}
					m.add(tracked.getIdentifier(), r.getId(), (WebtablePosition) tracked.getCurrentPosition(),
							!Objects.equal(tracked.getPrevObject(), tracked.getObject()));

				}, wr, WebtablePosition.DEFAULT_MAPPER);
			}
			
			for (int pos = 0; pos < tables.size(); ++pos) {
				boolean equal = prev.size() > pos && Objects.equal(prev.get(pos), tables.get(pos));
				if(equal) ++equalCount;

				matchings.get(matchings.size() - 1).add("pos" + pos, r.getId(), new WebtablePosition(pos), !equal);
			}
			

			prev.clear();
			prev.addAll(tables);
		}

		for (int i = 0; i < stores.size(); ++i) {

			PairEvaluationResult res = new PairEvaluation<WebtablePosition>().measure(matchingGold, matchings.get(i));
			
			List<ObjectEvaluation.ObjectEvaluationResult> resObject = new ObjectEvaluation<WebtablePosition>().measure(matchingGold, matchings.get(i));

			Map<String, Object> result = new HashMap<>(res.getResultMap());
			result.put("pagename", pageTitle);
			result.put("objectEvaluation", resObject);
			result.put("config", configs.get(i));
			result.put("matchTime", stores.get(i).getMatchTime());
			result.put("revisionCount", revisionsCount.get());
			result.put("tableRevisions", tableRevisions.get());
			result.put("obviousMatches", obviousMatches.get());
			result.put("clusterCount", matchingGold.getClusterCount());
			result.put("maximumObjectCount", maximumObjectCount.get());
			g.toJson(result, Map.class, jw);
			try {
				w.append("\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		w.flush();

	}

	protected void createConfigs(List<ObjectStore<Webtable>> stores, List<String> configs) {
		if(!best) {
			double[] limits = new double[] { 0.2d, 0.4d, 0.6d, 0.8d };
			double[] relaxLimits = new double[] { 0.95d };

			for (double l1 : new double[] { 0.99d }) {
				for (double l2 : limits) {
					for (double l3 : limits) {
						for (double rL : relaxLimits) {
							if (l1 <= 0.8d || l2 <= 0.8d || l3 <= 0.8d) {
								stores.add(getObjectStore(l1, l2, l3, rL, false));
								configs.add(l1 + "," + l2 + "," + l3 + "," + rL + ",OURAPPROACH-HUNGARIAN");
							}
						}
					}
				}
			}

			for (double l2 : new double[] { 0.001d, 0.2d, 0.4d, 0.6d, 0.8d }) {
				for (double rL : relaxLimits) {
					stores.add(getObjectStore2(l2, rL));
					configs.add(l2 + "," + l2 + "," + l2 + "," + rL + ",SCHEMA");

				}
			}


			for (double l1 : new double[] { 0.001d, 0.05d, 0.1d, 0.2d, 0.4d, 0.6d, 0.8d }) {
				for (double rL : new double[] {1.0d}) {
					if (l1 <= 1.0d) {
						stores.add(getObjectStoredAnchored(l1, rL));
						configs.add(l1 + "," + 0 + "," + 0 + "," + rL + ",ANCHORED");
					}
				}
			}
		} else {
			double l1 = 0.99d, l2 = 0.6d, l3 = 0.4d, rL = 0.95d;
			stores.add(getObjectStore(l1, l2, l3, rL, false));
			configs.add(l1 + "," + l2 + "," + l3 + "," + rL + ",OURAPPROACH-HUNGARIAN");

			l1 = 0.1d;
			stores.add(getObjectStore2(l1, rL));
			configs.add(l1 + "," + 0 + "," + 0 + "," + rL + ",SCHEMABASELINE");

			l1 = 0.1d;
			stores.add(getObjectStoredAnchored(l1, rL));
			configs.add(l1 + "," + 0 + "," + 0 + "," + rL + ",ANCHORED");
		}


		stores.add(getObjectStore(0, 0, 0, 0, true));
		configs.add(0 + "," + 0 + "," + 0 + "," + 0 + ",POSITION");
	}

	private static MatchingObjectBoWSimilarityWeighted sim = new MatchingObjectBoWSimilarityWeighted(true);
	private static MatchingObjectBoWSimilarityWeighted sim2 = new MatchingObjectBoWSimilarityWeighted(false);

	protected ObjectStore<Webtable> getObjectStore(double limit1, double limit2, double limit3, double relaxLimit,
												   boolean greedy) {
		return ObjectStore.createDefault(sim, sim2, limit1, limit2, limit3, relaxLimit, greedy);
	}
	protected ObjectStore<Webtable> getObjectStoredAnchored(double limit1, double relaxLimit) {

		List<Matcher<Webtable>> matchers = new ArrayList<>();

		matchers.add(new MatcherBuilder<Webtable>()
				.addRanking(new SimilarityRankingNoHistory<>(simAnchored, limit1, relaxLimit))
				.addRanking(new SchemaRanking()).addRanking(new HeaderRanking()).createMatcher(true));
		return new ObjectStore<>(matchers);
	}
	

	private static SchemaObjectSchemaBoWSimilarity simSchema = new SchemaObjectSchemaBoWSimilarity(true);
	
	private ObjectStore<Webtable> getObjectStore2(double limit2, double relaxLimit) {

		List<Matcher<Webtable>> matchers = new ArrayList<>();
		if (limit2 <= 1.0d)
			matchers.add(MatcherBuilder.createDefaultSimMatcher(simSchema, limit2, relaxLimit));
		return new ObjectStore<>(matchers);
	}

}
