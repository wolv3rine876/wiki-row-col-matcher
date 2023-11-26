package wikixmlsplit.evaluation.evaluators;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.evaluation.gsreader.GSReader;
import wikixmlsplit.evaluation.gsreader.GSReaderTable;
import wikixmlsplit.matching.Matcher;
import wikixmlsplit.matching.MatcherBuilder;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.ranking.SimilarityRankingNoHistory;
import wikixmlsplit.matching.similarity.CachingSimilarity;
import wikixmlsplit.matching.similarity.SchemaObjectSchemaBoWSimilarity;
import wikixmlsplit.matching.similarity.SimilarityMeasure;
import wikixmlsplit.wikitable.WikiTable;
import wikixmlsplit.wikitable.builder.TableBuilder;
import wikixmlsplit.wikitable.position.TablePosition;
import wikixmlsplit.wikitable.ranking.HeaderRanking;
import wikixmlsplit.wikitable.ranking.SchemaRanking;
import wikixmlsplit.wikitable.similarity.TableAnchoredSimilarity;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

public class TimeResolutionEvaluationTable extends TimeResolutionEvaluation<WikiTable, TablePosition> {

	private static SchemaObjectSchemaBoWSimilarity simSchema = new SchemaObjectSchemaBoWSimilarity(true);

	private static SimilarityMeasure<WikiTable> simAnchored;

	private TableBuilder tableBuilder = new TableBuilder(false);
	@Parameter(names = "-inputSubjectColumn", description = "Input subject column")
	private String subjectColumnInput = "subjectColumn/output.json";

	public static void main(String[] args) throws IOException {

		TimeResolutionEvaluationTable main = new TimeResolutionEvaluationTable();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}

	@Override
	protected void initSims() throws IOException {
		super.initSims();

		Map<String, Integer> subjectColumns = SubjectColumnLoader.loadSubjectColumns(Paths.get(subjectColumnInput));
		System.out.println("loaded " + subjectColumns.size() + " subject columns");


		simAnchored = new CachingSimilarity<>(new TableAnchoredSimilarity(subjectColumns),
				"simAnchoredCached");
	}

	@Override
	protected void createConfigs(List<ObjectStore<WikiTable>> stores, List<String> configs) {
		super.createConfigs(stores, configs);
		createConfigsBaseline(stores, configs);
		createConfigsAnchoredBaseline(stores, configs);
//		createConfigsJaro(stores, configs);
	}

	@Override
	protected void createBestConfig(List<ObjectStore<WikiTable>> stores, List<String> configs) {
		double l1 = 0.99d, l2 = 0.6d, l3 = 0.4d, rL = 0.95d;
		stores.add(getObjectStore(l1, l2, l3, rL));
		configs.add(l1 + "," + l2 + "," + l3 + "," + rL + ",OURAPPROACH-HUNGARIAN");

		if (!errorOutput) {
			l1 = 0.1d;
			stores.add(getObjectStoreBaseline(l1, rL));
			configs.add(l1 + "," + 0 + "," + 0 + "," + rL + ",SCHEMABASELINE");

			l1 = 0.1d;
			stores.add(getObjectStoredAnchored(l1, rL));
			configs.add(l1 + "," + 0 + "," + 0 + "," + rL + ",ANCHORED");
		}
	}

	protected void createConfigsBaseline(List<ObjectStore<WikiTable>> stores, List<String> configs) {
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

	protected ObjectStore<WikiTable> getObjectStoreBaseline(double limit1,
															double relaxLimit) {

		List<Matcher<WikiTable>> matchers = new ArrayList<>();

		matchers.add(MatcherBuilder.createDefaultSimMatcher(simSchema, limit1, relaxLimit));
		return new ObjectStore<>(matchers);
	}

	protected void createConfigsAnchoredBaseline(List<ObjectStore<WikiTable>> stores, List<String> configs) {
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


	protected ObjectStore<WikiTable> getObjectStoredAnchored(double limit1, double relaxLimit) {

		List<Matcher<WikiTable>> matchers = new ArrayList<>();

		matchers.add(new MatcherBuilder<WikiTable>()
				.addRanking(new SimilarityRankingNoHistory<>(simAnchored, limit1, relaxLimit))
				.addRanking(new SchemaRanking()).addRanking(new HeaderRanking()).createMatcher(true));
		return new ObjectStore<>(matchers);
	}


	@Override
	protected TablePosition getPosition(int pos) {
		return new TablePosition(pos);
	}

	@Override
	protected BiFunction<WikiTable, Integer, TablePosition> getMapper() {
		return TablePosition.DEFAULT_MAPPER;
	}

	private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withLocale(Locale.UK)
			.withZone(ZoneOffset.UTC);

	@Override
	protected List<WikiTable> getNewObjects(MyRevisionType r, Map<String, List<String>> nodes) {
		List<WikiTable> tables = tableBuilder.constructNewObjects(nodes);
		for (int pos = 0; pos < tables.size(); ++pos) {
			tables.get(pos).setFilename(df.format(r.getInstant()) + "-" + r.getId().toString() + "-" + pos + ".html");
		}
		return tables;
	}

	@Override
	protected GSReader<WikiTable, TablePosition> getGSReader() {
		return new GSReaderTable();
	}

}
