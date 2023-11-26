package wikixmlsplit.webtable.similarity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import wikixmlsplit.evaluation.evaluators.MemberColumnDetector;
import wikixmlsplit.matching.HungarianAlgorithm;
import wikixmlsplit.matching.similarity.SimilarityMeasure;
import wikixmlsplit.webtable.Webtable;
import wikixmlsplit.anchored.features.ColumnIntersectCounter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class WebtableAnchoredSimilarity implements SimilarityMeasure<Webtable> {

	private MemberColumnDetector<List<String>> detect = new MemberColumnDetector<>(d -> d);
	private ColumnIntersectCounter<List<String>> counter = new ColumnIntersectCounter<>(Function.identity());

	private final LoadingCache<Webtable, Integer> memberColumnCache;
	private final LoadingCache<WeightWrapper, int[]> hungarianCache;

	public WebtableAnchoredSimilarity(Map<String, Integer> subjectColumns) {
		this.memberColumnCache = CacheBuilder.newBuilder().maximumSize(1000)
				.build(new CacheLoader<>() {

					@Override
					public Integer load(Webtable key) {

						if (subjectColumns != null) {
							Integer i = subjectColumns.get(key.getFilename());
							if (i != null) {
								if (i >= key.getColumns().size()) {
									System.err.println("subject column " + i + " does not exist for " + key.getFilename());
								} else {
									return i;
								}
							} else {
								System.out.println("could not load subject column for " + key.getFilename());
							}
						}
						return detect.find(key.getColumns());
					}
				});

		this.hungarianCache = CacheBuilder.newBuilder().maximumSize(1000)
				.build(new CacheLoader<>() {
					@Override
					public int[] load(WeightWrapper weights) {
						HungarianAlgorithm algorithm = new HungarianAlgorithm(weights.weights);
						return algorithm.execute();
					}
				});
	}

	@Override
	public double getSimilarity(Webtable t1, Webtable t2) {
		List<List<String>> columns = t1.getColumns();
		if (columns.isEmpty())
			return 0.0d;

		int memberColumn = memberColumnCache.getUnchecked(t1);
		

		List<List<String>> prevColumns = t2.getColumns();
		if (prevColumns.isEmpty())
			return 0.0d;

		int memberColumnPrev = memberColumnCache.getUnchecked(t2);
		
		double[][] weights = counter.getIntersectCount(prevColumns, columns, memberColumnPrev, memberColumn);

		int[] assignment = hungarianCache.getUnchecked(new WeightWrapper(weights));
		double selectedSum = 0;
		for (int i = 0; i < weights.length; ++i) {
			if (assignment[i] >= 0)
				selectedSum += weights[i][assignment[i]];
		}

		return -selectedSum / Math.max(columns.size() * columns.get(0).size(),
				prevColumns.size() * prevColumns.get(0).size());
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
