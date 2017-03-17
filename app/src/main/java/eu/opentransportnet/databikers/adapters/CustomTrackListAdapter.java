package eu.opentransportnet.databikers.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Custom list adapter for route list
 *
 * @author Ilmars Svilsts
 */
public class CustomTrackListAdapter extends ArrayAdapter<String>{


    private final Activity context;
    private final List<String> web;
    private final List<String> distance;
    private final List<String> time;
    private final List<String> id;

    public CustomTrackListAdapter(Activity context,
                                  List<String> web, List<String> distance, List<String> time, List<String> id) {
        super(context, eu.opentransportnet.databikers.R.layout.list_single, web);
        this.context = context;
        this.web = web;
        this.distance = distance;
        this.time = time;
        this.id = id;

    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(eu.opentransportnet.databikers.R.layout.list_mytrack, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(eu.opentransportnet.databikers.R.id.text_list);
        TextView txtDistance = (TextView) rowView.findViewById(eu.opentransportnet.databikers.R.id.distance_list);
        TextView txtTime = (TextView) rowView.findViewById(eu.opentransportnet.databikers.R.id.time_list);
        ImageView imageView = (ImageView) rowView.findViewById(eu.opentransportnet.databikers.R.id.prog_upload);

        txtTitle.setTag(id.get(position));
        txtDistance.setText(distance.get(position) + " km");
        txtTime.setText(time.get(position).replace(".", "h ") + " min");
        txtTitle.setText(web.get(position));
        if(!isValidInteger(id.get(position))){
            imageView.setVisibility(View.VISIBLE);
        }
        return rowView;
    }
    public static Boolean isValidInteger(String value) {
        try {
            Integer val = Integer.valueOf(value);
            if (val != null)
                return true;
            else
                return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
