package eu.opentransportnet.databikers.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import eu.opentransportnet.databikers.adapters.RouteProblemAdapter;
import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.utils.Classificators;

/**
 * RouteProblem activity
 *
 * @author Kristaps Krumins
 */
public class Report2RouteActivity extends BaseActivity {

    public static final int GO_BACK_RESULT = 1003;
    private static final int ROUTE_DETAIL_REQUEST = 1002;
    // ArrayAdapter that draw each list item
    private RouteProblemAdapter mAdapter;
    private int mIssueId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        setContentView(eu.opentransportnet.databikers.R.layout.activity_report_2_route);
        setToolbarTitle(eu.opentransportnet.databikers.R.string.title_route_problem);
        setToolbarBackBtn(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(GO_BACK_RESULT);
                        finish();
                    }
                });

        Intent intent = getIntent();
        mIssueId = intent.getIntExtra("problemID", -1);

        // Descriptions cant be null
        String[] descriptions = getDescriptions(mIssueId);
        mAdapter = new RouteProblemAdapter(this, this, descriptions, mIssueId,
                intent.getDoubleExtra("lat", 200), intent.getDoubleExtra("lng", 200));
        intent.putExtra("lng", intent.getDoubleExtra("lng", 200));
        ListView listView = (ListView) findViewById(eu.opentransportnet.databikers.R.id.route_problem_list);
        listView.setAdapter(mAdapter);
    }

    public void startNextActivity(Intent intent) {
        startActivityForResult(intent, ROUTE_DETAIL_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ROUTE_DETAIL_REQUEST) {
            if (resultCode != Report3DetailsActivity.GO_BACK_RESULT) finish();
        }
    }

    private String[] getDescriptions(int id) {
        switch (id) {
            case Classificators.ISSUE_ROAD_CONDITION:
                return getResources().getStringArray(eu.opentransportnet.databikers.R.array.road_condition);
            case Classificators.ISSUE_ROAD_SIGNALISATION:
                return getResources().getStringArray(eu.opentransportnet.databikers.R.array.traffic_signs);
            case Classificators.ISSUE_HINDRANCE_ALONG_THE_ROAD:
                return getResources().getStringArray(eu.opentransportnet.databikers.R.array.hindrance_along_road);
            case Classificators.ISSUE_ROAD_WORKS:
                return getResources().getStringArray(eu.opentransportnet.databikers.R.array.road_works);
            default:
                return null;
        }
    }

}
