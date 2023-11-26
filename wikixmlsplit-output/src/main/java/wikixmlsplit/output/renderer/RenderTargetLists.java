package wikixmlsplit.output.renderer;

import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.lists.WikiList;
import wikixmlsplit.lists.builder.WikiListBuilder;
import wikixmlsplit.lists.position.WikiListPosition;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RenderTargetLists implements IRenderTarget {

	private final MatchingObjectBoWSimilarityWeighted simStrict = new MatchingObjectBoWSimilarityWeighted(true);
	private final MatchingObjectBoWSimilarityWeighted simRelaxed = new MatchingObjectBoWSimilarityWeighted(false);
	private WikiListBuilder wikilistBuilder = new WikiListBuilder();
	private ObjectStore<WikiList> objectStore;

	@Override
	public void configureObjectStore(boolean weighted, double limit1, double limit2, double limit3, double relaxLimit) {
		objectStore = ObjectStore.createDefault(simStrict, simRelaxed, limit1, limit2, limit3, relaxLimit, false);
	}

	@Override
	public List<RenderResult> track(MyRevisionType r, Map<String, List<String>> nodes) {
		List<WikiList> tables = wikilistBuilder.constructNewObjects(nodes);
		List<RenderResult> result = new ArrayList<>();

		objectStore.handleNewRevision(tables, (tracked) -> {
			if (!tracked.isActive()) {
				return;
			}

			WikiList tab = tracked.getObject();

			result.add(new RenderResult(tracked.getIdentifier(), tracked.getCurrentPosition().getPositionString(),
					tab.getNode(), tab.getHeadings(), ""));
		}, r, WikiListPosition.DEFAULT_MAPPER);
		return result;
	}
}
