package eu.opentransportnet.databikers.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import eu.opentransportnet.databikers.adapters.RouteProblemAdapter;
import eu.opentransportnet.databikers.models.BaseActivity;
import eu.opentransportnet.databikers.utils.Classificators;

/**
 * SelectProblem activity
 *
 * @author Kristaps Krumins
 */
public class Report1ProblemsActivity extends BaseActivity {

    private static final int ROUTE_PROBLEM_REQUEST = 1000;
    private static final int ROUTE_DETAIL_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        initUi();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setContentView(eu.opentransportnet.databikers.R.layout.activity_report_1_problems);
        super.onConfigurationChanged(newConfig);
        initUi();
    }

    /**
     * Starts {@link Report2RouteActivity} activity with extra data. Extra data contains problem ID.
     *
     * @param view the View that called onClick
     */
    public void startRouteProblem(View view) {
        int viewId = view.getId();
        int id;

        switch (viewId) {
            case eu.opentransportnet.databikers.R.id.road_condition:
                id = Classificators.ISSUE_ROAD_CONDITION;
                break;
            case eu.opentransportnet.databikers.R.id.road_signalisation:
                id = Classificators.ISSUE_ROAD_SIGNALISATION;
                break;
            case eu.opentransportnet.databikers.R.id.hindrance:
                id = Classificators.ISSUE_HINDRANCE_ALONG_THE_ROAD;
                break;
            case eu.opentransportnet.databikers.R.id.road_works:
                id = Classificators.ISSUE_ROAD_WORKS;
                break;
            case eu.opentransportnet.databikers.R.id.other:
                id = Classificators.ISSUE_WITHOUT_CLASSIFICATION;
                break;
            default:
                id = Classificators.ISSUE_WITHOUT_CLASSIFICATION;
                break;
        }

        Intent extras = getIntent();

        if (id == Classificators.ISSUE_WITHOUT_CLASSIFICATION) {
            Intent intent = new Intent(this, Report3DetailsActivity.class);
            intent.putExtra("issueID", id);
            intent.putExtra("imageResID", RouteProblemAdapter.getResId(id));
            intent.putExtra("lat", extras.getDoubleExtra("lat", 200));
            intent.putExtra("lng", extras.getDoubleExtra("lng", 200));
            intent.putExtra("description", getString(eu.opentransportnet.databikers.R.string.other));
            startActivityForResult(intent, ROUTE_DETAIL_REQUEST);
        } else {
            Intent intent = new Intent(this, Report2RouteActivity.class);
            intent.putExtra("problemID", id);
            intent.putExtra("lat", extras.getDoubleExtra("lat", 200));
            intent.putExtra("lng", extras.getDoubleExtra("lng", 200));
            startActivityForResult(intent, ROUTE_PROBLEM_REQUEST);
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ROUTE_PROBLEM_REQUEST) {
            if (resultCode != Report2RouteActivity.GO_BACK_RESULT) finish();
        } else if (requestCode == ROUTE_DETAIL_REQUEST) {
            if (resultCode != Report3DetailsActivity.GO_BACK_RESULT) finish();
        }
    }

    private void initUi() {
        setContentView(eu.opentransportnet.databikers.R.layout.activity_report_1_problems);
        setToolbarTitle(eu.opentransportnet.databikers.R.string.select_problem_type);
        initToolbarBackBtn();
    }

}