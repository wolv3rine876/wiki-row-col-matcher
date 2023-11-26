package wikixmlsplit.evaluation.gsreader;

import com.google.common.base.Objects;
import wikixmlsplit.matching.Matching;
import wikixmlsplit.webtable.Webtable;
import wikixmlsplit.webtable.io.Revision;
import wikixmlsplit.webtable.io.WebpageIO;
import wikixmlsplit.webtable.position.WebtablePosition;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GSReaderWebtable {

    public static Matching<WebtablePosition> loadMatching(Path inputFolder, List<Revision> revisions) {
        File[] directories = inputFolder.toFile().listFiles(File::isDirectory);

        Matching<WebtablePosition> matching = new Matching<>();

        Map<String, Webtable> previousVersion = new HashMap<>();

        Map<String, String> fileMap = new HashMap<>();
        for (File dir : directories) {
            File[] files = dir.listFiles(File::isFile);
            for (File file : files) {
                fileMap.put(file.getName(), dir.getName());
            }
        }

        for (Revision r : revisions) {

            List<Webtable> tables = WebpageIO.constructWebTables(r);

            for (int i = 0; i < tables.size(); ++i) {
                Webtable table = tables.get(i);
                // TODO: empty table check? (they might not have been labeled in the gold
                // standard)

                String fileName = r.getId().toString() + "-" + r.getId().toString() + "-" + i + ".html";

                String clusterName = fileMap.get(fileName);
                if (clusterName != null) {
                    matching.add(clusterName, r.getId(), new WebtablePosition(i),
                            !Objects.equal(previousVersion.get(clusterName), table));
                    previousVersion.put(clusterName, table);
                }

            }
        }
        return matching;
    }

}