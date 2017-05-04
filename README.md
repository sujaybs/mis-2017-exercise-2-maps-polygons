# mis-2017-exercise-2-maps-polygons

This project is an Android application which uses the google API services. It allows users to create custom markers on the map with a custom message for each marker. It also lets users create a custom marked polygon with a custom message. The application finds the centroid and calculates the area of the polygon on the map in square kilometers.

## UI

The UI consists of a [Text Field](https://developer.android.com/reference/android/widget/EditText.html), a [Button](https://developer.android.com/reference/android/widget/Button.html) (CLEAR ALL) which will erase previously stored markers and a polygon draw button in the lower right side of the screen. 

The text field is used to type custom messages for all the custom markers. A custom marker is created when an user does a long press on the map. The draw polygon button is activated by clicking, it changes to a tick mark which is the draw mode. The user will have to long press on different parts of the map to create a polygon. The points are shown as transparent markers and the centroid is shown as a bold marker.

## Implementation 

String and integer constants such as location permissions, data points, key strings, request check settings and active polygon none are declared as references to send and filter signals. 

### Iconincs

[Iconics](https://github.com/mikepenz/Android-Iconics) are used to show the start and stop polygon operations on the application. The advantage of this android library is to include vector icons everywhere in the project. This library lets developer scale with no limit, use any color at any time, provide a contour, and many additional customizations. 

```
  private IconicsDrawable ICON_DRAW_POLYGON;
  private IconicsDrawable ICON_STOP_POLYGON; 
```

### OnCreate

The iconics are called here to define the two states of the polygon - draw_polygon and stop_polygon. HashMaps, ArrayList and LinkedHashSet are used to manage entities such as markers and polygons. The mEventHandler and mapEventHandler are classes defined for callbacks to connect to google API. The location request is set for not accessing location more than 10 seconds to preserve battery. The OnClickListener has two functions : ClearAllButton(erase stored markers) and load/save markers. 

### OnResume and OnPause

These functions are defined to incorporate the intermediate states while drawing the polygon. 

### [Shared Preferences](https://developer.android.com/guide/topics/data/data-storage.html#pref)

This storage method is used to save the markers along with the custom string messages as key-value pairs. 

### Google API

There are already exisiting functions in the [Google API](https://developers.google.com/android/guides/setup) to calculate the [area](http://googlemaps.github.io/android-maps-utils/javadoc/com/google/maps/android/SphericalUtil.html) of a polygon on the map and the [centroid](https://developers.google.com/android/reference/com/google/android/gms/maps/model/LatLngBounds.Builder) of the polygons. We have used them in this application. 
