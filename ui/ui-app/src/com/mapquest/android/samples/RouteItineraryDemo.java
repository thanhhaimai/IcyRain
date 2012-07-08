package com.mapquest.android.samples;

import java.util.List;

import android.location.Address;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mapquest.android.Geocoder;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.RouteManager;
import com.mapquest.android.maps.RouteResponse;
import com.mapquest.android.maps.ServiceResponse.Info;

/**
 * This demo is to give you and idea on how to implement customized route
 * itinerary
 */
public class RouteItineraryDemo extends SimpleMap {

	@Override
	protected int getLayoutId() {
		return R.layout.custom_route_itinerary_demo;
	}

	protected MyLocationOverlay myLocationOverlay;
	protected MapView mapView;
	protected EditText start;

	@Override
	protected void init() {
		super.init();

		// find the objects we need to interact with
		mapView = (MapView) findViewById(R.id.map);
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		final RelativeLayout mapLayout = (RelativeLayout) findViewById(R.id.mapLayout);
		final RelativeLayout itineraryLayout = (RelativeLayout) findViewById(R.id.itineraryLayout);
		final Button createRouteButton = (Button) findViewById(R.id.createRouteButton);
		final Button showItineraryButton = (Button) findViewById(R.id.showItineraryButton);
		final Button showMapButton = (Button) findViewById(R.id.showMapButton);
		final Button clearButton = (Button) findViewById(R.id.clearButton);
		final Button setTime = (Button) findViewById(R.id.setTime);

		// PULL : current location
		// start = (EditText) findViewById(R.id.startTextView);
		final EditText end = (EditText) findViewById(R.id.endTextView);
		final RouteItineraryView itinerary = (RouteItineraryView) findViewById(R.id.itinerary);

		final RouteManager routeManager = new RouteManager(this);
		routeManager.setMapView(this.mapView);
		routeManager.setDebug(true);
		routeManager.setRouteCallback(new RouteManager.RouteCallback() {
			@Override
			public void onError(RouteResponse routeResponse) {
				Info info = routeResponse.info;
				int statusCode = info.statusCode;

				StringBuilder message = new StringBuilder();
				message.append("Unable to create route.\n").append("Error: ")
						.append(statusCode).append("\n").append("Message: ")
						.append(info.messages);
				Toast.makeText(getApplicationContext(), message.toString(),
						Toast.LENGTH_LONG).show();
				createRouteButton.setEnabled(true);
			}

			@Override
			public void onSuccess(RouteResponse routeResponse) {
				clearButton.setVisibility(View.VISIBLE);
				if (showItineraryButton.getVisibility() == View.GONE
						&& showMapButton.getVisibility() == View.GONE) {
					showItineraryButton.setVisibility(View.VISIBLE);
				}
				itinerary.setRouteResponse(routeResponse);
				createRouteButton.setEnabled(true);
			}

		});

		// attach the show itinerary listener
		showItineraryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapLayout.setVisibility(View.GONE);
				itineraryLayout.setVisibility(View.VISIBLE);
				showItineraryButton.setVisibility(View.GONE);
				showMapButton.setVisibility(View.VISIBLE);
			}
		});

		// attach the show map listener
		showMapButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapLayout.setVisibility(View.VISIBLE);
				itineraryLayout.setVisibility(View.GONE);
				showMapButton.setVisibility(View.GONE);
				showItineraryButton.setVisibility(View.VISIBLE);
			}
		});

		// attach the clear route listener
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				routeManager.clearRoute();
				clearButton.setVisibility(View.GONE);
				showItineraryButton.setVisibility(View.GONE);
				showMapButton.setVisibility(View.GONE);
				mapLayout.setVisibility(View.VISIBLE);
				itineraryLayout.setVisibility(View.GONE);
			}
		});
		
		/*
		// attach the set time listener
		setTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new TimePickerFragment();
				newFragment.show(this., "timePicker");
			}
		});
		*/
			

		// create an onclick listener for the instructional text
		createRouteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				hideSoftKeyboard(view);
				createRouteButton.setEnabled(false);
				Log.d("location", "hello there");

				// PULL put current location here
				// myLocationOverlay.enableMyLocation();
				/*
				 * myLocationOverlay.runOnFirstFix(new Runnable() {
				 * 
				 * @Override public void run() {
				 */
				// final GeoPoint currentLocation = myLocationOverlay
				// .getMyLocation();
				Log.d("Location", "Running");
				final GeoPoint currentLocation = new GeoPoint(32.9102789,
						-117.1616412);
				Log.d("location", currentLocation.getLatitude() + ", "
						+ currentLocation.getLongitude());
				mapView.getController().animateTo(currentLocation);
				mapView.getController().setZoom(14);
				//mapView.getOverlays().add(myLocationOverlay);
				//myLocationOverlay.setFollowing(true);
				Geocoder geocoder = new Geocoder(getBaseContext());
				double latitude = currentLocation.getLatitude();
				double longitude = currentLocation.getLongitude();
				List<Address> locations = null;
				Address location = null;

				Log.d("location", "HELLO");

				try {
					locations = geocoder
							.getFromLocation(latitude, longitude, 1);
					location = locations.get(0);
				} catch (Exception e) {
					Log.d("location", e.toString());
				}

				String startAt = "San Francisco";
				if (location != null) {
					final String propLoc = location.getAddressLine(0) + ", " + location.getAddressLine(1) + ", " + location.getPostalCode();
					/*
					runOnUiThread(new Runnable() {
						@Override
						public void run() {

							start.setText(propLoc);
						}
					});
					*/
					Log.d("location", propLoc);
					startAt = propLoc;
				}
				Log.d("location", "HELLO");
				Log.d("location", startAt);
				// String endAt = getText(end);
				String endAt = "Berkeley";

				routeManager.createRoute(startAt, endAt);
				// }
				// });

			}
		});

	}
}
