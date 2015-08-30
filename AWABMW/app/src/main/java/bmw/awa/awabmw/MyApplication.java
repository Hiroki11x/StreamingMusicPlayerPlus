package bmw.awa.awabmw;

/**
 * Created by hirokinaganuma on 15/08/29.
 */

import com.activeandroid.ActiveAndroid;
//AndroidManifestでAppNameを定義する
public class MyApplication extends com.activeandroid.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // ActiveAndroidの初期化を行う
        ActiveAndroid.initialize(this);

    }
}