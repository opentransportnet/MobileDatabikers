package eu.opentransportnet.databikers.activities;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.utils.Utils;

import java.util.Locale;

/**
 * @author Kristaps Krumins
 */
public class ChangeLangActivity extends BaseActivity {
    private ListView mLanguageList;
    private String[] mLangCodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        setContentView(eu.opentransportnet.databikers.R.layout.activity_change_language);
        setToolbarTitle(eu.opentransportnet.databikers.R.string.title_language);
        initToolbarBackBtn();

        MainActivity.mLanguageBtn=getString(eu.opentransportnet.databikers.R.string.new_button);

        TextView language = (TextView) findViewById(eu.opentransportnet.databikers.R.id.current_language);
        language.setText(Locale.getDefault().getDisplayLanguage());

        // Gets ListView object from xml
        mLanguageList = (ListView) findViewById(eu.opentransportnet.databikers.R.id.language_list);

        Resources res = getResources();
        String[] languages = res.getStringArray(eu.opentransportnet.databikers.R.array.languages);
        mLangCodes = res.getStringArray(eu.opentransportnet.databikers.R.array.language_codes);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                eu.opentransportnet.databikers.R.layout.black_list_item, languages);
        mLanguageList.setAdapter(adapter);
        mLanguageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Utils.changeLanguage(getBaseContext(), mLangCodes[position]);
                finish();
            }

        });
    }
}
