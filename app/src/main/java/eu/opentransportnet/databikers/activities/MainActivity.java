package eu.opentransportnet.databikers.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import eu.opentransportnet.databikers.BuildConfig;
import eu.opentransportnet.databikers.adapters.CustomListAdapter;
import eu.opentransportnet.databikers.interfaces.VolleyRequestListener;
import eu.opentransportnet.databikers.listeners.SlideMenuClickListener;
import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.models.User;
import eu.opentransportnet.databikers.network.NetworkReceiver;
import eu.opentransportnet.databikers.network.RequestQueueSingleton;
import eu.opentransportnet.databikers.network.Requests;
import eu.opentransportnet.databikers.network.UploadTask;
import eu.opentransportnet.databikers.utils.Const;
import eu.opentransportnet.databikers.utils.SessionManager;
import eu.opentransportnet.databikers.utils.Utils;
import com.library.routerecorder.RouteAlert;
import com.library.routerecorder.RouteRecorder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author Kristaps Krumins
 * @author Ilmars Svilsts
 */
public class MainActivity extends BaseActivity implements RouteAlert.RouteAlertListener,
        View.OnClickListener {
    public final String TAG_DELETE_USER = "delete user request";

    private static final String LOG_TAG = "MainActivity";
    private static final String ROUTE_FILE_PATH = "track";
    private static final int RC_STATS = 1002;
    private static final int RC_STATS_DO_NOT_CONTINUE = 1003;

    public static Activity sActivity;
    public static String mFilePath = "";
    public static String mEncodedKmlFile = "";
    public static String sIssueImageBase64;
    public static String mDelete;
    public static String mLanguageBtn = null;
    public static String[] mName = {""};
    public static int mStats = 1;
    public static int sCanUploadTracks = 1;
    public static int mRef = 1;

    private static Context sContext;
    private static RouteRecorder sRouteRec;
    private static int sMovementType = -1;
    private static boolean sOfflineMode = true;

    private DrawerLayout mDrawer;
    private NetworkReceiver mNetworkReceiver;
    private ListView mDrawerList;
    private double mReportPinLat;
    private double mReportPinLng;
    private boolean mReportPin = false;
    private SessionManager mSessionManager = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        sContext = this;
        sActivity = this;
        mSessionManager = new SessionManager(this);
        setContentView(eu.opentransportnet.databikers.R.layout.activity_home);
        initDrawer();
        initToolbar();
        initRouteRecorder();
        createDefaultFolders();
        mNetworkReceiver = new NetworkReceiver(this);
        sMovementType = mSessionManager.getUser().getBikeType();
        mLanguageBtn = getString(eu.opentransportnet.databikers.R.string.new_button);

        ImageView reportBtn = (ImageView) findViewById(eu.opentransportnet.databikers.R.id.report_button);
        reportBtn.setOnClickListener(this);
        ImageView routeRecordButton = (ImageView) findViewById(eu.opentransportnet.databikers.R.id.route_record_button);
        routeRecordButton.setOnClickListener(this);

        UploadTask.getInstance(this).startScheduledUpload();

        TextView version = (TextView) findViewById(eu.opentransportnet.databikers.R.id.version);
        String versionName = BuildConfig.VERSION_NAME;
        try {
            if (getString(eu.opentransportnet.databikers.R.string.svn_version).equals("null")) {
                version.setText("v" + versionName);
            } else {
                version.setText("v" + versionName + " (" + getString(eu.opentransportnet.databikers.R.string.svn_version) + ")");
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registers BroadcastReceiver to track network connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh all text fields in case language has been changed.
        setToolbarTitle(eu.opentransportnet.databikers.R.string.title_route);
        setDrawerAdapter();
        refreshButtons();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        setResult(RESULT_OK, data);
        super.finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RequestQueueSingleton.getInstance(sContext).cancelAllPendingRequests(TAG_DELETE_USER);
        sRouteRec.stopRecording(false);
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_STATS) {
            if (resultCode == Activity.RESULT_OK) {
                // Save track
                Bundle extras = data.getExtras();
                setTrackButtons(false, extras.getString("name"), extras.getDouble("date"),
                        extras.getDouble("dist"), extras.getDouble("lat_start"),
                        extras.getDouble("lon_start"), extras.getBoolean("is_public"));
            } else {
                // Track saving canceled
                sRouteRec.getMainLocation().removeFinishPoint();
            }
        } else if (requestCode == RC_STATS_DO_NOT_CONTINUE) {
            if (resultCode == Activity.RESULT_OK) {
                // Save track
                Bundle extras = data.getExtras();
                setTrackButtons(false, extras.getString("name"), extras.getDouble("date"),
                        extras.getDouble("dist"), extras.getDouble("lat_start"),
                        extras.getDouble("lon_start"), extras.getBoolean("is_public"));
            } else {
                // No activity from user for long time and track too short
                // Delete current track files
                String fileName = sRouteRec.getCurrRecordedRoute();
                File file = new File(getFilesDir(),
                        "/" + Const.STORAGE_PATH_TRACK + "/" + fileName + ".csv");
                file.delete();

                file = new File(getFilesDir(),
                        "/" + Const.STORAGE_PATH_TRACK + "/" + fileName + ".kml");
                file.delete();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case eu.opentransportnet.databikers.R.id.report_button:
                if (!sRouteRec.isRecording()) {
                    if (mReportPin) {
                        sRouteRec.getWebView().loadUrl("javascript:reportPinMovedBefore = 5000");
                        Intent selectProblem = new Intent(this, Report1ProblemsActivity.class);
                        selectProblem.putExtra("lat", mReportPinLat);
                        selectProblem.putExtra("lng", mReportPinLng);
                        startActivity(selectProblem);
                    } else {
                        Utils.showToastAtTop(this, getString(eu.opentransportnet.databikers.R.string.pin_location));
                    }
                } else {
                    sRouteRec.getWebView().loadUrl("javascript:reportPinMovedBefore = 5000");
                    Intent selectProblem = new Intent(this, Report1ProblemsActivity.class);
                    selectProblem.putExtra("lat", sRouteRec.getLatitude());
                    selectProblem.putExtra("lng", sRouteRec.getLongitude());
                    startActivity(selectProblem);
                }
                break;
            case eu.opentransportnet.databikers.R.id.route_record_button:
                StartTrack();
                break;
        }
    }

    @JavascriptInterface
    public void setReportCoord(double lat, double lng) {
        mReportPinLat = lat;
        mReportPinLng = lng;
        setReportPin(true);
    }

    @JavascriptInterface
    public void setReportPin(boolean value) {
        mReportPin = value;
    }

    @JavascriptInterface
    public void onIssueClick(int issueId) {
        Utils.logD(LOG_TAG, "onIssueClick issueID:" + issueId);

        if (issueId > 1) {
            showIssue(issueId);
        }
    }

    @Override
    public void onDialogPositiveClick(int id) {
    }

    @Override
    public void onDialogNegativeClick(int id) {
        if (id == RouteAlert.ROUTE_TOO_SHORT_ALERT) {
            // Recording stopped
            setStartTrackButtons();
        } else if (id == RouteAlert.ROUTE_NO_MOVEMENT) {
            if (sRouteRec.isRouteFileCreated()) {
                Intent stats = new Intent(this, StatsActivity.class);
                stats.putExtra("com.antwerp.MESSAGE", sRouteRec.getCurrRecordedRoute());
                String EXTRA = "com.antwerp.Nop";
                stats.putExtra(EXTRA, "ok");
                startActivityForResult(stats, RC_STATS_DO_NOT_CONTINUE);

                setStartTrackButtons();
            } else {
                setStartTrackButtons();
            }
        }
    }

    /**
     * After "New" button click starts route recording
     */
    public void StartTrack() {
        String EXTRA_MESSAGE = "com.antwerp.MESSAGE";
        TextView text = (TextView) findViewById(eu.opentransportnet.databikers.R.id.route_record_button_text);
        ImageView button = (ImageView) findViewById(eu.opentransportnet.databikers.R.id.route_record_button);
        String sharedFact = text.getText().toString();
        String start = getString(eu.opentransportnet.databikers.R.string.new_button);
        String stop = getString(eu.opentransportnet.databikers.R.string.stop_button);
        //starts new route
        if (start.equals(sharedFact) && sRouteRec.startRecNewRoute(sMovementType)) {
            text.setText(stop);
            button.setImageResource(eu.opentransportnet.databikers.R.drawable.u6);
        }
        //stops route, because it have already been started
        else if (stop.equals(sharedFact)) {
            //open activity of recorded route info
            if (sRouteRec.isRouteFileCreated()) {
                sRouteRec.getMainLocation().setFinishPoint();
                Intent c = new Intent(this, StatsActivity.class);

                String EXTRA = "com.antwerp.Nop";
                c.putExtra(EXTRA_MESSAGE, sRouteRec.getCurrRecordedRoute());
                c.putExtra(EXTRA, "ok");
                startActivityForResult(c, RC_STATS);
            }
            //route shorter than 10m, alert user
            else {
                RouteAlert routeAlert = new RouteAlert(this, sRouteRec);
                routeAlert.showRouteTooShortDialog();
            }
        }
    }

    /**
     * Checks if user wants to save route, if yes then calls saveTrackInfo method
     * @param startNew if new route is started
     * @param mName route name
     * @param mDate route date
     * @param mDist route distance
     * @param mLon route start longitude
     * @param mLat route start latitude
     * @param isPublic is route public or private
     */
    public void setTrackButtons(boolean startNew, String mName, double mDate,
                                double mDist, double mLon, double mLat, boolean isPublic) {
        TextView text = (TextView) sActivity.findViewById(eu.opentransportnet.databikers.R.id.route_record_button_text);
        ImageView button = (ImageView) sActivity.findViewById(eu.opentransportnet.databikers.R.id.route_record_button);

        if (startNew) {
            if (sRouteRec.startRecNewRoute(sMovementType)) {
                String stop = getString(eu.opentransportnet.databikers.R.string.stop_button);
                text.setText(stop);
                button.setImageResource(eu.opentransportnet.databikers.R.drawable.u6);
            }
        } else {
            sRouteRec.finishRoute();
            String start = getString(eu.opentransportnet.databikers.R.string.new_button);
            text.setText(start);
            button.setImageResource(eu.opentransportnet.databikers.R.drawable.u7);
            saveTrackInfo(mName, mDate, mDist, mLon, mLat, isPublic);
        }
    }

    /**
     * Set track buttons
     */
    public void setStartTrackButtons() {
        TextView text = (TextView) sActivity.findViewById(eu.opentransportnet.databikers.R.id.route_record_button_text);
        ImageView button = (ImageView) sActivity.findViewById(eu.opentransportnet.databikers.R.id.route_record_button);
        String start = getString(eu.opentransportnet.databikers.R.string.new_button);
        text.setText(start);
        button.setImageResource(eu.opentransportnet.databikers.R.drawable.u7);
        text = (TextView) sActivity.findViewById(eu.opentransportnet.databikers.R.id.report_button_activity_home);
        String rep = getString(eu.opentransportnet.databikers.R.string.report);
        text.setText(rep);
    }

    public static byte[] loadFileAsBytesArray(String fileName) throws Exception {

        File file = new File(fileName);
        int length = (int) file.length();
        BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file));
        byte[] bytes = new byte[length];
        reader.read(bytes, 0, length);
        reader.close();
        return bytes;
    }

    private static String encode(String sourceFile) throws Exception {

        byte[] base64EncodedData = loadFileAsBytesArray(sourceFile);

        String imageEncodeds = Base64.encodeToString(base64EncodedData, Base64.NO_WRAP);

        return imageEncodeds;
    }


    /**
     * Puts all info about route in json file and saves locally.
     * Calls Requests.registerTrack method, which registers track using services.
     * @param mName route name
     * @param duration route duration
     * @param distance route distance
     * @param mLon route longitude
     * @param mLat route latitude
     * @param isPublic is route public or not
     */
    public void saveTrackInfo(String mName, double duration, double distance, double mLon, double
            mLat, boolean isPublic) {
        org.json.simple.JSONObject obj = new org.json.simple.JSONObject();
        JSONArray EMPTY = new JSONArray();
        User user = mSessionManager.getUser();
        obj.put("name", mName);
        obj.put("duration", duration);
        obj.put("distance", distance);
        obj.put("transportId", user.getBikeType());
        obj.put("appId", Const.APPLICATION_ID);
        obj.put("userId", mSessionManager.getUser().getHashedEmail());
        obj.put("lat_start", mLat);
        obj.put("lon_start", mLon);
        obj.put("trackRatings", EMPTY);
        obj.put("weatherList", EMPTY);
        obj.put("is_public", isPublic);
        obj.put("start_address", getLocAddress(this, mLat, mLon));

        try {
            Bitmap bm = BitmapFactory.decodeFile(MainActivity.getContext().getFilesDir() +
                    "/photo/" + StatsActivity.mTrackId + ".png");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
            byte[] b = baos.toByteArray();
            String encodedImage = Base64.encodeToString(b, Base64.NO_WRAP);
            obj.put("picture", encodedImage);

        } catch (Exception e) {
            e.printStackTrace();
            obj.put("picture", "null");
        }
        try {
            obj.put("trackFileCsv", encode(MainActivity.getContext().getFilesDir() +
                    "/track/" + StatsActivity.mTrackId + ".csv"));
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("trackFileCsv", "null");
        }

        try {
            obj.put("route_kml", encode(MainActivity.getContext().getFilesDir() +
                    "/track/" + StatsActivity
                    .mTrackId + ".kml"));
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("route_kml", "null");
        }
        saveReportLocally(obj);
        JSONObject track = null;
        try {
            track = new JSONObject(obj.toJSONString());
        } catch (JSONException e) {
            return;
        }
        String fileName = sRouteRec.getCurrRecordedRoute();
        Requests.registerTrack(this, track, fileName);
    }

    /**
     * Saves route locally
     * @param report route in json format
     * @return true if ok, false if error
     */
    public boolean saveReportLocally(org.json.simple.JSONObject report) {

        mFilePath = MainActivity.getContext().getFilesDir() +
                "/info/" + sRouteRec.getCurrRecordedRoute() + ".json";

        // get file
        File jsonFile = new File(mFilePath);
        String previousJson;
        if (jsonFile.exists()) {
            try {
                JSONParser parser = new JSONParser();
                Object obj = null;
                try {
                    // get data as object
                    obj = parser.parse(new FileReader(mFilePath));
                } catch (org.json.simple.parser.ParseException e) {
                    e.printStackTrace();
                }
                if (obj != null) {
                    // convert object to string in JSON syntax
                    previousJson = ((org.json.simple.JSONArray) obj).toJSONString();
                } else {
                    previousJson = "[]"; //empty JSONArray
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            previousJson = "[]"; //empty JSONArray
        }
        JSONArray fullObj;
        try {
            // Make JSON array from string
            fullObj = new JSONArray(previousJson);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        // Add to JSON array report
        fullObj.put(report);
        // generate string from the object
        String jsonString = null;
        jsonString = report.toString();
        // write back JSON file
        if (!writeJSON(jsonString)) {
            // If writing failed
            return false;
        }
        System.gc();
        return true;
    }

    /**
     * Write json object in file
     * @param obj json object as string
     * @return if any error
     */
    public static boolean writeJSON(String obj) {
        try {
            File report = new File(mFilePath);
            if (!report.exists()) {
                report.createNewFile();
            }
            FileWriter file = new FileWriter(mFilePath);
            file.write(obj);
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //MainActivity sContext
    public static Context getContext() {
        return sContext;
    }

    public static RouteRecorder getRouteRecorder() {
        return sRouteRec;
    }

    /**
     * Changes latitude an longitude to user readable address
     * @param context context of activity
     * @param latitude  address latitude
     * @param longitude address longitude
     * @return  returns address as string
     */
    public static String getLocAddress(Context context, double latitude, double longitude) {

        Geocoder gc = new Geocoder(context);

        if (gc.isPresent()) {
            List<Address> list = null;
            try {
                list = gc.getFromLocation(latitude, longitude, 1);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException iae) {
            }
            if (list != null) {
                if (list.size() != 0) {
                    Address address = list.get(0);
                    String Adrese = address.getAddressLine(0);
                    Adrese += ", ";
                    Adrese += address.getAddressLine(1);
                    Adrese += ", ";
                    Adrese += address.getAddressLine(2);
                    return String.valueOf(Adrese);
                }
            } else {
                return String.valueOf(latitude) + " " + String.valueOf(longitude);
            }
        }
        return String.valueOf(latitude) + " " + String.valueOf(longitude);
    }

    /**
     * Sets user movement type
     */
    public static void setMovementType(int movementType) {
        sMovementType = movementType;
        if (sRouteRec != null) {
            sRouteRec.setMovementType(movementType);
        }
    }

    /**
     * Sets user main navigation drawer adapter
     */
    private void setDrawerAdapter() {
        String[] drawerItems = getResources().getStringArray(eu.opentransportnet.databikers.R.array.drawer_items);

        Integer[] drawerItemImages = {
                eu.opentransportnet.databikers.R.drawable.u40,
                eu.opentransportnet.databikers.R.drawable.u25,
                eu.opentransportnet.databikers.R.drawable.u33,
                eu.opentransportnet.databikers.R.drawable.change_language,
                eu.opentransportnet.databikers.R.drawable.dis,
                eu.opentransportnet.databikers.R.drawable.bin,
                eu.opentransportnet.databikers.R.drawable.log_out
        };

        CustomListAdapter adapter = new CustomListAdapter(
                MainActivity.this,
                drawerItems,
                drawerItemImages);

        mDrawerList.setAdapter(adapter);
    }

    /**
     * If language changed, change button texts
     */
    private void refreshButtons() {
        TextView text = (TextView) findViewById(eu.opentransportnet.databikers.R.id.route_record_button_text);
        String sharedFact = text.getText().toString();
        if (mLanguageBtn == null) {
            String newBtn = getString(eu.opentransportnet.databikers.R.string.new_button);
            text.setText(newBtn);
        } else {
            if (mLanguageBtn.equals(sharedFact)) {
                String newBtn = getString(eu.opentransportnet.databikers.R.string.new_button);
                text.setText(newBtn);
            } else {
                String stop = getString(eu.opentransportnet.databikers.R.string.stop_button);
                text.setText(stop);
            }
        }
        TextView report = (TextView) findViewById(eu.opentransportnet.databikers.R.id.report_button_activity_home);
        report.setText(getString(eu.opentransportnet.databikers.R.string.report));
    }

    /**
     * Initializes toolbar
     */
    private void initToolbar() {
        setToolbarTitle(eu.opentransportnet.databikers.R.string.title_route);
        ImageButton drawer = (ImageButton) findViewById(eu.opentransportnet.databikers.R.id.back_button);
        drawer.setBackgroundResource(eu.opentransportnet.databikers.R.drawable.ic_ab_drawer);
        drawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawer.openDrawer(Gravity.START);
            }
        });
    }

    private void initRouteRecorder() {
        sRouteRec = (RouteRecorder) getFragmentManager().findFragmentById(eu.opentransportnet.databikers.R.id.route_recorder);
        sRouteRec.setDefaultLocation(Const.DEFAULT_LATITUDE, Const.DEFAULT_LONGITUDE);
        sRouteRec.setTracking(true);
        sRouteRec.addAccelerometer();
        sRouteRec.setRouteFilePath(ROUTE_FILE_PATH);
        sRouteRec.setRouteAlert(new RouteAlert(this, sRouteRec));
        sRouteRec.addJavascriptInterface(this, "MainActivity");
        sRouteRec.loadWebView();
        sRouteRec.loadUrl("javascript:addWmsLayer(" + 0 + ",'"
                + Const.WMS_URL_POIS_ANTWERP + "','issues_antwerp')");
        sRouteRec.loadUrl("javascript:addWmsLayer(" + 1 + ",'"
                + Const.WMS_URL_ROUTES_ANTWERP + "','bicycle_trips')");
    }

    public void initDrawer() {
        mDrawer = (DrawerLayout) findViewById(eu.opentransportnet.databikers.R.id.drawer);
        mDrawer.setDrawerShadow(eu.opentransportnet.databikers.R.drawable.drawer_shadow, Gravity.START);
        mDrawerList = (ListView) findViewById(eu.opentransportnet.databikers.R.id.drawerlist);
        setDrawerAdapter();
        mDrawerList.setOnItemClickListener(new SlideMenuClickListener(mDrawer, this));

        User user = mSessionManager.getUser();
        // Sets name in drawer
        String fullName = user.getFirstName() + " " + user.getLastName();
        if (fullName == null || fullName == "") {
            fullName = "[no name provided]";
        }
        TextView displayName = (TextView) findViewById(eu.opentransportnet.databikers.R.id.display_name);
        displayName.setText(fullName);

        Bitmap photo = user.getRoundedPhoto();
        setPhotoInDrawer(photo);

        if (!user.hasPhoto()) {
            user.downloadPhotoAndPutInDrawer(this);
        }
    }

    public void setPhotoInDrawer(final Bitmap photo) {
        // Sets profile photo in drawer
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView profilePhoto = (ImageView) findViewById(eu.opentransportnet.databikers.R.id.profile_photo);
                profilePhoto.setImageBitmap(photo);
            }
        });
    }

    /**
     * Creates default folders for app
     */
    private void createDefaultFolders() {
        File folder = new File(getFilesDir() + "/" + Const.STORAGE_PATH_INFO);
        folder.mkdir();
        folder = new File(getFilesDir() + "/" + Const.STORAGE_PATH_TRACK);
        folder.mkdir();
        folder = new File(getFilesDir() + "/" + Const.STORAGE_PATH_REPORT);
        folder.mkdir();
        folder = new File(getFilesDir() + "/photo");
        folder.mkdir();
    }

    /**
     * Delete user content from server if user chooses so
     */
    public void deleteUser() {
        FrameLayout spinner;
        spinner = (FrameLayout) findViewById(eu.opentransportnet.databikers.R.id.progress);
        spinner.setVisibility(View.VISIBLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(sContext);
        builder.setTitle(sContext.getString(eu.opentransportnet.databikers.R.string.delete_user_title))
            .setMessage(sContext.getString(eu.opentransportnet.databikers.R.string.delete_user_content))
            .setPositiveButton(sContext.getString(eu.opentransportnet.databikers.R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        org.json.simple.JSONObject objs = new org.json.simple.JSONObject();
                        objs.put("appId", Const.APPLICATION_ID);
                        objs.put("userId", Utils.getHashedUserEmail(sContext));

                        String jsonBodyString = ((org.json.simple.JSONObject) objs).toJSONString();
                        JSONObject jsonBody = null;
                        try {
                            jsonBody = new JSONObject(jsonBodyString);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Requests.sendRequest(sContext, "http://" + Utils.getHostname() +
                            Utils.getUrlPathStart() + Requests.PATH_DELETE_USER, jsonBody,
                            new VolleyRequestListener<JSONObject>() {
                                @Override
                                public void onResult(JSONObject mUseritem) {
                                    if (mUseritem != null) {
                                        String a = "2";
                                        try {
                                            a = mUseritem.getString("responseCode");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        String check = "0";
                                        if (check.equals(a)) {
                                            FrameLayout spinner;
                                            spinner = (FrameLayout) findViewById(eu.opentransportnet.databikers.R.id.progress);
                                            spinner.setVisibility(View.GONE);

                                            Utils.showToastAtTop(sContext, sContext.getString(eu.opentransportnet.databikers.R.string.delete_user));
                                            Utils.deleteAllLocalFiles(sContext);
                                            new SessionManager(sActivity).forceLogoutUser();
                                        } else {
                                            FrameLayout spinner;
                                            spinner = (FrameLayout) findViewById(eu.opentransportnet.databikers.R.id.progress);
                                            spinner.setVisibility(View.GONE);
                                            Utils.showToastAtTop(sContext, sContext.getString(eu.opentransportnet.databikers.R.string.error_activity_stats));
                                        }
                                    }
                                }

                                @Override
                                public void onError(JSONObject error) {
                                    FrameLayout spinner;
                                    spinner = (FrameLayout) findViewById(eu.opentransportnet.databikers.R.id.progress);
                                    spinner.setVisibility(View.GONE);
                                    Utils.showToastAtTop(sContext, sContext.getString(eu.opentransportnet.databikers.R.string.error_activity_stats));
                                }
                            }, TAG_DELETE_USER);
                    }
                })
            .setNegativeButton(sContext.getString(eu.opentransportnet.databikers.R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        FrameLayout spinner;
                        spinner = (FrameLayout) findViewById(eu.opentransportnet.databikers.R.id.progress);
                        spinner.setVisibility(View.GONE);
                    }
                });
        builder.create().show();
    }

    /**
     * Show reported issue, which is selected by user
     * @param id issues id
     */
    public void showIssue(final int id) {
        runOnUiThread(new Runnable() {

            public void run() {
                FrameLayout spinner;
                spinner = (FrameLayout) findViewById(eu.opentransportnet.databikers.R.id.progress);
                spinner.setVisibility(View.VISIBLE);
                JSONObject objs = new JSONObject();

                try {
                    objs.put("issueId", id);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Requests.sendRequest(sContext, "http://" + Utils.getHostname() +
                        Utils.getUrlPathStart() + Requests.PATH_LOAD_ISSUE,
                        objs, new VolleyRequestListener<JSONObject>() {
                            @Override
                            public void onResult(JSONObject mUseritem) {
                                if (mUseritem != null) {
                                    String a = "2";
                                    String check = "0";
                                    try {
                                        a = mUseritem.getString("responseCode");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    if (check.equals(a)) {

                                        FrameLayout spinner;
                                        spinner = (FrameLayout) findViewById(eu.opentransportnet.databikers.R.id.progress);
                                        spinner.setVisibility(View.GONE);


                                        Intent issue = new Intent(sContext, ReportInfoActivity
                                                .class);

                                        // Cant pass large data through intent so remove picture
                                        // from JSON
                                        try {
                                            sIssueImageBase64 = mUseritem.getString("picture");
                                            mUseritem.remove("picture");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        issue.putExtra("json", mUseritem.toString());
                                        startActivity(issue);
                                    } else {
                                        FrameLayout spinner;
                                        spinner = (FrameLayout) findViewById(eu.opentransportnet.databikers.R.id.progress);
                                        spinner.setVisibility(View.GONE);
                                        Utils.showToastAtTop(sContext, sContext.getString(eu.opentransportnet.databikers.R
                                                .string.something_went_wrong));
                                    }
                                }
                            }

                            @Override
                            public void onError(JSONObject error) {
                                FrameLayout spinner;
                                spinner = (FrameLayout) findViewById(eu.opentransportnet.databikers.R.id.progress);
                                spinner.setVisibility(View.GONE);
                                Utils.showToastAtTop(sContext, sContext.getString(eu.opentransportnet.databikers.R.string.something_went_wrong));
                            }
                        }, null);
            }
        });
    }
}