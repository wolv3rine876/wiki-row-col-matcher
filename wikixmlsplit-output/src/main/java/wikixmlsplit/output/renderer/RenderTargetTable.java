package wikixmlsplit.output.renderer;

import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;
import wikixmlsplit.wikitable.WikiTable;
import wikixmlsplit.wikitable.builder.TableBuilder;
import wikixmlsplit.wikitable.position.TablePosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RenderTargetTable implements IRenderTarget {

	private MatchingObjectBoWSimilarityWeighted sim = new MatchingObjectBoWSimilarityWeighted(true);
	private MatchingObjectBoWSimilarityWeighted sim2 = new MatchingObjectBoWSimilarityWeighted(false);

	private TableBuilder tableBuilder = new TableBuilder(false);
	private ObjectStore<WikiTable> objectStore;

	
	@Override
	public void configureObjectStore(boolean weighted, double limit1, double limit2, double limit3,
			double relaxLimit) {
		objectStore = ObjectStore.createDefault(sim, sim2, limit1, limit2, limit3, relaxLimit, false);
	}
	

	@Override
	public List<RenderResult> track(MyRevisionType r, Map<String, List<String>> nodes) {
		List<WikiTable> tables = tableBuilder.constructNewObjects(nodes);
		List<RenderResult> result = new ArrayList<>();
		
		objectStore.handleNewRevision(tables, (tracked) -> {
			if (!tracked.isActive()) {
				return;
			}

			WikiTable tab = tracked.getObject();
			
			result.add(new RenderResult(tracked.getIdentifier(), tracked.getCurrentPosition().getPositionString(), tab.getNode(), tab.getHeadings(), tab.getCaption()));
		}, r, TablePosition.DEFAULT_MAPPER);
		return result;
	}
}
