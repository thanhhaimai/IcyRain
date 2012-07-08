#PULL UI

This is the UI for our Android app. It relies on the MapQuest API. Most of the code that will need to be looked at is in `src/com/mapquest/android/samples/RouteItineraryDemo.java`. 

## Notes:
If testing on phone, set `useOnPhone` to `true` in `RouteItineraryDemo.java`.

This is based off of the MapQuest samples, so we need to change the app's icon and titles. 

The "Set Time" button does not work because of an issue with having to extend `FragmentActivity` (`RouteItineraryDemo` extends `SimpleMap`, so it can't extend `FragmentActivity` as well). Will ask Sharad for workaround.
