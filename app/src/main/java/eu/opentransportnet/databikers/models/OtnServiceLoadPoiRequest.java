package eu.opentransportnet.databikers.models;

/**
 *
 * @author Kristaps Krumins
 */
public class OtnServiceLoadPoiRequest {

    private int poiId;

    private String userId;

    private int appId;

    public int getPoiId() {
        return poiId;
    }

    public void setPoiId(int poiId) {
        this.poiId = poiId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }
}
