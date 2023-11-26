package wikixmlsplit.evaluation.evaluators;

import com.beust.jcommander.Parameter;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.evaluation.bases.AbstractGSRunner;
import wikixmlsplit.evaluation.bases.ObjectEvaluation;
import wikixmlsplit.evaluation.bases.ObjectEvaluation.ObjectEvaluationResult;
import wikixmlsplit.evaluation.bases.PairEvaluation;
import wikixmlsplit.evaluation.data.MatchingError;
import wikixmlsplit.evaluation.data.MatchingError.ErrorType;
import wikixmlsplit.evaluation.data.PairEvaluationResult;
import wikixmlsplit.evaluation.gsreader.GSReader;
import wikixmlsplit.io.NodeDeserializer;
import wikixmlsplit.matching.Matcher;
import wikixmlsplit.matching.MatcherBuilder;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.position.Position;
import wikixmlsplit.matching.ranking.ActiveTimeRanking;
import wikixmlsplit.matching.ranking.PositionRanking;
import wikixmlsplit.matching.ranking.SimilarityRanking;
import wikixmlsplit.matching.ranking.SimilarityRankingPositionRestricted;
import wikixmlsplit.matching.similarity.MatchingObject;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;
import wikixmlsplit.matching.similarity.SimilarityMeasure;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class TimeResolutionEvaluation<E extends MatchingObject, P extends Position> extends AbstractGSRunner {

	protected SimilarityMeasure<MatchingObject> sim;
	protected SimilarityMeasure<MatchingObject> sim2;

	@Parameter(names = "-best", description = "Use best config")
	protected boolean useBestConfig;
	
	@Parameter(names = "-windowSizeStage1", description = "Window size for stage 1")
	protected int windowSizeStage1 = 2;
	
	@Parameter(names = "-allResolutions", description = "Use all resolutions")
	protected boolean useAllResolutions;

	protected boolean errorOutput;

	@Parameter(names = "-errorOutputMatrix", description = "Use best config")
	protected String errorOutputMatrix;

	private List<MatchingError> errors = new ArrayList<>();

	@Override
	public void run() throws IOException {
		this.errorOutput = errorOutputMatrix != null;

		initSims();
		super.run();

		if (errorOutputMatrix != null) {
			outputErrorMatrix();
		}

	}

	protected void outputErrorMatrix() {
		try(PrintWriter pw = new PrintWriter(errorOutputMatrix)) {
			for (boolean trivial : new boolean[] { true, false }) {
				for (ErrorType eOutput : ErrorType.values()) {
					for (ErrorType eBaseline : ErrorType.values()) {
						List<MatchingError> errorList = errors.stream().filter(i -> i.getType() == eOutput
								&& i.getBaselineErrorType() == eBaseline && i.isTrivial() == trivial)
								.collect(Collectors.toList());
						if (errorList.isEmpty())
							continue;
						pw.println(trivial + "," + eOutput + "," + eBaseline + "," + errorList.size());
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	protected void initSims() throws IOException {
		sim = new MatchingObjectBoWSimilarityWeighted(true);
		sim2 = new MatchingObjectBoWSimilarityWeighted(false);
	}

	protected void evaluate(MyPageType page, Path inputFolder) throws IOException {

		long[] timeResolutions = useAllResolutions
				? new long[] { 0, 1000L * 60 * 60, 1000L * 60 * 60 * 24, 1000L * 60 * 60 * 24 * 7,
						1000L * 60 * 60 * 24 * 30, 1000L * 60 * 60 * 24 * 365 }
				: new long[] { 0 };

		for (long timeResolution : timeResolutions) {
			System.out.println(timeResolution);
			NodeDeserializer deserializer = new NodeDeserializer(timeResolution);
			evaluteAtTimeResolution(page, inputFolder, deserializer);
		}

	}

	@SuppressWarnings("unchecked")
	private void evaluteAtTimeResolution(MyPageType page, Path inputFolder, NodeDeserializer deserializer)
			throws IOException {
		Matching<P> matchingGold = getGSReader().loadMatching(deserializer, inputFolder, page);
		List<ObjectStore<E>> stores = new ArrayList<>();

		List<String> configs = new ArrayList<>();

		if (useBestConfig) {
			createBestConfig(stores, configs);
		} else {
			createConfigs(stores, configs);
		}

		List<Matching<P>> matchings = new ArrayList<>();
		for (int i = 0; i < stores.size(); ++i)
			matchings.add(new Matching<>());
		Matching<P> positionBaseline = new Matching<>();

		AtomicInteger revisionsCount = new AtomicInteger(0);
		AtomicInteger tableRevisions = new AtomicInteger(0);
		AtomicInteger obviousMatches = new AtomicInteger(0);
		AtomicInteger maximumObjectCount = new AtomicInteger(0);
		List<E> prev = new ArrayList<>();

		Matching<P> matchingTrivial = new Matching<>();
		//AtomicBoolean firstVersion = new AtomicBoolean(true);

		Map<String, E> fileIndex = new HashMap<>();
		deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
			List<E> tables = getNewObjects(r, nodes);
			for (int i = 0; i < tables.size(); ++i) {
				fileIndex.put(r.getId() + "-" + i, tables.get(i));
			}

			revisionsCount.incrementAndGet();
			tableRevisions.addAndGet(tables.size());
			maximumObjectCount.getAndAccumulate(tables.size(), Math::max);

			int equalCount = 0;
			for (int pos = 0; pos < tables.size(); ++pos) {
				boolean equal = prev.size() > pos && Objects.equals(prev.get(pos), tables.get(pos));
				if (equal)
					++equalCount;
				positionBaseline.add("pos" + pos, r.getId(), getPosition(pos), !equal);
			}
			if (equalCount >= tables.size() - 1) {
				obviousMatches.addAndGet(equalCount);

				for (int pos = 0; pos < tables.size(); ++pos) {
					boolean equal =  prev.size() > pos && Objects.equals(prev.get(pos), tables.get(pos));
					if (equal) {
						matchingTrivial.add("pos" + pos, r.getId(), getPosition(pos), !equal);
					}
				}
			}

			for (int i = 0; i < stores.size(); ++i) {
				final Matching<P> m = matchings.get(i);
				stores.get(i).handleNewRevision(tables, (tracked) -> {
					if (!tracked.isActive()) {
						return;
					}
					m.add(tracked.getIdentifier(), r.getId(), (P) tracked.getCurrentPosition(),
							!Objects.equals(tracked.getPrevObject(), tracked.getObject()));

				}, r, getMapper());
			}

			prev.clear();
			prev.addAll(tables);
		});

		if(outputFile != null) {
			for (int i = 0; i < stores.size(); ++i) {

				PairEvaluationResult res = new PairEvaluation<P>().measure(matchingGold, matchings.get(i));
				List<ObjectEvaluationResult> resObject = new ObjectEvaluation<P>().measure(matchingGold, matchings.get(i));

				Map<String, Object> result = getResultMap(page, deserializer, matchingGold, revisionsCount, tableRevisions, obviousMatches, maximumObjectCount, res, resObject, configs.get(i));
				result.put("matchTime", stores.get(i).getMatchTime());
				g.toJson(result, Map.class, jw);
				try {
					w.append("\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			PairEvaluationResult resBaseline = new PairEvaluation<P>().measure(matchingGold, positionBaseline);
			List<ObjectEvaluationResult> resObject = new ObjectEvaluation<P>().measure(matchingGold, positionBaseline);
			Map<String, Object> result = getResultMap(page, deserializer, matchingGold, revisionsCount, tableRevisions, obviousMatches, maximumObjectCount, resBaseline, resObject, "baseline");
			g.toJson(result, Map.class, jw);
			try {
				w.append("\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (errorOutput)
			outputErrors(matchingGold, stores, matchings, positionBaseline, matchingTrivial);

		if(w != null)
			w.flush();
	}

	private Map<String, Object> getResultMap(MyPageType page, NodeDeserializer deserializer, Matching<P> matchingGold, AtomicInteger revisionsCount, AtomicInteger tableRevisions, AtomicInteger obviousMatches, AtomicInteger maximumObjectCount, PairEvaluationResult res, List<ObjectEvaluationResult> resObject, String s) {
		Map<String, Object> result = new HashMap<>(res.getResultMap());
		result.put("pagename", page.getTitle());
		result.put("timeResolution", deserializer.getTimeResolution());
		result.put("objectEvaluation", resObject);
		result.put("config", s);
		result.put("revisionCount", revisionsCount.get());
		result.put("tableRevisions", tableRevisions.get());
		result.put("obviousMatches", obviousMatches.get());
		result.put("clusterCount", matchingGold.getClusterCount());
		result.put("maximumObjectCount", maximumObjectCount.get());
		return result;
	}

	protected void outputErrors(Matching<P> matchingGold, List<ObjectStore<E>> stores,
								List<Matching<P>> matchings, Matching<P> positionBaseline,
								Matching<P> matchingTrivial) {
		for (int i = 0; i < stores.size(); ++i) {

			List<MatchingError> res = new PairEvaluation<P>().getErrors(matchingGold, matchings.get(i),
					positionBaseline, matchingTrivial);

			errors.addAll(res);
		}
	}

	protected abstract void createBestConfig(List<ObjectStore<E>> stores, List<String> configs);

	protected abstract GSReader<E, P> getGSReader();

	protected abstract List<E> getNewObjects(MyRevisionType r, Map<String, List<String>> nodes);

	protected abstract BiFunction<E, Integer, P> getMapper();

	protected abstract P getPosition(int pos);

	protected void createConfigs(List<ObjectStore<E>> stores, List<String> configs) {
		double[] limits = new double[] { 0.2d, 0.4d, 0.6d, 0.8d };
		double l1 = 0.99d;
		double rL = 0.95d;

		for (double l2 : limits) {
			for (double l3 : limits) {
				if (l1 <= 0.8d || l2 <= 0.8d || l3 <= 0.8d) {
					stores.add(getObjectStore(l1, l2, l3, rL));
					configs.add(l1 + "," + l2 + "," + l3 + "," + rL + ",OURAPPROACH-HUNGARIAN");
				}
			}
		}

		stores.add(getObjectStoreNoPos(0.6d, 0.4d, rL, false));
		configs.add("2.0d," + 0.6d + "," + 0.4d + "," + rL + ",OURAPPROACH-HUNGARIAN-NO-POS");
	}

	protected ObjectStore<E> getObjectStore(double limit1, double limit2, double limit3, double relaxLimit) {

		List<Matcher<E>> matchers = new ArrayList<>();

		if (limit1 <= 1.0d)
			matchers.add(new MatcherBuilder<E>()
					.addRanking(new SimilarityRankingPositionRestricted<>(sim, limit1, relaxLimit, windowSizeStage1))
					.addRanking(new ActiveTimeRanking<>()).addRanking(new PositionRanking<>()).createMatcher(false));
		if (limit2 <= 1.0d)
			matchers.add(MatcherBuilder.createDefaultSimMatcher(sim, limit2, relaxLimit, false));
		if (limit3 <= 1.0d)
			matchers.add(MatcherBuilder.createDefaultSimMatcher(sim2, limit3, relaxLimit, false));
		return new ObjectStore<>(matchers);
	}

	protected ObjectStore<E> getObjectStoreNoPos(double limit2, double limit3, double relaxLimit, boolean greedy) {

		List<Matcher<E>> matchers = new ArrayList<>();

		if (limit2 <= 1.0d) {
			matchers.add(new MatcherBuilder<E>().addRanking(new SimilarityRanking<>(sim, limit2, relaxLimit))
					.addRanking(new PositionRanking<>()).createMatcher(greedy, true));
		}

		if (limit3 <= 1.0d) {
			matchers.add(new MatcherBuilder<E>().addRanking(new SimilarityRanking<>(sim2, limit3, relaxLimit))
					.addRanking(new PositionRanking<>()).createMatcher(greedy, true));
		}
		return new ObjectStore<>(matchers);
	}
}
