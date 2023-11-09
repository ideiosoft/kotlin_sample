package com.agricolum.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.agricolum.domain.response.Bale;
import com.agricolum.storage.database.DataBaseHelper;
import com.agricolum.ui.PermissionsManager;
import com.agricolum.ui.activities.MainActivity;
import com.agricolum.R;
import com.agricolum.storage.api.AgricolumStore;
import com.agricolum.domain.model.AActivity;
import com.agricolum.domain.model.Enclosure;
import com.agricolum.domain.model.Field;
import com.agricolum.domain.model.RealZone;
import com.agricolum.domain.model.Zone;
import com.agricolum.ui.bales.map.BalesMapManager;
import com.agricolum.ui.bales.map.BalesMapManagerListener;
import com.agricolum.ui.bales.map.gps.GpsClient;
import com.agricolum.ui.bales.map.gps.GpsClientListener;
import com.agricolum.ui.presenter.ActivityListPresenter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.gson.Gson;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.agricolum.ui.PermissionsManager.LOCATION_PERMISSIONS;

public class ActivityMapFragment extends Fragment
        implements OnMapReadyCallback, GoogleMap.OnPolygonClickListener, GpsClientListener, BalesMapManagerListener {

    private static final String TAG = ActivityMapFragment.class.getSimpleName();

    private AgricolumStore mStore;
    private MainActivity mActivity;
    private AActivity mAActivity;
    private GoogleMap mMap;
    private TextView mInfoLabel;
    private Menu menu;
    BalesMapManager balesMapManager;
    //endregion
    GpsClient gpsClient;
    private boolean gpsEnabled = false;
    SupportMapFragment mapFragment;
    ArrayList<AActivity> tempActivitiesList;
    public static ActivityMapFragment newInstance(AActivity activity) {
        ActivityMapFragment fragment = new ActivityMapFragment();
        Bundle args = new Bundle();
        args.putParcelable("activity", activity);
        fragment.setArguments(args);
        return fragment;
    }

    public ActivityMapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gpsClient = new GpsClient(getContext());
        gpsClient.setListener(this);
        setHasOptionsMenu(true);
        Bundle args = getArguments();
        if (args != null) {
            mAActivity = args.getParcelable("activity");
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (MainActivity) context;
        }
        mStore = AgricolumStore.client(context);
    }

    @Override
    public void onDetach() {
        mAActivity = null;
        mActivity = null;
        mStore = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_activity_map, container, false);

        // setup map
        mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        balesMapManager = new BalesMapManager(getContext(), this);
        tempActivitiesList=new ArrayList();
        balesMapManager.getMapAsync(mapFragment);
        mInfoLabel = (TextView) view.findViewById(R.id.enclosure_msg);
        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MainActivity.LOCATION_REQUEST_CODE) {
            if (grantResults!=null&&grantResults.length>0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (mMap == null) {
                    return;
                }
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mMap.setMyLocationEnabled(true);
            }
            }
        }
    }

    @Override
    public void onMapReady(@Nullable GoogleMap googleMap) {

        this.mMap = googleMap;

        if (!checkGooglePlayServiceAvailability(getContext(), -1)) {
            // TODO: display error
            return;
        }

        if (PermissionsManager.hasPermissions(LOCATION_PERMISSIONS, getContext())) {
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(true);
        } else {
            requestPermissions(LOCATION_PERMISSIONS, MainActivity.LOCATION_REQUEST_CODE);
        }

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		 
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setPadding(0, 80, 0, 0);    // set padding for top view
        Log.e("dfdf",new Gson().toJson(mAActivity));
        Log.e("taskkk id",new Gson().toJson(mAActivity.task_id));



        for(int i=0;i< ActivityListPresenter.mActivitiesList.size();i++)
        {
            if(ActivityListPresenter.mActivitiesList.get(i).task_id.equalsIgnoreCase(mAActivity.id)) {

if(!tempActivitiesList.contains(ActivityListPresenter.mActivitiesList.get(i))) {
    tempActivitiesList.add(ActivityListPresenter.mActivitiesList.get(i));
}
            }
            }


        for(int i=0;i< ActivityListPresenter.mActivitiesTasksList.size();i++)
        {
            if(ActivityListPresenter.mActivitiesTasksList.get(i).task_id.equalsIgnoreCase(mAActivity.id)) {

                if(!tempActivitiesList.contains(ActivityListPresenter.mActivitiesTasksList.get(i))) {
                    tempActivitiesList.add(ActivityListPresenter.mActivitiesTasksList.get(i));
                }

            }
            }




        centerMap(mMap, mAActivity);
        displayAll();

    }

    private void displayAll() {
        // draw the enclosures
        List<Enclosure> enclosures = new ArrayList<>();
        if(mAActivity.zones!=null)
        {

        for (Zone aZone : mAActivity.zones) {

            if (aZone.isEnclosure()) {

                Enclosure enclosure = mStore.getEnclosure(aZone.zoneable_id);


               double tempArea=0.0;
                for(int i=0;i< tempActivitiesList.size();i++)
                {
                    if(tempActivitiesList.get(i).zones!=null)
                    {
                        for(int j=0;j< tempActivitiesList.get(i).zones.size();j++) {
                            if(tempActivitiesList.get(i).zones.get(j).zoneable_id.equalsIgnoreCase(aZone.zoneable_id))
                            {

                                if(tempActivitiesList.get(i).zones.get(j).area!=null) {
                                    tempArea = tempArea + Double.parseDouble(tempActivitiesList.get(i).zones.get(j).area);
                                }
                            }

                        }
                    }

                }

String tempAreaWorked="0.0";
if(enclosure!=null)
{
if(enclosure.area_worked!=null)
{
    if(enclosure.area_worked.equalsIgnoreCase(""))
    {
        tempAreaWorked="0.0";
    }
    else
    {
        tempAreaWorked=enclosure.area_worked;
    }

    }
else
{
    tempAreaWorked="0.0";

}
}
if(tempArea<Double.parseDouble(tempAreaWorked))
{



    if (enclosure != null) {
        drawEnclosure(mMap, enclosure, false,"blue");
        enclosures.add(enclosure);
    }
}
else if(tempArea==Double.parseDouble(tempAreaWorked))
{



    if (enclosure != null) {
        drawEnclosure(mMap, enclosure, false,"green");
        enclosures.add(enclosure);
    }
}
else
{



    if (enclosure != null) {
        drawEnclosure(mMap, enclosure, false,"green");
        enclosures.add(enclosure);
    }
}


            } else if (aZone.isField()) {
                Field field = mStore.getField(aZone.zoneable_id);
                if (field != null) {
                    for (Enclosure enclosure : field.enclosures) {
                        drawEnclosure(mMap, enclosure, false, "green");
                        enclosures.add(enclosure);
                    }
                }
            }
        }


        }

      /*  if(enclosures!=null) {
            // draw enclosures boundaries
            drawEnclosures(mMap, enclosures, false);
        }*/
        if(mAActivity.real_zones!=null) {
            // draw enclosures fill
            for (RealZone zone : mAActivity.real_zones) {
                Enclosure enclosure = mStore.getEnclosure(zone.enclosure_id);
                if (enclosure == null) {
                    continue;
                }
                if (zone.area.equals(enclosure.area_worked)) {
                    zone.path = enclosure.path;
                }
            }
        }
        if(mAActivity.real_zones!=null) {
            drawRealZones(mMap, mAActivity.real_zones);
        }

        if(mAActivity.zones!=null) {
            // update info
            displayInfo(mAActivity.zones);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map, menu);
        this.menu = menu;
      //  menu.getItem(0).setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bales_gps: {
                if (gpsEnabled) {
                    onGpsLocationDisabled();

                } else {
                    if (gpsClient.hasPermission() && gpsClient.isLocationEnabled()) {
                        item.setIcon(R.drawable.ic_gps_enabled);
                        gpsEnabled = true;
                        gpsClient.startLocationUpdates();
                        balesMapManager.enableCurrentLocation();

                    } else {
                        gpsClient.checkPermissions(getActivity());
                    }
                }
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void centerMap(GoogleMap map, AActivity activity) {

        List<Zone> mapZones = new ArrayList<>();
        if (activity.zones != null) {
            for (Zone aZone : activity.zones) {
                if (aZone.isEnclosure()) {
                    // search in real zones
                    if(activity.real_zones!=null)
                    {

                    for (RealZone realZone : activity.real_zones) {
                        Log.e("xxxxw",activity.area);
                        if (realZone.enclosure_id.equals(aZone.zoneable_id)) {
                            Log.e("xxxxww",realZone.area);
                            if (realZone.path == null || realZone.path.isEmpty() || realZone.path.equals("null")) {
                                // sometimes, the api returns the path empty, try to find it on local store
                                Enclosure mEnclosure = mStore.getEnclosure(aZone.zoneable_id);
                                if (mEnclosure == null) {
                                    continue;
                                }
                                Log.e("xxx",mEnclosure.area_worked);

                                realZone.path = mEnclosure.path;
                            }


                            aZone.path = realZone.path;
                            mapZones.add(aZone);
                        }
                    }
                    }
                } else if (aZone.isField()) {
                    Field field = mStore.getField(aZone.zoneable_id);
                    if (field != null) {
                        if(field.enclosures!=null)
                        {
                        for (Enclosure enclosure : field.enclosures) {
                            mapZones.add(enclosure.getZone());
                        }}
                    }
                }
            }
        }

        LatLngBounds bounds = Zone.getBounds(mapZones);

        if (bounds != null) {
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.10); // offset from edges of the map 20% of screen
            CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            map.animateCamera(update);
        }
    }

    private void drawRealZones(GoogleMap map, List<RealZone> zones) {

        if (zones == null || zones.size() == 0) {
            Log.w(TAG, "Empty zones provided to draw on map.");
            throw new RuntimeException("Empty zones provided to draw on map.");
        }

        for (RealZone zone : zones) {

            List<PolygonOptions> polygons = zone.buildPolygons();
            drawPolygons(map, polygons);
        }

    }

    private void drawEnclosures(GoogleMap map, List<Enclosure> enclosures, boolean fill) {
        if(enclosures!=null) {
            for (Enclosure enclosure : enclosures) {
                drawEnclosure(map, enclosure, fill, "green");
            }
        }
    }

    private void drawPolygons(GoogleMap map, List<PolygonOptions> polygons) {
        if(polygons!=null)
        {
        for (PolygonOptions polygon : polygons) {
            polygon.strokeWidth(0);
            polygon.fillColor(getResources().getColor(R.color.map_secondary_fill));
            map.addPolygon(polygon);

        }
        }
    }

    private void drawEnclosure(GoogleMap googleMap, Enclosure enclosure, boolean fill, String green) {
        List<PolygonOptions> polygons = enclosure.getZone().buildPolygons();
        if(polygons!=null)
        {
        for (PolygonOptions polygon : polygons) {
            polygon.clickable(true);
            polygon.strokeWidth(5);
            if(green.equalsIgnoreCase("green")) {
                 polygon.strokeColor(getResources().getColor(R.color.green_A700));
            }
            else {
                polygon.strokeColor(getResources().getColor(R.color.map_secondary_outline));

            } polygon.fillColor(fill ? getResources().getColor(R.color.map_secondary_fill) : Color.TRANSPARENT);
            mMap.addPolygon(polygon).setTag(enclosure.id);
            mMap.setOnPolygonClickListener(this);
        }
        }
    }

    private void displayInfo(List<Zone> zones) {
        int totalEnclosures = 0;
        float totalArea = 0f;
        if(zones!=null)
        {
        for (Zone zone : zones) {
            if (zone.isEnclosure()) {
                totalEnclosures += 1;
                totalArea += Float.valueOf(zone.area);
            } else if (zone.isField()) {
                Field field = mStore.getField(zone.zoneable_id);
                if (field != null) {
                    totalEnclosures += field.sub_fields.size();
                }
                totalArea += Float.valueOf(zone.area);
            }
        }
        }
        String info = getString(R.string.enclosures) + ": " + totalEnclosures + " | " + getString(R.string.total_area) + ": " + String.format("%.2f", totalArea) + " ha.";
        mInfoLabel.setText(info);
    }

    public static boolean checkGooglePlayServiceAvailability(Context context, int versionCode) {
        // Query for the status of Google Play services on the device
        int statusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if ((statusCode == ConnectionResult.SUCCESS)
                && (GooglePlayServicesUtil.GOOGLE_PLAY_SERVICES_VERSION_CODE >= versionCode)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onPolygonClick(Polygon polygon) {

        Enclosure e = null;
        try {
            DataBaseHelper dbh = new DataBaseHelper(mActivity);
            dbh.open();
            e = dbh.getEnclosure(polygon.getTag().toString());
            dbh.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        if (e != null) {
            mActivity.showEnclosureDetail(e);
        }
    }


    @Override
    public void onGpsPermissionGranted() {

        gpsClient.checkLocation(getActivity());
    }

    @Override
    public void onGpsPermissionNotGranted() {
        gpsEnabled = false;
    }

    @Override
    public void onGpsLocationEnabled() {
        gpsEnabled = true;
        gpsClient.startLocationUpdates();
        balesMapManager.enableCurrentLocation();
    }

    @Override
    public void onGpsLocationDisabled() {
        gpsEnabled = false;
        gpsClient.stopLocationUpdates();

        MenuItem gpsIcon = menu.findItem(R.id.action_bales_gps);
        gpsIcon.setIcon(R.drawable.ic_gps_disabled);
    }

    @Override
    public void onGpsLocationNotGranted() {

    }

    @Override
    public void onUpdateLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        balesMapManager.centerOnLatLng(latLng, BalesMapManager.ZOOM_MY_LOCATION);
    }

    @Override
    public void onGetLastLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        balesMapManager.centerOnLatLng(latLng, BalesMapManager.ZOOM_MY_LOCATION);

    }


    @Override
    public void onMapReady(@androidx.annotation.Nullable GoogleMap googleMap, @NonNull String s) {
        this.mMap = googleMap;

        if (!checkGooglePlayServiceAvailability(getContext(), -1)) {
            // TODO: display error
            return;
        }

        if (PermissionsManager.hasPermissions(LOCATION_PERMISSIONS, getContext())) {
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(true);
        } else {
            requestPermissions(LOCATION_PERMISSIONS, MainActivity.LOCATION_REQUEST_CODE);
        }

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setPadding(0, 80, 0, 0);    // set padding for top view
        centerMap(mMap, mAActivity);
        displayAll();
    }

    @Override
    public void onMapZoomOut(float lastZoom, float newZoom) {

    }

    @Override
    public void onMapZoomIn(@NonNull LatLng position, float lastZoom, float newZoom) {

    }

    @Override
    public void onMapScroll(@NonNull LatLng position, float newZoom) {

    }

    @Override
    public void onBaleMarkerClick(@NonNull Bale bale) {

    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        balesMapManager.centerOnLatLng(latLng);
        gpsClient.stopLocationUpdates();
        gpsEnabled = false;
    }

    @Override
    public void onCameraMove() {
        onGpsLocationDisabled();
    }
}
