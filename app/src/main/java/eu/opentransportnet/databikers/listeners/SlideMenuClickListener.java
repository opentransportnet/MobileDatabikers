package eu.opentransportnet.databikers.listeners;

import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import eu.opentransportnet.databikers.activities.ChangeLangActivity;
import eu.opentransportnet.databikers.activities.DisclaimerActivity;
import eu.opentransportnet.databikers.activities.MainActivity;
import eu.opentransportnet.databikers.activities.SettingsActivity;
import eu.opentransportnet.databikers.activities.TracksActivity;
import eu.opentransportnet.databikers.utils.SessionManager;

/**
 * @author Ilmars Svilsts
 */
public class SlideMenuClickListener implements ListView.OnItemClickListener {
    private DrawerLayout drawer;
    MainActivity activity;
    public final static String EXTRA_MESSAGE = "com.antwerp.MESSAGE";
    public final static String EXTRA = "com.antwerp.Nop";

    public SlideMenuClickListener(DrawerLayout drawer, MainActivity activity) {
        this.drawer = drawer;
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // display view for selected drawer item
        switch (position) {
            case 0:
                drawer.closeDrawers();
                Intent a = new Intent(activity, TracksActivity.class);
                a.putExtra(EXTRA_MESSAGE, "1");
                MainActivity.sCanUploadTracks = 2;
                activity.startActivity(a);
                break;
            case 1:
                drawer.closeDrawers();
                Intent v = new Intent(activity, TracksActivity.class);
                v.putExtra(EXTRA_MESSAGE, "2");
                MainActivity.sCanUploadTracks = 2;
                activity.startActivity(v);
                break;
            case 2:
                drawer.closeDrawers();
                Intent b = new Intent(activity, SettingsActivity.class);
                activity.startActivity(b);
                break;
            case 3:
                drawer.closeDrawers();
                Intent changeLanguage = new Intent(activity, ChangeLangActivity.class);
                activity.startActivity(changeLanguage);
                break;
            case 4:
                drawer.closeDrawers();
                Intent dis = new Intent(activity, DisclaimerActivity.class);
                activity.startActivity(dis);
                break;
            case 5:
                drawer.closeDrawers();
                activity.deleteUser();
                break;
            case 6:
                drawer.closeDrawers();
                new SessionManager(activity).logoutUser();
                break;
        }
    }
}