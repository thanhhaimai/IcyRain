#PULL UI

This is the UI for our Android app. It relies on the MapQuest API. Most of the code that will need to be looked at is in `src/com/mapquest/android/samples/RouteItineraryDemo.java`. 

## Notes:
If testing on phone, set `useOnPhone` to `true` in `RouteItineraryDemo.java`.

This is based off of the MapQuest samples, so we need to change the app's icon and titles. 

The "Set Time" button does not work because of an issue with having to extend `FragmentActivity` (`RouteItineraryDemo` extends `SimpleMap`, so it can't extend `FragmentActivity` as well). Will ask Sharad for workaround.
	- [This article](http://developer.android.com/guide/topics/ui/controls/pickers.html) explains how date/time pickers work. It mentions that our activity has to extend `FragmentActivity` so that we can call `getSupportFragmentManager()`. From what I understand, if we only target Android 3.0 or above, this is not an issue (because we can call `getFragmentManager()` from any activity). I don't entirely understand this issue, but this is the general gist of it. 
	- Other helpful links
		- [StackOverflow: How to get around an activity requiring to extend both mapactivity and another activity](http://stackoverflow.com/questions/8525147/possible-to-get-around-an-activity-requiring-to-extend-both-mapactivity-and-anot)
		- [StackOverflow: How to create datepicker/timepicker dialogs in fragment class](http://stackoverflow.com/questions/6668619/how-to-create-datepicker-and-timepicker-dialogs-in-fragment-class)
