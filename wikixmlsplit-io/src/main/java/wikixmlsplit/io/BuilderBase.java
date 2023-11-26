package wikixmlsplit.io;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import joptsimple.internal.Strings;
import org.sweble.wikitext.parser.nodes.WtHeading;
import org.sweble.wikitext.parser.nodes.WtNode;
import wikixmlsplit.util.TextExtractVisitor;
import wikixmlsplit.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class BuilderBase<T> {
	private Gson g = Util.getJsonSerializer();

	private LoadingCache<String, WtNode> parseCache = createNodeCache();

	public List<T> constructNewObjects(Map<String, List<String>> extracted) {
		List<WtNode> tables = getNodes(extracted.get(getNodeType()));
		List<T> trackedObjects = new ArrayList<>();
		List<String> headings = new ArrayList<>();
		for (WtNode n : tables) {
			if (n instanceof WtHeading) {
				headings.add((String) new TextExtractVisitor().go(n));
			} else if (isTargetNode(n)) {
				Input input = new Input(Strings.join(headings, " | "), n);

				try {
					trackedObjects.add(nodeCache.get(input));
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				headings = new ArrayList<>();
			} else {
				System.err.println("Unexpected node type!");
			}

		}
		return trackedObjects;
	}
	
	public int getObjectCount(Map<String, List<String>> extracted) {
		List<WtNode> tables = getNodes(extracted.get(getNodeType()));
		int count = 0;
		for (WtNode n : tables) {
			if (n instanceof WtHeading) {
				
			} else if (isTargetNode(n)) {
				++count;
			} else {
				System.err.println("Unexpected node type!");
			}

		}
		return count;
	}

	protected abstract String getNodeType();

	protected List<WtNode> getNodes(List<String> parsed) {
		List<WtNode> nodes = new ArrayList<>(parsed.size());
		for (String tableString : parsed)
			nodes.add(parseCache.getUnchecked(tableString));
		return nodes;
	}

	private LoadingCache<String, WtNode> createNodeCache() {
		return CacheBuilder.newBuilder().recordStats().maximumSize(1000).build(new CacheLoader<>() {

			@Override
			public WtNode load(String i) {
				return g.fromJson(i, WtNode.class);
			}

		});
	}

	protected static class Input {
		private final String headings;
		private final WtNode node;

		public Input(String headings, WtNode node) {
			this.headings = headings;
			this.node = node;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((headings == null) ? 0 : headings.hashCode());
			result = prime * result + ((node == null) ? 0 : System.identityHashCode(node));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Input other = (Input) obj;
			if (headings == null) {
				if (other.headings != null)
					return false;
			} else if (!headings.equals(other.headings))
				return false;
			if (node != other.node)
				return false;
			return true;
		}

		public String getHeadings() {
			return headings;
		}

		public WtNode getNode() {
			return node;
		}

	}

	protected LoadingCache<Input, T> nodeCache;

	abstract protected boolean isTargetNode(WtNode n);
}
