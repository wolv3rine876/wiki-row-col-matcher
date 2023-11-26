package wikixmlsplit.output.renderer;

import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.infobox.Infobox;
import wikixmlsplit.infobox.builder.InfoboxBuilder;
import wikixmlsplit.infobox.position.InfoboxPosition;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RenderTargetInfobox implements IRenderTarget {
	private final MatchingObjectBoWSimilarityWeighted simStrict = new MatchingObjectBoWSimilarityWeighted(true);
	private final MatchingObjectBoWSimilarityWeighted simRelaxed = new MatchingObjectBoWSimilarityWeighted(false);
	private InfoboxBuilder infoboxBuilder = new InfoboxBuilder();
	private ObjectStore<Infobox> objectStore;

	
	@Override
	public void configureObjectStore(boolean weighted, double limit1, double limit2, double limit3,
			double relaxLimit) {
		objectStore = ObjectStore.createDefault(simStrict, simRelaxed, limit1, limit2, limit3, relaxLimit, false);
	}

	@Override
	public List<RenderResult> track(MyRevisionType r, Map<String, List<String>> nodes) {
		List<Infobox> tables = infoboxBuilder.constructNewObjects(nodes);
		List<RenderResult> result = new ArrayList<>();
		
		objectStore.handleNewRevision(tables, (tracked) -> {
			if (!tracked.isActive()) {
				return;
			}

			Infobox infobox = tracked.getObject();
			
			result.add(new RenderResult(tracked.getIdentifier(), tracked.getCurrentPosition().getPositionString(), infobox.getNode(), infobox.getHeadings(), infobox.getTemplate()));
		}, r, InfoboxPosition.DEFAULT_MAPPER);
		return result;
	}

}
