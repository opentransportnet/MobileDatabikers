package eu.opentransportnet.databikers.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.utils.Classificators;
import eu.opentransportnet.databikers.utils.SessionManager;

/**
 * Transport type choice menu for user
 * @author Ilmars Svilsts
 */

public class SettingsActivity extends BaseActivity {

    private SessionManager mSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(eu.opentransportnet.databikers.R.layout.activity_settings);
        mSessionManager = new SessionManager(this);

        TextView title = (TextView) findViewById(eu.opentransportnet.databikers.R.id.title);
        title.setText(eu.opentransportnet.databikers.R.string.title_my_bike);

        // Attaching the layout to the toolbar object
        Toolbar mToolbar = (Toolbar) findViewById(eu.opentransportnet.databikers.R.id.tool_bar);
        setSupportActionBar(mToolbar);

        ImageButton back = (ImageButton) findViewById(eu.opentransportnet.databikers.R.id.back_button);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        int type = mSessionManager.getUser().getBikeType();


        TextView heder;


        switch (type) {
            case 0:
                back.setVisibility(View.GONE);
                break;
            case 1010:
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.city_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                break;
            case 1040:
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.velo_text);
                heder.setTypeface(null, Typeface.BOLD);
                break;
            case 1020:
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.race_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                break;
            case 1050:
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.mountain_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                break;
            case 1030:
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.electric_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                break;
            case 1060:
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.carrier_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                break;
        }
    }

    public void onClick(View v) {
        TextView heder;
        switch (v.getId()) {
            case eu.opentransportnet.databikers.R.id.city_bike_img:
                clean();
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.city_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                mSessionManager.saveBikeType(Classificators.TRANSPORT_CITY_BIKE);
                MainActivity.setMovementType(Classificators.TRANSPORT_CITY_BIKE);
                finish();
                break;
            case eu.opentransportnet.databikers.R.id.velo_img:
                clean();
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.velo_text);
                heder.setTypeface(null, Typeface.BOLD);
                mSessionManager.saveBikeType(Classificators.TRANSPORT_CHILD_BIKE);
                MainActivity.setMovementType(Classificators.TRANSPORT_CHILD_BIKE);
                finish();
                break;
            case eu.opentransportnet.databikers.R.id.race_bike_img:
                clean();
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.race_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                mSessionManager.saveBikeType(Classificators.TRANSPORT_RACE_BIKE);
                MainActivity.setMovementType(Classificators.TRANSPORT_RACE_BIKE);
                finish();
                break;
            case eu.opentransportnet.databikers.R.id.mountain_bike_img:
                clean();
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.mountain_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                mSessionManager.saveBikeType(Classificators.TRANSPORT_MOUNTAIN_BIKE);
                MainActivity.setMovementType(Classificators.TRANSPORT_MOUNTAIN_BIKE);
                finish();
                break;
            case eu.opentransportnet.databikers.R.id.electric_bike_img:
                clean();
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.electric_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                mSessionManager.saveBikeType(Classificators.TRANSPORT_ELECTRIC_BIKE);
                MainActivity.setMovementType(Classificators.TRANSPORT_ELECTRIC_BIKE);
                finish();
                break;
            case eu.opentransportnet.databikers.R.id.carrier_bike_img:
                clean();
                heder = (TextView) findViewById(eu.opentransportnet.databikers.R.id.carrier_bike_text);
                heder.setTypeface(null, Typeface.BOLD);
                mSessionManager.saveBikeType(Classificators.TRANSPORT_CARRIER_BIKE);
                MainActivity.setMovementType(Classificators.TRANSPORT_CARRIER_BIKE);
                finish();
                break;
        }
    }

    /**
     * Take off any style from bike type textViews
     */
    private void clean() {
        TextView header;
        header = (TextView) findViewById(eu.opentransportnet.databikers.R.id.city_bike_text);
        header.setTypeface(null, Typeface.NORMAL);
        header = (TextView) findViewById(eu.opentransportnet.databikers.R.id.velo_text);
        header.setTypeface(null, Typeface.NORMAL);
        header = (TextView) findViewById(eu.opentransportnet.databikers.R.id.race_bike_text);
        header.setTypeface(null, Typeface.NORMAL);
        header = (TextView) findViewById(eu.opentransportnet.databikers.R.id.mountain_bike_text);
        header.setTypeface(null, Typeface.NORMAL);
        header = (TextView) findViewById(eu.opentransportnet.databikers.R.id.electric_bike_text);
        header.setTypeface(null, Typeface.NORMAL);
        header = (TextView) findViewById(eu.opentransportnet.databikers.R.id.carrier_bike_text);
        header.setTypeface(null, Typeface.NORMAL);
    }
}


