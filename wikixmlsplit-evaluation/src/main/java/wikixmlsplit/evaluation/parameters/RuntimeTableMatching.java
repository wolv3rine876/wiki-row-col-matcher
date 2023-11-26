package wikixmlsplit.evaluation.parameters;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.evaluation.bases.ObjectType;
import wikixmlsplit.io.BuilderBase;
import wikixmlsplit.io.NodeDeserializer;
import wikixmlsplit.io.PageIO;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.ObjectStore;
import wikixmlsplit.matching.position.SimplePosition;
import wikixmlsplit.matching.similarity.MatchingObject;
import wikixmlsplit.matching.similarity.MatchingObjectBoWSimilarityWeighted;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuntimeTableMatching {
	
	@Parameter(names = "-input", description = "Input files")
	protected String inputFiles;

	@Parameter(names = "-output", description = "Output files")
	protected String outputFile;

	private final PageIO pageIO = new PageIO();

	private final Gson g = new GsonBuilder().setPrettyPrinting().create();

	private FileWriter w;
	private JsonWriter jw;

	@Parameter(names = "-type")
	protected ObjectType type;

	public static void main(String[] args) throws IOException {

		RuntimeTableMatching main = new RuntimeTableMatching();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}
	
	public void run() throws IOException {
		if (outputFile != null)
			w = new FileWriter(outputFile);

		File dir = new File(inputFiles);
		File[] files = dir.listFiles((d, name) -> name.endsWith(".parsed"));
		if (files == null) {
			System.err.println("input is no valid directory!");
			return;
		}
		jw = new JsonWriter(w);
		// jw.beginArray();
		for (File parsed : files) {
			run(parsed.toPath());
		}
		// jw.endArray();
		jw.close();
		if (w != null)
			w.close();
	}

	public void run(Path parsed) throws IOException {
		MyPageType page = pageIO.read(parsed);
		System.out.println("Handling: " + page.getTitle());
		evaluate(page, true);
		evaluate(page, false);
	}

	protected void evaluate(MyPageType page, boolean useFirstMatchingPhase) {
		

		NodeDeserializer deserializer = new NodeDeserializer();

		Matching<SimplePosition> matching = new Matching<>();
		ObjectStore<MatchingObject> store = getObjectStore(useFirstMatchingPhase ? 0.99d : 2.0d, 0.6d, 0.4d, 0.95d, false);

		BuilderBase<? extends MatchingObject> tableBuilder = type.getBuilder();
		deserializer.deserialize(page.getRevisions(), (r, nodes) -> {
			List<MatchingObject> tables = (List<MatchingObject>) tableBuilder.constructNewObjects(nodes);

			long previousMatchTime = store.getMatchTime();
			long previousTime = System.nanoTime();
			int previousSize = store.size();
			store.handleNewRevision(tables, (tracked) -> {
				if (!tracked.isActive()) {
					return;
				}
				matching.add(tracked.getIdentifier(), r.getId(), (SimplePosition) tracked.getCurrentPosition(),
						!Objects.equal(tracked.getPrevObject(), tracked.getObject()));

			}, r, type.getMapper());
			Map<String, Object> map = new HashMap<>();
			map.put("useFirstPhase", useFirstMatchingPhase);
			map.put("timeMatch", store.getMatchTime() - previousMatchTime);
			map.put("time",  System.nanoTime() - previousTime);
			map.put("page", page.getTitle());
			map.put("tableObjectCountPrevious", previousSize);
			map.put("tableObjectCountNow", store.size());
			map.put("newTableCount", tables.size());
			map.put("type", type);
			
			g.toJson(map, Map.class, jw);
			try {
				w.append("\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

	}

	protected ObjectStore<MatchingObject> getObjectStore(double limit1, double limit2, double limit3, double relaxLimit,
			boolean greedy) {
		
		MatchingObjectBoWSimilarityWeighted sim = new MatchingObjectBoWSimilarityWeighted(true);
		MatchingObjectBoWSimilarityWeighted sim2 = new MatchingObjectBoWSimilarityWeighted(false);

		return ObjectStore.createDefault(sim, sim2, limit1, limit2, limit3, relaxLimit, greedy);
	}
	
	
}
