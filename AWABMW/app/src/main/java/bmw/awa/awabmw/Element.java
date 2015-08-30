package bmw.awa.awabmw;

import java.util.ArrayList;
import java.util.List;

public class Element {

    private String name;
    private ArrayList<Element> children = new ArrayList<Element>();

    public Element(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isParent() {
        return children.size() > 0;
    }

    public List<Element> getChildren() {
        return children;
    }

    public void addChild(Element element) {
        children.add(element);
    }
}
