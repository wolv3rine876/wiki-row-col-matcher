package wikixmlsplit.socrata.keys;

import wikixmlsplit.socrata.SocrataTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleUniqueDiscovery implements UCCDiscovery {

	public List<List<Integer>> discover(SocrataTable table) {

		List<List<Integer>> result = new ArrayList<>();
		for (int i = 0; i < table.getHeader().size(); ++i) {
			if (table.isUnique(i)) {
				result.add(Collections.singletonList(i));
			}
		}

		if (result.isEmpty()) {
			int bestUniqueCount = -1;
			int bestColumn = -1;

			for (int i = 0; i < table.getHeader().size(); ++i) {
				int uniqueCount = table.getUniqueCount(i);

				if (uniqueCount > bestUniqueCount) {
					bestColumn = i;
					bestUniqueCount = uniqueCount;
				}
			}
			if (bestColumn >= 0) {
				result.add(Collections.singletonList(bestColumn));
			}
		}

		return result;
	}

}
