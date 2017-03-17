package eu.opentransportnet.databikers.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import eu.opentransportnet.databikers.activities.Report2RouteActivity;
import eu.opentransportnet.databikers.activities.Report3DetailsActivity;
import eu.opentransportnet.databikers.utils.Classificators;

/**
 * RouteProblemAdapter
 *
 * @author Kristaps Krumins
 */
public class RouteProblemAdapter extends ArrayAdapter {
    private Context mContext;
    private String[] mDescriptions;
    private int mResId;
    private Report2RouteActivity mRp;
    private int mIssueId;
    private double mLat;
    private double mLng;

    /**
     * Sets {@link RouteProblemAdapter#mResId} depending of {@code id}
     *
     * @param context      the context
     * @param descriptions the problem descriptions
     * @param id           the ID of problem type
     */
    public RouteProblemAdapter(Context context, Report2RouteActivity rp, String[] descriptions, int id, double lat, double lng) {
        super(context, -1, descriptions);
        mContext = context;
        mDescriptions = descriptions;

        mResId = getResId(id);
        mIssueId = id;
        mRp = rp;
        mLat = lat;
        mLng = lng;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(eu.opentransportnet.databikers.R.layout.route_problem_item, parent, false);
        TextView textView = (TextView) rowView.findViewById(eu.opentransportnet.databikers.R.id.problem_name);
        textView.setText(mDescriptions[position]);

        ImageView imageView = (ImageView) rowView.findViewById(eu.opentransportnet.databikers.R.id.type_image);
        imageView.setImageResource(mResId);

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, Report3DetailsActivity.class);

                intent.putExtra("issueID", mIssueId + (position * 10) + 10);
                intent.putExtra("imageResID", mResId);
                intent.putExtra("description", mDescriptions[position]);
                intent.putExtra("lat", mLat);
                intent.putExtra("lng", mLng);

                mRp.startNextActivity(intent);
            }
        });
        return rowView;
    }

    /**
     * @param id the problem type id
     * @return the drawable id
     */
    public static int getResId(int id) {
        switch (id) {
            case Classificators.ISSUE_ROAD_CONDITION:
                return eu.opentransportnet.databikers.R.drawable.ic_cycle_crossing;
            case Classificators.ISSUE_ROAD_SIGNALISATION:
                return eu.opentransportnet.databikers.R.drawable.ic_car_cycle_line;
            case Classificators.ISSUE_HINDRANCE_ALONG_THE_ROAD:
                return eu.opentransportnet.databikers.R.drawable.ic_traffic_lights;
            case Classificators.ISSUE_ROAD_WORKS:
                return eu.opentransportnet.databikers.R.drawable.ic_road;
            case Classificators.ISSUE_WITHOUT_CLASSIFICATION:
                return eu.opentransportnet.databikers.R.drawable.ic_warning;
            default:
                return 0;
        }
    }
}
