package eu.opentransportnet.databikers.activities;

import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import eu.opentransportnet.databikers.interfaces.VolleyRequestListener;
import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.models.User;
import eu.opentransportnet.databikers.network.Requests;
import eu.opentransportnet.databikers.utils.SessionManager;
import eu.opentransportnet.databikers.utils.Utils;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import org.json.JSONObject;

/**
 * @author Kristaps Krumins
 */
public class LoginActivity extends BaseActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, View.OnClickListener {
    private static final int SETTINGS_ACTIVITY_REQUEST = 1001;
    private static final int RC_GOOGLE_ACCOUNT_PICKER = 1002;
    private static final int DISCLAIMER_ACTIVITY_REQUEST = 1003;
    private static final int RC_SIGN_IN = 0;
    private static final String TEST_EMAIL = "testuser2@test.com";

    // Google client to communicate with Google
    private GoogleApiClient mGoogleApiClient;
    private boolean mIntentInProgress;
    private ConnectionResult mConnectionResult;
    private boolean mLoginWithGoogle = false;
    private View mProgressBar;
    private SessionManager mSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        Utils.setContext(this);
        Utils.loadLocale(this);
        initUi();
        mSessionManager = new SessionManager(this);

        User user = mSessionManager.getUser();
        if (user.hasEmail()) {
            startMainActivity();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_GOOGLE_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    lockOrientation();
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    mGoogleApiClient = new GoogleApiClient.Builder(this)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .addApi(Plus.API, Plus.PlusOptions.builder().build())
                            .addScope(Plus.SCOPE_PLUS_LOGIN)
                            .setAccountName(accountName)
                            .addScope(new Scope("email")).build();
                    mGoogleApiClient.connect();
                }
                break;
            case RC_SIGN_IN:
                mIntentInProgress = false;
                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect();
                }
                break;
            case SETTINGS_ACTIVITY_REQUEST:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                mProgressBar.setVisibility(View.GONE);
                User user = mSessionManager.getUser();
                if (user.getBikeType() > 0) {
                    startMainActivity();
                }
                break;
            case DISCLAIMER_ACTIVITY_REQUEST:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                user = mSessionManager.getUser();

                if (user.getDisclaimer() == true) {
                    startMainActivity();
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case eu.opentransportnet.databikers.R.id.login_google:
                if (Utils.isConnected(this)) {
                    Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                            new String[]{"com.google"}, true, null, null, null,
                            null);
                    intent.putExtra("overrideTheme", 1);
                    intent.putExtra("overrideCustomTheme", 0);

                    try {
                        startActivityForResult(intent, RC_GOOGLE_ACCOUNT_PICKER);
                    } catch (ActivityNotFoundException e) {
                        Utils.showToastAtTop(this, getString(eu.opentransportnet.databikers.R.string.google_api_unavailable));
                    }
                } else {
                    Utils.showToastAtTop(this, getString(eu.opentransportnet.databikers.R.string.network_unavailable));
                }
                break;
            case eu.opentransportnet.databikers.R.id.login_default:
                if (Utils.isConnected(this)) {
                    mProgressBar.setVisibility(View.VISIBLE);

                    User user = new User(this);
                    user.setFirstName("Test");
                    user.setLastName("User");
                    user.setEmail(TEST_EMAIL);

                    mSessionManager.createLoginSession(user);
                    registerAndLogin(user.getHashedEmail());
                } else {
                    Utils.showToastAtTop(this, getString(eu.opentransportnet.databikers.R.string.network_unavailable));
                }
                break;
        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        getGoogleProfileInfo();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            mProgressBar.setVisibility(View.GONE);
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        if (!mIntentInProgress) {
            // store mConnectionResult
            mConnectionResult = result;
            resolveSignInError();
        } else {
            showDefaultError();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }


    private void resolveSignInError() {
        if (mConnectionResult != null && mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        } else {
            showDefaultError();
        }
    }

    private void getGoogleProfileInfo() {
        try {
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
                Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

                User user = new User(this);
                user.setFirstName(currentPerson.getName().getGivenName());
                user.setLastName(currentPerson.getName().getFamilyName());
                user.setEmail(Plus.AccountApi.getAccountName(mGoogleApiClient));
                if (currentPerson.hasImage()) {
                    String personPhotoUrl = currentPerson.getImage().getUrl();
                    user.setPhotoUrl(personPhotoUrl);
                }

                mSessionManager.createLoginSession(user);

                boolean requestSent = registerAndLogin(user.getHashedEmail());

                if (!requestSent) {
                    showDefaultError();
                }
            } else {
                showDefaultError();
            }
        } catch (Exception e) {
            showDefaultError();
            e.printStackTrace();
        }
    }

    private void lockOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    private void startMainActivity() {
        User user = mSessionManager.getUser();
        int vers = 0;
        try {

            if (getString(eu.opentransportnet.databikers.R.string.svn_version).equals("null")) {
            } else {
                vers = Integer.parseInt(getString(eu.opentransportnet.databikers.R.string.svn_version));
            }
        } catch (Exception e) {
        }

        if (user.getDisclaimer() == false || user.getDisclaimerV() < vers) {
            Intent i = new Intent(this, DisclaimerActivity.class);
            this.startActivityForResult(i, DISCLAIMER_ACTIVITY_REQUEST);
        } else if (user.getBikeType() < 1) {
            Intent i = new Intent(this, SettingsActivity.class);
            this.startActivityForResult(i, SETTINGS_ACTIVITY_REQUEST);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            mLoginWithGoogle = false;
            startActivity(intent);
            // Unlocks orientation
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private boolean registerAndLogin(String email) {
        return Requests.registerUser(getApplicationContext(), email,
                new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject response) {
                        int rc = Utils.getResponseCode(response);

                        if (rc == 0 || rc == 1) {
                            startMainActivity();
                            mProgressBar.setVisibility(View.GONE);
                        } else {
                            mProgressBar.setVisibility(View.GONE);
                            mSessionManager.clearUserSession();
                            Utils.showToastAtTop(getApplicationContext(),
                                    getString(eu.opentransportnet.databikers.R.string.login_error));
                        }
                    }

                    @Override
                    public void onError(JSONObject response) {
                        mProgressBar.setVisibility(View.GONE);
                        mSessionManager.clearUserSession();
                        Utils.showToastAtTop(getApplicationContext(),
                                getString(eu.opentransportnet.databikers.R.string.login_error));
                    }
                });
    }

    private void showDefaultError() {
        if (mLoginWithGoogle) {
            mProgressBar.setVisibility(View.GONE);
            Utils.showToastAtTop(this, getString(eu.opentransportnet.databikers.R.string.something_went_wrong));
        }
    }

    private void initUi() {
        setContentView(eu.opentransportnet.databikers.R.layout.activity_login);

        initLoseFocusInContent();

        mProgressBar = findViewById(eu.opentransportnet.databikers.R.id.loadingPanel);
        mProgressBar.setVisibility(View.GONE);

        findViewById(eu.opentransportnet.databikers.R.id.login_google).setOnClickListener(this);
        findViewById(eu.opentransportnet.databikers.R.id.login_antwerp).setOnClickListener(this);
        findViewById(eu.opentransportnet.databikers.R.id.login_default).setOnClickListener(this);
    }

}