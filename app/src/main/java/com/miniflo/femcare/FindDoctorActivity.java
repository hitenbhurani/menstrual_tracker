package com.miniflo.femcare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FindDoctorActivity - Hybrid OSM/Google Model
 * Map UI: OpenStreetMap (osmdroid) - Free
 * Navigation: Google Maps Intent - Free
 */
public class FindDoctorActivity extends AppCompatActivity {

    private static final int SEARCH_RADIUS_METERS = 10000; // 10km radius for suburban areas
    private static final float MIN_GOOGLE_RATING = 4.0f;
    private static final int MIN_WELL_KNOWN_REVIEW_COUNT = 50;
    private static final String REQUEST_TAG = "OVERPASS_QUERY";
    private static final String GOOGLE_MAPS_API_KEY_META_DATA = "com.google.android.geo.API_KEY";
    private static final String PLACES_ENDPOINT = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    private static final GeoPoint DEFAULT_SEARCH_POINT = new GeoPoint(19.2049, 73.1867); // Fallback only when device location is unavailable.
    private static final String[] OVERPASS_ENDPOINTS = {
            "https://overpass-api.de/api/interpreter",
            "https://lz4.overpass-api.de/api/interpreter"
    };

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private RequestQueue requestQueue;

    private RecyclerView recyclerClinics;
    private TextView tvEmptyState;
    private ProgressBar progressClinics;
    private Chip chipSortDistance;
    private Chip chipFilterRating;
    private Chip chipFilterWellKnown;
    private Chip chipFilterOpenNow;

    private ClinicAdapter clinicAdapter;
    private final List<Clinic> rawClinics = new ArrayList<>();
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();
    private GeoPoint userGeoPoint;
    private String searchAreaName = "";
    private String searchCenterMarkerTitle = "You are here";
    private boolean googleRatingDataLoaded = false;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fineGranted || coarseGranted) {
                    fetchLocationAndClinics();
                } else {
                    loadClinicsUsingDefaultLocation("Location permission denied. Showing clinics near a default area.");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // OSM Configuration must load BEFORE setContentView
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_doctor);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupRecyclerView();
        setupFilterChips();
        setupMapView();

        ensurePermissionsAndLoad();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        recyclerClinics = findViewById(R.id.recyclerClinics);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressClinics = findViewById(R.id.progressClinics);
        chipSortDistance = findViewById(R.id.chipSortDistance);
        chipFilterRating = findViewById(R.id.chipFilterRating);
        chipFilterWellKnown = findViewById(R.id.chipFilterWellKnown);
        chipFilterOpenNow = findViewById(R.id.chipFilterOpenNow);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);
    }

    private void setupRecyclerView() {
        // Hybrid logic: Directions button triggers free Google Maps app
        clinicAdapter = new ClinicAdapter(clinic -> {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + clinic.getLatitude() + "," + clinic.getLongitude());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Google Maps app not found.", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerClinics.setLayoutManager(new LinearLayoutManager(this));
        recyclerClinics.setAdapter(clinicAdapter);
    }

    private void setupFilterChips() {
        View.OnClickListener listener = view -> applyClinicFilters();
        chipSortDistance.setOnClickListener(listener);
        chipFilterRating.setOnClickListener(listener);
        chipFilterWellKnown.setOnClickListener(listener);
        chipFilterOpenNow.setOnClickListener(listener);
        setGoogleFilterChipsEnabled(false);
    }

    private void setupMapView() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        // Default center (India)
        mapView.getController().setCenter(new GeoPoint(20.5937, 78.9629));
    }

    private void ensurePermissionsAndLoad() {
        boolean fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fineGranted || coarseGranted) {
            fetchLocationAndClinics();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchLocationAndClinics() {
        showLoading(true);
        CancellationTokenSource tokenSource = new CancellationTokenSource();

        boolean fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            loadClinicsUsingDefaultLocation("Location permission unavailable. Showing clinics near a default area.");
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        searchCenterMarkerTitle = "You are here";
                        mapView.getController().animateTo(userGeoPoint);
                        addSearchCenterMarker(userGeoPoint, "You are here");
                        resolveSearchAreaAndFetchClinics(userGeoPoint);
                    } else {
                        loadClinicsUsingDefaultLocation("Using default location because GPS is unavailable.");
                    }
                })
                .addOnFailureListener(e -> loadClinicsUsingDefaultLocation("Location unavailable. Loaded nearby clinics from a default area."));
    }

    private void loadClinicsUsingDefaultLocation(String message) {
        showLoading(true);
        userGeoPoint = DEFAULT_SEARCH_POINT;
        searchCenterMarkerTitle = "Search center";
        mapView.getController().animateTo(userGeoPoint);
        addSearchCenterMarker(userGeoPoint, "Search center");
        if (message != null && !message.trim().isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        resolveSearchAreaAndFetchClinics(userGeoPoint);
    }

    private void resolveSearchAreaAndFetchClinics(GeoPoint point) {
        searchAreaName = "";
        geocoderExecutor.execute(() -> {
            String areaName = reverseGeocodeAreaName(point);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                searchAreaName = areaName;
                fetchClinicData();
            });
        });
    }

    @SuppressWarnings("deprecation")
    private String reverseGeocodeAreaName(GeoPoint point) {
        if (!Geocoder.isPresent()) return "";

        try {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(point.getLatitude(), point.getLongitude(), 1);
            if (addresses == null || addresses.isEmpty()) return "";

            Address address = addresses.get(0);
            String area = firstNonEmpty(
                    address.getSubLocality(),
                    address.getLocality(),
                    address.getSubAdminArea(),
                    address.getAdminArea()
            );
            String city = firstNonEmpty(
                    address.getLocality(),
                    address.getSubAdminArea(),
                    address.getAdminArea()
            );

            if (!isBlank(area) && !isBlank(city) && !area.equalsIgnoreCase(city)) {
                return area + ", " + city;
            }
            return area;
        } catch (IOException | IllegalArgumentException e) {
            return "";
        }
    }

    private void fetchClinicData() {
        String googleApiKey = getGoogleMapsApiKey();
        if (!isBlank(googleApiKey)) {
            setGoogleFilterChipsEnabled(false);
            fetchGoogleClinicData(googleApiKey);
            return;
        }

        googleRatingDataLoaded = false;
        setGoogleFilterChipsEnabled(false);
        fetchOsmClinicData();
    }

    private void fetchGoogleClinicData(String googleApiKey) {
        String url = String.format(Locale.US,
                "%s?location=%.6f,%.6f&radius=%d&keyword=%s&key=%s",
                PLACES_ENDPOINT,
                userGeoPoint.getLatitude(),
                userGeoPoint.getLongitude(),
                SEARCH_RADIUS_METERS,
                Uri.encode("gynecologist clinic hospital"),
                Uri.encode(googleApiKey));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (parseGooglePlacesResponse(response)) {
                        showLoading(false);
                    } else {
                        setGoogleFilterChipsEnabled(false);
                        fetchOsmClinicData();
                    }
                },
                error -> {
                    googleRatingDataLoaded = false;
                    setGoogleFilterChipsEnabled(false);
                    fetchOsmClinicData();
                });

        request.setRetryPolicy(new DefaultRetryPolicy(20000, 1, 1.0f));
        request.setTag(REQUEST_TAG);
        requestQueue.add(request);
    }

    private boolean parseGooglePlacesResponse(JSONObject response) {
        String status = response.optString("status", "");
        if ("ZERO_RESULTS".equals(status)) {
            googleRatingDataLoaded = false;
            return false;
        }

        if (!"OK".equals(status)) {
            googleRatingDataLoaded = false;
            if (!isBlank(status)) {
                Toast.makeText(this, buildGooglePlacesIssue(response, status), Toast.LENGTH_LONG).show();
            }
            return false;
        }

        JSONArray results = response.optJSONArray("results");
        googleRatingDataLoaded = true;
        setGoogleFilterChipsEnabled(true);
        rawClinics.clear();

        if (results != null) {
            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.optJSONObject(i);
                if (place == null) continue;

                String businessStatus = place.optString("business_status", "");
                if ("CLOSED_PERMANENTLY".equals(businessStatus)) continue;

                JSONObject geometry = place.optJSONObject("geometry");
                JSONObject location = geometry != null ? geometry.optJSONObject("location") : null;
                if (location == null) continue;

                double lat = location.optDouble("lat", Double.NaN);
                double lon = location.optDouble("lng", Double.NaN);
                if (Double.isNaN(lat) || Double.isNaN(lon)) continue;

                float rating = (float) place.optDouble("rating", 0);
                int reviewCount = place.optInt("user_ratings_total", 0);
                JSONObject openingHours = place.optJSONObject("opening_hours");
                boolean openStatusKnown = openingHours != null && openingHours.has("open_now");

                Clinic clinic = new Clinic();
                clinic.setPlaceId(place.optString("place_id", ""));
                clinic.setName(place.optString("name", getString(R.string.unknown_clinic)));
                clinic.setAddress(firstNonEmpty(
                        place.optString("vicinity", ""),
                        place.optString("formatted_address", ""),
                        searchAreaName,
                        getString(R.string.address_unavailable)
                ));
                clinic.setRating(rating);
                clinic.setReviewCount(reviewCount);
                clinic.setWellKnown(rating >= MIN_GOOGLE_RATING && reviewCount >= MIN_WELL_KNOWN_REVIEW_COUNT);
                clinic.setOpenStatusKnown(openStatusKnown);
                clinic.setOpenNow(openStatusKnown && openingHours.optBoolean("open_now", false));
                clinic.setLatitude(lat);
                clinic.setLongitude(lon);
                clinic.setDistanceKm(calculateDistanceKm(lat, lon));

                rawClinics.add(clinic);
            }
        }

        applyClinicFilters();
        return true;
    }

    private void fetchOsmClinicData() {
        // Broad search query for medical amenities in suburban areas
        String query = String.format(Locale.US,
                "[out:json][timeout:25];(node[\"amenity\"~\"hospital|clinic|doctors\"](around:%d,%.6f,%.6f);way[\"amenity\"~\"hospital|clinic|doctors\"](around:%d,%.6f,%.6f););out center;",
                SEARCH_RADIUS_METERS, userGeoPoint.getLatitude(), userGeoPoint.getLongitude(),
                SEARCH_RADIUS_METERS, userGeoPoint.getLatitude(), userGeoPoint.getLongitude());

        tryRequest(query, 0);
    }

    private void tryRequest(String query, int endpointIndex) {
        if (endpointIndex >= OVERPASS_ENDPOINTS.length) {
            showLoading(false);
            showEmptyState("Servers busy. Please try again in 1 minute.");
            return;
        }

        String url = OVERPASS_ENDPOINTS[endpointIndex] + "?data=" + Uri.encode(query);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    parseOsmResponse(response);
                    showLoading(false);
                },
                error -> tryRequest(query, endpointIndex + 1));

        // High timeout (20s) to handle slower Indian 4G/LTE mobile data
        request.setRetryPolicy(new DefaultRetryPolicy(20000, 1, 1.0f));
        request.setTag(REQUEST_TAG);
        requestQueue.add(request);
    }

    private void parseOsmResponse(JSONObject response) {
        JSONArray elements = response.optJSONArray("elements");
        googleRatingDataLoaded = false;
        setGoogleFilterChipsEnabled(false);
        rawClinics.clear();

        if (elements != null && elements.length() > 0) {
            for (int i = 0; i < elements.length(); i++) {
                JSONObject obj = elements.optJSONObject(i);
                if (obj == null) continue;

                JSONObject tags = obj.optJSONObject("tags");

                // Ways in OSM use a "center" object; nodes use "lat/lon".
                double lat = obj.optDouble("lat", Double.NaN);
                double lon = obj.optDouble("lon", Double.NaN);

                if (Double.isNaN(lat) || Double.isNaN(lon)) {
                    JSONObject center = obj.optJSONObject("center");
                    if (center != null) {
                        lat = center.optDouble("lat");
                        lon = center.optDouble("lon");
                    }
                }

                // Skip if we still do not have valid coordinates.
                if (Double.isNaN(lat) || lat == 0) continue;

                Clinic c = new Clinic();
                c.setName(tags != null ? tags.optString("name", "Medical Center") : "Medical Center");
                c.setAddress(buildClinicAddress(tags));

                c.setLatitude(lat);
                c.setLongitude(lon);
                c.setRating(0f);
                c.setReviewCount(0);
                c.setOpenStatusKnown(false);
                c.setOpenNow(false);
                c.setWellKnown(false);
                c.setDistanceKm(calculateDistanceKm(lat, lon));

                rawClinics.add(c);
            }
        }

        applyClinicFilters();
    }

    private void applyClinicFilters() {
        boolean sortByDistance = chipSortDistance == null || chipSortDistance.isChecked();
        boolean requireRating = chipFilterRating != null && chipFilterRating.isChecked();
        boolean requireWellKnown = chipFilterWellKnown != null && chipFilterWellKnown.isChecked();
        boolean requireOpenNow = chipFilterOpenNow != null && chipFilterOpenNow.isChecked();
        if (!googleRatingDataLoaded) {
            requireRating = false;
            requireWellKnown = false;
            requireOpenNow = false;
        }

        List<Clinic> filteredClinics = new ArrayList<>();
        for (Clinic clinic : rawClinics) {
            if (requireRating && clinic.getRating() < MIN_GOOGLE_RATING) continue;
            if (requireWellKnown && !clinic.isWellKnown()) continue;
            if (requireOpenNow && (!clinic.isOpenStatusKnown() || !clinic.isOpenNow())) continue;
            filteredClinics.add(clinic);
        }

        if (sortByDistance) {
            filteredClinics.sort(this::compareByDistanceThenRating);
        } else {
            filteredClinics.sort(this::compareByRatingThenDistance);
        }

        clinicAdapter.submitList(filteredClinics);
        renderClinicMarkers(filteredClinics);

        if (rawClinics.isEmpty()) {
            showEmptyState("No clinics found within 10km.");
        } else if (filteredClinics.isEmpty()) {
            if ((requireRating || requireWellKnown || requireOpenNow) && !googleRatingDataLoaded) {
                showEmptyState(getString(R.string.rating_filter_unavailable));
            } else {
                showEmptyState(getString(R.string.filtered_no_results));
            }
        } else {
            showEmptyState(null);
        }
    }

    private int compareByDistanceThenRating(Clinic first, Clinic second) {
        int distanceCompare = Float.compare(first.getDistanceKm(), second.getDistanceKm());
        if (distanceCompare != 0) return distanceCompare;
        return compareByRatingThenDistance(first, second);
    }

    private int compareByRatingThenDistance(Clinic first, Clinic second) {
        int ratingCompare = Float.compare(second.getRating(), first.getRating());
        if (ratingCompare != 0) return ratingCompare;

        int reviewCompare = Integer.compare(second.getReviewCount(), first.getReviewCount());
        if (reviewCompare != 0) return reviewCompare;

        return Float.compare(first.getDistanceKm(), second.getDistanceKm());
    }

    private void renderClinicMarkers(List<Clinic> clinics) {
        mapView.getOverlays().clear();
        if (userGeoPoint != null) {
            addSearchCenterMarker(userGeoPoint, searchCenterMarkerTitle);
        }

        for (Clinic clinic : clinics) {
            addClinicMarker(clinic);
        }
        mapView.invalidate();
    }

    private void addClinicMarker(Clinic clinic) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(clinic.getLatitude(), clinic.getLongitude()));
        marker.setTitle(clinic.getName());
        if (clinic.getRating() > 0 && clinic.getReviewCount() > 0) {
            marker.setSnippet(getString(R.string.map_snippet_format, clinic.getRating(), clinic.getReviewCount()));
        }
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        Drawable icon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default);
        if (icon != null) {
            icon.setTint(0xFFD32F2F);
            marker.setIcon(icon);
        }
        mapView.getOverlays().add(marker);
    }

    private float calculateDistanceKm(double lat, double lon) {
        float[] distResults = new float[1];
        Location.distanceBetween(userGeoPoint.getLatitude(), userGeoPoint.getLongitude(), lat, lon, distResults);
        return distResults[0] / 1000f;
    }

    private String getGoogleMapsApiKey() {
        if (!isBlank(BuildConfig.MAPS_API_KEY)) {
            return BuildConfig.MAPS_API_KEY.trim();
        }

        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = appInfo.metaData;
            if (metaData == null) return "";

            Object apiKeyValue = metaData.get(GOOGLE_MAPS_API_KEY_META_DATA);
            if (apiKeyValue == null) return "";

            String apiKey = String.valueOf(apiKeyValue).trim();
            if (apiKey.startsWith("$") || apiKey.contains("MAPS_API_KEY")) return "";
            return apiKey;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private String buildGooglePlacesIssue(JSONObject response, String status) {
        String errorMessage = response.optString("error_message", "");
        if (isBlank(errorMessage)) {
            return getString(R.string.error_places_status, status);
        }

        return getString(R.string.error_places_status_with_message, status, errorMessage);
    }

    private void setGoogleFilterChipsEnabled(boolean enabled) {
        setGoogleFilterChipEnabled(chipFilterRating, enabled);
        setGoogleFilterChipEnabled(chipFilterWellKnown, enabled);
        setGoogleFilterChipEnabled(chipFilterOpenNow, enabled);
    }

    private void setGoogleFilterChipEnabled(Chip chip, boolean enabled) {
        if (chip == null) return;
        if (!enabled) chip.setChecked(false);
        chip.setEnabled(enabled);
        chip.setAlpha(enabled ? 1f : 0.55f);
    }

    private String buildClinicAddress(JSONObject tags) {
        String fullAddress = getTagValue(tags, "addr:full");
        if (!isBlank(fullAddress)) return fullAddress;

        List<String> addressParts = new ArrayList<>();
        addAddressPart(addressParts, getTagValue(tags, "addr:housenumber"));
        addAddressPart(addressParts, getTagValue(tags, "addr:street"));
        addAddressPart(addressParts, getTagValue(tags, "addr:neighbourhood"));
        addAddressPart(addressParts, getTagValue(tags, "addr:suburb"));
        addAddressPart(addressParts, getTagValue(tags, "addr:place"));
        addAddressPart(addressParts, getTagValue(tags, "addr:city"));
        addAddressPart(addressParts, getTagValue(tags, "addr:town"));
        addAddressPart(addressParts, getTagValue(tags, "addr:village"));
        addAddressPart(addressParts, getTagValue(tags, "addr:district"));

        if (!addressParts.isEmpty()) {
            return TextUtils.join(", ", addressParts);
        }

        if (!isBlank(searchAreaName)) {
            return searchAreaName;
        }

        return "Nearby area";
    }

    private String getTagValue(JSONObject tags, String key) {
        return tags != null ? tags.optString(key, "").trim() : "";
    }

    private void addAddressPart(List<String> addressParts, String value) {
        if (isBlank(value)) return;

        String trimmedValue = value.trim();
        for (String existingPart : addressParts) {
            if (existingPart.equalsIgnoreCase(trimmedValue)) return;
        }
        addressParts.add(trimmedValue);
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void addSearchCenterMarker(GeoPoint point, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.person));
        mapView.getOverlays().add(marker);
    }

    private void showLoading(boolean loading) {
        progressClinics.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String msg) {
        if (msg == null) {
            tvEmptyState.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(msg);
        }
    }

    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }

    @Override
    protected void onPause() {
        requestQueue.cancelAll(REQUEST_TAG);
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        geocoderExecutor.shutdownNow();
        mapView.onDetach();
        super.onDestroy();
    }
}
