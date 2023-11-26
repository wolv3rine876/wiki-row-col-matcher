package wikixmlsplit.datastructures;

import org.sweble.wikitext.dumpreader.export_0_10.PageType;
import org.sweble.wikitext.dumpreader.export_0_10.RedirectType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MyPageType {

	protected String title;

	protected BigInteger id;
	protected RedirectType redirect;
	protected String restrictions;

	protected List<MyRevisionType> revisionOrUpload;

	public MyPageType() {

	}

	public MyPageType(PageType t) {
		this.title = t.getTitle();
		this.id = t.getId();
		this.redirect = t.getRedirect();
		this.restrictions = t.getRestrictions();
		this.revisionOrUpload = new ArrayList<>();
	}

	public void addRevison(MyRevisionType myRevisionType) {
		this.revisionOrUpload.add(myRevisionType);
	}

	public List<MyRevisionType> getRevisions() {
		return revisionOrUpload;
	}

	public String getTitle() {
		return title;
	}

	public void sortRevisions() {
		revisionOrUpload.sort((o1, o2) -> o1.getTimestamp().compare(o2.getTimestamp()));
	}

	public BigInteger getId() {
		return id;
	}

	public void setRevisions(List<? extends MyRevisionType> revisions) {
		this.revisionOrUpload = new ArrayList<>(revisions);
	}
}
