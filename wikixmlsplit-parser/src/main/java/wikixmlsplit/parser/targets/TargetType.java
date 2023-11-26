package wikixmlsplit.parser.targets;

import com.google.gson.Gson;
import de.fau.cs.osr.ptk.common.AstVisitor;
import org.sweble.wikitext.dumpreader.export_0_10.PageType;
import org.sweble.wikitext.dumpreader.export_0_10.RevisionType;
import org.sweble.wikitext.parser.ParserConfig;
import org.sweble.wikitext.parser.nodes.WtNode;
import wikixmlsplit.datastructures.MyPageType;
import wikixmlsplit.util.Util;

import java.util.regex.Pattern;

public enum TargetType {
	TABLE {
		@Override
		public boolean accepts(String wikitext) {
			// We only want to include revisions that contain at least one table.
			return wikitext.contains("{|") || Util.containsIgnoreCase(wikitext, "<table>");
		}

		@Override
		public AstVisitor<WtNode> getVistor(ParserConfig config, Gson serializer) {
			return new TableNodeExtractVisitor(serializer);
		}
	},
	INFOBOX {
		@Override
		public boolean accepts(String wikitext) {
			return wikitext.contains("{{") && Util.containsIgnoreCase(wikitext, "infobox");
		}
		
		@Override
		public AstVisitor<WtNode> getVistor(ParserConfig config, Gson serializer) {
			return new InfoboxNodeExtractVisitor(serializer);
		}
	},
	LISTS {
		private final Pattern listPattern = Pattern.compile("^\\s*[\\*#;:]", java.util.regex.Pattern.MULTILINE);
		
		@Override
		public boolean accepts(String wikitext) {
			// We only want to include revisions that contain at least one table.
			return listPattern.matcher(wikitext).find() || Util.containsIgnoreCase(wikitext, "<ul>") || Util.containsIgnoreCase(wikitext, "<ol>");
		}

		@Override
		public AstVisitor<WtNode> getVistor(ParserConfig config, Gson serializer) {
			return new ListNodeExtractVisitor(serializer);
		}
	};

	/**
	 * Should the revision source be included in the page file?
	 */
	public boolean accepts(PageType page, RevisionType revision) {
		return accepts(revision.getText().getValue());
	}

	/**
	 * Should the page produce a page file?
	 */
	public boolean accepts(MyPageType page) {
		return true;
	}
	
	public boolean accepts(PageType page) {
		return true;
	}
	
	public boolean accepts(String wikitext) {
		return true;
	}
	
	public abstract AstVisitor<WtNode> getVistor(ParserConfig config, Gson serializer);
}
