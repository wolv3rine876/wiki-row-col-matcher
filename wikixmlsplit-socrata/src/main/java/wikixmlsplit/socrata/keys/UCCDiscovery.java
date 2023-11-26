package wikixmlsplit.socrata.keys;

import wikixmlsplit.socrata.SocrataTable;

import java.util.List;

public interface UCCDiscovery {

	List<List<Integer>> discover(SocrataTable table);

}