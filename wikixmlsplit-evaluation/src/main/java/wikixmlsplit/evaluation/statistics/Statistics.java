package wikixmlsplit.evaluation.statistics;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.evaluation.bases.AbstractGSRunner;
import wikixmlsplit.evaluation.bases.ObjectType;
import wikixmlsplit.io.BuilderBase;
import wikixmlsplit.io.NodeDeserializer;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.position.SimplePosition;
import wikixmlsplit.matching.similarity.MatchingObject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

public class Statistics extends AbstractGSRunner {

	@Parameter(names = "-type")
	protected ObjectType type;

	@Parameter(names = "-tableStats")
	protected boolean writeTableStats;


	public static void main(String[] args) throws IOException {

		Statistics main = new Statistics();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}


	protected void evaluate(MyPageType page, Path inputFolder) {

		NodeDeserializer deserializer = new NodeDeserializer();
		Matching<SimplePosition> matchingGold = (Matching<SimplePosition>) type.getGSReader().loadMatching(deserializer, inputFolder, page);

		BuilderBase<? extends MatchingObject> tableBuilder = type.getBuilder();
		List<MatchingObject> prev = new ArrayList<>();
		ImmutableListMultimap<BigInteger, Match<SimplePosition>> matchesByRevision = matchingGold
				.getRevisions();

		AtomicLong obviousMatches = new AtomicLong(0);

		Map<String, TableStats> tableStats = new HashMap<>();

		deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
			List<? extends MatchingObject> tables = tableBuilder.constructNewObjects(nodes);

			int equalCount = 0;
			for (int pos = 0; pos < tables.size(); ++pos) {
				boolean equal = prev.size() > pos && Objects.equal(prev.get(pos), tables.get(pos));
				if (equal)
					++equalCount;
			}
			boolean containsTrivial = equalCount >= tables.size() - 1;
			if (containsTrivial) {
				obviousMatches.addAndGet(equalCount);
			}

			// int maxMovement = Math.max(prev.size(), tables.size());
			int maxMovement = tables.size();
			List<Match<SimplePosition>> changes = matchesByRevision.get(r.getId());
			for (Match<SimplePosition> m : changes) {
				String tableName = m.getClusterIdentifier();
				MatchingObject table = tables.get(m.getPosition().getIndex());
				if (tableStats.containsKey(tableName)) {
					tableStats.get(tableName).addUpdate(table, r.getInstant(), m.getPosition().getIndex(),
							containsTrivial, maxMovement);
				} else {
					tableStats.put(tableName, new TableStats(page.getTitle(), tableName, table, r.getInstant(),
							m.getPosition().getIndex(), containsTrivial, maxMovement));
				}
			}
			Set<String> found = Maps.uniqueIndex(changes, Match::getClusterIdentifier).keySet();
			for (Entry<String, TableStats> i : tableStats.entrySet()) {
				if (!found.contains(i.getKey())) {
					i.getValue().addUpdate(null, r.getInstant(), -1, containsTrivial, maxMovement);
				}
			}

			prev.clear();
			prev.addAll(tables);
		});

		for (TableStats stat : tableStats.values()) {
			if (writeTableStats) {
				Map<String, Object> map = stat.finish();
				map.put("type", type.name());
				g.toJson(map, Map.class, jw);
				try {
					w.append("\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				for (Map<String, Object> map : stat.tableRevisionStats()) {
					map.put("type", type.name());
					g.toJson(map, Map.class, jw);
					try {
						w.append("\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		try {
			w.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
