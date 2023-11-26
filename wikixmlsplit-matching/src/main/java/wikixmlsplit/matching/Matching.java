package wikixmlsplit.matching;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.position.Position;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/*
 * A list of matches.
 */
public class Matching<T extends Position> {

	private List<Match<T>> matches = new ArrayList<>();
	private String name;

	public Matching() {
		this(null);
	}
	
	public Matching(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void add(String clusterIdentifier, BigInteger revisionId, T position, boolean objectChanged) {
		Match<T> m = new Match<>(clusterIdentifier, revisionId, position, objectChanged);
		matches.add(m);
	}

	public List<Match<T>> getMatches() {
		return matches;
	}

	public ImmutableListMultimap<String, Match<T>> getClusters() {
		return Multimaps.index(matches, Match::getClusterIdentifier);
	}

	public ImmutableListMultimap<BigInteger, Match<T>> getRevisions() {
		return Multimaps.index(matches, Match::getRevisionId);
	}

	public ImmutableListMultimap<String, Match<T>> getOnlyChangesByCluster() {
		return Multimaps.index(matches.stream().filter(Match::isObjectChanged).iterator(),
				Match::getClusterIdentifier);
	}

	public ImmutableListMultimap<BigInteger, Match<T>> getOnlyChangesByRevision() {
		return Multimaps.index(matches.stream().filter(Match::isObjectChanged).iterator(), Match::getRevisionId);
	}

	public int getObjectChangeCount() {
		int count = 0;
		for (Match<?> m : matches)
			if (m.isObjectChanged())
				++count;
		return count;
	}
	
	public int getClusterCount() {
		return (int) matches.stream().map(Match::getClusterIdentifier).distinct().count();
	}
	

	public int size() {
		return matches.size();
	}

	public int getDecisionSize() {
		if (matches.isEmpty())
			return 0;

		BigInteger firstRevison = matches.get(0).getRevisionId();

		int firstTables = (int) matches.stream().filter(i -> Objects.equals(i.getRevisionId(), firstRevison)).count();
		return matches.size() - firstTables;
	}

	public Matching<T> filter(Set<BigInteger> revIds) {
		Matching<T> matching = new Matching<>(name);
		for (Match<T> m : matches) {
			if(revIds.contains(m.getRevisionId())) {
				matching.add(m.getClusterIdentifier(), m.getRevisionId(), m.getPosition(), m.isObjectChanged());
			}
		}
		return matching;
	}
	
	@Override
	public String toString() {
		return "Matching [matches=" + matches + ", name=" + name + "]";
	}
}
