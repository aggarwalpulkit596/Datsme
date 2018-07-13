package me.dats.com.datsme.Fragments;


import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import me.dats.com.datsme.Activities.ClusterRender;
import me.dats.com.datsme.Activities.MyItem;
import me.dats.com.datsme.Adapters.BubbleTransformation;
import me.dats.com.datsme.Adapters.SpacesItemDecoration;
import me.dats.com.datsme.Models.Users;
import me.dats.com.datsme.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class Discover_people extends Fragment implements OnMapReadyCallback {

    private ClusterManager<MyItem> mClusterManager;

    @BindView(R.id.toggle_profile_button)
    ImageButton toggle_profile_button;

    @BindView(R.id.profile_box)
    RelativeLayout Profile_box;

    public float Timer = 0;
    public CountDownTimer countDownTimer;
    private Animation animShow, animHide;

    boolean IsProfileVisible = false;

    @BindView(R.id.rv)
    RecyclerView mRecyclerView;
    private DatabaseReference mUserRef;

    private GoogleMap mMap;

    FusedLocationProviderClient mFusedLocationProviderClient;
    LocationCallback mLocationCallback;
    LocationRequest mLocationRequest;
    boolean mRequestingLocationUpdates;
    android.location.Location location;
    HashMap<String, LatLng> userMap = new HashMap<>();
    HashMap<String, Marker> markers = new HashMap<>();
    HashMap<String, Target> targets = new HashMap<>();
    int[][] worldview = new int[][]{
            {100, 50, 25, 50, 75},
            {50, 100, 25, 50, 75},
            {25, 25, 100, 50, 50},
            {30, 50, 50, 100, 50},
            {75, 75, 50, 50, 100}
    };
    Users mUser;

    float zoomLevel = 1.0f; //This goes up to 21


    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    public Discover_people() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_discover_people, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);


        mapFragment.getMapAsync(this);
        fetchusers();
        initAnimation();
        fetchlocation();
        toggle_profile_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (IsProfileVisible) {
                    IsProfileVisible = false;
                    Profile_box.startAnimation(animHide);
                    toggle_profile_button.setImageResource(R.drawable.ic_expand_more_black_24dp);
                    Profile_box.setVisibility(View.INVISIBLE);
                } else {
                    if (Timer > 0) {
                        countDownTimer.cancel();
                    }
                    IsProfileVisible = true;
                    toggle_profile_button.setImageResource(R.drawable.ic_expand_less_black_24dp);
                    Profile_box.setVisibility(View.VISIBLE);
                    Profile_box.startAnimation(animShow);
                }
            }
        });

        mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        mUserRef.keepSynced(true);
        mLocationCallback = new LocationCallback() {
            boolean firstlauch = true;

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                location = locationResult.getLastLocation();
                Map<String, Object> locationMap = new HashMap<>();
                locationMap.put("lattitude", location.getLatitude());
                locationMap.put("longitude", location.getLongitude());
                mUserRef.updateChildren(locationMap);
                if (firstlauch) {
                    firstlauch = false;
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), zoomLevel));
                }
            }
        };

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //to set max zoom and remove navigation button
//        mMap.setMaxZoomPreference(15.0f);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);


        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mFusedLocationProviderClient
                .requestLocationUpdates(mLocationRequest,
                        mLocationCallback, null);
        mRequestingLocationUpdates = true;

        //for changing design of map
        try {
            Calendar calendar = Calendar.getInstance();
            int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            if (hourOfDay > 5 && hourOfDay < 21) {
                boolean success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mymapstyle));
            } else {
                boolean success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mymapsnight));
            }
        } catch (Resources.NotFoundException e) {
            // Oops, looks like the map style resource couldn't be found!
        }


        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if (Timer > 0) {
                    countDownTimer.cancel();
                }
                zoomLevel++;
                if (IsProfileVisible) {
                    IsProfileVisible = false;
                    Profile_box.startAnimation(animHide);
                    toggle_profile_button.setImageResource(R.drawable.ic_expand_more_black_24dp);
                    Profile_box.setVisibility(View.INVISIBLE);
                }
            }

        });
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {

                if (Timer > 0) {
                    countDownTimer.cancel();
                }
                countDownTimer = new CountDownTimer(2000, 1000) {
                    @Override
                    public void onTick(long l) {
                        Timer = l / 1000;
                    }

                    @Override
                    public void onFinish() {
                        IsProfileVisible = true;
                        toggle_profile_button.setImageResource(R.drawable.ic_expand_less_black_24dp);
                        Profile_box.setVisibility(View.VISIBLE);
                        Profile_box.startAnimation(animShow);
                    }
                }.start();
            }
        });
        setMarkers();
        setUpClusterer();
    }

    private void setMarkers() {

        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users");
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull final DataSnapshot dataSnapshot, @Nullable String s) {

                mUser = dataSnapshot.getValue(Users.class);

                final String user_id = dataSnapshot.getKey();

                final LatLng latLng1 = new LatLng(mUser.getLattitude(), mUser.getLongitude());
                final MyItem myItem = new MyItem(mUser.getLattitude(), mUser.getLongitude(), mUser.getName(), dataSnapshot.getKey(),mUser.thumb_image);
                mClusterManager.addItem(myItem);
                mClusterManager.setRenderer(new ClusterRender(getActivity(),mMap,mClusterManager));
                mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MyItem>() {
                    @Override
                    public boolean onClusterClick(Cluster<MyItem> cluster) {

                        Log.d("Zoom", "onClusterClick: camerazoom"+mMap.getCameraPosition().zoom);
                        Log.d("Zoom", "onClusterClick: maxzoom"+mMap.getMaxZoomLevel());
                        if(mMap.getCameraPosition().zoom<mMap.getMaxZoomLevel()) {
                            zoomLevel++;
                            if(zoomLevel==21.0f)
                            {
                                zoomLevel=17.0f;
                            }
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cluster.getPosition(), zoomLevel));
                        }

                        return true;
                    }
                });
                mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MyItem>() {
                    @Override
                    public boolean onClusterItemClick(MyItem myItem) {
                        BottomSheetProfileFragment bottomSheetFragment = new BottomSheetProfileFragment();
                        BottomSheetProfileFragment.newInstance(myItem.getSnippet()).show(getActivity().getSupportFragmentManager(), bottomSheetFragment.getTag());
                        return true;
                    }
                });
//                targets.put(user_id, new Target() {
//                    @Override
//                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                        LatLng uid = userMap.get(dataSnapshot.getKey());
//                        if (uid == null) {


//                            Marker marker = mMap.addMarker(new MarkerOptions()
//                                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
//                                    .position(latLng1)
//                                    .title(user_id).snippet(user_id));
//                            markers.put(dataSnapshot.getKey(), marker);
//                            userMap.put(dataSnapshot.getKey(), latLng1);
//                            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//                                @Override
//                                public boolean onMarkerClick(Marker marker) {
//                                    BottomSheetProfileFragment bottomSheetFragment = new BottomSheetProfileFragment();
//                                    BottomSheetProfileFragment.newInstance(marker.getSnippet()).show(getActivity().getSupportFragmentManager(), bottomSheetFragment.getTag());
//                                    return true;
//                                }
//                            });

//                        }
//                    }
//
//                    @Override
//                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
//
//                    }
//
//                    @Override
//                    public void onPrepareLoad(Drawable placeHolderDrawable) {
//
//                    }
//                });
//
//                Picasso.get().load(mUser.getThumb_image()).resize(150, 150)
//                        .centerInside()
//                        .transform(new BubbleTransformation(10))
//                        .into(targets.get(user_id));

            }

            @Override
            public void onChildChanged(@NonNull final DataSnapshot dataSnapshot, @Nullable String s) {
                mUser = dataSnapshot.getValue(Users.class);
                final String user_id = dataSnapshot.getKey();
                final LatLng latLng1 = new LatLng(mUser.getLattitude(), mUser.getLongitude());
                targets.put(user_id, new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        LatLng uid = userMap.get(dataSnapshot.getKey());
                        Marker marker = markers.get(dataSnapshot.getKey());
                       // if (marker != null) marker.remove();
//                        marker = mMap.addMarker(new MarkerOptions()
//                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
//                                .position(latLng1)
//                                .title(user_id).snippet(user_id));
//                        markers.put(dataSnapshot.getKey(), marker);
//                        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//                            @Override
//                            public boolean onMarkerClick(Marker marker) {
//                                BottomSheetProfileFragment bottomSheetFragment = new BottomSheetProfileFragment();
//                                BottomSheetProfileFragment.newInstance(marker.getSnippet()).show(getActivity().getSupportFragmentManager(), bottomSheetFragment.getTag());
//                                return true;
//                            }
//                        });

                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {


                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });

                Picasso.get().load(mUser.getThumb_image()).resize(150, 150)
                        .centerInside()
                        .transform(new BubbleTransformation(10))
                        .into(targets.get(user_id));
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void initAnimation() {
        animShow = AnimationUtils.loadAnimation(getActivity(), R.anim.show_from_side);
        animHide = AnimationUtils.loadAnimation(getActivity(), R.anim.hide_from_side);
    }

    private void fetchlocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API).build();
        googleApiClient.connect();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                        try {
                            status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

                        break;
                }
            }
        });

    }

    private void fetchusers() {
        mRecyclerView.setHasFixedSize(true);
        int spacingInPixels = 10;
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        mRecyclerView.setItemViewCacheSize(10);


        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users");
        final FirebaseRecyclerOptions<Users> options =
                new FirebaseRecyclerOptions.Builder<Users>()
                        .setQuery(query, Users.class)
                        .build();

        FirebaseRecyclerAdapter firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Users, UsersViewHolder>(options) {
            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                return new UsersViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.user_layout, parent, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull final UsersViewHolder holder, final int position, @NonNull final Users model) {
                holder.bind(model);
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        BottomSheetProfileFragment bottomSheetFragment = new BottomSheetProfileFragment();
                        BottomSheetProfileFragment.newInstance(getRef(position).getKey()).show(getActivity().getSupportFragmentManager(), bottomSheetFragment.getTag());

                    }
                });

            }
        };
        mRecyclerView.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();

    }



    public static class UsersViewHolder extends RecyclerView.ViewHolder {

        View mView;

        UsersViewHolder(final View itemView) {
            super(itemView);
            mView = itemView;

        }

        public void setName(String name) {
            TextView userNameView = mView.findViewById(R.id.name);
            userNameView.setText(name);
        }

        void bind(Users model) {
            String name1 = model.getName();
            String[] arr = name1.split(" ");
            String fname = arr[0];
            setName(fname);
            setThumbImage(model.getThumb_image());
        }

        void setThumbImage(String thumbImage) {
            CircleImageView userImageView = mView.findViewById(R.id.image);
            if (!thumbImage.equals("default"))
                Picasso.get()
                        .load(thumbImage)
                        .placeholder(R.drawable.default_avatar)
                        .into(userImageView);

        }

    }

    private void setUpClusterer() {
        mClusterManager = new ClusterManager<MyItem>(getActivity(), mMap);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
    }
}
