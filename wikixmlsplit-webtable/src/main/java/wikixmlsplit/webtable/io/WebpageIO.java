package wikixmlsplit.webtable.io;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import wikixmlsplit.webtable.Webtable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WebpageIO {
	public static List<Revision> readWebpageRevisions(Path parsed) throws IOException {
		Gson g = new Gson();
		JsonReader reader = new JsonReader(new FileReader(parsed.toFile()));
		List<List<String>> x = g.fromJson(reader, List.class);
		Elements last = new Elements();
		List<Revision> revisions = new ArrayList<>();

		for (int i = 1; i < x.size(); ++i) {
			List<String> metaData = x.get(i);
			File content = new File(parsed + "-" + i + ".html");
			String input = new String(Files.readAllBytes(content.toPath()))
					.replaceAll("(http:)?(//web.archive.org)?/web/" + metaData.get(1) + "([a-z]{2}_)?/", "");
			Document doc = Jsoup.parse(input, "http://web.archive.org/");
			Elements tables = doc.select("table:not(:has(div))");
			if (containsNewTableRevision(last, tables)) {
				revisions.add(new Revision(metaData, doc));
			}

			last = tables;
		}
		return revisions;
	}
	
	private static boolean containsNewTableRevision(Elements last, Elements tables) {
		boolean isNewRevision = last.size() != tables.size();
		for (int j = 0; j < Math.min(last.size(), tables.size()); ++j) {
			isNewRevision |= !(tables.get(j)).outerHtml().equals(last.get(j).outerHtml());
		}

		return isNewRevision;
	}
	
	public static List<Webtable> constructWebTables(Revision r) {
		Elements tables = r.getContent().select("h1,h2,h3,h4,h5,h6,table:not(:has(table))");
		List<Webtable> webtables = new ArrayList<>();
		String[] header = new String[6];
		int tableIndex = 0;
		for (Element e : tables) {
			int headerIndex = 0;
			if (e.tag().equals(Tag.valueOf("table"))) {
				Webtable table = new Webtable(e, header);
				table.setFilename(r.getMetadata().get(1) + "-" + r.getMetadata().get(1) + "-" + tableIndex + ".html");
				webtables.add(table);
				continue;
			} else if (e.tag().equals(Tag.valueOf("h1"))) {
				headerIndex = 0;
			} else if (e.tag().equals(Tag.valueOf("h2"))) {
				headerIndex = 1;
			} else if (e.tag().equals(Tag.valueOf("h3"))) {
				headerIndex = 2;
			} else if (e.tag().equals(Tag.valueOf("h4"))) {
				headerIndex = 3;
			} else if (e.tag().equals(Tag.valueOf("h5"))) {
				headerIndex = 4;
			} else if (e.tag().equals(Tag.valueOf("h6"))) {
				headerIndex = 5;
			}

			header[headerIndex++] = Util.cleanCell(e.text());
			for (; headerIndex < 6; ++headerIndex) {
				header[headerIndex] = null;
			}
		}
		return webtables;
	}

}
