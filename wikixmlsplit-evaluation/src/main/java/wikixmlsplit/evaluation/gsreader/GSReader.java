package wikixmlsplit.evaluation.gsreader;

import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.io.NodeDeserializer;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.position.Position;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

public abstract class GSReader<E, P extends Position> {



	public Matching<P> loadMatching(NodeDeserializer deserializer, Path inputFolder, MyPageType page) {
		return loadMatching(deserializer, inputFolder, page, (a, b) -> {
		});
	}

	public Matching<P> loadMatching(NodeDeserializer deserializer, Path inputFolder, MyPageType page,
			BiConsumer<RevisionData, List<E>> consumer) {
		File[] directories = inputFolder.toFile().listFiles(File::isDirectory);

		Matching<P> matching = new Matching<>();

		Map<String, E> previousVersion = new HashMap<>();

		Map<String, String> fileMap = new HashMap<>();
		for (File dir : directories) {
			File[] files = dir.listFiles(File::isFile);
			for (File file : files) {
				if(!file.getName().contains("-"))
					continue;
				fileMap.put(file.getName().split("-", 2)[1], dir.getName());
			}
		}

		List<RevisionData> revisionList = new ArrayList<>();
		if (deserializer.getTimeResolution() > 0) {
			new NodeDeserializer(0).deserialize(page.getRevisions(), (r, nodes) -> revisionList.add(r));
		}

		deserializer.deserialize(page.getRevisions(), (searchR, nodes) -> {
			RevisionData r = null;
			if (deserializer.getTimeResolution() > 0) {
				for (RevisionData rev : revisionList) {
					if (!rev.getInstant().isAfter(searchR.getInstant())) {
						r = rev;
					} else
						break;
				}
			} else {
				r = searchR;
			}

			List<E> tables = getNewObjects(nodes);
			consumer.accept(r, tables);

			for (int i = 0; i < tables.size(); ++i) {
				E table = tables.get(i);
				// TODO: empty table check? (they might not have been labeled in the gold
				// standard)

				String fileName = r.getId().toString() + "-" + i + ".html";

				String clusterName = fileMap.get(fileName);
				if (clusterName != null) {
					matching.add(clusterName, searchR.getId(), getPosition(i),
							!Objects.equals(previousVersion.get(clusterName), table));
					previousVersion.put(clusterName, table);
				} else {
					System.err.println("did not find revision " + fileName);
				}

			}
		});
		return matching;
	}

	protected abstract List<E> getNewObjects(Map<String, List<String>> nodes);

	protected abstract P getPosition(int pos);

}
