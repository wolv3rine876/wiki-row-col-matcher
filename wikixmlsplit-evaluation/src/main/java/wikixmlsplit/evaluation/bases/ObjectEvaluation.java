package wikixmlsplit.evaluation.bases;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import wikixmlsplit.evaluation.data.ObjectVersionPair;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.position.Position;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectEvaluation<T extends Position> {
	public static class ObjectEvaluationResult {
	
		private int objectSize;
		private String objectName;
		private int mistakes;
		
		public ObjectEvaluationResult(int objectSize, String objectName, int mistakes) {
			super();
			this.objectSize = objectSize;
			this.objectName = objectName;
			this.mistakes = mistakes;
		}
		
		public int getObjectSize() {
			return objectSize;
		}
		public String getObjectName() {
			return objectName;
		}
		public int getMistakes() {
			return mistakes;
		}

		@Override
		public String toString() {
			return "ObjectEvaluationResult [objectSize=" + objectSize + ", objectName=" + objectName + ", mistakes="
					+ mistakes + "]";
		}
		
		
	}
	
	public List<ObjectEvaluationResult> measure(Matching<T> matchingGold, Matching<T> matchingMeasure) {
		
		List<ObjectVersionPair> pairsGold = PairEvaluation.getPairs(matchingGold);
		List<ObjectVersionPair> pairsMeasure = PairEvaluation.getPairs(matchingMeasure);
		
		Map<String, ObjectVersionPair> indexGold = Maps.uniqueIndex(pairsGold, ObjectVersionPair::getFilename2);
		
		Multiset<String> wrong = HashMultiset.create();
		for(ObjectVersionPair p : pairsMeasure) {
			ObjectVersionPair pairGold = indexGold.get(p.getFilename2());
			
			if (!Objects.equal(pairGold, p)) {
				wrong.add(pairGold.getCluster1());
				wrong.add(pairGold.getCluster2());
			}
		}
		ImmutableListMultimap<String, Match<T>> goldClusters = matchingGold.getClusters();
		
		List<ObjectEvaluationResult> result = new ArrayList<>();
		for(Entry<String, Collection<Match<T>>> e : goldClusters.asMap().entrySet()) {
			result.add(new ObjectEvaluationResult(e.getValue().size(), e.getKey(), wrong.count(e.getKey())));
		}
		
		return result;
	}
	

}
