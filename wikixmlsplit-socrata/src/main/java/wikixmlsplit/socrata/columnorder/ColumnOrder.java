package wikixmlsplit.socrata.columnorder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.Iterators;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ColumnOrder {
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Parameter(names = "-input")
	private String input;

	@Parameter(names = "-output")
	private String output;

	public static void main(String[] args) throws IOException {
		ColumnOrder main = new ColumnOrder();
		JCommander.newBuilder().addObject(main).build().parse(args);

		main.run();
	}

	private void run() throws IOException {
		Path file = Paths.get(input);
		List<Path> files = Arrays.stream(file.toFile().listFiles(File::isFile)).map(File::toPath)
				.collect(Collectors.toList());

		try (FileWriter w = new FileWriter(output); JsonWriter jw = new JsonWriter(w)) {
			for (Path p : files) {
				String id = p.getFileName().toString().substring(0, 9);
				List<String> result = getColumnOrder(p);

				gson.toJson(new OrderedSchema(id, result), OrderedSchema.class, jw);
				try {
					w.append("\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	protected List<String> getColumnOrder(Path file) throws IOException {
		List<String> lines = Files.readAllLines(file);

		Set<String> columns = new HashSet<>();
		for (String s : lines) {
			String[] line = gson.fromJson(s, String[].class);

			columns.addAll(Arrays.asList(line));
		}

		Graph<String, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);

		for (String column : columns) {
			g.addVertex(column);
		}

		for (String s : lines) {
			String[] line = gson.fromJson(s, String[].class);

			for (int i = 0; i < line.length - 1; ++i) {
				g.addEdge(line[i], line[i + 1]);
			}
		}

		List<String> result = new ArrayList<>();
		Iterators.addAll(result, new TopologicalOrderIterator<>(g));

		if (result.size() != columns.size())
			throw new IllegalStateException();
		return result;
	}
}
