package wikixmlsplit.output.json;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.JCommander;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarity;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;
import wikixmlsplit.wikirow.WikiTuple;
import wikixmlsplit.wikirow.datastructures.MyJsonTableOutputRow;
import wikixmlsplit.wikirow.datastructures.MyTableHistoryType;
import wikixmlsplit.wikirow.datastructures.MyTableRevisionType;
import wikixmlsplit.wikirow.position.ElementPosition;
import wikixmlsplit.wikirow.util.HTMLHelper;

public class JsonOutputRowMatcher extends Outputbase {

	public JsonOutputRowMatcher() {
		super(".output.json");
	}

	public static void main(String[] args) throws Exception {
		JsonOutputRowMatcher main = new JsonOutputRowMatcher();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.run();
	}

	@Override
	protected void processPath(Path inputPath) throws IOException {
		try (
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath.toString()), "UTF-8"))
		) {

			// Group by key
			Map<String, MyTableHistoryType> hists = new HashMap<>();

			// parse each line as json and build histories
			for(String line = br.readLine(); line != null; line = br.readLine()) {
				MyJsonTableOutputRow row = g.fromJson(line, MyJsonTableOutputRow.class);

				MyTableHistoryType hist = hists.get(row.getKey());
				if(hist == null) {
					hist = new MyTableHistoryType(row.getPageTitle(), row.getPageID(), row.getKey(), new LinkedList<>());
					hists.put(row.getKey(), hist);
				}
				// mark the previous revision as last
				if((row.getContentType().equals("DELETE") || row.getContextType().equals("DELETE"))) {
					if (!hist.getRevisions().isEmpty()) hist.getRevisions().get(hist.getRevisions().size() - 1).isLast(true);;
				}
				else {
					try {
						MyTableRevisionType revision = new MyTableRevisionType(row.getValidFrom(), row.getValidUntil(), row.getContent(), row.getRevisionId(), row.getPosition(), row.getUser());
						hist.getRevisions().add(revision);
					}
					// Table with no rows
					catch(IllegalArgumentException e) {}
					catch(Exception e) {
						logger.error(String.format("Error while normalizing table %s on revision %s page %s.", row.getKey(), row.getRevisionId().toString(), row.getPageTitle()), e);
					}
				}
			}

			// process each history
			for(MyTableHistoryType hist : hists.values()) {
		    if(hist.getRevisions().size() == 0) continue;
				
				long start = System.nanoTime();
				process(hist);
				long end = System.nanoTime();
				logger.trace(String.format("Processed Page '%s', Table: '%s' (%d revisions) in %d ms", hist.getPageTitle(), hist.getTableID(), hist.getRevisions().size(), (end - start) / 1000));
			}
		}
		catch(Exception e) {
			logger.error(String.format("Error while processing file %s", inputPath), e);
		}
	}

	private void process(MyTableHistoryType tableHist) {
		// get a list of all row matches for the page. Index EACH match by revisionId-position -> Match
		ImmutableMap<String, Match<ElementPosition>> rowIndex = Maps.uniqueIndex(getRowMatching(tableHist).getMatches(),
					v -> v.getRevisionId() + "-" + v.getPosition().getIndex());
			// get a list of all col matches for the page. Index EACH match by revisionId-position -> Match
		ImmutableMap<String, Match<ElementPosition>> colIndex = Maps.uniqueIndex(getColMatching(tableHist).getMatches(),
					v -> v.getRevisionId() + "-" + v.getPosition().getIndex());

		Map<String, Map<String, Object>> previous = new HashMap<>();
		Map<String, Map<String, Object>> clusters = new HashMap<>();
		Map<BigInteger, String> schemas = new HashMap<>();
		// List<Map<String, Object>> objects = new ArrayList<>();

		// used to compute the similarities with the first version	
		Map<String, WikiTuple> firstVersions = new HashMap<>();
		// used to compute the similarities with the previous version
		Map<String, WikiTuple> previousVersions = new HashMap<>();
		MatchingObjectBoWSimilarity measureStrict= new MatchingObjectBoWSimilarity(true);

		// for each revision of the page
		List<MyTableRevisionType> revisions = tableHist.getRevisions();
		MyTableRevisionType lastProcessedRev = null;
		for(MyTableRevisionType revision : revisions) {
			List<WikiTuple> rows = revision.getRows();
			for (Map<String, Object> p : previous.values()) {
				p.put("validTo", revision.getInstant());
			}

			// Contains the clusterIds of tables that have been deleted
			Set<String> removed = new HashSet<>(previous.keySet());
			for (int i = 0; i < rows.size(); ++i) {
				Match<ElementPosition> match = rowIndex.get(revision.getId() + "-" + i);
				if (match != null) {
					String key = match.getClusterIdentifier();
					// we saw a table for the clustId, so it was not removed
					removed.remove(key);

					WikiTuple row = rows.get(i);
					firstVersions.putIfAbsent(key, row);

					String text = String.join("", row.getTuples());

					// Check if the row changed in comparison to the last occurrence.
					String contentType = "CREATE";
					if (previous.containsKey(key) && !previous.get(key).get("contentType").equals("DELETE")) {
						Map<String, Object> prevObj = previous.get(key);
						if (Objects.equal(text, prevObj.get("text"))) {
							contentType = "UNMODIFIED";
						} else {
							contentType = "UPDATE";
						}
					}
					
					// Nothing changed
					// if (contentType.equals("UNMODIFIED"))
					// 	continue;
					
					lastProcessedRev = revision;

					Map<String, Object> cluster = clusters.get(key);
					if(cluster == null) {
						cluster = new HashMap<>();
						cluster.put("clusterId", key);
						cluster.put("revisions", new LinkedList<Map<String, Object>>());
						clusters.put(key, cluster);
					}

					// Add a new revision
					Map<String, Object> r = new HashMap<>();
					LinkedList<Map<String, Object>> h = (LinkedList<Map<String, Object>>)cluster.get("revisions");
					h.addLast(r);

					r.put("revisionID", revision.getId());
					r.put("revisionDate", revision.getValidFrom());
					r.put("position", i);
					r.put("contentType", contentType);
					r.put("similarityLast", previousVersions.containsKey(key) ? measureStrict.getSimilarity(previousVersions.get(key), row) : null);
					r.put("similarityFirst", measureStrict.getSimilarity(firstVersions.get(key), row));
					// Build cells
					ArrayList<Map<String, String>> cells = new ArrayList<>();
					for(int j=0; j < row.getTuples().size(); j++) {
						Map<String, String> cellObj = new HashMap<>();
						cellObj.put("content", row.getTuples().get(j));
						cellObj.put("columnId", colIndex.get(revision.getId() + "-" + j).getClusterIdentifier());
						cells.add(cellObj);
					}
					r.put("cells", cells);

					
					// mark the row as deleted if the table was deleted in the next revision
					if(revision.isLast()) markDeleted(r, revision.getValidFrom());

					schemas.putIfAbsent(revision.getRevisionId(), HTMLHelper.toTR(row.getSchema()));
					
					previousVersions.put(key, row);
					
					Map<String, Object> prev = new HashMap<>();
					prev.put("contentType", contentType);
					prev.put("text", text);
					previous.put(key, prev);
				}
			}
			for (String key : removed) {
				if (previous.get(key).get("contentType").equals("DELETE"))
					continue;

				LinkedList<Map<String, Object>> h = (LinkedList<Map<String, Object>>)clusters.get(key).get("revisions");
				Map<String, Object> lastRevision = h.getLast();
				markDeleted(lastRevision, revision.getValidFrom());

				Map<String, Object> prev = new HashMap<>();			
				prev.put("contentType", "DELETE");
				previous.put(key, prev);
			}
		};
		 
		Map<String, Object> output = new HashMap<>();
		output.put("pageID", tableHist.getPageID());
		output.put("pageTitle", tableHist.getPageTitle());
		output.put("tableID", tableHist.getTableID());
		output.put("schemas", schemas);
		output.put("rows", clusters.values());

		// add the html of the latest table. This is redundant, as the data is also containt in schemas and rows, but it makes
		// classifing the subject column easier.
		if(lastProcessedRev != null) {
			output.put("lastRevisionID", lastProcessedRev.getRevisionId());
			output.put("lastTable", lastProcessedRev.getTable());

			write(output);
		}
	}

	/*
	 * Extracts all row matchings out of the given page.
	 */
	public Matching<ElementPosition> getRowMatching(MyTableHistoryType tableHist) {
		MatchingObjectBoWSimilarityWeighted measureStrict= new MatchingObjectBoWSimilarityWeighted(true);
		MatchingObjectBoWSimilarityWeighted measureRelaxed = new MatchingObjectBoWSimilarityWeighted(false);
		ObjectStore<WikiTuple> rowStore = ObjectStore.createDefault(measureStrict, measureRelaxed, limit1, limit2, limit3, 0.95d, false);

		// A list containing all matches (tables that found some kind of match in a different revision).
		// Group by cluster id to identify the specific matches.
		Matching<ElementPosition> matching = new Matching<>();
		// for each revision of the page
		for(MyTableRevisionType revision : tableHist.getRevisions()) {
			// Assume that the first row represents the schema
			rowStore.handleNewRevision(revision.getRows(), (tracked) -> {
				if(!tracked.isActive()) return;

				matching.add(tracked.getIdentifier(), revision.getId(), (ElementPosition)tracked.getCurrentPosition(),
					!Objects.equal(tracked.getPrevObject(), tracked.getObject()));
			}, revision, ElementPosition.DEFAULT_MAPPER);
		}
		return matching;
	}

	/*
	 * Extracts all col matchings out of the given page.
	 */
	public Matching<ElementPosition> getColMatching(MyTableHistoryType tableHist) {
		MatchingObjectBoWSimilarityWeighted measureStrict= new MatchingObjectBoWSimilarityWeighted(true);
		MatchingObjectBoWSimilarityWeighted measureRelaxed = new MatchingObjectBoWSimilarityWeighted(false);
		ObjectStore<WikiTuple> colStore = ObjectStore.createDefault(measureStrict, measureRelaxed, limit1, limit2, limit3, 0.95d, false);

		// A list containing all matches (tables that found some kind of match in a different revision).
		// Group by cluster id to identify the specific matches.
		Matching<ElementPosition> matching = new Matching<>();
		// for each revision of the page
		for(MyTableRevisionType revision : tableHist.getRevisions()) {
			// Assume that the first row represents the schema
			colStore.handleNewRevision(revision.getCols(), (tracked) -> {
				if(!tracked.isActive()) return;

				matching.add(tracked.getIdentifier(), revision.getId(), (ElementPosition)tracked.getCurrentPosition(),
					!Objects.equal(tracked.getPrevObject(), tracked.getObject()));
			}, revision, ElementPosition.DEFAULT_MAPPER);
		}
		return matching;
	}

	private void markDeleted(Map<String, Object> map, Date deleteDate) {
		map.put("deleted", true);
		map.put("deleteDate", deleteDate);
	} 
}
