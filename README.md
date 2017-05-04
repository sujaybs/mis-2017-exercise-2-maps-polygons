# mis-2017-exercise-2-maps-polygons

This project is an Android application which uses the google API services. It allows users to create custom markers on the map with a custom message for each marker. It also lets users create a custom marked polygon with a custom message. The application finds the centroid and estimates area shown as a new additional marker in the centroid of the polygon with reasonable units depending on the actual size of the area.

<!--
## UI

The UI consists of a [Text Field](https://developer.android.com/reference/android/widget/EditText.html), a [Button](https://developer.android.com/reference/android/widget/Button.html) (CLEAR ALL) which will erase previously stored markers and a polygon draw button in the lower right side of the screen. 

The text field is used to type custom messages for all the custom markers. A custom marker is created when an user does a long press on the map. The draw polygon button is activated by clicking, it changes to a tick mark which is the draw mode. The user will have to long press on different parts of the map to create a polygon. The points are shown as transparent markers and the centroid is shown as a bold marker.
-->

## Dependencies 

We have used [Retrolambda](https://github.com/orfjackal/retrolambda) to support Java 8 features, Google Play services to use Google API, [Iconics](https://github.com/mikepenz/Android-Iconics) is used to include vector icons in the project. [Google Material Typeface](https://github.com/google/material-design-icons) is used from iconics library which is the prefered library for Android applications. [Google Maps Android API utility library](https://developers.google.com/maps/documentation/android-api/utility/) is used to calculate the area of polygon. 

## Markers 

We create a marker for current location with label "I am here!". Text entry field above the map can be used to specify the message that will be written to the next created marker (long click).   

`loadMarkers()` function is used for unpacking the markers. 

```
private void loadMarkers() {
        if (mMessageLocationMap != null && mSharedPreferences != null) {
            HashSet<String> messages = (HashSet<String>) mSharedPreferences.getStringSet(KEY_STRINGS, new HashSet<>());
            for (String message : messages) {
                float lat = mSharedPreferences.getFloat(message + "_lat", -1f);
                float lon = mSharedPreferences.getFloat(message + "_lon", -1f);
                if (lat != -1f && lon != -1f) {
                    LatLng latLng = new LatLng(lat, lon);
                    mMessageLocationMap.put(message, latLng);
                }
            }
        }
    }
```
    
    
`saveMarkers()` function is used for packing the markers.


```
private void saveMarkers() {
    if (mMessageLocationMap != null && !mMessageLocationMap.isEmpty()) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putStringSet(KEY_STRINGS, mMessageLocationMap.keySet());
        for (Map.Entry<String, LatLng> marker : mMessageLocationMap.entrySet()) {
            editor.putFloat(marker.getKey() + "_lat", (float) marker.getValue().latitude);
            editor.putFloat(marker.getKey() + "_lon", (float) marker.getValue().longitude);
        }
        editor.apply();
    }
}
```

## Polygons

We use Google API and [Google Maps Android API utility library](https://developers.google.com/maps/documentation/android-api/utility/) to access `LatLngBounds.Builder` and `SphericalUtil.computeArea()` respectively. 

We calculate the centroid coordinates using `LatLngBounds.Builder`:

```
LatLngBounds.Builder centroidBuilder = new LatLngBounds.Builder();

for (Iterator<LatLng> iterator = vertex.iterator(); iterator.hasNext(); ) {
  LatLng marker = iterator.next();

  ...

  centroidBuilder.include(marker);
}

centroidBuilder.build().getCenter()
```

With `SphericalUtil.computeArea()` and `BigDecimal` we return the actual size of the polygon in reasonable units. 

For the purpose of starting and finishing polygon creation we implement a pattern of `FloatingActionButton` with two icons from [Iconics](https://github.com/mikepenz/Android-Iconics) library.

```
  private IconicsDrawable ICON_DRAW_POLYGON;
  private IconicsDrawable ICON_STOP_POLYGON; 
```

The screenshot below shows the estimated area of the inner courtyard of Stadtschloss.

![Screenshot](/screenshot.png)
