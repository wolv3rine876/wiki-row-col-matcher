package wikixmlsplit.evaluation.evaluators;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import wikixmlsplit.evaluation.bases.ObjectEvaluation;
import wikixmlsplit.evaluation.bases.ObjectEvaluation.ObjectEvaluationResult;
import wikixmlsplit.evaluation.bases.PairEvaluation;
import wikixmlsplit.evaluation.data.MatchingError;
import wikixmlsplit.evaluation.data.MatchingError.ErrorType;
import wikixmlsplit.evaluation.data.PairEvaluationResult;
import wikixmlsplit.matching.*;
import wikixmlsplit.matching.position.Position;
import wikixmlsplit.matching.ranking.ActiveTimeRanking;
import wikixmlsplit.matching.ranking.PositionRanking;
import wikixmlsplit.matching.ranking.SimilarityRankingNoHistory;
import wikixmlsplit.matching.ranking.SimilarityRankingPositionRestricted;
import wikixmlsplit.matching.similarity.*;
import wikixmlsplit.socrata.SocrataRevision;
import wikixmlsplit.socrata.SocrataTable;
import wikixmlsplit.socrata.columnorder.OrderedSchema;
import wikixmlsplit.socrata.input.DatasetChangeSummary;
import wikixmlsplit.socrata.input.MetaData;
import wikixmlsplit.socrata.input.SocrataInputTable;
import wikixmlsplit.socrata.keys.KeySelection;
import wikixmlsplit.socrata.keys.SimpleUniqueDiscovery;
import wikixmlsplit.socrata.keys.UCCDiscovery;
import wikixmlsplit.socrata.position.SocrataPosition;
import wikixmlsplit.socrata.ranking.SocrataSchemaRanking;
import wikixmlsplit.socrata.similarity.SocrataTableAnchoredSimilarity;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class SocrataMatchingEvaluation {
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Parameter(names = "-input")
	private String input;

	@Parameter(names = "-inputChangeSummary")
	private String inputChangeSummary;

	@Parameter(names = "-inputMeta")
	private String inputMeta;

	@Parameter(names = "-inputOrderedSchema")
	private String inputOrderedSchema;

	@Parameter(names = "-attrNameWeight")
	private double attrNameWeight;

	@Parameter(names = "-fileEnding")
	private String fileEnding = ".json?";

	@Parameter(names = "-filterFile")
	private String filterFile;

	@Parameter(names = "-outputResult")
	private String outputResultFile;

	@Parameter(names = "-outputError")
	private String outputErrorFile;

	@Parameter(names = "-ignoreEmpty")
	private boolean ignoreEmpty;

	@Parameter(names = "-sortRows")
	private boolean sortRows;

	@Parameter(names = "-best")
	private boolean best;

	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
	private Map<String, Writer> writers = new HashMap<>();

	private ImmutableMap<String, DatasetChangeSummary> summaryMap;

	public static void main(String[] args) throws IOException {
		SocrataMatchingEvaluation main = new SocrataMatchingEvaluation();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}

	public void run() throws IOException {

		Path changeSummaryPath = Paths.get(inputChangeSummary);
		List<DatasetChangeSummary> changeSummaries = Files.readAllLines(changeSummaryPath).stream()
				.map(line -> gson.fromJson(line, DatasetChangeSummary.class)).collect(Collectors.toList());

		if (filterFile != null) {
			List<String> filter = Files.readAllLines(Paths.get(filterFile));

			Set<String> keep = new HashSet<>();
			for (String s : filter) {
				String[] parts = s.split("/");
				keep.add(parts[parts.length - 1].substring(0, 9));
			}
			changeSummaries.removeIf(s -> !keep.contains(s.getId()));
		}

		loadVersions();

		for (DatasetChangeSummary summary : changeSummaries) {
			summary.getVersionsWithChanges().retainAll(versions);
			summary.getDeletions().retainAll(versions);
		}

		Map<String, OrderedSchema> schemas = Collections.emptyMap();
		if (inputOrderedSchema != null) {
			schemas = Files.readAllLines(Paths.get(inputOrderedSchema)).stream()
					.map(line -> gson.fromJson(line, OrderedSchema.class))
					.collect(Collectors.toMap(OrderedSchema::getId, i -> i));
		}

		summaryMap = Maps.uniqueIndex(changeSummaries, DatasetChangeSummary::getId);

		Map<String, List<String>> updates = new HashMap<>();
		Map<String, List<String>> deletions = new HashMap<>();

		for (DatasetChangeSummary sum : changeSummaries) {
			for (String update : sum.getVersionsWithChanges()) {
				updates.computeIfAbsent(update, (a) -> new ArrayList<>()).add(sum.getId());
			}
			for (String deletion : sum.getDeletions()) {
				deletions.computeIfAbsent(deletion, (a) -> new ArrayList<>()).add(sum.getId());
			}
		}

		List<ObjectStore<SocrataTable>> stores = new ArrayList<>();

		List<String> configs = new ArrayList<>();

		Map<String, List<Integer>> keys = new HashMap<>();
		simAnchored = new CachingSimilarity<>(new SocrataTableAnchoredSimilarity(keys));

		createConfigs(stores, configs);
		List<Matching<SocrataPosition>> matchings = new ArrayList<>();
		for (int i = 0; i < stores.size(); ++i)
			matchings.add(new Matching<>());

		Matching<SocrataPosition> gold = new Matching<>();

		Map<String, SocrataTable> currentVersions = new HashMap<>();
		for (int i = 0; i < versions.size(); i++) {
			String dateString = versions.get(i);
			System.out.println("Handling date: " + dateString);
			SocrataRevision rev = getRevision(i, dateString);

			Map<String, MetaData> metaDataMap = loadMetaData(dateString);
			System.out.println(metaDataMap.size());

			currentVersions.keySet().removeAll(deletions.getOrDefault(dateString, Collections.emptyList()));

			int missing = 0;
			for (String id : updates.getOrDefault(dateString, Collections.emptyList())) {
				File jsonFile = new File(input + "/" + dateString + "/" + id + fileEnding);
				if (jsonFile.exists()) {
					SocrataTable table = getTable(dateString, id, metaDataMap.get(id));

					if (ignoreEmpty && table.isEmpty()) {
						System.out.println("Skipping " + id + " because it is empty.");
						currentVersions.remove(id);
						continue;
					} else {
						currentVersions.put(id, table);
					}

					UCCDiscovery discovery = new SimpleUniqueDiscovery();
					KeySelection selection = new KeySelection(table, schemas.get(id));
					List<List<Integer>> uniqueColumns = discovery.discover(table);

					if (uniqueColumns.isEmpty()) {
						System.err.println("No key found: " + id);
					} else {
						List<Integer> max = uniqueColumns.get(0);
						double scoreMax = 0.0d;
						for (List<Integer> unique : uniqueColumns) {
							double score = selection.getScore(unique);
							if (score > scoreMax) {
								scoreMax = score;
								max = unique;
							}
						}
						keys.put(id, max);
					}

				} else {
					System.err.println("Missing file " + jsonFile);
					++missing;
				}
			}
			System.out.println("Missing " + missing + " out of "
					+ updates.getOrDefault(dateString, Collections.emptyList()).size() + " files");

			ArrayList<SocrataTable> list = new ArrayList<>(currentVersions.size());
			int pos = 0;
			Set<String> changed = new HashSet<>(updates.getOrDefault(dateString, Collections.emptyList()));
			List<Entry<String, SocrataTable>> entrySet = new ArrayList<>(currentVersions.entrySet());
			Collections.shuffle(entrySet);
			for (Entry<String, SocrataTable> e : entrySet) {
				list.add(e.getValue());
				gold.add(e.getKey(), rev.getId(), new SocrataPosition(pos++), changed.contains(e.getKey()));
			}

			for (int j = 0; j < stores.size(); ++j) {
				final Matching<SocrataPosition> m = matchings.get(j);
				stores.get(j).handleNewRevision(list, (tracked) -> {
					if (!tracked.isActive()) {
						return;
					}
					m.add(tracked.getIdentifier(), rev.getId(), (SocrataPosition) tracked.getCurrentPosition(),
							!Objects.equal(tracked.getPrevObject(), tracked.getObject()));

				}, rev, SocrataPosition.DEFAULT_MAPPER);
			}

		}

		if (outputResultFile != null) {
			try (FileWriter w = new FileWriter(outputResultFile); JsonWriter jw = new JsonWriter(w)) {

				for (int i = 0; i < stores.size(); ++i) {

					PairEvaluationResult res = new PairEvaluation<SocrataPosition>().measure(gold, matchings.get(i));
					List<ObjectEvaluationResult> resObject = new ObjectEvaluation<SocrataPosition>().measure(gold,
							matchings.get(i));

					Map<String, Object> result = new HashMap<>(res.getResultMap());
					result.put("objectEvaluation", resObject);
					result.put("config", configs.get(i));
					result.put("matchTime", stores.get(i).getMatchTime());
					result.put("revisionCount", versions.size());
					result.put("clusterCount", gold.getClusterCount());

					gson.toJson(result, Map.class, jw);
					try {
						w.append("\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		if (outputErrorFile != null) {
			outputErrors(gold, stores, configs, matchings, new Matching<>());
		}
	}

	protected Map<String, MetaData> loadMetaData(String dateString) throws IOException {
		Map<String, MetaData> metaDataMap = new HashMap<>();
		Path metaPath = Paths.get(inputMeta).resolve(dateString);
		for (File f : metaPath.toFile().listFiles(File::isFile)) {
			for (MetaData m : gson.fromJson(Files.newBufferedReader(f.toPath()), MetaData[].class)) {
				if (summaryMap.containsKey(m.resource.id))
					metaDataMap.put(m.resource.id, m);
			}
		}
		return metaDataMap;
	}

	protected void loadVersions() {
		Path inputDir = Paths.get(input);
		versions = Arrays.stream(inputDir.toFile().listFiles(f -> !f.isFile())).map(File::getName)
				.collect(Collectors.toList());
		Collections.sort(versions);
	}

	protected <P extends Position> void outputErrors(Matching<P> matchingGold, List<ObjectStore<SocrataTable>> stores,
			List<String> configs, List<Matching<P>> matchings, Matching<P> matchingTrivial) throws IOException {
		Matching<P> positionBaseline = new Matching<>();
		for (int i = 0; i < stores.size(); ++i) {

			getFileWriter(configs.get(i)).append("<h1>Page</h1>");
			List<MatchingError> res = new PairEvaluation<P>().getErrors(matchingGold, matchings.get(i),
					positionBaseline, matchingTrivial);
			for (MatchingError error : res) {
				if (error.getType() == ErrorType.TP)
					continue;

				String object2 = error.getFilename2();
				Writer w = getFileWriter(configs.get(i)).append(String.valueOf(error)).append("<table><tr><td colspan=2>");
				render(w, object2, error.getClustername2Gold(), null);

				if (!error.getFilename1Output().isEmpty() && !error.getFilename1Gold().isEmpty()) {
					SocrataTable t0 = getTableFromFilename(error.getFilename2(), error.getClustername2Gold());
					SocrataTable t1 = getTableFromFilename(error.getFilename1Output(), error.getClustername1Gold());
					SocrataTable t2 = getTableFromFilename(error.getFilename1Gold(), error.getClustername2Gold());
					w.append("<p>Similarity: ").append(String.valueOf(sim1.getSimilarity(t1, t2))).append("</p>");

					w.append("<p>Similarity1: ").append(String.valueOf(sim1.getSimilarity(t1, t0))).append("</p>");
					w.append("<p>Similarity2: ").append(String.valueOf(sim1.getSimilarity(t2, t0))).append("</p>");
				}

				w.append("</td></tr><tr><td><h3>Output:</h3>");
				if (!error.getFilename1Output().isEmpty()) {
					String object1Output = error.getFilename1Output();

					render(w, object1Output, error.getClustername1Gold(), object2);
				}
				w.append("</td><td><h3>Gold:</h3>");
				if (!error.getFilename1Gold().isEmpty()) {
					String object1Gold = error.getFilename1Gold();
					render(w, object1Gold, error.getClustername2Gold(), object2);
				}
				w.append("</td></tr></table>");
			}
		}
		for (Writer w : writers.values())
			w.close();
	}

	private void render(Writer w, String object1Output, String cluster, String object2) throws IOException {

		SocrataTable table = getTableFromFilename(object1Output, cluster);

		w.append(table.toHtml(10));
		if (object2 != null) {
//			w.append("<p>Similarity:" + sim.getSimilarity(object1Output, object2) + ";" + sim2.getSimilarity(object1Output, object2) + "</p>");

		}
	}

	protected SocrataTable getTableFromFilename(String filename, String cluster) throws IOException {
		DatasetChangeSummary summary = summaryMap.get(cluster);

		int version = Integer.parseInt(filename.split("-")[0]);
		String versionName = versions.get(version);
		String lastUpdate = summary.getVersionsWithChanges().stream().filter(s -> s.compareTo(versionName) <= 0)
				.reduce((first, second) -> second).get();

		return getTable(lastUpdate, cluster, loadMetaData(versionName).get(cluster));
	}

	private Writer getFileWriter(String s) {
		return writers.computeIfAbsent(s, (a) -> {

			try {
				Writer w = new FileWriter(
						Paths.get(outputErrorFile).toAbsolutePath().getParent().resolve(s + ".html").toFile());
				w.append("<html><head><link rel=\"stylesheet\" href=\"style.css\"></head><body>");
				return w;
			} catch (IOException e) {
				e.printStackTrace();
				throw new UncheckedIOException(e);
			}
		});
	}

	private void createConfigs(List<ObjectStore<SocrataTable>> stores, List<String> configs) {

		if (!best) {
			double[] limits = new double[] { 0.05d, 0.1d, 0.2d, 0.4d, 0.6d, 0.8d };
			double[] relaxLimits = new double[] { 0.95d };

			for (double l1 : new double[] { 0.99d }) {
				for (double l2 : limits) {
					for (double l3 : limits) {
						for (double rL : relaxLimits) {
							if (l1 <= 1.0d || l2 <= 1.0d || l3 <= 1.0d) {
								stores.add(getObjectStore(l1, l2, l3, rL, false));
								configs.add(l1 + "," + l2 + "," + l3 + "," + rL + ",OURAPPROACH");
							}
						}
					}
				}
			}

			createConfigsBaseline(stores, configs);
			createConfigsAnchoredBaseline(stores, configs);
		} else {
			double l1 = 0.99d, l2 = 0.6d, l3 = 0.4d, rL = 0.95d;
			stores.add(getObjectStore(l1, l2, l3, rL, false));
			configs.add(l1 + "," + l2 + "," + l3 + "," + rL + ",OURAPPROACH-HUNGARIAN");

			l1 = 0.1d;
			stores.add(getObjectStoreBaseline(l1, rL));
			configs.add(l1 + "," + 0 + "," + 0 + "," + rL + ",SCHEMABASELINE");

			l1 = 0.1d;
			stores.add(getObjectStoredAnchored(l1, rL));
			configs.add(l1 + "," + 0 + "," + 0 + "," + rL + ",ANCHORED");
		}
	}

	protected void createConfigsBaseline(List<ObjectStore<SocrataTable>> stores, List<String> configs) {
		// double[] limits = new double[] { 0.2d, 0.4d, 0.6d, 0.8d, 0.99d };
		double[] relaxLimits = new double[] { 0.95d };

		for (double l1 : new double[] { 0.001d, 0.05d, 0.1d, 0.2d, 0.4d, 0.6d, 0.8d }) {
			for (double rL : relaxLimits) {
				if (l1 <= 1.0d) {
					stores.add(getObjectStoreBaseline(l1, rL));
					configs.add(l1 + "," + 0 + "," + 0 + "," + rL + ",SCHEMABASELINE");
				}
			}
		}
	}

	protected ObjectStore<SocrataTable> getObjectStoreBaseline(double limit1,
															   double relaxLimit) {

		List<Matcher<SocrataTable>> matchers = new ArrayList<>();
		matchers.add(new MatcherHash<>());
		matchers.add(MatcherBuilder.createDefaultSimMatcher(simSchema, limit1, relaxLimit, false));
		return new ObjectStore<>(matchers);
	}

	protected void createConfigsAnchoredBaseline(List<ObjectStore<SocrataTable>> stores, List<String> configs) {
		// double[] limits = new double[] { 0.2d, 0.4d, 0.6d, 0.8d, 0.99d };
		double[] relaxLimits = new double[] { 1.00 };

		for (double l1 : new double[] { 0.001d, 0.05d, 0.1d, 0.2d, 0.4d, 0.6d, 0.8d }) {
			for (double rL : relaxLimits) {
				if (l1 <= 1.0d) {
					stores.add(getObjectStoredAnchored(l1, rL));
					configs.add(l1 + "," + 0 + "," + 0 + "," + rL + ",ANCHORED");
				}
			}
		}
	}

	protected ObjectStore<SocrataTable> getObjectStoredAnchored(double limit1, double relaxLimit) {

		List<Matcher<SocrataTable>> matchers = new ArrayList<>();
		matchers.add(new MatcherHash<>());
		matchers.add(new MatcherBuilder<SocrataTable>()
				.addRanking(new SimilarityRankingNoHistory<>(simAnchored, limit1, relaxLimit))
				.addRanking(new SocrataSchemaRanking()).createMatcher(true));
		return new ObjectStore<>(matchers);
	}

	private SimilarityMeasure<MatchingObject> sim1 = new MatchingObjectBoWSimilarityWeighted(true);
	private SimilarityMeasure<MatchingObject> sim2 = new MatchingObjectBoWSimilarityWeighted(false);

	private SimilarityMeasure<SchemaObject> simSchema = new SchemaObjectSchemaBoWSimilarity(true);

	private SimilarityMeasure<SocrataTable> simAnchored;

	private List<String> versions;

	protected ObjectStore<SocrataTable> getObjectStore(double limit1, double limit2, double limit3, double relaxLimit,
			boolean greedy) {

		List<Matcher<SocrataTable>> matchers = new ArrayList<>();
		matchers.add(new MatcherHash<>());

		if (limit1 <= 1.0d) {

			matchers.add(new MatcherBuilder<SocrataTable>()
					.addRanking(new SimilarityRankingPositionRestricted<>(sim1, limit1, relaxLimit, 2))
					.addRanking(new ActiveTimeRanking<>()).addRanking(new PositionRanking<>()).createMatcher(greedy));
		}
		if (limit2 <= 1.0d)
			matchers.add(MatcherBuilder.createDefaultSimMatcher(sim1, limit2, relaxLimit, greedy));
		if (limit3 <= 1.0d)
			matchers.add(MatcherBuilder.createDefaultSimMatcher(sim2, limit3, relaxLimit, greedy));
		return new ObjectStore<>(matchers);
	}

	private SocrataTable getTable(String date, String cluster, MetaData metaData) throws IOException {
		String jsonName = input + "/" + date + "/" + cluster + fileEnding;
		try (FileReader fr = new FileReader(jsonName)) {
			SocrataInputTable socrataTable = gson.fromJson(fr, SocrataInputTable.class);
			return socrataTable.toSocrataTable(attrNameWeight, sortRows, metaData);
		}

	}

	private SocrataRevision getRevision(int i, String versionString) {
		LocalDate version = LocalDate.parse(versionString, dateTimeFormatter);
		return new SocrataRevision(BigInteger.valueOf(i), version.atStartOfDay(ZoneOffset.UTC).toInstant());
	}

}
