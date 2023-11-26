package wikixmlsplit.output.json;

import org.sweble.wikitext.parser.nodes.WtNode;

class ObjectInput {
    WtNode node;
    String pageName;

    public ObjectInput(String title, WtNode node) {
        this.pageName = title;
        this.node = node;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        result = prime * result + ((pageName == null) ? 0 : pageName.hashCode());
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
        ObjectInput other = (ObjectInput) obj;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        if (pageName == null) {
            if (other.pageName != null)
                return false;
        } else if (!pageName.equals(other.pageName))
            return false;
        return true;
    }
}
