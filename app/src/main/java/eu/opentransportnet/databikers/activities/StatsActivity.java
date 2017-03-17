package eu.opentransportnet.databikers.activities;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import eu.opentransportnet.databikers.R;
import eu.opentransportnet.databikers.interfaces.VolleyRequestListener;
import eu.opentransportnet.databikers.listeners.SlideMenuClickListener;
import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.network.RequestQueueSingleton;
import eu.opentransportnet.databikers.network.Requests;
import eu.opentransportnet.databikers.utils.Const;
import eu.opentransportnet.databikers.utils.SessionManager;
import eu.opentransportnet.databikers.utils.Utils;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.library.routerecorder.MainLocation;
import com.library.routerecorder.RouteRecorder;
import com.opencsv.CSVReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Activity that answers about Track information displaying.
 *
 * @author Ilmars Svilsts
 */

public class StatsActivity extends BaseActivity implements View.OnClickListener {

    public static String mTrackId = "";
    public String mEncodedKmlFile = "";
    
    private static Context sContext;

    private SessionManager mSessionManager = null;
    private RouteRecorder mRr;
    private MainLocation mMainLocation = null;
    private WebView mWebView;
    private LinearLayout mMainlayout;
    private LinearLayout mRootlayout;
    private boolean mChangedtext = false;
    private String mDate = "";
    private String mName = "";
    private String mDist = "";
    private String mLon = "";
    private String mLat = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Intent intent = getIntent();
        String messageTrackId = intent.getStringExtra(SlideMenuClickListener.EXTRA_MESSAGE);
        String trackRorW = intent.getStringExtra(SlideMenuClickListener.EXTRA);

        Bundle extras = intent.getExtras();
        boolean isFromPublicTracks = extras.getBoolean(TracksActivity.EXTRA_IS_PUBLIC_TRACKS);
        boolean trackIsPublic = extras.getBoolean(TracksActivity.EXTRA_IS_TRACK_PUBLIC);

        if(isFromPublicTracks){
            findViewById(R.id.layout_is_public).setVisibility(View.GONE);
        }

        mTrackId = messageTrackId;
        sContext = this;
        mWebView = (WebView) findViewById(R.id.webView);
        mSessionManager = new SessionManager(this);
        mRr = new RouteRecorder(this);

        mMainLocation = new MainLocation(mRr, this);
        mRr.reqLocUpdates(mMainLocation);

        // Enable Javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Sets whether JavaScript running in the sContext of a file scheme
        // URL should be allowed to access content from any origin
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        // Force links and redirects to open in the WebView instead of in a browser
        mWebView.setWebChromeClient(new WebChromeClient() {
            //Enable console.log() from JavaScript
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("StatsActivity.java", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        });
        mWebView.addJavascriptInterface(this, "Activity");

        //Sets title
        TextView title = (TextView) findViewById(R.id.title);
        title.setText(R.string.title_my_track);

        // Attaching the layout to the toolbar object
        Toolbar mToolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(mToolbar);

        //Initializing back button
        ImageButton back = (ImageButton) findViewById(R.id.back_button);
        back.setOnClickListener(this);

        float totalDistance = (float) 0.0;
        final SimpleDateFormat dateFormaterMs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
        final SimpleDateFormat dateFormaterCs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startDate = dateFormaterMs.format(new Date());
        String endDate = dateFormaterMs.format(new Date());
        String dataCSV = MainActivity.getContext().getFilesDir() + "/track/" + mTrackId + ".csv";
        if (!trackRorW.equals("ok")) {
            //Private track 
            if (!isFromPublicTracks) {
                //Show edit pencil.
                ImageButton editButton = (ImageButton) findViewById(R.id.edit_Button);
                editButton.setVisibility(View.VISIBLE);
                editButton.setOnClickListener(this);
                //Show delete button.
                ImageButton deleteButton = (ImageButton) findViewById(R.id.delete_button);
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(this);
                //Needs to be gone so title is in middle.
                ImageButton closeButton = (ImageButton) findViewById(R.id.closeButton);
                closeButton.setVisibility(View.GONE);
            } 
            //Public track
            else {
                //Hide pencil icon.
                ImageView pen = (ImageView) findViewById(R.id.pen_image);
                pen.setVisibility(View.GONE);
                //Switch to show text(from edit text).
                ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.edit_text);
                switcher.showNext();
                //Needs to be invisible so title is in middle.
                ImageButton closeButton = (ImageButton) findViewById(R.id.closeButton);
                closeButton.setVisibility(View.INVISIBLE);
            }


            final String[] Name = {"No name"};
            if (!MainActivity.mName[0].equals("")) {
                Name[0] = MainActivity.mName[0];
            }

            String trackJson="/info/" + mTrackId + ".json";
            File jsonFile = 
                    new File(MainActivity.getContext().getFilesDir() + trackJson);

            if (jsonFile.exists()) {

                JSONObject obj =
                        getJSONObject(MainActivity.getContext().getFilesDir() + trackJson);
                try {
                    Name[0] = obj.getString("name");
                    trackIsPublic = obj.getBoolean("is_public");
                    if (Name[0].equals("")) {
                        Name[0] = "No name";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mEncodedKmlFile = "";
            }

            TextView name = (TextView) findViewById(R.id.edit_name_textfield);
            name.setText(Name[0]);
            name.addTextChangedListener(textWatcher);

            TextView names = (TextView) findViewById(R.id.name);
            names.setText(Name[0]);

            FrameLayout save = (FrameLayout) findViewById(R.id.save);
            save.setVisibility(FrameLayout.GONE);

            ((Switch) findViewById(R.id.is_public)).setChecked(trackIsPublic);
        }


        DataPoint[] elevationPoints = null;
        DataPoint[] qualityPoints = null;
        int minElevation = 0;
        int maxElevation = 0;
        int avgElevation = 0;
        int startingElevation = 0;
        int endingElevation = 0;
        float maxSpeed = 0;
        float climbingDistance = 0;
        float descendingDistance = 0;
        float prew = -1;

        List<List<String>> pathList = new ArrayList<List<String>>();
        File csvFile = new File(dataCSV);
        try {
            if (csvFile.exists()) {
                int counter = 0;
                try {
                    CSVReader reader = new CSVReader(new FileReader(dataCSV), ';');
                    String[] nextLine;
                    while ((nextLine = reader.readNext()) != null) {
                        if (counter == 1) {

                            startDate = nextLine[6];
                        } else if (!(counter == 0)) {
                            mLat = nextLine[0];
                            mLon = nextLine[1];
                            pathList.add(Arrays.asList(nextLine[2], nextLine[3], nextLine[4],
                                    nextLine[5], nextLine[6]));
                        }
                        counter++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //The dynamic array would be empty
                }

                counter = counter - 2;
                if (counter == 0) counter = 1;

                qualityPoints = new DataPoint[counter + 1];
                elevationPoints = new DataPoint[counter + 1];

                qualityPoints[0] = new DataPoint(0, -5);
                elevationPoints[0] = new DataPoint(0, 0);
                for (int i = 0; i < pathList.size(); i++) {
                    endDate = pathList.get(i).get(4);

                    totalDistance += Float.parseFloat(pathList.get(i).get(2));
                    float x, y;
                    float quality;

                    x = Float.parseFloat(pathList.get(i).get(0));
                    y = Float.parseFloat(pathList.get(i).get(1));

                    if (x > prew) {
                        climbingDistance += Float.parseFloat(pathList.get(i).get(2));
                    }
                    if (x < prew) {
                        descendingDistance += Float.parseFloat(pathList.get(i).get(2));
                    }
                    prew = x;
                    quality = y;

                    if (quality > 14) quality = (float) 14.0;

                    qualityPoints[i + 1] = new DataPoint(totalDistance / 1000, quality - 5);
                    elevationPoints[i + 1] = new DataPoint(totalDistance / 1000, x);

                    if (maxElevation < Integer.valueOf(pathList.get(i).get(0))) {
                        maxElevation = Integer.valueOf(pathList.get(i).get(0));
                    }
                    if (minElevation > Integer.valueOf(pathList.get(i).get(0))) {
                        minElevation = Integer.valueOf(pathList.get(i).get(0));
                    }

                    avgElevation += Integer.valueOf(pathList.get(i).get(0));
                    if (i == 0) {
                        startingElevation = Integer.valueOf(pathList.get(i).get(0));
                    }
                    endingElevation = Integer.valueOf(pathList.get(i).get(0));

                    if (maxSpeed < Float.valueOf(pathList.get(i).get(3))) {
                        maxSpeed = Float.valueOf(pathList.get(i).get(3));
                    }
                }
                avgElevation = avgElevation / counter;
            } else {
                qualityPoints = new DataPoint[1];
                qualityPoints[0] = new DataPoint(0, -5);
                elevationPoints = new DataPoint[1];
                elevationPoints[0] = new DataPoint(0, 0);
            }
        } catch (Exception e) {
            qualityPoints = new DataPoint[1];
            qualityPoints[0] = new DataPoint(0, -5);
            elevationPoints = new DataPoint[1];
            elevationPoints[0] = new DataPoint(0, 0);
        }
        
        /**
         * Initializing Elevation graph.
         */
        GraphView graph = (GraphView) findViewById(R.id.elevationGraph);

        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(elevationPoints);
        graph.addSeries(series);

        int x = (int) minElevation;
        // set manual X bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(x);
        graph.getViewport().setMaxY(Math.ceil(maxElevation));

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(Math.ceil(totalDistance / 1000));

        graph.getGridLabelRenderer().setLabelVerticalWidth(70);

        // enable scaling
        graph.getViewport().setScalable(true);

        // titles
        String start = getString(R.string.elevationprofile_activity_stats);
        graph.setTitle(start);
        start = getString(R.string.elevationm_activity_stats);
        graph.getGridLabelRenderer().setVerticalAxisTitle(start);
        start = getString(R.string.distancekm_activity_stats);
        graph.getGridLabelRenderer().setHorizontalAxisTitle(start);

        /**
         * Initializing Road quality graph.
         */

        graph = (GraphView) findViewById(R.id.qualityGraph);

        Date date1 = null;
        Date date2 = null;

        try {
            date1 = (Date) dateFormaterMs.parse(startDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            date2 = (Date) dateFormaterMs.parse(endDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //in milliseconds
        long diff = date2.getTime() - date1.getTime();

        
        float avgSpeed = (float) (((totalDistance) / (diff / 1000)) * 3.6);

        if (totalDistance < 0) {
            totalDistance = 0;
        }

        startDate = dateFormaterCs.format(date1);
        endDate = dateFormaterCs.format(date2);


        TextView text = (TextView) findViewById(R.id.tDistance);
        text.setText(String.format("%.02f", totalDistance / 1000) + " km");
        text = (TextView) findViewById(R.id.totalDistance);
        text.setText(String.format("%.02f", totalDistance / 1000) + " km");
        text = (TextView) findViewById(R.id.totalTime);
        text.setText(String.valueOf(getHeaderDate(diff)));
        mDate = String.valueOf(getDat(diff));
        mDist = String.valueOf(totalDistance / 1000);
        text = (TextView) findViewById(R.id.tTime);
        text.setText(String.valueOf(getHeaderDate(diff)));
        text = (TextView) findViewById(R.id.startTime);
        text.setText(startDate);
        text = (TextView) findViewById(R.id.endTime);
        text.setText(endDate);
        text = (TextView) findViewById(R.id.minElevation);
        text.setText(minElevation + " m");
        text = (TextView) findViewById(R.id.maxElevation);
        text.setText(maxElevation + " m");
        text = (TextView) findViewById(R.id.avgElevation);
        text.setText(avgElevation + " m");
        text = (TextView) findViewById(R.id.startingElevation);
        text.setText(startingElevation + " m");
        text = (TextView) findViewById(R.id.endingElevation);
        text.setText(endingElevation + " m");
        text = (TextView) findViewById(R.id.maxSpeed);
        text.setText(maxSpeed + " km/h");
        text = (TextView) findViewById(R.id.avgspeed);
        if (Float.isNaN(avgSpeed)) {
            text.setText("0.0" + " km/h");
        } else if (Math.round(avgSpeed) > maxSpeed) {
            text.setText(maxSpeed + " km/h");
        } else {
            text.setText(Math.round(avgSpeed) + " km/h");
        }
        text = (TextView) findViewById(R.id.climbingDistance);
        text.setText(String.format("%.02f", climbingDistance / 1000) + " km");
        text = (TextView) findViewById(R.id.descendingDistance);
        text.setText(String.format("%.02f", descendingDistance / 1000) + " km");


        series = new LineGraphSeries<DataPoint>(qualityPoints);

        graph.addSeries(series);

        // set manual X bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-5);
        graph.getViewport().setMaxY(9);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(Math.ceil(totalDistance / 1000));

        // enable scaling
        graph.getViewport().setScalable(true);

        // titles
        graph.getGridLabelRenderer().setVerticalAxisTitle(" ");


        String distanceKm = getString(R.string.distancekm_activity_stats);
        graph.getGridLabelRenderer().setHorizontalAxisTitle(distanceKm);

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);

        String good = getString(R.string.good_activity_stats);
        String average = getString(R.string.average_activity_stats);
        String poor = getString(R.string.poor_activity_stats);

        staticLabelsFormatter.setVerticalLabels(new String[]{good, average, poor});

        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

        if (MainActivity.mEncodedKmlFile != "") {
            mEncodedKmlFile = MainActivity.mEncodedKmlFile;
            MainActivity.mEncodedKmlFile = "";
        }
        mWebView.loadUrl("file:///android_asset/www/track.html");
    }


    @JavascriptInterface
    public String getTrackKmlFile() {
        File kmlFile;
        if (mEncodedKmlFile == "") {
            // Gets local file
            kmlFile = new File(sContext.getFilesDir() + "/track/" + mTrackId + ".kml");
        } else {
            // Decode file
            byte[] decodedBytes = Base64.decode(mEncodedKmlFile, Base64.DEFAULT);
            kmlFile = new File(this.getCacheDir(), "trackKmlFile.kml");
            try {
                BufferedOutputStream writer =
                        new BufferedOutputStream(new FileOutputStream(kmlFile));
                writer.write(decodedBytes);
                writer.flush();
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "file://" + kmlFile.getAbsolutePath();
    }

    @JavascriptInterface
    public String getIconNames() {
        return MainActivity.getRouteRecorder().getIconNames();
    }

    @JavascriptInterface
    public double getLongitude() {
        return mRr.getLongitude();
    }

    @JavascriptInterface
    public double getLatitude() {
        return mRr.getLatitude();
    }

    /**
     * Return total time for route
     * @param millis total time in milli seconds
     * @return returns total time for route
     */
    public static String getHeaderDate(long millis) {
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        String min = String.format("%02d", minute);
        String time = hour + "h " + min + " min";
        if (Integer.parseInt(min) < 1) {
            time = hour + "h 01min";
        }
        return time;
    }

    /**
     * Total route time for needed format to save in file
     * @param millis total time in milli seconds
     * @return returns total time for route
     */
    public static String getDat(long millis) {
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        String min = String.format("%02d", minute);
        String time = hour + "." + min;

        if (Integer.parseInt(min) < 1) {
            time = hour + ".01";
        }
        return time;
    }

    /**
     * Getting JSON object from file
     * @param filename file location
     * @return json object from file
     */
    public static JSONObject getJSONObject(String filename) {
        String jsonData = "";
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
        JSONObject objj = null;
        try {
            objj = new JSONObject(jsonData);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return objj;
    }


    /**
     * Shows distance block in Track information
     */
    public void showDistance(View v) {
        ImageView image = (ImageView) findViewById(R.id.time_speed_image);
        image.setImageResource(R.drawable.plus);
        image = (ImageView) findViewById(R.id.elevation_image);
        image.setImageResource(R.drawable.plus);
        image = (ImageView) findViewById(R.id.road_quality_image);
        image.setImageResource(R.drawable.plus);
        mRootlayout = (LinearLayout) findViewById(R.id.Time);
        mRootlayout.setVisibility(LinearLayout.GONE);
        mRootlayout = (LinearLayout) findViewById(R.id.Elevation);
        mRootlayout.setVisibility(LinearLayout.GONE);
        mRootlayout = (LinearLayout) findViewById(R.id.Quality);
        mRootlayout.setVisibility(LinearLayout.GONE);
        mMainlayout = (LinearLayout) findViewById(R.id.Distance);
        if (mMainlayout.getVisibility() == View.VISIBLE) {
            image = (ImageView) findViewById(R.id.distance_image);
            image.setImageResource(R.drawable.plus);
            mMainlayout.setVisibility(LinearLayout.GONE);
        } else {
            image = (ImageView) findViewById(R.id.distance_image);
            image.setImageResource(R.drawable.minus);
            mMainlayout.setVisibility(LinearLayout.VISIBLE);
        }
        sendScroll();
    }

    /**
     * Shows time and speed block in Track information
     */
    public void showTime(View v) {
        ImageView image = (ImageView) findViewById(R.id.distance_image);
        image.setImageResource(R.drawable.plus);
        image = (ImageView) findViewById(R.id.elevation_image);
        image.setImageResource(R.drawable.plus);
        image = (ImageView) findViewById(R.id.road_quality_image);
        image.setImageResource(R.drawable.plus);
        mRootlayout = (LinearLayout) findViewById(R.id.Time);
        if (mRootlayout.getVisibility() == View.VISIBLE) {
            image = (ImageView) findViewById(R.id.time_speed_image);
            image.setImageResource(R.drawable.plus);
            mRootlayout.setVisibility(LinearLayout.GONE);
        } else {
            image = (ImageView) findViewById(R.id.time_speed_image);
            image.setImageResource(R.drawable.minus);
            mRootlayout.setVisibility(LinearLayout.VISIBLE);
        }
        mRootlayout = (LinearLayout) findViewById(R.id.Elevation);
        mRootlayout.setVisibility(LinearLayout.GONE);
        mRootlayout = (LinearLayout) findViewById(R.id.Quality);
        mRootlayout.setVisibility(LinearLayout.GONE);
        mMainlayout = (LinearLayout) findViewById(R.id.Distance);
        mMainlayout.setVisibility(LinearLayout.GONE);
        sendScroll();
    }

    /**
     * Shows elevation block in Track information
     */
    public void showElevation(View v) {
        ImageView image = (ImageView) findViewById(R.id.distance_image);
        image.setImageResource(R.drawable.plus);
        image = (ImageView) findViewById(R.id.time_speed_image);
        image.setImageResource(R.drawable.plus);
        image = (ImageView) findViewById(R.id.road_quality_image);
        image.setImageResource(R.drawable.plus);
        mRootlayout = (LinearLayout) findViewById(R.id.Time);
        mRootlayout.setVisibility(LinearLayout.GONE);
        mRootlayout = (LinearLayout) findViewById(R.id.Elevation);
        if (mRootlayout.getVisibility() == View.VISIBLE) {
            image = (ImageView) findViewById(R.id.elevation_image);
            image.setImageResource(R.drawable.plus);
            mRootlayout.setVisibility(LinearLayout.GONE);
        } else {
            image = (ImageView) findViewById(R.id.elevation_image);
            image.setImageResource(R.drawable.minus);
            mRootlayout.setVisibility(LinearLayout.VISIBLE);
        }
        mRootlayout = (LinearLayout) findViewById(R.id.Quality);
        mRootlayout.setVisibility(LinearLayout.GONE);
        mMainlayout = (LinearLayout) findViewById(R.id.Distance);
        mMainlayout.setVisibility(LinearLayout.GONE);
        sendScroll();
    }

    /**
     * Shows road quality block in Track information
     */
    public void showQuality(View v) {
        ImageView image = (ImageView) findViewById(R.id.distance_image);
        image.setImageResource(R.drawable.plus);
        image = (ImageView) findViewById(R.id.time_speed_image);
        image.setImageResource(R.drawable.plus);
        image = (ImageView) findViewById(R.id.elevation_image);
        image.setImageResource(R.drawable.plus);
        mRootlayout = (LinearLayout) findViewById(R.id.Time);
        mRootlayout.setVisibility(LinearLayout.GONE);
        mRootlayout = (LinearLayout) findViewById(R.id.Elevation);
        mRootlayout.setVisibility(LinearLayout.GONE);
        mRootlayout = (LinearLayout) findViewById(R.id.Quality);
        if (mRootlayout.getVisibility() == View.VISIBLE) {
            image = (ImageView) findViewById(R.id.road_quality_image);
            image.setImageResource(R.drawable.plus);
            mRootlayout.setVisibility(LinearLayout.GONE);
        } else {
            image = (ImageView) findViewById(R.id.road_quality_image);
            image.setImageResource(R.drawable.minus);
            mRootlayout.setVisibility(LinearLayout.VISIBLE);
        }
        mMainlayout = (LinearLayout) findViewById(R.id.Distance);
        mMainlayout.setVisibility(LinearLayout.GONE);
        sendScroll();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        RequestQueueSingleton.getInstance(sContext).cancelAllPendingRequests("edittrack");
        if (mMainLocation != null) {
            mRr.rmLocUpdates(mMainLocation);
        }
        if (MainActivity.mStats > 1) {
            MainActivity.mStats--;
        }
        if (MainActivity.mStats == 1) {
            MainActivity.mEncodedKmlFile = "";
        }
        MainActivity.mName[0] = "";

        if (MainActivity.mDelete != null) {
            File file = new File(MainActivity.mDelete);
            if (file.exists() && !file.isDirectory() && file != null) {
                file.delete();
            }
        }
    }


    private void sendScroll() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ScrollView scroll = (ScrollView) findViewById(R.id.scrollView);
                        scroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }).start();
    }

    /**
     * Saves route data after "save" button click
     */
    public void saveTrack(View v) {
        EditText mEdit = (EditText) findViewById(R.id.edit_name_textfield);
        mName = mEdit.getText().toString();
        String start = getString(R.string.save_button);

        Intent data = new Intent();
        data.putExtra("check", false);
        data.putExtra("name", mName);
        data.putExtra("date", Double.parseDouble(mDate));
        data.putExtra("dist", Double.parseDouble(mDist));
        data.putExtra("lat_start", Double.parseDouble(mLat));
        data.putExtra("lon_start", Double.parseDouble(mLon));
        data.putExtra("is_public", ((Switch) findViewById(R.id.is_public)).isChecked());
        setResult(Activity.RESULT_OK, data);
        System.gc();
        finish();

        start = getString(R.string.saved_activity_stats);
        Utils.showToastAtTop(this, start);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {}
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button:
                if (mChangedtext) {
                    new AlertDialog.Builder(sContext)
                            .setTitle(R.string.warning_activity_stats)
                            .setMessage(R.string.warning_changed)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setNegativeButton(R.string.cancel_activity_stats, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {}
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    finish();
                }
                break;
            case R.id.edit_Button:
                new AlertDialog.Builder(this)
                    .setTitle(R.string.warning_activity_stats)
                    .setMessage(R.string.warning_text_activity_stats)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ProgressBar spinner;
                            ImageButton editButton =
                                    (ImageButton) findViewById(R.id.edit_Button);
                            ImageButton deleteButton =
                                    (ImageButton) findViewById(R.id.delete_button);
                            TextView mName = (TextView) findViewById(R.id.edit_name_textfield);
                            String text = mName.getText().toString();

                            spinner = (ProgressBar) findViewById(R.id.progressBar_toolbar);
                            spinner.setVisibility(View.VISIBLE);
                            editButton.setVisibility(View.GONE);
                            deleteButton.setVisibility(View.GONE);
                            UpdateTrack(text, mTrackId);
                        }
                    })
                    .setNegativeButton(R.string.cancel_activity_stats, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
                break;
            case R.id.delete_button:
                new AlertDialog.Builder(this)
                    .setTitle(R.string.warning_activity_stats)
                    .setMessage(R.string.warning_text_activity_stats)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ProgressBar spinner;
                            ImageButton editButton = (ImageButton) findViewById(R.id.edit_Button);
                            ImageButton deleteButton =
                                    (ImageButton) findViewById(R.id.delete_button);
                            spinner = (ProgressBar) findViewById(R.id.progressBar_toolbar);
                            spinner.setVisibility(View.VISIBLE);
                            deleteButton.setVisibility(View.GONE);
                            editButton.setVisibility(View.GONE);
                            DeleteTrack();
                        }
                    })
                    .setNegativeButton(R.string.cancel_activity_stats, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
                break;
        }
    }

    /**
     * User can change name of his route
     * If user chooses to update info, this method is called
     * @param name new name of route
     * @param id id of route
     */
    public void UpdateTrack(final String name, String id) {
        boolean isTrackPublic = ((Switch) findViewById(R.id.is_public)).isChecked();

        if (isNumeric(id)) {
            org.json.simple.JSONObject objs = new org.json.simple.JSONObject();
            objs.put("trackId", id);
            objs.put("appId", Const.APPLICATION_ID);
            objs.put("userId", Utils.getHashedUserEmail(this));
            objs.put("name", name);
            objs.put("is_public", isTrackPublic);

            String jsonBodyString = ((org.json.simple.JSONObject) objs).toJSONString();
            JSONObject jsonBody = null;
            try {
                jsonBody = new JSONObject(jsonBodyString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jsonBody != null) {
                Requests.sendRequest(this, "http://" + Utils.getHostname() +
                    Utils.getUrlPathStart() + Requests.PATH_UPDATE_TRACK, jsonBody,
                    new VolleyRequestListener<JSONObject>() {
                        @Override
                        public void onResult(JSONObject trackList) {
                            if (trackList != null) {
                                ProgressBar spinner;
                                spinner = (ProgressBar) findViewById(R.id.progressBar_toolbar);

                                spinner.setVisibility(View.GONE);
                                ImageButton editButton =
                                        (ImageButton) findViewById(R.id.edit_Button);
                                editButton.setVisibility(View.VISIBLE);
                                ImageButton deleteButton =
                                        (ImageButton) findViewById(R.id.delete_button);
                                deleteButton.setVisibility(View.VISIBLE);
                                String start = getString(R.string.edit_activity_stats);
                                System.gc();
                                finish();
                                Utils.showToastAtTop(sContext, start);
                            }
                        }

                        @Override
                        public void onError(JSONObject error) {
                            ProgressBar spinner;
                            spinner = (ProgressBar) findViewById(R.id.progressBar_toolbar);
                            ImageButton editButton =
                                    (ImageButton) findViewById(R.id.edit_Button);
                            editButton.setVisibility(View.VISIBLE);
                            ImageButton deleteButton =
                                    (ImageButton) findViewById(R.id.delete_button);
                            deleteButton.setVisibility(View.VISIBLE);
                            spinner.setVisibility(View.GONE);
                            String start = getString(R.string.error_activity_stats);
                            Utils.showToastAtTop(sContext, start);
                        }
                    }, "edittrack");
            }
        } else {
            JSONObject obj =
                    getJSONObject(MainActivity.getContext().getFilesDir() +
                            "/info/" + mTrackId + ".json");
            try {
                obj.put("name", name);
                obj.put("is_public", isTrackPublic);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            writeJSON(obj.toString(), name);
            ProgressBar spinner;
            spinner = (ProgressBar) findViewById(R.id.progressBar_toolbar);
            ImageButton editButton = (ImageButton) findViewById(R.id.edit_Button);
            editButton.setVisibility(View.VISIBLE);
            ImageButton deleteButton = (ImageButton) findViewById(R.id.delete_button);
            deleteButton.setVisibility(View.VISIBLE);

            spinner.setVisibility(View.GONE);
            String start = getString(R.string.edit_activity_stats);
            finish();
            Utils.showToastAtTop(this, start);
        }
        MainActivity.mRef = 2;
    }

    /**
     * Used for updating json route file info
     * @param obj object to be writen in file
     * @param name name of file
     * @return if everything was success
     */
    public boolean writeJSON(String obj, String name) {
        try {
            File report = new File(MainActivity.getContext().getFilesDir() +
                    "/info/" + mTrackId + ".json");
            if (!report.exists()) {
                report.createNewFile();
            }
            FileWriter file = new FileWriter(MainActivity.getContext().getFilesDir() +
                    "/info/" + mTrackId + ".json");
            file.write(obj);
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * User can delete his route
     */
    public void DeleteTrack() {
        if (!isNumeric(mTrackId)) {
            // Delete local files
            File file = new File(this.getFilesDir(),
                    "/" + Const.STORAGE_PATH_INFO + "/" + mTrackId + ".json");
            file.delete();

            file = new File(this.getFilesDir(),
                    "/" + Const.STORAGE_PATH_TRACK + "/" + mTrackId + ".csv");
            file.delete();

            file = new File(this.getFilesDir(),
                    "/" + Const.STORAGE_PATH_TRACK + "/" + mTrackId + ".kml");
            file.delete();
            String start = getString(R.string.delete_activity_stats);
            ProgressBar spinner;
            spinner = (ProgressBar) findViewById(R.id.progressBar_toolbar);
            ImageButton deleteButton = (ImageButton) findViewById(R.id.delete_button);
            deleteButton.setVisibility(View.VISIBLE);
            ImageButton editButton = (ImageButton) findViewById(R.id.edit_Button);
            editButton.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.GONE);
            finish();
            Utils.showToastAtTop(this, start);
        } else {
            org.json.simple.JSONObject objs = new org.json.simple.JSONObject();

            objs.put("trackId", mTrackId);
            objs.put("userId", mSessionManager.getUser().getHashedEmail());
            objs.put("appId", Const.APPLICATION_ID);

            String jsonBodyString = ((org.json.simple.JSONObject) objs).toJSONString();
            JSONObject jsonBody = null;
            try {
                jsonBody = new JSONObject(jsonBodyString);
            } catch (JSONException e) {
                e.printStackTrace();
            }


            Requests.sendRequest(this, "http://" + Utils.getHostname() + Utils.getUrlPathStart()
                + Requests.PATH_DELETE_TRACK, jsonBody, new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject trackList) {
                        if (trackList != null) {
                            String start = getString(R.string.delete_activity_stats);
                            ProgressBar spinner;
                            spinner = (ProgressBar) findViewById(R.id.progressBar_toolbar);
                            ImageButton deleteButton =
                                    (ImageButton) findViewById(R.id.delete_button);
                            deleteButton.setVisibility(View.VISIBLE);
                            spinner.setVisibility(View.GONE);
                            finish();
                            Utils.showToastAtTop(sContext, start);
                        } else {
                            String start = getString(R.string.delete1_activity_stats);
                            ProgressBar spinner;
                            spinner = (ProgressBar) findViewById(R.id.progressBar_toolbar);
                            ImageButton deleteButton =
                                    (ImageButton) findViewById(R.id.delete_button);
                            deleteButton.setVisibility(View.VISIBLE);
                            ImageButton editButton = (ImageButton) findViewById(R.id.edit_Button);
                            editButton.setVisibility(View.VISIBLE);
                            spinner.setVisibility(View.GONE);
                            Utils.showToastAtTop(sContext, start);
                        }
                    }

                    @Override
                    public void onError(JSONObject error) {
                        String start = getString(R.string.error_activity_stats);
                        ProgressBar spinner;
                        spinner = (ProgressBar) findViewById(R.id.progressBar_toolbar);
                        ImageButton deleteButton = (ImageButton) findViewById(R.id.delete_button);
                        deleteButton.setVisibility(View.VISIBLE);
                        ImageButton editButton = (ImageButton) findViewById(R.id.edit_Button);
                        editButton.setVisibility(View.VISIBLE);
                        spinner.setVisibility(View.GONE);
                        finish();
                        Utils.showToastAtTop(sContext, start);
                    }
                }, "edittrack");
        }
        MainActivity.mRef = 2;
    }

    /**
     * Check if string is a number
     * @param str number as string
     * @return if string is a number
     */
    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private TextWatcher textWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {}
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mChangedtext = true;
        }
    };
}


