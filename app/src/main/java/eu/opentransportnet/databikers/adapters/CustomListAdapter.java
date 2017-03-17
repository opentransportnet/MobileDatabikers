package eu.opentransportnet.databikers.adapters;


import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Custom list adapter for navigation
 *
 * @author Ilmars Svilsts
 */
public class CustomListAdapter extends ArrayAdapter<String>{

    private final Activity context;
    private final String[] web;
    private final Integer[] imageId;

    public CustomListAdapter(Activity context,
                             String[] web, Integer[] imageId) {
        super(context, eu.opentransportnet.databikers.R.layout.list_single, web);
        this.context = context;
        this.web = web;
        this.imageId = imageId;

    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(eu.opentransportnet.databikers.R.layout.list_single, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(eu.opentransportnet.databikers.R.id.txt);

        ImageView imageView = (ImageView) rowView.findViewById(eu.opentransportnet.databikers.R.id.img);
        txtTitle.setText(web[position]);

        imageView.setImageResource(imageId[position]);
        return rowView;
    }
}
