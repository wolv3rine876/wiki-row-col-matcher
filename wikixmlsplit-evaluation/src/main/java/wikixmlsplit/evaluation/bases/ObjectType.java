package wikixmlsplit.evaluation.bases;

import wikixmlsplit.evaluation.gsreader.GSReader;
import wikixmlsplit.evaluation.gsreader.GSReaderInfobox;
import wikixmlsplit.evaluation.gsreader.GSReaderTable;
import wikixmlsplit.evaluation.gsreader.GSReaderWikiList;
import wikixmlsplit.infobox.builder.InfoboxBuilder;
import wikixmlsplit.infobox.position.InfoboxPosition;
import wikixmlsplit.io.BuilderBase;
import wikixmlsplit.lists.builder.WikiListBuilder;
import wikixmlsplit.lists.position.WikiListPosition;
import wikixmlsplit.matching.position.SimplePosition;
import wikixmlsplit.matching.similarity.MatchingObject;
import wikixmlsplit.wikitable.builder.TableBuilder;
import wikixmlsplit.wikitable.position.TablePosition;

import java.util.function.BiFunction;

public enum ObjectType {
	TABLE {
		@Override
		public GSReader<? extends MatchingObject, ? extends SimplePosition> getGSReader() {
			return new GSReaderTable();
		}

		@Override
		public BuilderBase<? extends MatchingObject> getBuilder() {
			return new TableBuilder(false);
		}

		@Override
		public BiFunction<MatchingObject, Integer, ? extends SimplePosition> getMapper() {
			return (table, rank) -> new TablePosition(rank);
		}
	},
	INFOBOX {
		@Override
		public GSReader<? extends MatchingObject, ? extends SimplePosition> getGSReader() {
			return new GSReaderInfobox();
		}

		@Override
		public BuilderBase<? extends MatchingObject> getBuilder() {
			return new InfoboxBuilder();
		}

		@Override
		public BiFunction<MatchingObject, Integer, ? extends SimplePosition> getMapper() {
			return (table, rank) -> new InfoboxPosition(rank);
		}
	},
	LIST {
		@Override
		public GSReader<? extends MatchingObject, ? extends SimplePosition> getGSReader() {
			return new GSReaderWikiList();
		}

		@Override
		public BuilderBase<? extends MatchingObject> getBuilder() {
			return new WikiListBuilder();
		}

		@Override
		public BiFunction<MatchingObject, Integer, ? extends SimplePosition> getMapper() {
			return (table, rank) -> new WikiListPosition(rank);
		}
	};

	public abstract GSReader<? extends MatchingObject, ? extends SimplePosition> getGSReader();

	public abstract BuilderBase<? extends MatchingObject> getBuilder();

	public abstract BiFunction<MatchingObject, Integer, ? extends SimplePosition> getMapper();
}