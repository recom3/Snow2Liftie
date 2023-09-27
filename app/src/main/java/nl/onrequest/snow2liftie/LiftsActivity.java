package nl.onrequest.snow2liftie;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.os.connectivity.IHUDConnectivity;
import com.reconinstruments.ui.carousel.CarouselItem;
import com.reconinstruments.ui.dialog.CarouselDialog;
import com.reconinstruments.ui.dialog.DialogBuilder;
import com.reconinstruments.ui.list.ReconListView;
import com.reconinstruments.ui.list.SimpleListActivity;
import com.reconinstruments.ui.view.ButtonActionView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LiftsActivity extends SimpleListActivity implements LocationListener,IHUDConnectivity{

    private int ResortSelection = 0;
    private Handler handler;
    private LocationManager locationManager;
    private Location location;
    private static NotificationManager mNotificationManager;
    private int refresh_rate;
    private ResponseReceiver receiver;
    private Animation animRotate;
    private IntentFilter filter;
    //!!!set to true
    private boolean hasNetworkAccess = true;

    Runnable runnable = new Runnable()
    {
        @Override
        public void run() {
            if (hasNetworkAccess) {
                TextView v = (TextView) findViewById(R.id.update);
                if (v != null) {
                    v.setText("--:--");
                }
                ImageView im = (ImageView) findViewById(R.id.img_refresh);
                if (im != null) {
                    im.startAnimation(animRotate);
                }
                LiftieDataService.startActionGetLiftData(LiftsActivity.this);
            } else {
                ShowWarningNotification("No connection", getApplicationContext());
            }
        }
    };

    @Override
    public void onLocationChanged(Location l) {
        location = l;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public class ResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP = "nl.onrequest.snow2liftie.action.DATA_LOADED";

        @Override
        public void onReceive(Context context, Intent intent) {
            ImageView im = (ImageView) findViewById(R.id.img_refresh);
            if (im != null) {
                im.clearAnimation();
            }
            String result = intent.getStringExtra(LiftieDataService.RESULT_LIFTDATA);
            Object json;
            if (result.equals("")) {
                ShowWarningNotification("No data found", getApplicationContext());
                return;
            }
            if (result.equals("NO_CONNECTION")) {
                ShowWarningNotification("No connection", getApplicationContext());
                return;
            }
            try {
                json = new JSONTokener(result).nextValue();
                if (json instanceof JSONObject) {
                    Helper.getInstance().SaveLiftDataToFile(result);
                    SimpleDateFormat d = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String dateString = d.format(new Date(Helper.getInstance().getLastUpdate()));
                    TextView v = (TextView) findViewById(R.id.update);
                    if (v != null) {
                        v.setText(dateString);
                    }
                    if (Helper.getInstance().lifts.length == 0) {
                        ShowWarningNotification("No lifts found", getApplicationContext());
                    } else {
                        setContents(Helper.getInstance().lifts);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            handler.postDelayed(runnable, refresh_rate * 1000 * 60);
        }
    }

    public static void ShowWarningNotification(String warning, Context context) {
        Notification notification = new Notification.Builder(context)
                .setContentTitle(warning)
                .setSmallIcon(R.drawable.warning_icon)
                .setContentText(warning)
                .build();
        mNotificationManager.notify(0, notification);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        animRotate =  AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.POWER_LOW);
        location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria,false));
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        refresh_rate = ((Helper.CheckedRefreshRateSelectionItem) Helper.getInstance().refreshrates[Helper.getInstance().getRefreshRate()]).value;
        receiver = new ResponseReceiver();
        filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(receiver, filter);
        //This is not implemented yet
        //Helper.getInstance().mHUDConnectivityManager.register(this);
    }

    public void SetContent() {
        if (Helper.getInstance().current_resort.getPath().equals("")) {
            setContentView(R.layout.no_resort);
            ButtonActionView btnSelect = (ButtonActionView) findViewById(R.id.select_resort);
            btnSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CreateResortsDialog();
                }
            });
        } else {
            setContentView(R.layout.activity_lifts);
            mListView = (ReconListView) findViewById(android.R.id.list);
            mListView.setClickable(false);
            TextView r = (TextView) findViewById(R.id.title);
            if (r != null) {
                r.setText(Helper.getInstance().current_resort.getTitle());
            }
            TextView v = (TextView) findViewById(R.id.update);
            if (v != null) {
                v.setText("--:--");
            }
            if (hasNetworkAccess) {
                ImageView im = (ImageView) findViewById(R.id.img_refresh);
                if (im != null) {
                    im.startAnimation(animRotate);
                }
                LiftieDataService.startActionGetLiftData(LiftsActivity.this);
            } else {
                ShowWarningNotification("No connection", getApplicationContext());
            }
        }
    }

    private void CreateResortsDialog() {
        CarouselItem[] resorts = Helper.getInstance().resorts;
        List<CarouselItem> resorts_list = new ArrayList<CarouselItem>();
        final List<CarouselItem> finalResorts;
        Boolean found = false;
        int range = ((Helper.CheckedRangeSelectionItem) Helper.getInstance().ranges[Helper.getInstance().getRange()]).value;
        if (location != null && range > 0) {
            for (CarouselItem resort : resorts) {
                Helper.CheckedResortSelectionItem r = (Helper.CheckedResortSelectionItem) resort;
                Location resort_loc = new Location("");
                resort_loc.setLatitude(r.location.getLatitude());
                resort_loc.setLongitude(r.location.getLongitude());
                float distanceInMeters = resort_loc.distanceTo(location);
                if (distanceInMeters < range*1000) {
                    resorts_list.add(resort);
                    if (r.getPath().equals(Helper.getInstance().current_resort.getPath())) {
                        found = true;
                    }
                }
            }
            if (!found) {
                resorts_list.add(resorts[ResortSelection]);
            }
        } else {
            //no location or show all
            resorts_list = Arrays.asList(resorts);
        }
        finalResorts = resorts_list;
        DialogBuilder builder = new DialogBuilder(this).setTitle("Select resort");
        CarouselDialog selectionDialog = builder.createSelectionDialog(finalResorts, ResortSelection, new CarouselDialog.OnItemSelectedListener() {
            @Override
            public void onItemSelected(CarouselDialog dialog, CarouselItem item, int position) {
                ResortSelection = position;
                Helper.CheckedResortSelectionItem r = (Helper.CheckedResortSelectionItem) finalResorts.get(ResortSelection);
                Helper.getInstance().setSavedResort(r.getPath());
                Helper.getInstance().current_resort = r;
                dialog.dismiss();
            }
        });
        selectionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                SetContent();
            }
        });
        selectionDialog.show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT :
            {
                handler.removeCallbacks(runnable);
                startActivity(new Intent(LiftsActivity.this, SettingsActivity.class));
                break;
            }
            case KeyEvent.KEYCODE_DPAD_CENTER:
            {
                handler.removeCallbacks(runnable);
                CreateResortsDialog();
                break;
            }
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_BACK:
            {
                finish();
                break;
            }
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(receiver, filter);
        SetContent();
    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        //This is not implemented yet
        //Helper.getInstance().mHUDConnectivityManager.unregister(this);
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onDeviceName(String s) {

    }

    @Override
    public void onConnectionStateChanged(ConnectionState connectionState) {

    }

    @Override
    public void onNetworkEvent(NetworkEvent networkEvent, boolean b) {
        this.hasNetworkAccess = b;
    }
}
