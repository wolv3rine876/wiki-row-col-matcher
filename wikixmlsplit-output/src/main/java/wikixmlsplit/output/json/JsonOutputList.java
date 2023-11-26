package wikixmlsplit.output.json;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;

import com.beust.jcommander.JCommander;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.lists.WikiList;
import wikixmlsplit.lists.builder.WikiListBuilder;
import wikixmlsplit.lists.position.WikiListPosition;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarity;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;

public class JsonOutputList extends Outputbase {

	public JsonOutputList() {
		super(".parsed");
	}

	private final WikiConfig wikiConfig = DefaultConfigEnWp.generate();
	private final WikiListBuilder tableBuilder = new WikiListBuilder();

	public static void main(String[] args) throws Exception {
		JsonOutputList main = new JsonOutputList();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.run();
	}

	@Override
	protected void processPath(Path inputPath) throws IOException {
		MyPageType page = pageIO.read(inputPath);
		ImmutableMap<String, Match<WikiListPosition>> index = Maps.uniqueIndex(getMatching(page).getMatches(),
				v -> v.getRevisionId() + "-" + v.getPosition().getIndex());

		Map<String, Map<String, Object>> previous = new HashMap<>();
		List<Map<String, Object>> objects = new ArrayList<>();

		Map<String, WikiList> firstVersions = new HashMap<>();
		Map<String, WikiList> previousVersions = new HashMap<>();
		MatchingObjectBoWSimilarity measureStrict= new MatchingObjectBoWSimilarity(true);

		deserializer.deserialize(page.getRevisions(), (r, parsed) -> {
			List<WikiList> tables = tableBuilder.constructNewObjects(parsed);
			for (Map<String, Object> p : previous.values()) {
				p.put("validTo", r.getInstant());
			}

			Set<String> removed = new HashSet<>(previous.keySet());
			for (int i = 0; i < tables.size(); ++i) {
				Match<WikiListPosition> match = index.get(r.getId() + "-" + i);
				if (match != null) {
					String key = match.getClusterIdentifier();
					removed.remove(key);

					WikiList table = tables.get(i);
					firstVersions.putIfAbsent(key, table);

					List<String> content = table.getItems();

					String contentType = "CREATE";
					if (previous.containsKey(key) && !previous.get(key).get("contentType").equals("DELETE")) {
						Map<String, Object> prevObj = previous.get(key);
						if (Objects.equal(content, prevObj.get("content"))) {
							contentType = "UNMODIFIED";
						} else {
							contentType = "UPDATE";
						}
					}
					
					String contextType = "CREATE";
					if (previous.containsKey(key) && !previous.get(key).get("contextType").equals("DELETE")) {
						Map<String, Object> prevObj = previous.get(key);
						if (Objects.equal(i, prevObj.get("position"))
								&& Objects.equal(table.getHeadings(), prevObj.get("headings"))) {
							contextType = "UNMODIFIED";
						} else {
							contextType = "UPDATE";
						}
					}

					if (contentType.equals("UNMODIFIED") && contextType.equals("UNMODIFIED"))
						continue;

					Map<String, Object> map = new HashMap<>();
					addMetaData(page, r, parsed, map);

					map.put("key", key);
					map.put("position", i);
					map.put("contentType", contentType);
					map.put("contextType", contextType);
					map.put("headings", table.getHeadings());
					map.put("content", content);
					map.put("contentHash", content.hashCode());
					map.put("itemCount", table.getItemCount());
					map.put("similarityLast", previousVersions.containsKey(key) ? measureStrict.getSimilarity(previousVersions.get(key), table) : 0.0d);
					map.put("similarityFirst", measureStrict.getSimilarity(firstVersions.get(key), table));
					previousVersions.put(key, table);

					objects.add(map);
					previous.put(key, map);

				}
			}
			for (String key : removed) {
				if (previous.get(key).get("contentType").equals("DELETE"))
					continue;

				Map<String, Object> map = new HashMap<>();
				addMetaData(page, r, parsed, map);
				map.put("key", key);

				map.put("contentType", "DELETE");
				map.put("contextType", "DELETE");

				objects.add(map);
				previous.put(key, map);

			}

		});

		for (Map<String, Object> map : objects) {
			write(map);
		}
	}


	public Matching<WikiListPosition> getMatching(MyPageType page) {
		MatchingObjectBoWSimilarityWeighted measureStrict= new MatchingObjectBoWSimilarityWeighted(true);
		MatchingObjectBoWSimilarityWeighted measureRelaxed = new MatchingObjectBoWSimilarityWeighted(false);
		ObjectStore<WikiList> tableStore = ObjectStore.createDefault(measureStrict, measureRelaxed, limit1, limit2, limit3, 0.95d, false);

		Matching<WikiListPosition> matching = new Matching<>();
		deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
			List<WikiList> tables = tableBuilder.constructNewObjects(nodes);
			tableStore.handleNewRevision(tables, (tracked) -> {
				if (!tracked.isActive()) {
					return;
				}
				matching.add(tracked.getIdentifier(), r.getId(), (WikiListPosition) tracked.getCurrentPosition(),
						!Objects.equal(tracked.getPrevObject(), tracked.getObject()));
			}, r, WikiListPosition.DEFAULT_MAPPER);
		});
		return matching;
	}

}
