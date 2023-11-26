package wikixmlsplit.socrata.similarity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterators;
import wikixmlsplit.anchored.features.ColumnIntersectCounter;
import wikixmlsplit.matching.HungarianAlgorithm;
import wikixmlsplit.matching.similarity.SimilarityMeasure;
import wikixmlsplit.socrata.SocrataTable;

import java.util.*;
import java.util.function.Function;

public class SocrataTableAnchoredSimilarity implements SimilarityMeasure<SocrataTable> {

	private ColumnIntersectCounter<List<String>> counter = new ColumnIntersectCounter<>(Function.identity());

	private final Map<String, List<Integer>> memberColumnsMap;
	private final LoadingCache<WeightWrapper, int[]> hungarianCache;

	public SocrataTableAnchoredSimilarity(Map<String, List<Integer>> keys) {
						this.memberColumnsMap = keys;
		this.hungarianCache = CacheBuilder.newBuilder().maximumSize(1000)
				.build(new CacheLoader<>() {
                    @Override
                    public int[] load(WeightWrapper weights) {
                        HungarianAlgorithm algorithm = new HungarianAlgorithm(weights.weights);
                        return algorithm.execute();
                    }
                });
	}
	
	public int getMemberColumn(SocrataTable t1) {
		int column =  Iterators.getOnlyElement(memberColumnsMap.get(t1.getMeta().resource.id).iterator());
		if(column >= t1.getHeader().size())
			column = 0;
		return column;
	}

	@Override
	public double getSimilarity(SocrataTable t1, SocrataTable t2) {
		List<List<String>> columns = t1.getColumns();
		if (columns.isEmpty())
			return 0.0d;

		int memberColumn = getMemberColumn(t1);

		List<List<String>> prevColumns = t2.getColumns();
		if (prevColumns.isEmpty())
			return 0.0d;

		int memberColumnPrev = getMemberColumn(t2);

		if (!hasOverlap(columns.get(memberColumn), prevColumns.get(memberColumnPrev))) {
			return 0.0d;
		}

		double[][] weights = counter.getIntersectCount(prevColumns, columns, memberColumnPrev, memberColumn);

		int[] assignment = hungarianCache.getUnchecked(new WeightWrapper(weights));
		double selectedSum = 0;
		for (int i = 0; i < weights.length; ++i) {
			if (assignment[i] >= 0)
				selectedSum += weights[i][assignment[i]];
		}

		return -selectedSum
				/ Math.max(columns.size() * columns.get(0).size(), prevColumns.size() * prevColumns.get(0).size());
	}

	private boolean hasOverlap(List<String> list, List<String> list2) {
		if (list.size() < list2.size()) {
			Set<String> set = new HashSet<>(list);
			for (String s : list2) {
				if (set.contains(s))
					return true;
			}
		} else {
			Set<String> set = new HashSet<>(list2);
			for (String s : list) {
				if (set.contains(s))
					return true;
			}
		}

		return false;
	}

	private static class WeightWrapper {
		final double[][] weights;
		final int hash;

		public WeightWrapper(double[][] weights) {
			this.weights = weights;
			this.hash = Arrays.deepHashCode(weights);
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof WeightWrapper))
				return false;

			WeightWrapper wrapper = (WeightWrapper) obj;
			return Arrays.deepEquals(weights, wrapper.weights);
		}
	}

}
