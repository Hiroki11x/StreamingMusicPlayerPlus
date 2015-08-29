package bmw.awa.awabmw;

/**
 * Created by hirokinaganuma on 15/08/29.
 */
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Items")
public class Item extends Model {

    @Column(name = "track_name")
    public String track_name;

    @Column(name = "previewUrl")
    public String previewUrl;

    @Column(name = "artworkUrl100")
    public String artworkUrl100;

    @Column(name = "artistName")
    public String artistName;

    @Column(name = "collectionName")
    public String collectionName;

    @Column(name = "registerTime")
    public int registerTime;

    public Item() {
        super();
    }

    public Item(String track_name,
                String previewUrl,
                String artworkUrl100,
                String artistName,
                String collectionName,
                int registerTime) {
        super();
        this.track_name = track_name;
        this.previewUrl = previewUrl;
        this.artworkUrl100 = artworkUrl100;
        this.artistName = artistName;
        this.collectionName = collectionName;
        this.registerTime = registerTime;
    }
}