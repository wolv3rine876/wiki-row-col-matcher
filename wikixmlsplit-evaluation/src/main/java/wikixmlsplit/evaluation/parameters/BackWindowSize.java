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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BackWindowSize extends AbstractGSRunner {

	@Parameter(names = "-type")
	protected ObjectType type;

	public static void main(String[] args) throws IOException {

		BackWindowSize main = new BackWindowSize();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}

	protected void evaluate(MyPageType page, Path inputFolder) {
		NodeDeserializer deserializer = new NodeDeserializer();
		Matching<SimplePosition> matchingGold = (Matching<SimplePosition>) type.getGSReader().loadMatching(deserializer,
				inputFolder, page);

		BuilderBase<? extends MatchingObject> tableBuilder = type.getBuilder();
		ImmutableListMultimap<BigInteger, Match<SimplePosition>> onlyChangesByRevision = matchingGold
				.getOnlyChangesByRevision();
		SimilarityMeasure<MatchingObject> tableSimJaccard = new MatchingObjectBoWSimilarity(true);
		SimilarityMeasure<MatchingObject>  tableSimContainment = new MatchingObjectBoWSimilarity(false);

		Map<String, List<MatchingObject>> last = new HashMap<>();
		deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
			List<? extends MatchingObject> tables = tableBuilder.constructNewObjects(nodes);
			List<Match<SimplePosition>> changes = onlyChangesByRevision.get(r.getId());

			for (Match<SimplePosition> m : changes) {
				MatchingObject table = tables.get(m.getPosition().getIndex());
				for (Entry<String, List<MatchingObject>> entry : last.entrySet()) {
					List<MatchingObject> previousVersions = entry.getValue();
					double maxBeforeJaccard = tableSimJaccard
							.getSimilarity(previousVersions.get(previousVersions.size() - 1), table);
					double maxBeforeContainment = tableSimContainment
							.getSimilarity(previousVersions.get(previousVersions.size() - 1), table);

					for (int i = 1; i < Math.min(15, previousVersions.size()); ++i) {
						MatchingObject previous = previousVersions.get(previousVersions.size() - i);

						if (entry.getKey().contentEquals(m.getClusterIdentifier())) {

							double simJaccard = tableSimJaccard.getSimilarity(previous, table);
							double simContainment = tableSimContainment.getSimilarity(previous, table);
							Map<String, Object> map = new HashMap<>();
							map.put("simJaccardImprovment", simJaccard > maxBeforeJaccard || i == 1 ? true : false);
							map.put("simContainmentImprovement",
									simContainment > maxBeforeContainment || i == 1 ? true : false);
							map.put("index", -i);
							map.put("type", type);
							// map.put("match", entry.getKey().contentEquals(m.getClusterIdentifier()));
							g.toJson(map, Map.class, jw);
							try {
								w.append("\n");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							maxBeforeJaccard = Math.max(simJaccard, maxBeforeJaccard);
							maxBeforeContainment = Math.max(simContainment, maxBeforeContainment);
						}
					}
				}

				last.computeIfAbsent(m.getClusterIdentifier(), (a) -> new ArrayList<>()).add(table);
			}
		});

	}

}
