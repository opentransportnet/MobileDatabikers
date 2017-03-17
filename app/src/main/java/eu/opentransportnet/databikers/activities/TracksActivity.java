package eu.opentransportnet.databikers.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import eu.opentransportnet.databikers.R;
import eu.opentransportnet.databikers.adapters.CustomTrackListAdapter;
import eu.opentransportnet.databikers.interfaces.VolleyRequestListener;
import eu.opentransportnet.databikers.listeners.SlideMenuClickListener;
import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.network.RequestQueueSingleton;
import eu.opentransportnet.databikers.network.Requests;
import eu.opentransportnet.databikers.utils.SessionManager;
import eu.opentransportnet.databikers.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity that answers about Track information displaying.
 *
 * @author Ilmars Svilsts
 */

public class TracksActivity extends BaseActivity {
    public static final String TAG_TRACK_LIST_REQUEST = "track list request";
    public static final String EXTRA_IS_PUBLIC_TRACKS = "is_public_tracks";
    public static final String EXTRA_IS_TRACK_PUBLIC = "is_track_public";
    public static final  String EXTRA_MESSAGE = "com.antwerp.MESSAGE";
    public static final  String EXTRA = "com.antwerp.Nop";
    public final String TAG_TRACK_INFO_REQUEST = "track info request";
    public int mCheck = 1;

    private static Context sContext;
    private File mTrackFile;
    private CustomTrackListAdapter mPrivateAdapter = null;
    private CustomTrackListAdapter mPublicAdapter = null;
    private SessionManager mSessionManager = null;
    private int mDownload = 0;

    ListView mDrawerList;
    List<String> mDistance = new ArrayList<>();
    List<String> mTime = new ArrayList<>();
    List<String> mNames = new ArrayList<>();
    List<String> mID = new ArrayList<>();
    List<String> mPublicDistance = new ArrayList<>();
    List<String> mPublicTime = new ArrayList<>();
    List<String> mPublicNames = new ArrayList<>();
    List<String> mPublicId = new ArrayList<>();

    static CustomTrackListAdapter mAdapter;
    ViewSwitcher mViewSwitcher;
    Animation mSlideInLeft, mSlideOutRight;
    int mErrors = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);
        Intent intent = getIntent();
        String message = intent.getStringExtra(SlideMenuClickListener.EXTRA_MESSAGE);

        mCheck = Integer.parseInt(message);
        mSessionManager = new SessionManager(this);

        //Sets title
        TextView title = (TextView) findViewById(R.id.title);
        title.setText(R.string.title_activity_tracks);
        // Attaching the layout to the toolbar object
        Toolbar mToolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(mToolbar);
        //Initializing back button
        ImageButton back = (ImageButton) findViewById(R.id.back_button);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        sContext = this;

        final FrameLayout progressBar = (FrameLayout) findViewById(R.id.progress);
        progressBar.setVisibility(View.VISIBLE);

        mPrivateAdapter = new
                CustomTrackListAdapter(TracksActivity.this, mNames, mDistance, mTime, mID);

        mPublicAdapter = new
                CustomTrackListAdapter
                (TracksActivity.this, mPublicNames, mPublicDistance, mPublicTime, mPublicId);
        updateAdapter();

        mDrawerList = (ListView) findViewById(R.id.listView);
        mDrawerList.setDivider(null);
        mDrawerList.setDividerHeight(0);
        mDrawerList.setAdapter(mPublicAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (haveNetworkConnection()) {
                    FrameLayout progressBar = (FrameLayout) findViewById(R.id.progress);
                    progressBar.setVisibility(View.VISIBLE);
                    TextView title = (TextView) view.findViewById(R.id.text_list);
                    final String str = title.getTag().toString().replace(".json", "");

                    final String TAG_TRACK_INFO_REQUEST = "track info request";
                    final File[] csvFile = {
                        new File(MainActivity.getContext().getFilesDir() + "/track/" + str + ".csv")
                    };

                    String userId = mSessionManager.getUser().getHashedEmail();
                    Requests.getTrackInfo(sContext, userId, Integer.parseInt(str),
                        new VolleyRequestListener<JSONObject>() {
                            @Override
                            public void onResult(JSONObject trackInfo) {
                                JSONObject trackObject = trackInfo;
                                try {
                                    MainActivity.mName[0] = trackObject.getString("name");
                                    if (!csvFile[0].exists()) {
                                        trackObject.getString("trackFilecsv");

                                        if (trackObject.getString("trackFilecsv") != "null") {

                                            byte[] decodedByte = android.util.Base64.decode(
                                                    trackObject.getString("trackFilecsv"), 0);

                                            try {
                                                File file2 = new File(
                                                        MainActivity.getContext().getFilesDir() +
                                                                "/track/" + str + ".csv");
                                                FileOutputStream os =
                                                        new FileOutputStream(file2, true);
                                                os.write(decodedByte);
                                                os.close();

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    MainActivity.mEncodedKmlFile =
                                            trackObject.getString("route_kml");

                                    MainActivity.mDelete = MainActivity.getContext().getFilesDir() +
                                            "/track/" + str + ".csv";
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Intent c = new Intent(TracksActivity.this, StatsActivity.class);
                                c.putExtra(EXTRA_MESSAGE, str);
                                c.putExtra(EXTRA, "no");
                                c.putExtra(EXTRA_IS_PUBLIC_TRACKS, true);
                                TracksActivity.this.startActivity(c);
                                FrameLayout progressBar = (FrameLayout) findViewById(R.id.progress);
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(JSONObject error) {
                                String start = getString(R.string.error_activity_stats);
                                Utils.showToastAtTop(sContext, start);
                                FrameLayout progressBar = (FrameLayout) findViewById(R.id.progress);
                                progressBar.setVisibility(View.GONE);
                            }
                        }, TAG_TRACK_INFO_REQUEST);

                } else {
                    Utils.showToastAtTop(sContext, getString(R.string.network_error));
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        mDrawerList = (ListView) findViewById(R.id.mylistView);
        mDrawerList.setDivider(null);
        mDrawerList.setDividerHeight(0);
        mDrawerList.setAdapter(mPrivateAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                FrameLayout progressBar = (FrameLayout) findViewById(R.id.progress);
                progressBar.setVisibility(View.VISIBLE);
                TextView title = (TextView) view.findViewById(R.id.text_list);
                final String str = title.getTag().toString().replace(".json", "");
                if (isInteger(str)) {
                    if (haveNetworkConnection()) {
                        final File[] csvFile = {new File(MainActivity.getContext().getFilesDir() +
                                "/track/" + str + ".csv")};

                        String userId = mSessionManager.getUser().getHashedEmail();

                        Requests.getTrackInfo(sContext, userId, Integer.parseInt(str),
                            new VolleyRequestListener<JSONObject>() {
                                @Override
                                public void onResult(JSONObject trackInfo) {
                                    JSONObject trackObject = trackInfo;
                                    boolean isTrackPublic = false;

                                    try {
                                        isTrackPublic = trackInfo.getBoolean("is_public");
                                        MainActivity.mName[0] = trackObject.getString("name");
                                        if (!csvFile[0].exists()) {
                                            trackObject.getString("trackFilecsv");

                                            if (trackObject.getString("trackFilecsv") != "null") {

                                                byte[] decodedByte = android.util.Base64.decode(
                                                        trackObject.getString("trackFilecsv"), 0);

                                                try {
                                                    File file2 = new File(
                                                            MainActivity.getContext().getFilesDir()
                                                                    + "/track/" + str + ".csv");
                                                    FileOutputStream os =
                                                            new FileOutputStream(file2, true);
                                                    os.write(decodedByte);
                                                    os.close();
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }

                                        MainActivity.mEncodedKmlFile =
                                                trackObject.getString("route_kml");

                                        MainActivity.mDelete =
                                                MainActivity.getContext().getFilesDir() +
                                                        "/track/" + str + ".csv";
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    Intent c = new Intent(TracksActivity.this, StatsActivity.class);
                                    c.putExtra(EXTRA_MESSAGE, str);
                                    c.putExtra(EXTRA, "no");
                                    c.putExtra(EXTRA_IS_PUBLIC_TRACKS, false);
                                    c.putExtra(EXTRA_IS_TRACK_PUBLIC, isTrackPublic);
                                    TracksActivity.this.startActivity(c);
                                    FrameLayout progressBar =
                                            (FrameLayout) findViewById(R.id.progress);
                                    progressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onError(JSONObject error) {
                                    String start = getString(R.string.error_activity_stats);
                                    Utils.showToastAtTop(sContext, start);

                                    FrameLayout progressBar =
                                            (FrameLayout) findViewById(R.id.progress);
                                    progressBar.setVisibility(View.GONE);
                                }
                            }, TAG_TRACK_INFO_REQUEST);
                    } else {
                        Utils.showToastAtTop(sContext, getString(R.string.network_error));
                        progressBar.setVisibility(View.GONE);
                    }

                } else {
                    Utils.showToastAtTop(sContext, getResources().getString(R.string.track_cloud));
                    Intent c = new Intent(TracksActivity.this, StatsActivity.class);
                    c.putExtra(EXTRA_MESSAGE, str);
                    c.putExtra(EXTRA, "no");
                    TracksActivity.this.startActivity(c);
                    progressBar = (FrameLayout) findViewById(R.id.progress);
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        if (mCheck == 2) {
            findViewById(R.id.btn2).setBackgroundResource(R.drawable.cliced);
            findViewById(R.id.btn1).setBackgroundResource(R.drawable.top);
            mViewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

            mSlideInLeft = AnimationUtils.loadAnimation(this,
                    android.R.anim.slide_in_left);
            mSlideOutRight = AnimationUtils.loadAnimation(this,
                    android.R.anim.slide_out_right);

            mViewSwitcher.setInAnimation(mSlideInLeft);
            mViewSwitcher.setOutAnimation(mSlideOutRight);
            mViewSwitcher.showNext();
        } else {
            findViewById(R.id.btn1).setBackgroundResource(R.drawable.cliced);
            findViewById(R.id.btn2).setBackgroundResource(R.drawable.top);
        }
    }

    /**
     * Get Json array from file
     * @param filename file name where json array is located in
     * @return json array
     */
    public static JSONArray getJSONArrays(String filename) {
        String jsonData = "[";
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null) {
                jsonData += line + "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }
        jsonData += "]";
        JSONArray objj = null;
        try {
            objj = new JSONArray(jsonData);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return objj;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (MainActivity.mRef == 2) {
            MainActivity.mRef = 1;
            final FrameLayout progressBar = (FrameLayout) findViewById(R.id.progress);
            progressBar.setVisibility(View.VISIBLE);

            mPrivateAdapter.clear();

            mDistance = new ArrayList<>();
            mTime = new ArrayList<>();
            mNames = new ArrayList<>();
            mID = new ArrayList<>();
            mPublicDistance = new ArrayList<>();
            mPublicTime = new ArrayList<>();
            mPublicNames = new ArrayList<>();
            mPublicId = new ArrayList<>();

            mPrivateAdapter = null;
            mPrivateAdapter = new
                    CustomTrackListAdapter(TracksActivity.this, mNames, mDistance, mTime, mID);

            mPublicAdapter = null;
            mPublicAdapter = new
                    CustomTrackListAdapter
                    (TracksActivity.this, mPublicNames, mPublicDistance, mPublicTime, mPublicId);

            mDrawerList = (ListView) findViewById(R.id.mylistView);
            mDrawerList.setDivider(null);
            mDrawerList.setDividerHeight(0);
            mDrawerList.setAdapter(mPrivateAdapter);

            updateAdapter();
        }
    }

    /**
     * Update list of routes
     * @param array route
     * @param server if route is from server or no
     * @param mPublic if route is public
     */
    public void postJsonArray(JSONArray array, boolean server, boolean mPublic) {
        try {
            for (int j = 0; j < array.length(); j++) {
                array.get(j);
                JSONObject obj = new JSONObject(array.get(j).toString());

                String Name = obj.getString("name");
                if (Name.equals("")) {
                    Name = "No name";
                }

                if (server) {
                    if (mPublic) {
                        mPublicId.add(obj.getString("trackId"));
                    } else {
                        mID.add(obj.getString("trackId"));
                    }

                } else {
                    mID.add(mTrackFile.getName());
                }

                if (mPublic) {
                    mPublicNames.add(Name);
                } else {
                    mNames.add(Name);
                }

                if (server) {
                    if (mPublic) {
                        float f = Float.parseFloat(obj.getString("distance"));
                        mPublicDistance.add(String.format("%.02f", f));
                        f = Float.parseFloat(obj.getString("duration"));
                        mPublicTime.add(String.format("%.02f", f));

                    } else {
                        float f = Float.parseFloat(obj.getString("distance"));
                        mDistance.add(String.format("%.02f", f));

                        f = Float.parseFloat(obj.getString("duration"));
                        mTime.add(String.format("%.02f", f));
                    }
                } else {
                    mDistance.add(obj.getString("distance"));
                    mTime.add(obj.getString("duration"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Switch view to my tracks
     */
    public void changeMyTrack(View v) {
        if (mCheck == 1) {
            mViewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

            mSlideInLeft = AnimationUtils.loadAnimation(this,
                    android.R.anim.slide_in_left);
            mSlideOutRight = AnimationUtils.loadAnimation(this,
                    android.R.anim.slide_out_right);

            mViewSwitcher.setInAnimation(mSlideInLeft);
            mViewSwitcher.setOutAnimation(mSlideOutRight);
            mViewSwitcher.showNext();
            findViewById(R.id.btn2).setBackgroundResource(R.drawable.cliced);
            findViewById(R.id.btn1).setBackgroundResource(R.drawable.top);
            mCheck = 2;
        }
    }

    /**
     * Switch view to public tracks
     */
    public void changeTrack(View v) {
        if (mCheck == 2) {
            mViewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

            mSlideInLeft = AnimationUtils.loadAnimation(this,
                    android.R.anim.slide_in_left);
            mSlideOutRight = AnimationUtils.loadAnimation(this,
                    android.R.anim.slide_out_right);

            mViewSwitcher.setInAnimation(mSlideInLeft);
            mViewSwitcher.setOutAnimation(mSlideOutRight);
            mViewSwitcher.showNext();
            findViewById(R.id.btn1).setBackgroundResource(R.drawable.cliced);
            findViewById(R.id.btn2).setBackgroundResource(R.drawable.top);
            mCheck = 1;
        }
    }

    View.OnClickListener myhandler1 = new View.OnClickListener() {
        public void onClick(View v) {
            // it was the 1st button
        }
    };


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {}
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RequestQueueSingleton.getInstance(sContext).cancelAllPendingRequests(TAG_TRACK_INFO_REQUEST);
        RequestQueueSingleton.getInstance(sContext).cancelAllPendingRequests(TAG_TRACK_LIST_REQUEST);
        MainActivity.sCanUploadTracks = 1;
        MainActivity.mRef = 1;
    }

    /**
     * Check if string is int
     * @param input string
     * @return if int or no
     */
    public boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Updates route adapter
     */
    private void updateAdapter() {
        final FrameLayout progressBar = (FrameLayout) findViewById(R.id.progress);
        String userId = mSessionManager.getUser().getHashedEmail();

        File dir = new File(MainActivity.getContext().getFilesDir() + "/info/");
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
        } else {
            for (int i = 0; i < files.length; i++) {
                mTrackFile = files[i];
                JSONArray array = getJSONArrays(MainActivity.getContext().getFilesDir() + "/info/" +
                        mTrackFile.getName());

                postJsonArray(array, false, false);
            }
        }
        if (haveNetworkConnection()) {
            // Gets public tracks
            Requests.getUsersTracks(this, userId, false, new VolleyRequestListener<JSONObject>
                    () {
                @Override
                public void onResult(JSONObject response) {
                    int rc = Utils.getResponseCode(response);

                    if (rc != 0) {
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    JSONArray trackList;

                    try {
                        trackList = (JSONArray) response.get("trackList");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }

                    if (trackList == null) {
                        Utils.showToastAtTop(sContext, getString(R.string.error_activity_stats));
                    }
                    postJsonArray(trackList, true, true);
                    mPublicAdapter.notifyDataSetChanged();
                    if (mDownload == 0 && mErrors == 1) {
                        progressBar.setVisibility(View.GONE);
                        Utils.showToastAtTop(sContext, getString(R.string.error_activity_stats));
                        mErrors = 0;
                        mDownload = 0;
                    } else if (mDownload == 0) {
                        mDownload = 1;
                    } else {
                        progressBar.setVisibility(View.GONE);
                        mDownload = 0;
                        mDownload = 0;
                    }
                }

                @Override
                public void onError(JSONObject error) {
                    if (mDownload == 1 && mErrors == 0) {
                        progressBar.setVisibility(View.GONE);
                        Utils.showToastAtTop(sContext, getString(R.string.error_activity_stats));
                        mErrors = 0;
                        mDownload = 0;
                    } else if (mErrors == 0) {
                        mErrors = 1;
                    } else {
                        Utils.showToastAtTop(sContext, getString(R.string.error_activity_stats));
                        mErrors = 0;
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }, TAG_TRACK_LIST_REQUEST);

            // Gets user tracks
            Requests.getUsersTracks(this, userId, true, new VolleyRequestListener<JSONObject>
                    () {
                @Override
                public void onResult(JSONObject response) {
                    progressBar.setVisibility(View.GONE);
                    JSONArray trackList;

                    try {
                        trackList = (JSONArray) response.get("trackList");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }

                    postJsonArray(trackList, true, false);

                    mPrivateAdapter.addAll();
                    mPrivateAdapter.notifyDataSetChanged();
                    mAdapter = mPrivateAdapter;

                    if (mDownload == 0 && mErrors == 1) {
                        progressBar.setVisibility(View.GONE);
                        Utils.showToastAtTop(sContext, getString(R.string.error_activity_stats));
                        mErrors = 0;
                        mDownload = 0;
                    } else if (mDownload == 0) {
                        mDownload = 1;
                    } else {
                        mErrors = 0;
                        mDownload = 0;
                    }
                }

                @Override
                public void onError(JSONObject error) {
                    if (mDownload == 1 && mErrors == 0) {
                        progressBar.setVisibility(View.GONE);
                        Utils.showToastAtTop(sContext, getString(R.string.error_activity_stats));
                        mErrors = 0;
                        mDownload = 0;
                    } else if (mErrors == 0) {
                        mErrors = 1;
                    } else {
                        Utils.showToastAtTop(sContext, getString(R.string.error_activity_stats));
                        mErrors = 0;
                        mDownload = 0;
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }, TAG_TRACK_LIST_REQUEST);
        } else {
            Utils.showToastAtTop(sContext, getString(R.string.network_error));
            progressBar.setVisibility(View.GONE);
        }

    }

    /**
     * Checks if user has internet connection
     * @return if internet is connected
     */
    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}