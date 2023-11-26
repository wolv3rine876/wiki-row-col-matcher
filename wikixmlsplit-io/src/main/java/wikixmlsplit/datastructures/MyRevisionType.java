package wikixmlsplit.datastructures;

import org.sweble.wikitext.dumpreader.export_0_10.CommentType;
import org.sweble.wikitext.dumpreader.export_0_10.ContributorType;
import org.sweble.wikitext.dumpreader.export_0_10.RevisionType;
import org.sweble.wikitext.dumpreader.export_0_10.TextType;
import wikixmlsplit.matching.data.RevisionData;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MyRevisionType implements RevisionData {

	protected BigInteger id;
	protected BigInteger parentid;
	protected XMLGregorianCalendar timestamp;
	protected ContributorType contributor;
	protected boolean minor;
	protected CommentType comment;
	protected String model;
	protected String format;
	protected TextType text;

	protected List<String> parsed;
	protected String sha1;
	
	protected transient Instant instant;

	public MyRevisionType() {

	}

	public MyRevisionType(RevisionType t) {
		this.id = t.getId();
		this.parentid = t.getParentid();
		this.timestamp = t.getTimestamp();
		this.contributor = t.getContributor();
		this.minor = t.getMinor() != null;
		this.comment = t.getComment();
		this.model = t.getModel();
		this.format = t.getFormat();
		this.text = t.getText();
		this.sha1 = t.getSha1();
		this.parsed = null;

	}
	
	public MyRevisionType(MyRevisionType t) {
		this.id = t.id;
		this.parentid = t.parentid;
		this.timestamp = t.timestamp;
		this.contributor = t.contributor;
		this.minor = t.minor;
		this.comment = t.comment;
		this.model = t.model;
		this.format = t.format;
		this.text = t.text;
		this.sha1 = t.sha1;
		this.parsed = t.parsed;
	}

	public BigInteger getId() {
		return id;
	}

	public TextType getText() {
		return text;
	}
	
	public CommentType getComment() {
		return comment;
	}
	
	public void removeText() {
		this.text = null;
	}
	
	public void removeParsed() {
		this.parsed = null;
	}

	public void addParsed(String parse2) {
		this.text = null;
		if (this.parsed == null)
			this.parsed = new ArrayList<>();
		this.parsed.add(parse2);
	}

	public XMLGregorianCalendar getTimestamp() {
		return timestamp;
	}
	
	public ContributorType getContributor() {
		return contributor;
	}
	
	public List<String> getParsed() {
		return parsed;
	}

	public Instant getInstant() {
		if(instant == null)
			instant = timestamp.toGregorianCalendar().toInstant();
		return instant;
	}

}
