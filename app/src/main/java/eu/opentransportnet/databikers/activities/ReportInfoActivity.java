package eu.opentransportnet.databikers.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import eu.opentransportnet.databikers.interfaces.VolleyRequestListener;
import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.network.Requests;
import eu.opentransportnet.databikers.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Ilmars Svilsts
 */

public class ReportInfoActivity extends BaseActivity implements View.OnClickListener {
    String mJson = "json";
    String mLat = "lat";
    String mLng = "lng";
    String mLoadUrl = "file:///android_asset/www/report.html";

    public double mLatitude;
    public double mLongitude;

    private Context mContext;
    private int mIssueId = -1;
    private RelativeLayout mLoadingPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(eu.opentransportnet.databikers.R.layout.activity_report_info);
        mLoadingPanel = (RelativeLayout) findViewById(eu.opentransportnet.databikers.R.id.loading_panel);
        initToolbarBackBtn();
        pictureInit();

        ImageButton deleteButton = (ImageButton) findViewById(eu.opentransportnet.databikers.R.id.delete_button);
        deleteButton.setVisibility(View.VISIBLE);
        deleteButton.setOnClickListener(this);

        Intent intent = getIntent();
        String json = intent.getStringExtra(mJson);

        mLatitude = intent.getDoubleExtra(mLat, 200);
        mLongitude = intent.getDoubleExtra(mLng, 200);

        try {
            JSONObject obj = new JSONObject(json);
            infoInit(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        final WebView webView = (WebView) findViewById(eu.opentransportnet.databikers.R.id.webView);
        WebSettings mWebSettings = webView.getSettings();
        // Enable Javascript
        mWebSettings.setJavaScriptEnabled(true);
        // Force links and redirects to open in the WebView instead of in a browser
        webView.setWebChromeClient(new WebChromeClient() {
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            //Enable console.log() from JavaScript
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("Report3Details.java", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        });
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        webView.loadUrl(mLoadUrl);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.loadUrl("javascript:showLoc(" + mLatitude + "," + mLongitude + ")");
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        pictureInit();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case eu.opentransportnet.databikers.R.id.delete_button:
                deleteIssue();
                break;
        }
    }

    /**
     * Delete reported issue
     */
    private void deleteIssue() {
        mLoadingPanel.setVisibility(View.VISIBLE);

        boolean requestSent = Requests.deleteIssue(this, mIssueId,
                new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject response) {
                        mLoadingPanel.setVisibility(View.GONE);
                        int rc = Utils.getResponseCode(response);

                        if (rc == 0) {
                            Utils.showToastAtTop(mContext, mContext.getString(eu.opentransportnet.databikers.R.string.issue_deleted));
                            finish();
                            return;
                        } else if (rc == 502) {
                            Utils.showToastAtTop(mContext, mContext.getString(eu.opentransportnet.databikers.R.string.issue_delete_only_owner));
                        } else {
                            Utils.showToastAtTop(mContext, mContext.getString(eu.opentransportnet.databikers.R.string.something_went_wrong));
                        }
                    }

                    @Override
                    public void onError(JSONObject response) {
                        mLoadingPanel.setVisibility(View.GONE);
                    }
                }, null);

        if (!requestSent) {
            mLoadingPanel.setVisibility(View.GONE);
        }
    }


    void pictureInit() {
        final LinearLayout l = (LinearLayout) findViewById(eu.opentransportnet.databikers.R.id.picture_layout_report_info);
        ViewTreeObserver observer = l.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                init();
                l.getViewTreeObserver().removeGlobalOnLayoutListener(
                        this);
            }
        });
    }

    protected void init() {
        final LinearLayout l = (LinearLayout) findViewById(eu.opentransportnet.databikers.R.id.picture_layout_report_info);
        int a = l.getHeight();
        int b = l.getWidth();
        ImageView layout = (ImageView) findViewById(eu.opentransportnet.databikers.R.id.issue_picture_report_info_activity);
        // Gets the layout params that will allow you to resize the layout
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
        // Changes the height and width to the specified *pixels*
        params.height = a;
        layout.setLayoutParams(params);
    }

    /**
     * Initialize report data in layout
     * @param json reported issue info
     */
    void infoInit(JSONObject json) {
        //Adding title
        TextView title = (TextView) findViewById(eu.opentransportnet.databikers.R.id.title);
        String txt = getString(eu.opentransportnet.databikers.R.string.report) + " - Other";
        try {
            txt = getString(eu.opentransportnet.databikers.R.string.report) + " - " + json.getString("issueTypeName");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        title.setText(txt);

        //Adding description
        try {
            txt = json.getString("description");
            if (txt != "") {
                TextView text = (TextView) findViewById(eu.opentransportnet.databikers.R.id.description_report_info_activity);
                text.setText(txt);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Adding long and lat to webview
        try {
            mLatitude = json.getDouble("latitude");
            mLongitude = json.getDouble("longitude");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Adding date
        try {
            txt = json.getString("report_date");
            if (txt != "") {
                TextView text = (TextView) findViewById(eu.opentransportnet.databikers.R.id.date_report_info_activity);
                text.setText(txt);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Adding picture
        txt = MainActivity.sIssueImageBase64;
        if (txt == "" || txt == null || txt.length() == 0) {
            LinearLayout img = (LinearLayout) findViewById(eu.opentransportnet.databikers.R.id.picture_layout_report_info);
            img.setVisibility(View.GONE);
        } else {
            try {
                byte[] decodedString = Base64.decode(txt, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ImageView img = (ImageView) findViewById(eu.opentransportnet.databikers.R.id.issue_picture_report_info_activity);
                img.setImageBitmap(decodedByte);
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            }
        }

        try {
            mIssueId = json.getInt("issueId");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}












