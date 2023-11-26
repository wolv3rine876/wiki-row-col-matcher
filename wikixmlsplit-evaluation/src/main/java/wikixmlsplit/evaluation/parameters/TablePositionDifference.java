package wikixmlsplit.evaluation.parameters;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableListMultimap;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.evaluation.bases.AbstractGSRunner;
import wikixmlsplit.evaluation.bases.ObjectType;
import wikixmlsplit.io.BuilderBase;
import wikixmlsplit.io.NodeDeserializer;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.position.SimplePosition;
import wikixmlsplit.matching.similarity.MatchingObject;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarity;
import wikixmlsplit.matching.similarity.SimilarityMeasure;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TablePositionDifference extends AbstractGSRunner {

	@Parameter(names = "-type")
	protected ObjectType type;

	public static void main(String[] args) throws IOException {

		TablePositionDifference main = new TablePositionDifference();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}

	protected void evaluate(MyPageType page, Path inputFolder) {
		NodeDeserializer deserializer = new NodeDeserializer();
		Matching<SimplePosition> matchingGold = (Matching<SimplePosition>) type.getGSReader().loadMatching(deserializer,
				inputFolder, page);

		BuilderBase<? extends MatchingObject> tableBuilder = type.getBuilder();
		ImmutableListMultimap<BigInteger, Match<SimplePosition>> onlyChangesByRevision = matchingGold.getOnlyChangesByRevision();

		SimilarityMeasure<MatchingObject> tableSimJaccard = new MatchingObjectBoWSimilarity(true);
		SimilarityMeasure<MatchingObject>  tableSimContainment = new MatchingObjectBoWSimilarity(false);

		Map<String, MatchingObject> last = new HashMap<>();
		Map<String, Integer> lastPos = new HashMap<>();
		deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
			List<? extends MatchingObject> tables = tableBuilder.constructNewObjects(nodes);
			List<Match<SimplePosition>> changes = onlyChangesByRevision.get(r.getId());

			for (Match<SimplePosition> m : changes) {
				int tablePos = m.getPosition().getIndex();
				MatchingObject table = tables.get(tablePos);
				if (last.get(m.getClusterIdentifier()) != null) {
					MatchingObject p = last.get(m.getClusterIdentifier());
					
					for(Entry<String, MatchingObject> e : last.entrySet()) {
						double simJaccard = tableSimJaccard.getSimilarity(e.getValue(), table);
						double simContainment = tableSimContainment.getSimilarity(e.getValue(), table);

						Map<String, Object> map = new HashMap<>();
						map.put("simJaccard", simJaccard);
						map.put("simContainment", simContainment);

						int previousPos = lastPos.get(e.getKey());
						int posDifference = previousPos - tablePos;
						map.put("positionDifference", posDifference);
						map.put("match", e.getValue() == p);
						map.put("type", type);
						g.toJson(map, Map.class, jw);
						try {
							w.append("\n");
						} catch (IOException ex) {
							// TODO Auto-generated catch block
							ex.printStackTrace();
						}
					}
				}
			}
			
			for (Match<SimplePosition> m : changes) {
				int tablePos = m.getPosition().getIndex();
				MatchingObject table = tables.get(tablePos);
				last.put(m.getClusterIdentifier(), table);
				lastPos.put(m.getClusterIdentifier(), tablePos);
			}
		});

	}

}
