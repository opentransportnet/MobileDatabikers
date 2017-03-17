package eu.opentransportnet.databikers.network;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import eu.opentransportnet.databikers.activities.Report3DetailsActivity;
import eu.opentransportnet.databikers.interfaces.VolleyRequestListener;
import eu.opentransportnet.databikers.utils.Const;
import eu.opentransportnet.databikers.utils.OtnCrypto;
import eu.opentransportnet.databikers.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;

/**
 * Uploads data to server
 *
 * @author Kristaps Krumins
 */
public class Requests {
    public static final String PATH_REGISTER_USER = "/platform/users/addUser";
    public static final String PATH_REGISTER_TRACK = "/platform/tracks/addTracks";
    public static final String PATH_REPORT_ISSUE = "/platform/issues/issueReport";
    public static final String PATH_UPDATE_TRACK = "/platform/tracks/updateTrack";
    public static final String PATH_DELETE_TRACK = "/platform/tracks/deleteTrack";
    public static final String PATH_DELETE_USER = "/platform/users/deleteUserContent";
    public static final String PATH_GET_TRACKS = "/platform/tracks/getTracks";
    public static final String PATH_LOAD_TRACK = "/platform/tracks/getTrack";
    public static final String PATH_DELETE_ISSUE = "/platform/issues/deleteIssue";
    public static final String PATH_LOAD_ISSUE = "/platform/issues/loadIssue";


    public static final String TAG_REGISTER_TRACK_REQUEST = "register track request";

    private static final String LOG_TAG = "Requests";

    private static Integer mNotificationId = Const.NOTIFICATION_BASE_FOR_UPLOAD;

    /**
     * Sends JSONObject to server
     *
     * @param ctx            The context
     * @param showErrorToast If true then error toast will be shown when error is detected
     * @param url            The URL for request
     * @param jsonBody       The JSONObject to be sent
     * @param listener       The listener for request response
     * @param tag            The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean sendRequest(final Context ctx, final boolean showErrorToast, String url, JSONObject
            jsonBody, final VolleyRequestListener<JSONObject> listener, String tag) {

        if (!Utils.isConnected(ctx)) {
            Utils.logD(LOG_TAG, "No Internet connectivity");

            if (showErrorToast) {
                Utils.showToastAtTop(ctx, ctx.getString(eu.opentransportnet.databikers.R.string.network_unavailable));
            }

            return false;
        } else if (jsonBody == null) {
            Utils.logD(LOG_TAG, "JSONObject is 'null'");

            if (showErrorToast) {
                Utils.showToastAtTop(ctx, ctx.getString(eu.opentransportnet.databikers.R.string.something_went_wrong));
            }

            return false;
        }

        Utils.logD(LOG_TAG, "Request url:" + url);
        Utils.logD(LOG_TAG, "JSON data:" + jsonBody.toString());

        Request request;

        if (Utils.isEncryption() == false) {
            request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Utils.logD(LOG_TAG, "JSON response:" + response);
                            listener.onResult(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (showErrorToast) {
                                Utils.showToastAtTop(ctx, ctx.getString(eu.opentransportnet.databikers.R.string.something_went_wrong));
                            }

                            if (error != null && error.networkResponse != null) {
                                Utils.logD(LOG_TAG, "Error response. Response code " + error
                                        .networkResponse.statusCode);
                            }

                            listener.onError(null);
                        }
                    });
        } else {
            final String encryptedBody = OtnCrypto.encrypt(jsonBody.toString());
            Utils.logD(LOG_TAG, "encrypted JSON data:" + encryptedBody);

            request = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String encryptedResponse) {
                            String responseString = OtnCrypto.decrypt(encryptedResponse);
                            JSONObject response;

                            try {
                                response = new JSONObject(responseString);
                                Utils.logD(LOG_TAG, "JSON response:" + response);
                                listener.onResult(response);
                                return;
                            } catch (JSONException e) {
                                e.printStackTrace();

                                if (showErrorToast) {
                                    Utils.showToastAtTop(ctx, ctx.getString(eu.opentransportnet.databikers.R.string.something_went_wrong));
                                }

                                Utils.logD(LOG_TAG, "Decrypted message is not in JSON format");
                                listener.onError(null);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (showErrorToast) {
                                Utils.showToastAtTop(ctx, ctx.getString(eu.opentransportnet.databikers.R.string.something_went_wrong));
                            }

                            if (error != null && error.networkResponse != null) {
                                Utils.logD(LOG_TAG, "Error response. Response code " + error
                                        .networkResponse.statusCode);
                            }

                            listener.onError(null);
                        }
                    }) {

                @Override
                public byte[] getBody() throws AuthFailureError {
                    return encryptedBody.getBytes();
                }

                @Override
                public String getBodyContentType() {
                    return "text/plain";
                }
            };
        }

        // Adds request to request queue
        RequestQueueSingleton.getInstance(ctx.getApplicationContext())
                .addToRequestQueue(request, tag);

        return true;
    }

    /**
     * Sends JSONObject to server and does not show error messages (toasts)
     *
     * @param ctx      The context
     * @param url      The URL for request
     * @param jsonBody The JSONObject to be sent
     * @param listener The listener for request response
     * @param tag      The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean sendRequest(Context ctx, String url, JSONObject jsonBody,
                                      final VolleyRequestListener<JSONObject> listener,
                                      String tag) {
        return sendRequest(ctx, false, url, jsonBody, listener, tag);
    }

    public static boolean registerUser(Context ctx, String userEmail,
                                       final VolleyRequestListener<JSONObject> listener) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userEmail);
            jsonBody.put("appId", Const.APPLICATION_ID);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_REGISTER_USER;

        return sendRequest(ctx, url, jsonBody, listener, null);
    }

    /**
     * Uploads track (register track)
     *
     * @param ctx      The context
     * @param jsonBody The JSON body for request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean registerTrack(final Context ctx, JSONObject jsonBody, final String
            fileName) {
        Notification.Builder notificationBuilder = new Notification.Builder(ctx
                .getApplicationContext());
        notificationBuilder.setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setTicker("");
        final Integer notificationID = mNotificationId;
        final NotificationManager mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_REGISTER_TRACK;

        boolean requestSent = sendRequest(ctx, url, jsonBody,
                new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject object) {
                        mNotificationManager.cancel(notificationID);
                        int rc = Utils.getResponseCode(object);

                        if(rc != 0){
                            return;
                        }

                        // Delete local files
                        File file = new File(ctx.getFilesDir(),
                                "/" + Const.STORAGE_PATH_INFO + "/" + fileName + ".json");
                        file.delete();

                        file = new File(ctx.getFilesDir(),
                                "/" + Const.STORAGE_PATH_TRACK + "/" + fileName + ".csv");
                        file.delete();

                        file = new File(ctx.getFilesDir(),
                                "/" + Const.STORAGE_PATH_TRACK + "/" + fileName + ".kml");
                        file.delete();
                    }

                    @Override
                    public void onError(JSONObject object) {
                        mNotificationManager.cancel(notificationID);
                    }
                }, TAG_REGISTER_TRACK_REQUEST);

        if (!requestSent) {
            return false;
        }

        Notification notification = notificationBuilder.build();
        //Send the notification
        mNotificationManager.notify(notificationID, notification);

        return true;
    }

    /**
     * Uploads report issue to server
     * TODO: comment
     *
     * @param ctx      The context
     * @param jsonBody The JSON body for request
     * @param save     On error should we save report locally?
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean reportIssue(final Context ctx, final JSONObject jsonBody,
                                      final boolean save, final String filePath) {
        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_REPORT_ISSUE;

        boolean requestSent = sendRequest(ctx, url, jsonBody,
                new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject object) {
                        int rc = Utils.getResponseCode(object);

                        if (rc != 0) {
                            // Error
                            if (save) {
                                Date date = new Date();
                                Report3DetailsActivity.saveReportLocally(ctx, date, jsonBody);
                            }
                        } else {
                            // Report issue saved on server
                            if (save == false) {
                                File file = new File(filePath);
                                file.delete();
                            }
                        }
                    }

                    @Override
                    public void onError(JSONObject object) {
                        if (save) {
                            Date date = new Date();
                            Report3DetailsActivity.saveReportLocally(ctx, date, jsonBody);
                        }
                    }
                }, null);

        if (!requestSent) {
            return false;
        }

        return true;
    }

    public static boolean reportIssue(final Context ctx, final JSONObject jsonBody) {
        return reportIssue(ctx, jsonBody, true, null);
    }

    /**
     * Downloads track list from server for given user
     *
     * @param ctx          The context
     * @param userEmail    The user email
     * @param onlyMyTracks If true then downloads only users tracks, else public
     * @param listener     The listener for request response
     * @param tag          The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean getUsersTracks(Context ctx, String userEmail, boolean onlyMyTracks,
                                         final VolleyRequestListener<JSONObject> listener,
                                         String tag) {
        String path = PATH_GET_TRACKS;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userEmail);
            jsonBody.put("appId", Const.APPLICATION_ID);
            if (onlyMyTracks) {
                jsonBody.put("isMine", true);
                //jsonBody.put("isPublic", false);
            } else {
                jsonBody.put("isMine", false);
                jsonBody.put("isPublic", true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String loadPtUrl = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + path;

        return Requests.sendRequest(ctx, loadPtUrl, jsonBody, listener, tag);
    }

    /**
     * Downloads track information for given track
     *
     * @param ctx       The context
     * @param userEmail The user email
     * @param trackId   The track ID
     * @param listener  The listener for request response
     * @param tag       The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean getTrackInfo(Context ctx, String userEmail, int trackId,
                                       final VolleyRequestListener<JSONObject> listener,
                                       String tag) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userEmail);
            jsonBody.put("trackId", trackId);
            jsonBody.put("appId", Const.APPLICATION_ID);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String loadPtUrl = "http://" + Utils.getHostname() + Utils.getUrlPathStart() +
                PATH_LOAD_TRACK;

        return Requests.sendRequest(ctx, loadPtUrl, jsonBody, listener, tag);
    }

    /**
     * Delete users reported issue. Default request error toasts enabled.
     *
     * @param ctx      The context
     * @param issueId    The POI ID
     * @param listener The listener for request response
     * @param tag      The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean deleteIssue(Context ctx, int issueId,
                                      final VolleyRequestListener<JSONObject> listener,
                                      String tag) {
        String userEmail = Utils.getHashedUserEmail(ctx);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userEmail);
            jsonBody.put("issueId", issueId);
            jsonBody.put("appId", Const.APPLICATION_ID);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_DELETE_ISSUE;

        return sendRequest(ctx, true, url, jsonBody, listener, tag);
    }

}
