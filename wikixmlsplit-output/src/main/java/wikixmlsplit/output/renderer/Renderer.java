package wikixmlsplit.output.renderer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.parser.parser.LinkTargetException;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.io.NodeDeserializer;
import wikixmlsplit.io.PageIO;
import wikixmlsplit.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Renders a parsed page to html (output by wikixmlsplit.parser.Reader), matches
 * the contained tables over multiple revisions and outputs the clusters to
 * individual folders.
 */
public class Renderer {
	
	private enum RenderTarget {
		TABLE, INFOBOX, LIST
	}

	@Parameter(names = "-input", description = "Input file")
	protected String inputFile;

	@Parameter(names = "-output", description = "Output path")
	protected String outputFile;
	
	@Parameter(names = "-renderTarget")
	protected RenderTarget renderTarget;

	private PageIO pageIO = new PageIO();

	public static void main(String[] args) throws Exception {
		Renderer main = new Renderer();
		JCommander.newBuilder().addObject(main).build().parse(args);
		main.run();
	}

	public void run() throws IOException {
		if (inputFile.endsWith(".parsed")) {
			// parse individual file
			processPath(Paths.get(inputFile));
		} else {
			// parse all files in a folder
			File dir = new File(inputFile);
			File[] files = dir.listFiles((d, name) -> name.endsWith(".parsed"));
			if (files == null) {
				System.err.println("No directory");
				return;
			}
			for (File f : files) {
				// System.out.println(f.toString());
				processPath(f.toPath());
			}
		}
	}

	private void processPath(Path inputPath) throws IOException {
		MyPageType page = pageIO.read(inputPath);

		HashMap<String, Multiset<String>> captions = new HashMap<>();
		HashMap<String, Multiset<String>> sections = new HashMap<>();

		IRenderTarget renderTarget = getRenderTarget();

		renderTarget.configureObjectStore(true, 0.99d, 0.6d, 0.4d, 0.95d);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withLocale(Locale.UK)
				.withZone(ZoneOffset.UTC);
		Path outputFolder = Paths.get(outputFile + "/" + Util.makeSafeFileName(page.getTitle()) + "/");
		Files.createDirectories(outputFolder);
		Writer revisionWriter = Files.newBufferedWriter(outputFolder.resolve("revisions.csv"));

		new NodeDeserializer().deserialize(page.getRevisions(), (r, nodes) -> {
			List<RenderResult> result = renderTarget.track(r, nodes);
			try {
				revisionWriter.append(String.valueOf(r.getId())).append(",").append(r.getInstant().toString())
						.append(",").append(String.valueOf(result.size())).append("\n");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			for (RenderResult res : result) {
				try {
					String html = MyHtmlRenderer.print(htmlCallback, wikiConfig,
							PageTitle.make(wikiConfig, page.getTitle()), res.getNode());
					Path folder = outputFolder.resolve(res.getHashCode() + "/");
					Files.createDirectories(folder);
					Writer w = Files.newBufferedWriter(folder.resolve(formatter.format(r.getInstant()) + "-"
							+ r.getId().toString() + "-" + res.getPositionString() + ".html"));

					if (res.getCaption() != null && !res.getCaption().isEmpty())
						w.write("<p>Caption:" + res.getCaption() + "</p>");
					if (res.getHeadings() != null && !res.getHeadings().isEmpty())
						w.write("<p>Headings:" + res.getHeadings() + "</p>");
					w.write(html);
					w.close();
				} catch (LinkTargetException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (res.getCaption() != null && !res.getCaption().isEmpty()) {
					captions.computeIfAbsent(res.getHashCode(), (a) -> HashMultiset.create()).add(res.getCaption());
				}

				if (res.getHeadings() != null && !res.getHeadings().isEmpty()) {
					sections.computeIfAbsent(res.getHashCode(), (a) -> HashMultiset.create()).add(res.getHeadings());
				}
			}
		});
		revisionWriter.close();

		rename(outputFolder, captions, sections);

	}

	private IRenderTarget getRenderTarget() {
		IRenderTarget renderTarget = null;
		switch(this.renderTarget) {
			case TABLE:
				renderTarget = new RenderTargetTable();
				break;
			case LIST:
				renderTarget = new RenderTargetLists();
				break;
			case INFOBOX:
				renderTarget = new RenderTargetInfobox();
				break;
			default:
				System.exit(1);
				break;
		}
		return renderTarget;
	}

	private void rename(Path outputFolder, HashMap<String, Multiset<String>> captions,
			HashMap<String, Multiset<String>> sections) {
		HashSet<String> keys = new HashSet<>(captions.keySet());
		keys.addAll(sections.keySet());

		for (String i : keys) {
			String maxCaption = getMostCommonElement(captions.get(i));
			String maxName = getMostCommonElement(sections.get(i));

			String newName = maxCaption + (maxCaption.isEmpty() || maxName.isEmpty() ? "" : " - ") + maxName;

			newName = newName.substring(0, Math.min(60, newName.length()));
			String fileName = Util.makeSafeFileName(i + " -" + newName);
			File dir = outputFolder.resolve(i + "/").toFile();

			if (!dir.isDirectory())
				continue;
			File newDir = new File(dir.getParent() + "/" + fileName);
			dir.renameTo(newDir);
		}

	}

	private String getMostCommonElement(Multiset<String> set) {
		int maxCount = 0;
		String maxCaption = "";
		if (set != null) {
			for (Multiset.Entry<String> e : set.entrySet()) {
				if (e.getCount() >= maxCount) {
					maxCaption = e.getElement();
					maxCount = e.getCount();
				}
			}
		}
		return maxCaption;
	}

	private WikiConfig wikiConfig = DefaultConfigEnWp.generate();
	private MyHtmlRendererCallback htmlCallback = new MyHtmlRendererCallback();
}
