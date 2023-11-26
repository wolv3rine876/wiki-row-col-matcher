package wikixmlsplit.wikirow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import wikixmlsplit.matching.similarity.BagStore;
import wikixmlsplit.matching.similarity.SchemaObject;
import wikixmlsplit.wikirow.util.HTMLHelper;

public class WikiTuple implements SchemaObject {

  protected List<String> tupleHTML;
  /*
   * The content of the cells but without any html
   */
  protected List<String> tupleText;

  protected List<String> schemaHTML;
  /*
   * The content of the schema (<th> or first row).
   */
  protected List<String> schemaText;

  private Multiset<String> bagOfWords = null;
  private Multiset<String> schemaBoW = null;

  public WikiTuple(List<String> tuple) {
    this(null, tuple);
  }

  public WikiTuple(List<String> schemaHTML, List<String> tuples) {
    this.tupleHTML = tuples;
    this.schemaHTML = schemaHTML;

    this.schemaText = schemaHTML.stream().map((td) -> HTMLHelper.getDisplayText(" ", td)).collect(Collectors.toList());
    this.tupleText = tupleHTML.stream().map((td) -> HTMLHelper.getDisplayText(" ", td)).collect(Collectors.toList());
  }

  private static BagStore<Multiset<String>> sets = new BagStore<>();

  @Override
  public String toString() {
    return String.join(" ", tupleText);
  }

  public List<String> getTuples() {
    return tupleHTML;
  }

  public List<String> getSchema() {
    return schemaHTML;
  }

  @Override
  public Multiset<String> getBagOfValues(int limit, boolean includeHeaders) {
    if (bagOfWords == null) {
      Multiset<String> set = HashMultiset.create();
      addTokens(limit, set, this.toString());
      set.remove("", set.count(""));
      bagOfWords = sets.get(set);
    }
    return bagOfWords;
  }

  @Override
  public Multiset<String> getSchemaBoW(int limit) {
    if(schemaBoW == null) {
      Multiset<String> set = HashMultiset.create();
      addTokens(limit, set, String.join(" ", schemaText));
      set.remove("", set.count(""));
      schemaBoW = sets.get(set);
    }
    return schemaBoW;
  }

  private void addTokens(int limit, Multiset<String> set, String text) {
		String[] words = text.split("[^A-Za-z0-9]+", limit + 1);

    if (limit > 0 && words.length > limit) {
      set.addAll(Arrays.asList(words).subList(0, limit));
    } else {
      Collections.addAll(set, words);
    }
	}
}
