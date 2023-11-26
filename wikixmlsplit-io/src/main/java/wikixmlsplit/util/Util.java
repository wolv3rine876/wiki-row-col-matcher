package wikixmlsplit.util;

import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.fau.cs.osr.ptk.common.ast.AstNode;
import de.fau.cs.osr.ptk.common.ast.RtData;
import de.fau.cs.osr.ptk.common.json.AstNodeJsonTypeAdapter;
import de.fau.cs.osr.ptk.common.json.AstRtDataJsonTypeAdapter;
import de.fau.cs.osr.ptk.common.serialization.NodeFactory;
import org.sweble.wikitext.engine.config.WikiConfigImpl;
import org.sweble.wikitext.engine.serialization.EngineAstNodeConverter;
import org.sweble.wikitext.parser.WtRtData;
import org.sweble.wikitext.parser.nodes.WtImEndTag;
import org.sweble.wikitext.parser.nodes.WtImStartTag;
import org.sweble.wikitext.parser.nodes.WtNode;

import java.util.Arrays;
import java.util.Collections;

public class Util {

	public static boolean containsIgnoreCase(String src, String what) {
		final int length = what.length();
		if (length == 0)
			return true; // Empty string is contained
	
		final char firstLo = Character.toLowerCase(what.charAt(0));
		final char firstUp = Character.toUpperCase(what.charAt(0));
	
		for (int i = src.length() - length; i >= 0; i--) {
			// Quick check before calling the more expensive regionMatches() method:
			final char ch = src.charAt(i);
			if (ch != firstLo && ch != firstUp)
				continue;
	
			if (src.regionMatches(true, i, what, 0, length))
				return true;
		}
	
		return false;
	}

	public static Gson getJsonSerializer() {
		final AstNodeJsonTypeAdapter<WtNode> nodeConverter = AstNodeJsonTypeAdapter.forNodeType(WtNode.class);
		EngineAstNodeConverter.setup(new WikiConfigImpl(), nodeConverter);
	
		// As long as GSON does not handle Object collections and polymorphism
		// correctly, the "warnings" attribute cannot be serialized
		nodeConverter.suppressProperty("warnings");
		nodeConverter.suppressNode(WtImStartTag.class);
		nodeConverter.suppressNode(WtImEndTag.class);
		nodeConverter.setNodeFactory(new NodeFactory<>() {
			NodeFactory<WtNode> nf = nodeConverter.getNodeFactory();

			@Override
			public WtNode instantiateNode(Class<?> clazz) {

				return nf.instantiateNode(clazz);
			}

			@Override
			public WtNode instantiateDefaultChild(NamedMemberId id, Class<?> childType) {
				return nf.instantiateDefaultChild(id, childType);
			}

			@Override
			public Object instantiateDefaultProperty(NamedMemberId id, Class<?> type) {
				if (id.memberName.equals("warnings"))
					return Collections.EMPTY_LIST;
				return nf.instantiateDefaultProperty(id, type);
			}
		});
	
		AstRtDataJsonTypeAdapter<WtRtData> rtdConverter = new AstRtDataJsonTypeAdapter<>(WtRtData.class);
		EngineAstNodeConverter.setup(rtdConverter);
	
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(RtData.class, rtdConverter);
		builder.registerTypeHierarchyAdapter(AstNode.class, nodeConverter);
		builder.serializeNulls();
		return builder.create();
	}

	public static String makeSafeFileName(String s) {
		String safe = s.replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_\\-&]+", "");
		return safe.substring(0, Math.min(60, safe.length()));
	}


	public static void addWords(int limit, Multiset<String> set, String value) {
		String[] words = value.split("[^A-Za-z0-9]+", limit + 1);
		for(int i = 0; i < words.length; ++i) {
			words[i] = words[i].toLowerCase();
		}
		if (limit > 0 && words.length > limit) {
			set.addAll(Arrays.asList(words).subList(0, limit));
		} else {
			Collections.addAll(set, words);
		}
	}
}
