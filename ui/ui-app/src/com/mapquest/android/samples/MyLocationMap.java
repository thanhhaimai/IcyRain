package com.mapquest.android.samples;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.MapActivity;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;

public class MyLocationMap extends MapActivity {
    
	protected MapView myMap;
	protected MyLocationOverlay myLocationOverlay;
	protected Button followMeButton;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.followme_map);
        
        followMeButton=(Button)findViewById(R.id.followMeButton);
        followMeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				myLocationOverlay.setFollowing(true);
			}
		});
        
        setupMapView();
        setupMyLocation();
    }

    /**
     * This will set up a MapQuest map with a MyLocation Overlay
     */
	private void setupMapView() {
		this.myMap = (MapView) findViewById(R.id.map);
		
		// enable the zoom controls
		myMap.setBuiltInZoomControls(true);
	}
	
	protected void setupMyLocation() {
		this.myLocationOverlay = new MyLocationOverlay(this, myMap);
		
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.runOnFirstFix(new Runnable() {
			@Override
			public void run() {
				GeoPoint currentLocation = myLocationOverlay.getMyLocation(); 
				myMap.getController().animateTo(currentLocation);
				myMap.getController().setZoom(14);
				myMap.getOverlays().add(myLocationOverlay);
				myLocationOverlay.setFollowing(true);
			}
		});
	}
	
	@Override
	protected void onResume() {
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.enableCompass();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		myLocationOverlay.disableCompass();
		myLocationOverlay.disableMyLocation();
	}

	@Override
	public boolean isRouteDisplayed() {
		return false;
	}
}