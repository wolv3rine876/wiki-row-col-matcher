package wikixmlsplit.evaluation.bases;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import wikixmlsplit.evaluation.data.MatchingError;
import wikixmlsplit.evaluation.data.MatchingError.ErrorType;
import wikixmlsplit.evaluation.data.ObjectVersionPair;
import wikixmlsplit.evaluation.data.PairEvaluationResult;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.position.Position;

import java.util.*;
import java.util.stream.Collectors;

public class PairEvaluation<T extends Position> {

	public PairEvaluationResult measure(Matching<T> matchingGold, Matching<T> matchingMeasure) {
		List<ObjectVersionPair> pairsGold = getPairs(matchingGold);
		Set<ObjectVersionPair> pairsGoldSet = new HashSet<>(pairsGold);
		List<ObjectVersionPair> pairsMeasure = getPairs(matchingMeasure);
		Set<ObjectVersionPair> pairsMeasureSet = new HashSet<>(pairsMeasure);

		int truePositive = 0;
		int trueNewCluster = 0;
		int falseNewCluster = 0;
		int wrongMatch = 0;
		int missingNewCluster = 0;
		Set<String> wrongMatchCandidate = new HashSet<>();
		for (ObjectVersionPair pair : pairsMeasure) {
			if (pairsGoldSet.contains(pair)) {
				if (!pair.isSameCluster()) {
					++trueNewCluster;
				} else
					++truePositive;
			} else {
				if (!pair.isSameCluster())
					++falseNewCluster;
				else
					wrongMatchCandidate.add(pair.getFilename2());
			}
		}

		for (ObjectVersionPair pair : pairsGold) {
			if (!pairsMeasureSet.contains(pair)) {
				if (!pair.isSameCluster()) {
					++missingNewCluster;
					wrongMatchCandidate.remove(pair.getFilename2());
				} else {
					// wrongMatchCandidate.add(pair.getFilename2());
				}
			}
		}
		wrongMatch = wrongMatchCandidate.size();
		return new PairEvaluationResult(truePositive, trueNewCluster, falseNewCluster, wrongMatch, missingNewCluster);

	}

	public List<MatchingError> getErrors(Matching<T> matchingGold, Matching<T> matchingMeasure,
			Matching<T> matchingBaseline, Matching<T> matchingTrivial) {
		List<ObjectVersionPair> pairsGold = getPairs(matchingGold);
		List<ObjectVersionPair> pairsMeasure = getPairs(matchingMeasure);
		List<ObjectVersionPair> pairsBaseline = getPairs(matchingBaseline);

		Map<String, ObjectVersionPair> indexGold = Maps.uniqueIndex(pairsGold, ObjectVersionPair::getFilename2);
		Map<String, ObjectVersionPair> indexOutput = Maps.uniqueIndex(pairsMeasure, ObjectVersionPair::getFilename2);
		Map<String, ObjectVersionPair> indexBaseline = Maps.uniqueIndex(pairsBaseline, ObjectVersionPair::getFilename2);

		Set<String> indexTrivial = matchingTrivial.getMatches().stream()
				.map(i -> i.getRevisionId() + "-" + i.getPosition().getPositionString()).collect(Collectors.toSet());
		List<MatchingError> problems = new ArrayList<>();
		for (String filename2 : Sets.union(Sets.union(indexGold.keySet(), indexOutput.keySet()),
				indexBaseline.keySet())) {
			ObjectVersionPair gold = indexGold.get(filename2);
			ObjectVersionPair output = indexOutput.get(filename2);
			ObjectVersionPair baseline = indexBaseline.get(filename2);
			
			// what should the actual match have been matched to?
			String cluster1Gold = indexGold.containsKey(output.getFilename1()) ?  indexGold.get(output.getFilename1()).getCluster2() : null;

			ErrorType resultMatching = getErrorType(gold, output);
			ErrorType resultBaseline = baseline != null ? getErrorType(gold, baseline) : ErrorType.TP;
			problems.add(new MatchingError(resultMatching, resultBaseline, indexTrivial.contains(filename2), output.getFilename1(), filename2,gold.getFilename1(), cluster1Gold, gold.getCluster2()));
		}
		return problems;
	}

	private ErrorType getErrorType(ObjectVersionPair gold, ObjectVersionPair output) {
		ErrorType resultMatching = ErrorType.TP;
		if (!Objects.equal(gold, output)) {
			if (!gold.isSameCluster() && output.isSameCluster()) {
				resultMatching = ErrorType.FP;
			} else if (gold.isSameCluster() && !output.isSameCluster()) {
				resultMatching = ErrorType.FN;
			} else {
				resultMatching = ErrorType.FN_FP;
			}
		}
		return resultMatching;
	}

	public static <T extends Position> List<ObjectVersionPair> getPairs(Matching<T> matching) {
		ImmutableListMultimap<String, Match<T>> clusters = matching.getClusters();
		List<ObjectVersionPair> pairs = new ArrayList<>();
		for (String s : clusters.keySet()) {
			Match<T> last = null;
			for (Match<T> m : clusters.get(s)) {
				pairs.add(new ObjectVersionPair(
						last != null ? last.getRevisionId() + "-" + last.getPosition().getPositionString() : "",
						last != null ? s : "", m.getRevisionId() + "-" + m.getPosition().getPositionString(), s));

				last = m;
			}
		}
		return pairs;
	}
}
