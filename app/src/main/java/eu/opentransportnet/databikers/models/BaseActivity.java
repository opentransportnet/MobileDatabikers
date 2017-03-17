package eu.opentransportnet.databikers.models;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import eu.opentransportnet.databikers.utils.Utils;

/**
 *
 * @author Kristaps Krumins
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Utils.loadLocale(this);
    }

    public void setToolbarTitle(int resId) {
        TextView title = (TextView) findViewById(eu.opentransportnet.databikers.R.id.title);
        if (title != null) {
            title.setText(resId);
        }
    }

    public void initToolbarBackBtn() {
        setToolbarBackBtn(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
    }

    public void setToolbarBackBtn(View.OnClickListener listener) {
        ImageButton back = (ImageButton) findViewById(eu.opentransportnet.databikers.R.id.back_button);
        if (back != null) {
            back.setOnClickListener(listener);
        }

    }

    /**
     * Initializes lose focus feature for content view
     */
    public void initLoseFocusInContent() {
        View view = findViewById(android.R.id.content);
        enableLoseFocusInView(view);
    }

    /**
     * Enables to lose focus form EditText when clicked outside
     */
    public void enableLoseFocusInView(View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    View currentView = getCurrentFocus();
                    if(currentView != null) {
                        imm.hideSoftInputFromWindow(currentView.getWindowToken(), 0);
                    }
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                enableLoseFocusInView(innerView);
            }
        }
    }
}
