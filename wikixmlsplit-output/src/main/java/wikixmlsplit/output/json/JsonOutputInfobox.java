package wikixmlsplit.output.json;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.beust.jcommander.JCommander;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.infobox.Infobox;
import wikixmlsplit.infobox.builder.InfoboxBuilder;
import wikixmlsplit.infobox.position.InfoboxPosition;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarity;

public class JsonOutputInfobox extends Outputbase {
	
	public JsonOutputInfobox() {
		super(".parsed");
	}

	public static void main(String[] args) throws Exception {
		JsonOutputInfobox main = new JsonOutputInfobox();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.run();
	}

	protected void processPath(Path inputPath) throws IOException {
		MyPageType page = pageIO.read(inputPath);
		InfoboxBuilder infoboxBuilder = new InfoboxBuilder();
		List<Match<InfoboxPosition>> matches = getMatching(page, infoboxBuilder).getMatches();
		ImmutableMap<String, Match<InfoboxPosition>> index = Maps.uniqueIndex(matches,
				v -> v.getRevisionId() + "-" + v.getPosition().getIndex());
		List<Map<String, Object>> objects = new ArrayList<>();

		Map<String, Map<String, Object>> previous = new HashMap<>();
		Map<Pair<String, PropertyId>, Map<String, Object>> previousChanges = new HashMap<>();
		Map<String, Map<PropertyId, String>> previousAttributes = new HashMap<>();

		deserializer.deserialize(page.getRevisions(), (r, parsed) -> {
			List<Infobox> infoboxes = infoboxBuilder.constructNewObjects(parsed);
			for (Map<String, Object> p : previous.values()) {
				p.put("validTo", r.getInstant());
			}
			for (Map<String, Object> p : previousChanges.values()) {
				p.put("valueValidTo", r.getInstant());
			}

			Set<String> removed = new HashSet<>(previous.keySet());
			for (int i = 0; i < infoboxes.size(); ++i) {
				Match<InfoboxPosition> match = index.get(r.getId() + "-" + i);
				if (match != null) {
					String key = match.getClusterIdentifier();
					removed.remove(key);

					Infobox infobox = infoboxes.get(i);

					Map<PropertyId, String> currentAttributes = getCurrentAttributes(page, r, infobox);

					List<Map<String, Object>> updates = getChanges(previousChanges, key,
							previousAttributes.getOrDefault(key, Collections.emptyMap()), currentAttributes);
					previousAttributes.put(key, currentAttributes);

					String type = "CREATE";
					if (previous.containsKey(key) && !previous.get(key).get("type").equals("DELETE")) {
						type = updates.isEmpty() ? "UNMODIFIED" : "UPDATE";
					}

					if (type.equals("UNMODIFIED"))
						continue;

					Map<String, Object> map = new HashMap<>();
					addMetaData(page, r, parsed, map);

					map.put("key", key);

					map.put("position", i);
					map.put("template", infobox.getTemplate());
					map.put("attributes", infobox.getAttributes());
					map.put("changes", updates);

					map.put("type", type);
					objects.add(map);
					previous.put(key, map);

				}
			}
			for (String key : removed) {
				if (previous.get(key).get("type").equals("DELETE"))
					continue;

				Map<String, Object> map = new HashMap<>();
				addMetaData(page, r, parsed, map);
				map.put("key", key);

				map.put("type", "DELETE");

				List<Map<String, Object>> updates = getChanges(previousChanges, key,
						previousAttributes.getOrDefault(key, Collections.emptyMap()), Collections.emptyMap());
				map.put("changes", updates);
				previousAttributes.remove(key);

				objects.add(map);
				previous.put(key, map);

			}

		});

		for (Map<String, Object> p : previousChanges.values()) {
			p.remove("valueValidTo");
		}

		for (Map<String, Object> map : objects) {
			write(map);
		}

	}

	private Map<PropertyId, String> getCurrentAttributes(MyPageType page, MyRevisionType r, Infobox infobox) {
		Map<PropertyId, String> currentAttributes = new HashMap<>();
		for (Entry<String, String> e : infobox.getAttributes().entrySet()) {
			currentAttributes.put(new PropertyId("attribute", e.getKey()), e.getValue());
		}
		currentAttributes.put(new PropertyId("meta", "template"), infobox.getTemplate());
		return currentAttributes;
	}

	private List<Map<String, Object>> getChanges(Map<Pair<String, PropertyId>, Map<String, Object>> previousChanges,
			String key, Map<PropertyId, String> map, Map<PropertyId, String> currentAttrbutes) {
		List<Map<String, Object>> updates = new ArrayList<>();
		for (PropertyId attrKey : Sets.union(map.keySet(), currentAttrbutes.keySet())) {
			if (!Objects.equal(map.get(attrKey), currentAttrbutes.get(attrKey))) {
				Map<String, Object> change = new HashMap<>();
				change.put("property", attrKey);
				if (map.containsKey(attrKey))
					change.put("previousValue", map.get(attrKey));
				if (currentAttrbutes.containsKey(attrKey))
					change.put("currentValue", currentAttrbutes.get(attrKey));
				updates.add(change);
				previousChanges.put(Pair.of(key, attrKey), change);
			}
		}
		return updates;
	}

	public Matching<InfoboxPosition> getMatching(MyPageType page, InfoboxBuilder infoboxBuilder) {
		MatchingObjectBoWSimilarity simStrict = new MatchingObjectBoWSimilarity(true);
		MatchingObjectBoWSimilarity simRelaxed = new MatchingObjectBoWSimilarity(false);
		ObjectStore<Infobox> infoboxStore = ObjectStore.createDefault(simStrict, simRelaxed, limit1, limit2, limit3, 0.95d, false);

		Matching<InfoboxPosition> matching = new Matching<>();
		deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
			List<Infobox> infoboxes = infoboxBuilder.constructNewObjects(nodes);
			infoboxStore.handleNewRevision(infoboxes, (tracked) -> {
				if (!tracked.isActive()) {
					return;
				}
				matching.add(tracked.getIdentifier(), r.getId(), (InfoboxPosition) tracked.getCurrentPosition(),
						!Objects.equal(tracked.getPrevObject(), tracked.getObject()));
			}, r, InfoboxPosition.DEFAULT_MAPPER);
		});
		return matching;
	}

}
