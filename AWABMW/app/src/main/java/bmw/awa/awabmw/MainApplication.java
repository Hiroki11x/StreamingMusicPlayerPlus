package bmw.awa.awabmw;

import android.app.Application;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by satsukies on 15/09/02.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // If you want to set default font, activate this code below.
        initCalligraphy();
    }

    private void initCalligraphy() {
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
    }
}
