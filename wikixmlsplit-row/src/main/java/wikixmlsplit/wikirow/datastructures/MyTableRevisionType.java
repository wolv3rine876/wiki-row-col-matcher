package wikixmlsplit.wikirow.datastructures;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.wikirow.WikiTuple;
import wikixmlsplit.wikirow.util.HTMLHelper;

public class MyTableRevisionType implements RevisionData {
  
  protected Date validFrom;
  protected Date validUntil;
  /*
   * The raw HTML content of the table.
   */
  protected String content;
  protected BigInteger revisionId;
  /*
   * The tables position (index) on the article page.
   */
  protected int position;
  protected MyContributorType user;
  /*
   * Indicates that this table was deleted in the next revision.
   */
  protected boolean last;
  protected List<WikiTuple> rows;
  protected List<WikiTuple> cols;

  private List<String> head;

  public MyTableRevisionType() {}

    public MyTableRevisionType(Date validFrom, Date validUntil, String content, BigInteger revisionId, int position,
        MyContributorType user) {
    this.validFrom = validFrom;
    this.validUntil = validUntil;
    this.content = content;
    this.revisionId = revisionId;
    this.position = position;
    this.user = user;
    this.last = false;

    HTMLHelper helper = new HTMLHelper(content);
    
    List<List<String>> tableData = helper.getRowwiseBody();
    // get schema
    head = helper.getRowwiseHead();
    final List<String> finalSchema = head;
    // get table body
    rows = tableData.stream().map((td) -> new WikiTuple(finalSchema, td)).collect(Collectors.toList());

    // get the col-wise representation
    List<String> colHeads = helper.getColwiseHead();
    List<List<String>> colBody = helper.getColwiseBody();

    cols = new ArrayList<>();
    for(int i = 0; i < Math.max(colHeads.size(), colBody.size()); i++) {
      cols.add(new WikiTuple(
        Arrays.asList(i < colHeads.size() ? colHeads.get(i) : ""),
        i < colBody.size() ? colBody.get(i) : new ArrayList<>()
      ));
    }
  }

  @Override
  public Instant getInstant() {
    return getValidFrom().toInstant();
  }

  @Override
  public BigInteger getId() {
    return getRevisionId();
  }

  public Date getValidFrom() {
    return validFrom;
  }

  public Date getValidUntil() {
    return validUntil;
  }

  public String getContent() {
    return content;
  }

  public BigInteger getRevisionId() {
    return revisionId;
  }

  public int getPosition() {
    return position;
  }

  public MyContributorType getUser() {
    return user;
  }

  public boolean isLast() {
    return last;
  }

  public void isLast(boolean b) {
    last = b;
  }

  public List<WikiTuple> getRows() {
    return rows;
  }

  public List<WikiTuple> getCols() {
    return cols;
  }

  public String getTable() {
    String[] parts = new String[]{
      "<table>",
      HTMLHelper.toTR(head),
      String.join("\n", rows.stream().map((r) -> HTMLHelper.toTR( r.getTuples())).collect(Collectors.toList())),
      "</table>"
    };
    return String.join("\n", parts);
  }
}