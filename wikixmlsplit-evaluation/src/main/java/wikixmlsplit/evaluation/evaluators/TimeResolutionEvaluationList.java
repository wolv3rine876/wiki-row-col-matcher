package wikixmlsplit.evaluation.evaluators;

import com.beust.jcommander.JCommander;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.evaluation.gsreader.GSReader;
import wikixmlsplit.evaluation.gsreader.GSReaderWikiList;
import wikixmlsplit.lists.WikiList;
import wikixmlsplit.lists.builder.WikiListBuilder;
import wikixmlsplit.lists.position.WikiListPosition;
import wikixmlsplit.matching.ObjectStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class TimeResolutionEvaluationList extends TimeResolutionEvaluation<WikiList, WikiListPosition> {

	private WikiListBuilder listBuilder = new WikiListBuilder();


	public static void main(String[] args) throws IOException {
		TimeResolutionEvaluationList main = new TimeResolutionEvaluationList();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}


	@Override
	protected void createConfigs(List<ObjectStore<WikiList>> stores, List<String> configs) {
		super.createConfigs(stores, configs);
	}

	protected void createBestConfig(List<ObjectStore<WikiList>> stores, List<String> configs) {
		double l1 = 0.99d, l2 = 0.6d, l3=0.4d, rL = 0.95d;
		stores.add(getObjectStore(l1, l2, l3, rL));
		configs.add(l1 + "," + l2 + "," + l3 + "," + rL + ",OURAPPROACH-HUNGARIAN");
	}


	@Override
	protected WikiListPosition getPosition(int pos) {
		return new WikiListPosition(pos);
	}

	@Override
	protected BiFunction<WikiList, Integer, WikiListPosition> getMapper() {
		return WikiListPosition.DEFAULT_MAPPER;
	}

	@Override
	protected List<WikiList> getNewObjects(MyRevisionType r, Map<String, List<String>> nodes) {
		return listBuilder.constructNewObjects(nodes);
	}

	@Override
	protected GSReader<WikiList, WikiListPosition> getGSReader() {
		return new GSReaderWikiList();
	}
}
