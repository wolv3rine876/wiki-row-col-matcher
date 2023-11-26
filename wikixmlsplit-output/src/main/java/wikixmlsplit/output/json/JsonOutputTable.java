package wikixmlsplit.output.json;

import com.beust.jcommander.JCommander;
import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.matching.*;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarity;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;
import wikixmlsplit.output.renderer.MyHtmlRenderer;
import wikixmlsplit.output.renderer.MyHtmlRendererCallback;
import wikixmlsplit.wikitable.WikiTable;
import wikixmlsplit.wikitable.builder.TableBuilder;
import wikixmlsplit.wikitable.position.TablePosition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class JsonOutputTable extends Outputbase {

	public JsonOutputTable() {
		super(".parsed");
	}

	private final WikiConfig wikiConfig = DefaultConfigEnWp.generate();
	private final MyHtmlRendererCallback htmlCallback = new MyHtmlRendererCallback();
	private final TableBuilder tableBuilder = new TableBuilder(false);

	public static void main(String[] args) throws Exception {
		JsonOutputTable main = new JsonOutputTable();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.run();
	}

	@Override
	protected void processPath(Path inputPath) throws IOException {
		// Read the page (consists of page title and revisions)
		MyPageType page = pageIO.read(inputPath);
		// get a list of all matches for the page. Index EACH match by revisionId-position -> Match
		ImmutableMap<String, Match<TablePosition>> index = Maps.uniqueIndex(getMatching(page).getMatches(),
				v -> v.getRevisionId() + "-" + v.getPosition().getIndex());

		Map<String, Map<String, Object>> previous = new HashMap<>();
		List<Map<String, Object>> objects = new ArrayList<>();

		// used to compute the similarities with the first version	
		Map<String, WikiTable> firstVersions = new HashMap<>();
		// used to compute the similarities with the previous version
		Map<String, WikiTable> previousVersions = new HashMap<>();
		MatchingObjectBoWSimilarity measureStrict= new MatchingObjectBoWSimilarity(true);

		// for each revision of the page
		deserializer.deserialize(page.getRevisions(), (r, parsed) -> {
			// get all tables
			List<WikiTable> tables = tableBuilder.constructNewObjects(parsed);
			for (Map<String, Object> p : previous.values()) {
				p.put("validTo", r.getInstant());
			}

			// Contains the clusterIds of tables that have been deleted
			Set<String> removed = new HashSet<>(previous.keySet());
			for (int i = 0; i < tables.size(); ++i) {
				Match<TablePosition> match = index.get(r.getId() + "-" + i);
				if (match != null) {
					String key = match.getClusterIdentifier();
					// we saw a table for the clustId, so it was not removed
					removed.remove(key);

					WikiTable table = tables.get(i);
					firstVersions.putIfAbsent(key, table);
					
					String content = cache.getUnchecked(new ObjectInput(page.getTitle(), table.getNode()));

					String contentType = "CREATE";
					// If we saw a table with the same clusterId, check for content changes
					if (previous.containsKey(key) && !previous.get(key).get("contentType").equals("DELETE")) {
						Map<String, Object> prevObj = previous.get(key);
						// than we check if the content was updated or not
						if (Objects.equal(content, prevObj.get("content"))) {
							contentType = "UNMODIFIED";
						} else {
							contentType = "UPDATE";
						}
					}
					
					String contextType = "CREATE";
					// If we saw a table with the same clusterId, check for context changes (schema, pos)
					if (previous.containsKey(key) && !previous.get(key).get("contextType").equals("DELETE")) {
						Map<String, Object> prevObj = previous.get(key);
						if (Objects.equal(i, prevObj.get("position"))
								&& Objects.equal(table.getCaption(), prevObj.get("caption"))
								&& Objects.equal(table.getHeadings(), prevObj.get("headings"))) {
							contextType = "UNMODIFIED";
						} else {
							contextType = "UPDATE";
						}
					}
					
					// Nothing changed
					if (contentType.equals("UNMODIFIED") && contextType.equals("UNMODIFIED"))
						continue;

					Map<String, Object> map = new HashMap<>();
					addMetaData(page, r, parsed, map);

					map.put("key", key);
					map.put("position", i);
					map.put("contentType", contentType);
					map.put("contextType", contextType);
					map.put("caption", table.getCaption());
					map.put("headings", table.getHeadings());
					map.put("content", content);
					map.put("contentHash", content.hashCode());
					map.put("rows", table.getRowCount());
					map.put("columns", table.getColumns().size());
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
			write(map, page.getId().toString());
		}
	}

	/*
	 * Extracts all matchings out of the given page.
	 */
	public Matching<TablePosition> getMatching(MyPageType page) {
		MatchingObjectBoWSimilarityWeighted measureStrict= new MatchingObjectBoWSimilarityWeighted(true);
		MatchingObjectBoWSimilarityWeighted measureRelaxed = new MatchingObjectBoWSimilarityWeighted(false);
		ObjectStore<WikiTable> tableStore = ObjectStore.createDefault(measureStrict, measureRelaxed, limit1, limit2, limit3, 0.95d, false);

		// A list containing all matches (tables that found some kind of match in a different revision).
		// Group by cluster id to identify the specific matches.
		Matching<TablePosition> matching = new Matching<>();
		// for each revision of the page
		deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
			// extract and parse the nodes that are tables
			List<WikiTable> tables = tableBuilder.constructNewObjects(nodes);
			tableStore.handleNewRevision(tables, (tracked) -> {
				if (!tracked.isActive()) {
					return;
				}
				matching.add(tracked.getIdentifier(), r.getId(), (TablePosition) tracked.getCurrentPosition(),
						!Objects.equal(tracked.getPrevObject(), tracked.getObject()));
			}, r, TablePosition.DEFAULT_MAPPER);
		});
		return matching;
	}

	private LoadingCache<ObjectInput, String> cache = CacheBuilder.newBuilder().maximumSize(1_000).build(new CacheLoader<>() {

		@Override
		public String load(ObjectInput input) throws Exception {
			return MyHtmlRenderer.print(htmlCallback, wikiConfig,
					PageTitle.make(wikiConfig, input.pageName), input.node);
		}
	});

}
