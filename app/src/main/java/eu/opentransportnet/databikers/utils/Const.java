package eu.opentransportnet.databikers.utils;

/**
 *
 * @author Kristaps Krumins
 */
public final class Const {
    public static final int APPLICATION_ID = 10;

    public static final double DEFAULT_LATITUDE = 51.217935;
    public static final double DEFAULT_LONGITUDE = 4.415309;

    public static final String STORAGE_PATH_INFO = "info";
    public static final String STORAGE_PATH_TRACK = "track";
    public static final String STORAGE_PATH_REPORT = "report";

    public static final Integer NOTIFICATION_BASE_FOR_UPLOAD = 1000000;

    public static final String WMS_URL_POIS_ANTWERP = "http://"+Utils.getHostname()+"/cgi-bin/mapserv?map=/data/www/maps/issues_antwerp.map";

    public static final String WMS_URL_ROUTES_ANTWERP = "http://"+Utils.getHostname()+"/cgi-bin/mapserv?map=/data/www/maps/routes_antwerp.map";

}

