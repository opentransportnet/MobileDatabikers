package eu.opentransportnet.databikers.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.models.User;
import eu.opentransportnet.databikers.network.Requests;
import eu.opentransportnet.databikers.utils.Const;
import eu.opentransportnet.databikers.utils.SessionManager;
import eu.opentransportnet.databikers.utils.Utils;
import com.library.routerecorder.SaveSensorData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Route problem details activity
 *
 * @author Kristaps Krumins
 */
public class Report3DetailsActivity extends BaseActivity implements View.OnClickListener {

    public static final SimpleDateFormat TIME_FORMAT_FOR_REPORT = new SimpleDateFormat
            ("yyyy-MM-dd'T'HH:mm:ss");
    public static final int GO_BACK_RESULT = 1003;
    public static final String PICTURE_FOLDER_NAME = "OTN";

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final Object REPORT_PREFIX = "Report_";

    private ImageView mPhotoView;
    private String mCurrentPhotoPath = null;
    private int mIssueId;
    private EditText mDescription;
    private boolean mUiLoaded = false;
    private boolean editAndLoadNewPhoto = false;
    private double mLatitude;
    private double mLongitude;
    private int mImageResID;
    private String mProblemText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        Utils.loadLocale(this);
        setContentView(eu.opentransportnet.databikers.R.layout.activity_report_3_details);
        setToolbarTitle(eu.opentransportnet.databikers.R.string.title_report_details);
        setToolbarBackBtn(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(GO_BACK_RESULT);
                        finish();
                    }
                });
        enableLoseFocusInView(findViewById(eu.opentransportnet.databikers.R.id.report_details));

        Intent intent = getIntent();

        mDescription = (EditText) findViewById(eu.opentransportnet.databikers.R.id.description);
        // Sets saved description
        String description = intent.getStringExtra("Description");
        if (description != null) {
            mDescription.setText(description);
        }

        // Sets saved picture path
        String photoPath = intent.getStringExtra("PhotoPath");
        if (photoPath != null) {
            mCurrentPhotoPath = photoPath;
        }

        mLatitude = intent.getDoubleExtra("lat", 200);
        mLongitude = intent.getDoubleExtra("lng", 200);

        mIssueId = intent.getIntExtra("issueID", -1);

        mImageResID = intent.getIntExtra("imageResID", -1);
        ImageView imageView = (ImageView) findViewById(eu.opentransportnet.databikers.R.id.type_image);
        imageView.setImageResource(mImageResID);

        mProblemText = intent.getStringExtra("description");
        TextView problemName = (TextView) findViewById(eu.opentransportnet.databikers.R.id.problem_name);
        problemName.setText(mProblemText);

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
        webView.loadUrl("file:///android_asset/www/report.html");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.loadUrl("javascript:showLoc(" + mLatitude + "," + mLongitude + ")");
            }
        });

        mPhotoView = (ImageView) findViewById(eu.opentransportnet.databikers.R.id.photoView);

        findViewById(eu.opentransportnet.databikers.R.id.send_report).setOnClickListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Intent intent = getIntent();
        intent.putExtra("Description", mDescription.getText().toString());
        intent.putExtra("PhotoPath", mCurrentPhotoPath);

        finish();
        startActivity(intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        mUiLoaded = true;
        if (editAndLoadNewPhoto) {
            editCurrentImage();
            setPic();
            galleryAddPic();
        } else if (mCurrentPhotoPath != null) setPic();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (!mUiLoaded) {
                editAndLoadNewPhoto = true;
            } else {
                editCurrentImage();
                setPic();
                galleryAddPic();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case eu.opentransportnet.databikers.R.id.send_report:
                if (mDescription.getText().length() == 0) {
                    Utils.showToastAtTop(this, getString(eu.opentransportnet.databikers.R.string.no_description));
                    return;
                }
                File directory = new File(MainActivity.getContext().getFilesDir() + "/report");
                if (!directory.exists()) {
                    directory.mkdir();
                }

                Date date = new Date();

                JSONObject reportObj = new JSONObject();
                try {
                    SessionManager sessionManager = new SessionManager(this);
                    User user = sessionManager.getUser();
                    reportObj.put("userId", user.getHashedEmail());
                    reportObj.put("appId", Const.APPLICATION_ID);
                    reportObj.put("issueTypeId", mIssueId);
                    reportObj.put("description", mDescription.getText());
                    reportObj.put("datetime", TIME_FORMAT_FOR_REPORT.format(date));

                    if (mCurrentPhotoPath != null) {
                        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                        reportObj.put("picture", User.encodeToBase64(bitmap));
                    } else {
                        reportObj.put("picture", "");
                    }

                    reportObj.put("latitude", mLatitude);
                    reportObj.put("longitude", mLongitude);
                } catch (JSONException e) {
                    showError();
                    e.printStackTrace();
                }

                boolean requestAdded = Requests.reportIssue(this, reportObj);

                if (requestAdded) {
                    finish();
                    Utils.showToastAtTop(this, getString(eu.opentransportnet.databikers.R.string.report_saved));
                } else {
                    if (saveReportLocally(this, date, reportObj)) {
                        finish();
                        Utils.showToastAtTop(this, getString(eu.opentransportnet.databikers.R.string.report_saved));
                    } else {
                        showError();
                    }
                }

                break;
        }
    }

    public void takePicture(View button) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
        String imageFileName = "Report_" + timeStamp;

        File envPictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File storageDir = new File(envPictures, PICTURE_FOLDER_NAME);
        File storagePictureDir = new File(envPictures.getAbsolutePath());

        if (!storagePictureDir.exists()) {
            storagePictureDir.mkdir();
        }

        if (!storageDir.exists()) {
            storageDir.mkdir();
        }

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mPhotoView.getWidth();
        int targetH = mPhotoView.getHeight();
        if (targetH == 0 || targetW == 0) {
            return;
        }

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mPhotoView.setImageBitmap(bitmap);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void editCurrentImage() {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        if (photoW > photoH && photoW > 1280) {
            bitmap = Bitmap.createScaledBitmap(bitmap, 1280, 720, false);
        } else if (photoH > 1280) {
            bitmap = Bitmap.createScaledBitmap(bitmap, 720, 1280, false);
        } else {
            return;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        try {
            FileOutputStream fo = new FileOutputStream(mCurrentPhotoPath);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError() {
        Utils.showToastAtTop(this, getString(eu.opentransportnet.databikers.R.string.report_not_saved));
    }

    public static boolean saveReportLocally(Context ctx, Date date, JSONObject reportObj) {
        // Save report locally

        // Report file name without extension
        String reportName = REPORT_PREFIX + SaveSensorData.TIME_FORMAT_FOR_FILE_NAME
                .format(date);

        File file = new File(ctx.getFilesDir(),
                "/" + Const.STORAGE_PATH_REPORT + "/" + reportName + ".json");

        if (file.exists()) {
            return true;
        }

        // Creates empty file
        File reportFile = com.library.routerecorder.Utils.createEmptyFile(ctx, "report", reportName,
                ".json");
        if (reportFile != null) {
            try {
                FileWriter fileWriter = new FileWriter(reportFile);

                fileWriter.write(reportObj.toString());
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}
