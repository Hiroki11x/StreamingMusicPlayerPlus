package bmw.awa.awabmw;

import java.util.ArrayList;
import java.util.List;

public class Element {

    private String name;
    private Item item;
    private ArrayList<Element> children = new ArrayList<>();

    public Element(String name) {
        this.name = name;
    }

    public Element(String name, Item item) {
        this.name = name;
        this.item = item;
    }

    public Element(Item item){
        this.item = item;
    }

    public String getName() {
        return name;
    }

    public String getTrackName(){
        return item.track_name;
    }

    public String getArtworkUrl100(){
        return item.artworkUrl100;
    }

    public String getCollectionName(){
        return item.collectionName;
    }

    public String getpreviewUrl(){
        return item.previewUrl;
    }

    public int getRegisterTime(){
        return item.registerTime;
    }

    public String getArtistName(){
        return item.artistName;
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
