package wikixmlsplit.evaluation.parameters;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableListMultimap;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.evaluation.bases.AbstractGSRunner;
import wikixmlsplit.evaluation.bases.ObjectType;
import wikixmlsplit.io.BuilderBase;
import wikixmlsplit.io.NodeDeserializer;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.data.Match;
import wikixmlsplit.matching.position.SimplePosition;
import wikixmlsplit.matching.similarity.MatchingObject;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarity;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DeletionAndRecreationCount extends AbstractGSRunner {

	@Parameter(names = "-type")
	protected ObjectType type;

	private Set<String> currentlyExisting;

	private int count = 0;
	public static void main(String[] args) throws IOException {

		DeletionAndRecreationCount main = new DeletionAndRecreationCount();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}
	
	@Override
	public void run() throws IOException {
		this.json = false;
		super.run();
		if (outputFile != null) {
			try(PrintWriter writer = new PrintWriter(outputFile)) {
				writer.println(count);
			}
		}
	}

	@Override
	protected void evaluate(MyPageType page, Path inputFolder) {
		NodeDeserializer deserializer = new NodeDeserializer(0);

		Matching<SimplePosition> matchingGold = (Matching<SimplePosition>) type.getGSReader().loadMatching(deserializer,
				inputFolder, page);
		ImmutableListMultimap<BigInteger, Match<SimplePosition>> onlyChangesByRevision = matchingGold
				.getOnlyChangesByRevision();

		BuilderBase<? extends MatchingObject> objectBuilder = type.getBuilder();

		Map<String, MatchingObject> previousVersion = new HashMap<>();
		currentlyExisting = Collections.emptySet();

		MatchingObjectBoWSimilarity sim = new MatchingObjectBoWSimilarity(true);
		

		deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
			List<? extends MatchingObject> objects = objectBuilder.constructNewObjects(nodes);
			List<Match<SimplePosition>> changes = onlyChangesByRevision.get(r.getId());

			for (Match<SimplePosition> m : changes) {
				MatchingObject table = objects.get(m.getPosition().getIndex());

				MatchingObject previous = previousVersion.put(m.getClusterIdentifier(), table);
				if(previous != null && !currentlyExisting.contains(m.getClusterIdentifier()) && sim.getSimilarity(previous, table) >= 0.95d) {
					count++;
				}
			}

			currentlyExisting = changes.stream().map(Match::getClusterIdentifier).collect(Collectors.toSet());
		});
	}

}
