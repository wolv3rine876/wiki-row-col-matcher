package wikixmlsplit.evaluation.statistics;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class TableRevisionStats {
	private long timeDifference;
	private Instant date;
	
	private int position;
	private int positionDifference;
	
	private double jaccardSimilarity;
	private double containmentSimilarity;
	
	private int tableCount;
	
	
	
	public TableRevisionStats(long timeDifference, Instant date, int position, int positionDifference,
			double jaccardSimilarity, double containmentSimilarity, int tableCount) {
		super();
		this.timeDifference = timeDifference;
		this.date = date;
		this.position = position;
		this.positionDifference = positionDifference;
		this.jaccardSimilarity = jaccardSimilarity;
		this.containmentSimilarity = containmentSimilarity;
		this.tableCount = tableCount;
	}



	public Map<String, Object> finish() {
		Map<String, Object> result = new HashMap<>();
		result.put("date", date.toString());
		result.put("timeDifference",timeDifference);
		result.put("position", position);
		result.put("positionDifference", positionDifference);
		result.put("jaccardSimilarity", jaccardSimilarity);
		result.put("containmentSimilarity", containmentSimilarity);
		result.put("tableCount", tableCount);
		return result;
	}
}
