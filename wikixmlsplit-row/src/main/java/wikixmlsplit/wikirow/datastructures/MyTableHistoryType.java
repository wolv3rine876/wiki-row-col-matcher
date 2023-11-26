package wikixmlsplit.wikirow.datastructures;

import java.math.BigInteger;
import java.util.List;

/*
 * Represent a matched table history (used for row matching)
 */
public class MyTableHistoryType {
  
  protected String pageTitle;

	protected BigInteger pageID;

  protected String tableID;

  /*
   * A list of matched revisions.
   */
	protected List<MyTableRevisionType> revisions;

	public MyTableHistoryType() {}

  public MyTableHistoryType(String pageTitle, BigInteger pageID, String tableID, List<MyTableRevisionType> revisions) {
    this.pageTitle = pageTitle;
    this.pageID = pageID;
    this.tableID = tableID;
    this.revisions = revisions;
  }

  public String getPageTitle()  {
    return pageTitle;
  }

  public BigInteger getPageID() {
    return pageID;
  }

  public String getTableID() {
    return tableID;
  }

  public List<MyTableRevisionType> getRevisions() {
    return revisions;
  }
}