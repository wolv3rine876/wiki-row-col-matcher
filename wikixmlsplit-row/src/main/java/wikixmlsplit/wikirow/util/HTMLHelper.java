package wikixmlsplit.wikirow.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

public class HTMLHelper {
  
  public static String toTR(List<String> tds) {
    return "<tr>" + String.join("", tds) + "</tr>";
  }
  /*
   * Extracts the display-text of the given html.
   */
  public static String getDisplayText(String seperator, String html) {
    Document doc = Jsoup.parse(html);
    return String.join(seperator, getDisplayText(doc));
  }
  public static List<String> getDisplayText(Document doc) {
    List<String> texts = new LinkedList<>();
    for(Element e : doc.children()) {
      getDisplayText(e, texts);
    }
    return texts;
  }

  private static List<String> getDisplayText(Node node, List<String> previouStrings) {
    if(!node.childNodes().isEmpty()) {
      for(Node n : node.childNodes()) {
        getDisplayText(n, previouStrings);
      }
      return previouStrings;
    }

    if(node instanceof TextNode) {
      TextNode textNode = (TextNode)node;
      if(!textNode.isBlank()) previouStrings.add(textNode.text());
    }
    else if(node instanceof Element) {
      Element element = (Element)node;
      if(!element.text().equals("")) previouStrings.add(element.text());
      else if(element.tagName().equals("a")) {
        String title = element.attr("title");
        if(!title.equals("")) previouStrings.add(title);
      }
    }

    return previouStrings;
  }

  private Document doc;

  private List<Element> rows;

  public HTMLHelper(String html) {
    if(html == null) throw new IllegalArgumentException("HTML is null");

    if(html.startsWith("<tr") && html.endsWith("</tr>")) {
      html = "<table>" + html + "</table>";
    }

    doc = Jsoup.parse(html);
    rows = normalize();
  }

  public List<String> getRowwiseHead() {
    return findTableHead();
  }

  public List<String> getColwiseHead() {
    return findTableHead();
  }

  public List<List<String>> getRowwiseBody() {
    return findTableBody();
  }

  public List<List<String>> getColwiseBody() {
    List<List<String>> body = findTableBody();
    
    List<List<String>> transposedBody = new LinkedList<>();

    int maxColCount = body.size() == 0 ? 0 : Collections.max(body.stream().map((tr) -> tr.size()).collect(Collectors.toList()));
    for(int colIdx = 0; colIdx < maxColCount; colIdx++) {
      List<String> col = new LinkedList<>();
      for(List<String> row : body) {
        if(colIdx < row.size()) col.add(row.get(colIdx));
      }
      transposedBody.add(col);
    }

    return transposedBody;
  }

  @Override
  public String toString() {
    return String.join("||", getDisplayText(doc));
  }

  private int getSpan(Element e, String attr) {
    int span = 1;
    if(e.hasAttr(attr)) {
      try {
        span = Integer.parseInt(e.attr(attr));
      }
      catch(NumberFormatException ex) {}
    }
    return span;
  }

  private List<Element> normalize() {
    List<Element> rows = doc.select("tr");

    if(rows.size() == 0) throw new IllegalArgumentException("Table has now rows");

    HashMap<Integer, List<Element>> spanningCells = new HashMap<>();
    // Copy cells with rowspan into the next rows
    for(Element row : rows) {
      
      // Remove TextNodes
      row.childNodes().stream().filter((n) -> n instanceof TextNode).forEach((n) -> n.remove());

      Elements cells = row.children();
      int colIdx, spanIdx;
      for(colIdx = spanIdx = 0; colIdx < cells.size() || spanningCells.containsKey(spanIdx); colIdx++) {

        Element cell = null;

        // There exists a cell that spans into this row and col
        if(spanningCells.containsKey(spanIdx)) {
          
          // Insert the spannig cell
          List<Element> cellCopies = spanningCells.get(spanIdx);
          cell = cellCopies.remove(0);
          cells.add(colIdx, cell);
          row.insertChildren(colIdx, Collections.singletonList(cell));
                    
          // Check if all spannig cells have been consumed
          if(cellCopies.isEmpty()) spanningCells.remove(spanIdx);
        }

        // Check if this cell is spanning
        else {
          
          cell = cells.get(colIdx);
          int rowSpan = getSpan(cell, "rowspan");
          cell.removeAttr("rowspan");

          if(rowSpan > 1) {
            Element finalCell = cell;
            spanningCells.put(spanIdx, IntStream.range(0, rowSpan - 1).mapToObj(t -> finalCell.clone()).collect(Collectors.toList()));
          }
        }

        int colSpan = getSpan(cell, "colspan");
        spanIdx += colSpan;
      }

      // normalize colspans
      for(colIdx= 0; colIdx < row.children().size();) {

        Element cell = row.children().get(colIdx);

        int colSpan = getSpan(cell, "colspan");
        cell.removeAttr("colspan");

        if(colSpan > 1) {
          List<Element> cellCopies = IntStream.range(0, colSpan - 1).mapToObj(t -> cell.clone()).collect(Collectors.toList());
          try {
            row.insertChildren(colIdx+1, cellCopies);
          }
          catch(Exception e) {}
        }

        colIdx += colSpan;
      }
    }

    // Detect orientation
    int rowCellCount = rows.get(0).children().stream().filter((cell) -> cell.hasText()).collect(Collectors.toList()).size();
    int colCellCount = rows.stream().filter((tr) -> tr.children().size() > 0 ? tr.child(0).hasText() : false).collect(Collectors.toList()).size();
    int rowTHs = rows.get(0).children().stream().filter((e) -> e.tagName().equals("th")).collect(Collectors.toList()).size();
    int colTHs = rows.stream().filter((tr) -> tr.children().size() > 0 ? tr.child(0).tagName().equals("th") : false).collect(Collectors.toList()).size();
    boolean isHorizontal = true;
    if(colTHs == colCellCount && rowTHs < rowCellCount) isHorizontal = false;

    // transpose
    if(!isHorizontal) {
      List<Element> transposedRows = new LinkedList<>();
      Element tr = new Element("tr");

      int maxRowLen = Collections.max(rows.stream().map((row) -> row.children().size()).collect(Collectors.toList()));
      for(int colIdx=0; colIdx < maxRowLen; colIdx++) {
        for(Element row : rows) {
          if(row.children().size() > 0) tr.appendChild(row.child(0));
        }
        transposedRows.add(tr);
        tr = new Element("tr");
      }

      rows = transposedRows;
    }

    // Get all <tr> and remove the ones that are empty or have just one (giant) child
    if (rows.size() > 0) {
      rows = rows.stream().filter((tr) -> !tr.children().isEmpty() && !tr.children().stream().allMatch((th) -> tr.child(0).text().equals(th.text()))).collect(Collectors.toList());
    }

    // "Crop" the cols if needed
    int colCount = getMajority(rows.stream().map((tr) -> tr.children().size()).collect(Collectors.toList()));
    rows.forEach((tr) -> tr.children().stream().skip(colCount).forEach((cell) -> cell.remove()));

    return rows;
  }

  private int getMajority(List<Integer> l) {
    if (l.isEmpty()) return 0;

    HashMap<Integer, Integer> m = new HashMap<>();

    for(Integer i : l) {
      if(!m.containsKey(i)) m.put(i, 0);
      m.put(i, m.get(i) + 1);
    }

    return Collections.max(m.entrySet(),
      new Comparator<Map.Entry<Integer, Integer>>() {
      @Override
      public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
          return o1.getValue().compareTo(o2.getValue());
      }
    }).getKey();
  }

  private List<String> findTableHead() {
    List<String> result = new LinkedList<>();

    // find the first rows with <th>s
    List<List<Element>> head = new LinkedList<>();
    int maxColCount = -1;
    for(Element tr : rows) {
      if(!tr.select("td").isEmpty() && maxColCount >= 0) break;
      List<Element> row = tr.select("th,td");
      head.add(row);
      maxColCount = Math.max(maxColCount, row.size());
    }

    // join multiple header
    for(int colIdx = 0; colIdx < maxColCount; colIdx++) {
      LinkedHashSet<String> values = new LinkedHashSet<>();

      for(List<Element> row : head) {
        if(colIdx < row.size()) values.add(row.get(colIdx).text());
      }

      result.add(String.join(" ", values));
    }

    return result.stream().map((v) -> "<th>" + v +  "</th>").collect(Collectors.toList());
  }
  private List<List<String>> findTableBody() {
    List<Element> filteredRows = rows
      .stream()
      .filter((tr) -> !tr.select("td").isEmpty() && tr.select("th").size() < tr.children().size() && tr.select("table").isEmpty())
      .collect(Collectors.toList());
    // Convert all cells to <td>
    filteredRows.forEach((tr) -> tr.children().tagName("td"));
    // Split each row into a list of <td>s and map them to HTML strings.
    return filteredRows
      .stream()
      .map((tr) -> tr.select("td")
        .stream()
        .map((td) -> td.outerHtml())
        .collect(Collectors.toList()))
      .collect(Collectors.toList());
  }
}