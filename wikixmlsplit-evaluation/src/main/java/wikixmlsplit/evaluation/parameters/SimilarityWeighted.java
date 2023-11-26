package wikixmlsplit.evaluation.parameters;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.EvictingQueue;
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
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimilarityWeighted extends AbstractGSRunner {
    @Parameter(names = "-type")
    protected ObjectType type;

    public static void main(String[] args) throws IOException {
        SimilarityWeighted main = new SimilarityWeighted();
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

        Map<String, EvictingQueue<MatchingObject>> last = new HashMap<>();
        Map<String, MatchingObject> lastTable = new HashMap<>();

        MatchingObjectBoWSimilarity simJaccard = new MatchingObjectBoWSimilarity(true);
        MatchingObjectBoWSimilarity simContainment = new MatchingObjectBoWSimilarity(false);
        MatchingObjectBoWSimilarityWeighted weightedSimJaccard = new MatchingObjectBoWSimilarityWeighted(true);
        MatchingObjectBoWSimilarityWeighted weightedSimContainment = new MatchingObjectBoWSimilarityWeighted(false);
        deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
            List<? extends MatchingObject> tables = tableBuilder.constructNewObjects(nodes);
            List<Match<SimplePosition>> changes = onlyChangesByRevision.get(r.getId());

            weightedSimJaccard.register(new ArrayList<>(last.values()), tables);
            weightedSimContainment.register(new ArrayList<>(last.values()), tables);

            for (Match<SimplePosition> m : changes) {
                MatchingObject table = tables.get(m.getPosition().getIndex());
                if (last.get(m.getClusterIdentifier()) != null) {
                    MatchingObject previousTable = lastTable.get(m.getClusterIdentifier());
                    for (MatchingObject other : lastTable.values()) {

                        outputBasicStats(other == previousTable,
                                simContainment.getSimilarity(other, table),
                                simJaccard.getSimilarity(other, table),
                                weightedSimContainment.getSimilarity(other, table),
                                weightedSimJaccard.getSimilarity(other, table));

                    }
                }
            }

            for (Match<SimplePosition> m : changes) {
                MatchingObject table = tables.get(m.getPosition().getIndex());
                if (last.get(m.getClusterIdentifier()) != null) {
                    last.get(m.getClusterIdentifier()).add(table);
                } else {
                    EvictingQueue<MatchingObject> previousTable = EvictingQueue.create(5);
                    previousTable.add(table);
                    last.put(m.getClusterIdentifier(), previousTable);
                }
                lastTable.put(m.getClusterIdentifier(), table);
            }
        });

    }


    private void outputBasicStats(boolean match, double containmentSim, double jaccardSim, double containmentSimW, double jaccardSimW) {
        Map<String, Object> map = new HashMap<>();
        map.put("match", match);
        map.put("containmentSim", containmentSim);
        map.put("jaccardSim", jaccardSim);
        map.put("weightedContainmentSim", containmentSimW);
        map.put("weightedJaccardSim", jaccardSimW);
        g.toJson(map, Map.class, jw);
        try {
            w.append("\n");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
