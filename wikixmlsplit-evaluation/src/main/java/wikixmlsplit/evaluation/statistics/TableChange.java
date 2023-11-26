package wikixmlsplit.evaluation.statistics;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
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

public class TableChange {
	@Parameter(names = "-input", description = "Input files")
	protected String inputFiles;

	@Parameter(names = "-output", description = "Output files")
	protected String outputFile;

	private final Gson g = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantTypeAdapter())
			.setPrettyPrinting().create();

	private FileWriter w;
	private JsonWriter jw;

	public static void main(String[] args) throws IOException {

		TableChange main = new TableChange();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.run();
	}

	public void run() throws IOException {
		if (outputFile != null)
			w = new FileWriter(outputFile);

		jw = new JsonWriter(w);
		Map<Integer, Multiset<Integer>> age = new HashMap<>();

		ZonedDateTime end = Instant.parse("2019-09-01T00:00:00.00Z").atZone(ZoneOffset.UTC);
		Files.lines(Paths.get(inputFiles)).forEach(line -> {
			List<VersionInfo> versions = g.fromJson(line, TableInfo.class).versions;

			Instant firstVersion = versions.get(0).validFrom;

			ZonedDateTime time = firstVersion.atZone(ZoneOffset.UTC);
			int monthAge = (int) ChronoUnit.MONTHS.between(time, end);
			for (int i = 0; i < monthAge; ++i) {
				time = time.plus(1, ChronoUnit.MONTHS);
				Instant currentVersion = null;
				for (VersionInfo v : versions) {
					if (!time.toInstant().isBefore(v.validFrom)) {
						currentVersion = v.type.equals("DELETE") ? null : v.validFrom;
					} else {
						break;
					}
				}
				if (currentVersion != null)
					age.computeIfAbsent(i, (a) -> HashMultiset.create())
							.add((int) ChronoUnit.DAYS.between(currentVersion, time));

			}
		});

		for (Entry<Integer, Multiset<Integer>> c : age.entrySet()) {

			for (com.google.common.collect.Multiset.Entry<Integer> e : c.getValue().entrySet()) {
				Map<String, Object> obj = new HashMap<>();
				obj.put("month", c.getKey());
				obj.put("age", e.getElement());
				obj.put("count", e.getCount());
				g.toJson(obj, HashMap.class, jw);
				try {
					w.append("\n");
				} catch (IOException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}
			}
		}

		jw.close();
		if (w != null)
			w.close();
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

}
