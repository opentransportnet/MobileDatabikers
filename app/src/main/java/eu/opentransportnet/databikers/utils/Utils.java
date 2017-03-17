package eu.opentransportnet.databikers.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 *
 * @author Ilmars Svilsts
 * @author Kristaps Krumins
 */
public class Utils {
    // Should be set on app start
    private static Context mContext;

    public static void showToastAtTop(Context ctx, String message) {
        CharSequence text = message;
        Toast toast = Toast.makeText(ctx, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 110);
        toast.show();
    }

    /**
     * The ConnectivityManager to query the active network and determine if it has Internet
     * connectivity
     *
     * @param ctx The context
     * @return {@code true} if there is Internet connectivity, otherwise {@code false}
     */
    public static boolean isConnected(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    public static void saveLocale(Context ctx, String lang) {
        SharedPreferences langPref = ctx.getSharedPreferences("language", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = langPref.edit();
        editor.putString("languageToLoad", lang);
        editor.commit();
    }

    public static void loadLocale(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("language", Activity.MODE_PRIVATE);
        String language = prefs.getString("languageToLoad", "");
        changeLanguage(ctx, language);
    }


    public static void changeLanguage(Context ctx, String lang) {
        if (lang.equalsIgnoreCase("")) {
            // Use default
            lang = "en";
        }

        Locale locale = new Locale(lang);
        saveLocale(ctx, lang);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = locale;
        ctx.getResources().updateConfiguration(config, ctx.getResources().getDisplayMetrics());
    }

    /**
     * Makes given image in circle form
     *
     * @param bitmap the image
     * @return circle image
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = bitmap.getWidth() / 2;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static void deleteAllLocalFiles(Context ctx) {
        File routesJsonDir = new File(ctx.getFilesDir() + "/" + Const.STORAGE_PATH_INFO);
        File[] routesJson = routesJsonDir.listFiles();

        if (routesJson != null) {
            for (File file : routesJson) {
                file.delete();
            }
        }

        File trackCsvDir = new File(ctx.getFilesDir() + "/" + Const.STORAGE_PATH_TRACK);
        File[] routesCsv = trackCsvDir.listFiles();

        if (routesCsv != null) {
            for (File file : routesCsv) {
                file.delete();
            }
        }

        File reportDir = new File(ctx.getFilesDir() + "/" + Const.STORAGE_PATH_REPORT);
        File[] reports = reportDir.listFiles();

        if (reports != null) {
            for (File file : reports) {
                file.delete();
            }
        }
    }

    public static final String hashMd5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getHashedUserEmail(Context ctx) {
        return new SessionManager(ctx).getUser().getHashedEmail();
    }

    public static String getHostname() {
        if (Config.getInstance(mContext).isRelease()) {
            return Config.getInstance(mContext).getReleaseHostname();
        } else {
            return Config.getInstance(mContext).getDebugHostname();
        }
    }

    public static void logD(String tag, String message) {
        if (Config.getInstance(mContext).logMessages()) {
            Log.d(tag, message);
        }
    }

    public static void setContext(Context ctx){
        mContext = ctx;
    }

    public static boolean isEncryption() {
        return Config.getInstance(mContext).isEncryption();
    }

    public static String getUrlPathStart(){
        if(isEncryption()){
            return "/otnMobileServicesEncrypted";
        } else {
            return "/otnMobileServices";
        }
    }

    public static int getResponseCode(JSONObject response){
        int responseCode = -1;
        try {
            responseCode = response.getInt("responseCode");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return responseCode;
    }
}
