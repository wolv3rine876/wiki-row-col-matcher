package wikixmlsplit.wikirow.datastructures;

import java.math.BigInteger;
import java.util.Date;

public class MyJsonTableOutputRow {
  protected String pageTitle;
  protected String caption;
  protected Date validFrom;
  protected Date validUntil;
  protected BigInteger pageID;
  protected String content;
  protected BigInteger revisionId;
  protected String contentType;
  protected String contextType;
  protected int position;
  protected String key;
  protected MyContributorType user;
  protected String headings;

  public MyJsonTableOutputRow() {}

  public String getPageTitle() {
    return pageTitle;
  }

  public void setPageTitle(String pageTitle) {
    this.pageTitle = pageTitle;
  }

  public String getCaption() {
    return caption;
  }

  public void setCaption(String caption) {
    this.caption = caption;
  }

  public Date getValidFrom() {
    return validFrom;
  }

  public void setValidFrom(Date validFrom) {
    this.validFrom = validFrom;
  }

  public Date getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(Date validUntil) {
    this.validUntil = validUntil;
  }

  public BigInteger getPageID() {
    return pageID;
  }

  public void setPageID(BigInteger pageID) {
    this.pageID = pageID;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public BigInteger getRevisionId() {
    return revisionId;
  }

  public void setRevisionId(BigInteger revisionId) {
    this.revisionId = revisionId;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getContextType() {
    return contextType;
  }

  public void setContextType(String contextType) {
    this.contextType = contextType;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public MyContributorType getUser() {
    return user;
  }

  public void setUser(MyContributorType user) {
    this.user = user;
  }

  public String getHeadings() {
    return headings;
  }

  public void setHeadings(String headings) {
    this.headings = headings;
  }
}
