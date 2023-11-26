package wikixmlsplit.evaluation.statistics;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import wikixmlsplit.util.InstantTypeAdapter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TableAge {
	@Parameter(names = "-input", description = "Input files")
	protected String inputFiles;

	@Parameter(names = "-output", description = "Output files")
	protected String outputFile;

	private final Gson g = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantTypeAdapter())
			.setPrettyPrinting().create();

	private FileWriter w;
	private JsonWriter jw;

	public static void main(String[] args) throws IOException {

		TableAge main = new TableAge();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.run();
	}

	public void run() throws IOException {
		if (outputFile != null)
			w = new FileWriter(outputFile);


		jw = new JsonWriter(w);
		Map<Instant, UpdateCount> missed = createSnapshots();

		Files.lines(Paths.get(inputFiles)).forEach(line -> {
			List<VersionInfo> versions = g.fromJson(line, TableInfo.class).versions;

			String content = versions.get(versions.size() - 1).type.equals("DELETE") ? null
					: versions.get(versions.size() - 1).contentHash;

			for (Entry<Instant, UpdateCount> e : missed.entrySet()) {

				String currentContent = null;
				for (VersionInfo v : versions) {
					if (!e.getKey().isBefore(v.validFrom)) {
						currentContent = v.type.equals("DELETE") ? null : v.contentHash;
					} else {
						break;
					}
				}
				if (Objects.equal(currentContent, content)) {
					if(currentContent == null) {
						e.getValue().missed++;
					} else {
						e.getValue().unchanged++;
					}
				} else if (currentContent == null) {
					e.getValue().inserts++;
				} else if (content == null) {
					e.getValue().deletes++;
				} else {
					e.getValue().updates++;
				}

			}
		});
		
		for(UpdateCount c : missed.values()) {
			g.toJson(c, UpdateCount.class, jw);
			try {
				w.append("\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

		jw.close();
		if (w != null)
			w.close();
	}

	private Map<Instant, UpdateCount> createSnapshots() {
		Map<Instant, UpdateCount> missed = new HashMap<>();

		Instant start = Instant.parse("2000-01-01T00:00:00.00Z");
		Instant end = Instant.parse("2019-09-01T00:00:00.00Z");
		ZonedDateTime current = end.atZone(ZoneOffset.UTC);
		while (!current.isBefore(start.atZone(ZoneOffset.UTC))) {
			missed.put(current.toInstant(), new UpdateCount(current.toInstant()));
			current = current.minus(1, ChronoUnit.MONTHS);

		}
		return missed;
	}

	private static class TableInfo {
		private List<VersionInfo> versions;
	}

	private static class VersionInfo {
		private String type;
		private Instant validFrom;
		private Instant validTo;
		private String contentHash;
	}

	private static class UpdateCount {
		private Instant valid;
		private int missed;
		private int inserts;
		private int deletes;
		private int updates;
		private int unchanged;
		
		public UpdateCount(Instant valid) {
			this.valid = valid;
		}

		@Override
		public String toString() {
			return "UpdateCount [inserts=" + inserts + ", deletes=" + deletes + ", updates=" + updates + ", unchanged="
					+ unchanged + "]";
		}
	}
}
