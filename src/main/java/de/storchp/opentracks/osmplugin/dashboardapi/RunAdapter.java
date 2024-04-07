package de.storchp.opentracks.osmplugin.dashboardapi;

import static org.oscim.map.Animator.ANIM_MOVE;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.oscim.core.BoundingBox;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.android.MapView;
import org.oscim.core.GeoPoint;
import org.oscim.layers.GroupLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.PathLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.utils.animation.Easing;


import java.text.DecimalFormat;
import java.util.List;

import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.maps.MovementDirection;
import de.storchp.opentracks.osmplugin.utils.MapMode;
import de.storchp.opentracks.osmplugin.utils.MapUtils;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class RunAdapter extends RecyclerView.Adapter<RunAdapter.RunViewHolder> implements ItemizedLayer.OnItemGestureListener<MarkerInterface> {
    private List<Run> runs;
    private DecimalFormat formatter;
    private MapView mapView;
    private TableLayout previousRunView;
    private GroupLayer polylinesLayer;
    private MovementDirection movementDirection;
    private ItemizedLayer waypointsLayer;
    private MapMode mapMode;

    public RunAdapter(List<Run> runs, MapView mapView) {
        this.runs = runs;
        this.formatter = new DecimalFormat("#0.00");
        previousRunView = null;
        this.mapView = mapView;
        polylinesLayer = new GroupLayer(mapView.map());
        mapMode = PreferencesUtils.getMapMode();
        movementDirection = new MovementDirection();
    }

    @NonNull
    @Override
    public RunAdapter.RunViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.run_item, parent, false);
        return new RunAdapter.RunViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RunAdapter.RunViewHolder holder, int position) {
        Run run = runs.get(position);
        holder.bind(run);
    }

    @Override
    public int getItemCount() {
        return runs.size();
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerInterface item) {
        return false;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerInterface item) {
        return false;
    }

    public class RunViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView avgSpeed;
        private TextView maxSpeed;
        private TextView runTime;
        private TextView distance;
        private TableLayout runView;
        public RunViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_Name);
            avgSpeed = itemView.findViewById(R.id.avgSpeedInput);
            maxSpeed = itemView.findViewById(R.id.maxSpeedInput);
            runTime = itemView.findViewById(R.id.runTimeInput);
            distance = itemView.findViewById(R.id.distanceInput);
            runView = itemView.findViewById(R.id.runView);
        }

        public void bind(Run run) {
            name.setText(run.getName());
            avgSpeed.setText(String.format(Locale.getDefault(), "%.2f km/h", run.getAverageSpeed()));
            maxSpeed.setText(String.format(Locale.getDefault(), "%.2f km/h", run.getMaxSpeed()));
            runTime.setText(formatDuration(run.getDuration()));
            distance.setText(String.format(Locale.getDefault(), "%.2f meters", run.getDistance()));

            runView.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), run.getName() + " Clicked", Toast.LENGTH_SHORT).show();
                runView.setBackgroundColor(Color.rgb(212, 221, 232));

                // Removing the previous layer if any
                mapView.map().layers().remove(polylinesLayer);

                for (int i =  0; i < polylinesLayer.map().layers().size(); i++) {
                    Layer layer = polylinesLayer.map().layers().get(i);
                    if (layer instanceof PathLayer) {
                        polylinesLayer.map().layers().remove(i);
                    }
                }

                PathLayer line = new PathLayer(mapView.map(), Color.BLUE, 12);
                // Add points to the PathLayer
                line.addPoint(run.getStartPoint());
                line.addPoint(run.getEndPoint());
                // Add the PathLayer to the MapView
                polylinesLayer.map().layers().add(line);
                mapView.map().layers().add(polylinesLayer);

                List<GeoPoint> latLongs = line.getPoints();
                Waypoint startWayPoint = new Waypoint(run.getStartPoint(), "start point");
                Waypoint endWayPoint = new Waypoint(run.getEndPoint(), "end point");
                final MarkerItem startPin = MapUtils.createTappableMarker(mapView.getContext(), startWayPoint);
                final MarkerItem endPin = MapUtils.createTappableMarker(mapView.getContext(), endWayPoint);
                MarkerSymbol greenSymbol = MapUtils.createMarkerSymbol(mapView.getContext(), R.drawable.ic_marker_green_pushpin_modern, false, MarkerSymbol.HotspotPlace.CENTER);
                MarkerSymbol redSymbol = MapUtils.createMarkerSymbol(mapView.getContext(), R.drawable.ic_marker_red_pushpin_modern, false, MarkerSymbol.HotspotPlace.CENTER);
                startPin.setMarker(greenSymbol);
                endPin.setMarker(redSymbol);

                if (waypointsLayer != null) {
                    mapView.map().layers().remove(waypointsLayer);
                }
                waypointsLayer = createWaypointsLayer();
                mapView.map().layers().add(waypointsLayer);
                waypointsLayer.addItem(startPin);
                waypointsLayer.addItem(endPin);

                BoundingBox boundingBox = new BoundingBox(latLongs);
                updateMapPositionAndRotation(boundingBox.getCenterPoint());
                mapView.map().animator().animateTo(500, boundingBox, Easing.Type.LINEAR, ANIM_MOVE);
            });
        }
    }

    private String formatDuration(int durationInSeconds) {
        return DateUtils.formatElapsedTime(durationInSeconds);
    }

    private ItemizedLayer createWaypointsLayer() {
        MarkerSymbol symbol = MapUtils.createPushpinSymbol(mapView.getContext());
        return new ItemizedLayer(mapView.map(), symbol);
    }

    private void updateMapPositionAndRotation(final GeoPoint myPos) {
        mapView.map().getMapPosition().setPosition(myPos).setBearing(mapMode.getHeading(movementDirection));
    }
}
