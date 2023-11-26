package wikixmlsplit.socrata.keys;

import wikixmlsplit.socrata.columnorder.OrderedSchema;
import wikixmlsplit.socrata.SocrataTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class KeySelection {

	private SocrataTable table;

	private Map<Integer, Integer> order;

	public KeySelection(SocrataTable table, OrderedSchema schema) {
		this.table = table;

		order = new HashMap<>();
		if (schema != null) {
			int pos = 0;
			Map<String, Integer> posMap = new HashMap<>();
			for (String column : schema.getColumns()) {
				posMap.put(column, pos++);
			}

			pos = 0;
			int missingPos = posMap.size();
			for (String originalColumn : table.getHeader()) {
				if (posMap.containsKey(originalColumn)) {
					order.put(pos++, posMap.get(originalColumn));
				} else {
					Optional<Entry<String, Integer>> match = posMap.entrySet().stream()
							.filter(entry -> originalColumn.startsWith("_" + entry.getKey())).findFirst();
					if (match.isPresent()) {
						order.put(pos++, match.get().getValue());
					} else {
						System.err.println("Missing column: " + originalColumn);
						order.put(pos++, missingPos++);
					}

				}
			}
		} else {
			System.err.println("No ordered schema found!");
			for (int pos = 0; pos < table.getHeader().size(); ++pos) {
				order.put(pos, pos);
			}
		}
	}

	public double getScore(List<Integer> key) {
		double score = 0.0d;

		score += getLengthScore(key);

		score += getValueScore(key);

		score += getPositionScore(key);

		return score / 3.0d;
	}

	private double getLengthScore(List<Integer> key) {
		return 1.0d / key.size();
	}

	private double getValueScore(List<Integer> key) {
		double maximumLength = 8.0d;

		for (List<String> row : table.getRows()) {
			double currentLength = 0;
			for (Integer column : key) {
				currentLength += row.get(column).length();
			}
			maximumLength = Math.max(currentLength, maximumLength);
		}
		return 1.0d / (maximumLength - 7.0d);
	}

	private double getPositionScore(List<Integer> originalkey) {
		List<Integer> key = originalkey.stream().map(order::get).collect(Collectors.toList());

		double result = 0.0d;

		result += 1.0d / (key.get(0) + 1);

		double between = 0.0d;
		for (int i = 0; i < key.size() - 1; ++i) {
			between += key.get(i + 1) - key.get(i);
		}
		result += 1.0d / (between + 1.0d);

		return result / 2.0d;
	}
}
