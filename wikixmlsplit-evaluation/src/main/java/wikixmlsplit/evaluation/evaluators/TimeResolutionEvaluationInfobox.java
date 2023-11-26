package wikixmlsplit.evaluation.evaluators;

import com.beust.jcommander.JCommander;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.evaluation.gsreader.GSReader;
import wikixmlsplit.evaluation.gsreader.GSReaderInfobox;
import wikixmlsplit.infobox.Infobox;
import wikixmlsplit.infobox.builder.InfoboxBuilder;
import wikixmlsplit.infobox.position.InfoboxPosition;
import wikixmlsplit.matching.Matcher;
import wikixmlsplit.matching.MatcherBuilder;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.similarity.SchemaObjectSchemaBoWSimilarity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class TimeResolutionEvaluationInfobox extends TimeResolutionEvaluation<Infobox, InfoboxPosition> {

	private InfoboxBuilder infoboxBuilder = new InfoboxBuilder();

	private SchemaObjectSchemaBoWSimilarity simSchema = new SchemaObjectSchemaBoWSimilarity(true);

	public static void main(String[] args) throws IOException {
		TimeResolutionEvaluationInfobox main = new TimeResolutionEvaluationInfobox();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}


	@Override
	protected void createConfigs(List<ObjectStore<Infobox>> stores, List<String> configs) {
		super.createConfigs(stores, configs);
		createConfigsBaseline(stores, configs);
	}

	@Override
	protected void createBestConfig(List<ObjectStore<Infobox>> stores, List<String> configs) {
		double l1 = 0.99d, l2 = 0.6d, l3 = 0.4d, rL = 0.95d;
		stores.add(getObjectStore(l1, l2, l3, rL));
		configs.add(l1 + "," + l2 + "," + l3 + "," + rL + ",OURAPPROACH-HUNGARIAN");

		if (!errorOutput) {
			l1 = 0.1d;
			stores.add(getObjectStoreBaseline(l1, rL));
			configs.add(l1 + "," + l2 + "," + l3 + "," + rL + ",SCHEMABASELINE");
		}
	}

	protected void createConfigsBaseline(List<ObjectStore<Infobox>> stores, List<String> configs) {
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

	protected ObjectStore<Infobox> getObjectStoreBaseline(double limit1,
                                                          double relaxLimit) {

		List<Matcher<Infobox>> matchers = new ArrayList<>();

		matchers.add(MatcherBuilder.createDefaultSimMatcher(simSchema, limit1, relaxLimit));
		return new ObjectStore<>(matchers);
	}


	@Override
	protected InfoboxPosition getPosition(int pos) {
		return new InfoboxPosition(pos);
	}

	@Override
	protected BiFunction<Infobox, Integer, InfoboxPosition> getMapper() {
		return InfoboxPosition.DEFAULT_MAPPER;
	}

	@Override
	protected List<Infobox> getNewObjects(MyRevisionType r, Map<String, List<String>> nodes) {
		return infoboxBuilder.constructNewObjects(nodes);
	}

	@Override
	protected GSReader<Infobox, InfoboxPosition> getGSReader() {
		return new GSReaderInfobox();
	}
}
