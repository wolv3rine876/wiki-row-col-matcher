package wikixmlsplit.matching.similarity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CachingSimilarity<T> implements SimilarityMeasure<T> {

	private final LoadingCache<Pair<T>, Double> sims;
	private int count;

	private String name;

	public CachingSimilarity(SimilarityMeasure<T> sim, String name) {
		this(sim);
		this.name = name;
	}

	public CachingSimilarity(SimilarityMeasure<T> sim) {
		sims = CacheBuilder.newBuilder().recordStats().maximumSize(1000).expireAfterWrite(3, TimeUnit.MINUTES).build(new CacheLoader<>() {
            public Double load(Pair<T> key) {
                return sim.getSimilarity(key.getObject1(), key.getObject2());
            }
        });
		if(name == null)
			name = sim.toString();
	}

	@Override
	public double getSimilarity(T object1, T object2) {
		try {
			if (count++ == 500000) {
				if (name != null) {
					System.out.println(name);
				}
				System.out.println(sims.stats());
				count = 0;
			}
			return sims.get(new Pair<>(object1, object2));
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0.0;
	}

}
