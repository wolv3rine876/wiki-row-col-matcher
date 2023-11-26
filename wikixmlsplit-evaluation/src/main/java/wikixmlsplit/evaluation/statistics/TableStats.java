package wikixmlsplit.evaluation.statistics;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import wikixmlsplit.infobox.Infobox;
import wikixmlsplit.lists.WikiList;
import wikixmlsplit.matching.similarity.MatchingObject;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarity;
import wikixmlsplit.wikitable.WikiTable;

import java.time.Instant;
import java.util.*;

public class TableStats {

	private static final Instant DUMP_DATE = new Date(117, Calendar.DECEMBER, 3).toInstant();

	public enum UpdateType {
		INSERT_FRESH, INSERT_OLD, UPDATE_FRESH, UPDATE_OLD, DELETE, UNCHANGED
	}

	private Multiset<UpdateType> updateTypes;
	private Instant firstAppearance;
	private Instant lastAppearance;

	private Instant lastSeen;
	private long activeLifetime;

	private int startColumnCount;
	private int minimumColumnCount;
	private int maximumColumnCount;

	private int startRowCount;
	private int minimumRowCount;
	private int maximumRowCount;

	private Set<MatchingObject> previousVersions;
	private MatchingObject previous = null;
	private MatchingObject first = null;

	private String pageTitle;
	private String tableName;

	private List<TableRevisionStats> revisionStats;

	private int lastPosition;
	private int maxTableCount = 0;

	public TableStats(String pageTitle, String tableName, MatchingObject table, Instant firstAppearance, int position,
			boolean mightBeTrivial, int tableCount) {
		this.pageTitle = pageTitle;
		this.tableName = tableName;
		int rowCount = getRowCount(table);
		this.minimumRowCount = this.maximumRowCount = this.startRowCount = rowCount;
		if (table instanceof WikiTable) {
			int columnCount = ((WikiTable) table).getColumns().size();
			this.minimumColumnCount = this.maximumColumnCount = this.startColumnCount = columnCount;
		}
		this.firstAppearance = firstAppearance;

		this.previousVersions = new HashSet<>();
		this.updateTypes = HashMultiset.create();
		this.revisionStats = new ArrayList<>();

		addUpdate(table, firstAppearance, position, mightBeTrivial, tableCount);
	}

	public void addUpdate(MatchingObject table, Instant date, int position, boolean mightBeTrivial, int tableCount) {
		boolean firstInsert = previousVersions.isEmpty();
		UpdateType type = getType(table);
		this.previous = table;
		this.updateTypes.add(type);
		if (first == null) {
			first = table;
		}

		this.maxTableCount = Math.max(tableCount, maxTableCount);

		if (table != null) {
			if (!firstInsert && (!mightBeTrivial || type != UpdateType.UNCHANGED || position != lastPosition)) {
				revisionStats.add(new TableRevisionStats(date.toEpochMilli() - firstAppearance.toEpochMilli(), date,
						position, position - lastPosition, sim.getSimilarity(first, table),
						sim2.getSimilarity(first, table), tableCount));
			}
			lastPosition = position;
		}

		if (type == UpdateType.UNCHANGED)
			return;

		if (lastSeen != null) {
			activeLifetime += date.toEpochMilli() - lastSeen.toEpochMilli();
		}
		if (type == UpdateType.DELETE) {
			this.lastAppearance = date;
			this.lastSeen = null;
		} else {
			this.lastAppearance = null;
			this.lastSeen = date;

			int rowCount = getRowCount(table);

			this.minimumRowCount = Math.min(minimumRowCount, rowCount);
			this.maximumRowCount = Math.max(maximumRowCount, rowCount);
			if (table instanceof WikiTable) {

				int columnCount = ((WikiTable) table).getColumns().size();

				this.minimumColumnCount = Math.min(minimumColumnCount, columnCount);
				this.maximumColumnCount = Math.max(maximumColumnCount, columnCount);
			}
		}
	}

	protected int getRowCount(MatchingObject table) {
		int rowCount = 0;
		if (table instanceof WikiTable) {
			rowCount = ((WikiTable) table).getRowCount();
		} else if (table instanceof WikiList) {
			rowCount = ((WikiList) table).getItemCount();
		} else if (table instanceof Infobox) {
			rowCount = ((Infobox) table).getAttributeCount();
		}
		return rowCount;
	}

	private UpdateType getType(MatchingObject table) {
		if (Objects.equal(table, previous))
			return UpdateType.UNCHANGED;

		if (table != null) {
			if (previousVersions.isEmpty() || lastAppearance != null) {
				return previousVersions.add(table) ? UpdateType.INSERT_FRESH : UpdateType.INSERT_OLD;
			} else {
				return previousVersions.add(table) ? UpdateType.UPDATE_FRESH : UpdateType.UPDATE_OLD;
			}
		}
		return UpdateType.DELETE;
	}

	public Map<String, Object> finish() {
		if (lastAppearance == null) {
			activeLifetime += DUMP_DATE.toEpochMilli() - lastSeen.toEpochMilli();
			lastAppearance = DUMP_DATE;
		}

		Map<String, Object> result = new HashMap<>();
		result.put("pageTitle", pageTitle);
		result.put("tableName", tableName);

		result.put("firstAppearance", firstAppearance);
		result.put("lastAppearance", lastAppearance);
		result.put("activeLifetime", activeLifetime);
		result.put("totalLifetime", lastAppearance.toEpochMilli() - firstAppearance.toEpochMilli());

		result.put("startColumnCount", startColumnCount);
		result.put("minimumColumnCount", minimumColumnCount);
		result.put("maximumColumnCount", maximumColumnCount);

		result.put("startRowCount", startRowCount);
		result.put("minimumRowCount", minimumRowCount);
		result.put("maximumRowCount", maximumRowCount);

		for (UpdateType u : UpdateType.values())
			result.put(u.name(), updateTypes.count(u));

		return result;
	}

	public List<Map<String, Object>> tableRevisionStats() {
		List<Map<String, Object>> result = new ArrayList<>();
		for (TableRevisionStats stat : revisionStats) {
			Map<String, Object> map = stat.finish();
			map.put("pageTitle", pageTitle);
			map.put("tableName", tableName);
			map.put("maxTableCount", maxTableCount);
			result.add(map);
		}
		return result;
	}

	private static MatchingObjectBoWSimilarity sim = new MatchingObjectBoWSimilarity(true);
	private static MatchingObjectBoWSimilarity sim2 = new MatchingObjectBoWSimilarity(false);

}
