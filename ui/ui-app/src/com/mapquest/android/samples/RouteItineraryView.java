package com.mapquest.android.samples;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapquest.android.maps.RouteResponse;
import com.mapquest.android.maps.RouteResponse.Location;
import com.mapquest.android.maps.RouteResponse.Route;
import com.mapquest.android.maps.RouteResponse.Route.Leg;
import com.mapquest.android.maps.RouteResponse.Route.Leg.Maneuver;
import com.mapquest.android.maps.RouteResponse.Route.Leg.Maneuver.Sign;

/**
 * This view is intended to be copied, sub-classed and/or modified in your application as a base
 * to get started adding custom/advanced routes narratives to your application. It shows how to digest
 * the route information and create narrative from it.   It depends on two layout files:<br/>
 * 	<li/>custom_route_itinerary.xml used to describe what the route itinerary looks
 * 	<li/>custom_route_manuever.xml that describes what each maneuver looks like in the listView   
 * This class gets wired up in the custom_route_itinerary_demo.xml layout ( look for 
 * com.mapquest.android.samples.RouteItineraryView node near the end of the file)
 */
public class RouteItineraryView extends RelativeLayout {
	
	private TextView start;
	private TextView end;
	private TextView distance;
	private TextView time;
	private ListView narrativeList;
	private String distanceUnit;
	
	public RouteItineraryView(Context context) {
		super(context);
		init(context);
	}
	
	public RouteItineraryView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void init(Context context){
		LayoutInflater li = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.custom_route_itinerary, this, true);
		
		start=(TextView)findViewById(R.id.start);
		end=(TextView)findViewById(R.id.end);
		distance=(TextView)findViewById(R.id.distance);
		time=(TextView)findViewById(R.id.time);
		narrativeList=(ListView)findViewById(R.id.narrative);
	}
	
	/**
	 * Called when the async task for requesting data has returned, 
	 * @param routeResponse
	 */
	public void setRouteResponse(RouteResponse routeResponse){
		
		if(routeResponse==null)return;
		
		Route route = routeResponse.route;
		if(route==null) return;
	
		List<Location> locations = route.locations;
		if(locations.size()==0) return;
		distanceUnit="M".equals(route.options.unit)? "mi" : "km";
		start.setText(route.locations.get(0).toString());
		end.setText(route.locations.get(route.locations.size()-1).toString());
		distance.setText(new DecimalFormat("#.##").format(route.distance)+" "+distanceUnit);
		time.setText(route.formattedTime);
		
		List<Leg> legs=routeResponse.route.legs;
		ManueverArrayAdapter arrayAdapter = new ManueverArrayAdapter(
				narrativeList.getContext(), R.layout.custom_route_manuver,routeResponse);
				
		narrativeList.setAdapter(arrayAdapter);
		if(legs!=null){
			for(Leg leg:legs){
				List<Maneuver> manuevers=leg.maneuvers;
				if(manuevers!=null){
					for(Maneuver manuever:manuevers){
						arrayAdapter.add(manuever);
					}
				}
			}
		}
		
		narrativeList.invalidate();
	
	}

	/**
	 * Simple ListAdapter to draw manuevers in a ListView
	 */
	public class ManueverArrayAdapter extends ArrayAdapter<Maneuver>{
		
		private int textViewResourceId=-1;
		private DecimalFormat df = new DecimalFormat("#.####");
		private RouteResponse routeResponse;
		
		//Simple cache of icons, if the object hit is type Bitmap then the icon is
		//ready to use, otherwise its either being downloaded or there was an 
		//error while downloading it
		public Map<String,Object> iconCache=new ConcurrentHashMap<String, Object>();
		
		public ManueverArrayAdapter(Context context, 
				int textViewResourceId, RouteResponse routeResponse) {
			super(context, textViewResourceId);
			this.textViewResourceId=textViewResourceId;
			this.routeResponse=routeResponse;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			ViewHolder holder;//so we can reuse the view

			// reuse the passed in view if supplied
			if(convertView == null) {
				LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(textViewResourceId, parent, false);
				
				holder = new ViewHolder();
				holder.roadShieldContainer = (LinearLayout)convertView.findViewById(R.id.roadShieldContainer);
				holder.narrative = (TextView)convertView.findViewById(R.id.narrativeText);
				holder.distance = (TextView)convertView.findViewById(R.id.distance);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder)convertView.getTag();
			}
			
			Maneuver man = getItem(position);			
			holder.narrative.setText(man.narrative);
			String dist=df.format(man.distance);
			if(!"0".equals(dist)){
				holder.distance.setText(dist+" "+distanceUnit);
			}
			
			//load the maneuver icon
			holder.roadShieldContainer.removeAllViews();
			if(!"".equals(man.iconUrl)){
				ImageView iv=buildImageView();
				holder.roadShieldContainer.addView(iv);
				addBitmap(man.iconUrl, iv);
			}

			//load the signs up	
			for(Sign sign:man.signs){
				if(!"".equals(sign.url)){
					ImageView iv=buildImageView();
					holder.roadShieldContainer.addView(iv);
					addBitmap(sign.url, iv);
				}
			}

			return convertView;
		}
		
		private ImageView buildImageView(){
			//TODO we should center these icons 
			ImageView imageView=new ImageView(getContext());
			LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			imageView.setLayoutParams(params);
			return imageView;
		}

		/**
		 * class to cache the view contents
		 */
		class ViewHolder{
			public LinearLayout roadShieldContainer;
			public TextView narrative;
			public TextView distance;
		}
		
		void addBitmap(String imageUrl, ImageView imageView) {
			Object o=iconCache.get(imageUrl);
			if(o!=null && o instanceof Bitmap){
				imageView.setImageBitmap((Bitmap)o);
			}else{
				new IconFetcher(imageUrl,imageView).execute();
			}
	    }
		
		/**
		 * Fetches road icons and adds them the supplied imageView when done
		 */
		class IconFetcher extends AsyncTask<Void, Void, Bitmap>{

			private final String imageUrl;;
			private final ImageView imageView;
			
			IconFetcher(String imageUrl,ImageView imagdView){
				this.imageView=imagdView;
				this.imageUrl=imageUrl;
			}
			
			@Override
			protected Bitmap doInBackground(Void... params) {
				iconCache.put(imageUrl,new Object());//forces only one download per url
				 try {
		            URL url = new URL(imageUrl);
		            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		            connection.setReadTimeout(3000);
		            connection.setConnectTimeout(3000);
		            connection.connect();
		            InputStream input = connection.getInputStream();
		            return BitmapFactory.decodeStream(input);
		        } catch (IOException e) {
		            return null;
		        }
			}
			
			@Override
			protected void onPostExecute(Bitmap icon) {
				if(icon!=null){
					iconCache.put(imageUrl,icon);
					imageView.setImageBitmap(icon);
				}
			}
			
			
		}
		
		

		
	}
	
}
