package nl.onrequest.snow2liftie;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.view.View;

import com.reconinstruments.os.connectivity.HUDConnectivityManager;
import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;
import com.reconinstruments.ui.carousel.CarouselItem;
import com.reconinstruments.ui.carousel.StandardCarouselItem;
import com.reconinstruments.ui.list.StandardListItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Iterator;

public class Helper {
    private static String resorts_file = "resorts.json";
    public HUDConnectivityManager mHUDConnectivityManager;
    private static SharedPreferences sharedPreferences;
    private NotificationManager mNotificationManager;
    private Activity activity;
    public CarouselItem[] resorts = null;
    public CheckedResortSelectionItem current_resort;
    public StandardListItem[] lifts = null;
    private static final Helper holder = new Helper();
    public static Helper getInstance() {return holder;}

    public CarouselItem[] refreshrates = {
            new CheckedRefreshRateSelectionItem("1 min",1),
            new CheckedRefreshRateSelectionItem("5 mins",5),
            new CheckedRefreshRateSelectionItem("10 mins",10),
            new CheckedRefreshRateSelectionItem("15 mins",15),
            new CheckedRefreshRateSelectionItem("30 mins",30)
    };

    public CarouselItem[] ranges = {
            new CheckedRangeSelectionItem("show all",0),
            new CheckedRangeSelectionItem("100 km",100),
            new CheckedRangeSelectionItem("300 km",300),
            new CheckedRangeSelectionItem("500 km",500),
            new CheckedRangeSelectionItem("1000 km",1000)
    };

    public class CheckedRefreshRateSelectionItem extends StandardCarouselItem {
        int value;
        public CheckedRefreshRateSelectionItem(String title,int value) {
            super(title);
            this.value = value;
        }

        @Override
        public void updateView(View view) {
            super.updateView(view);
            view.findViewById(R.id.checkmark).setVisibility(value==((CheckedRefreshRateSelectionItem) refreshrates[Helper.getInstance().getRefreshRate()]).value?View.VISIBLE:View.INVISIBLE);
        }

        @Override
        public int getLayoutId() {
            return R.layout.carousel_item_checkmark;
        }

    }
    public class CheckedRangeSelectionItem extends StandardCarouselItem {
        int value;

        public CheckedRangeSelectionItem(String title, int value) {
            super(title);
            this.value = value;
        }

        @Override
        public void updateView(View view) {
            super.updateView(view);
            view.findViewById(R.id.checkmark).setVisibility(value == ((CheckedRangeSelectionItem) ranges[Helper.getInstance().getRange()]).value ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public int getLayoutId() {
            return R.layout.carousel_item_checkmark;
        }

    }

    public void ShowDisconnectedNotification() {
        mNotificationManager = (NotificationManager) this.activity.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(this.activity.getApplicationContext())
                .setContentTitle("No connection")
                .setContentText("No connection")
                .build();
        mNotificationManager.notify(0, notification);
    }

    public void setActivity(Activity a) {
        activity = a;
    }
    public void setSharedPreferences(SharedPreferences s) {
        sharedPreferences = s;
    }
    public void setHUDConnectivityManager(HUDConnectivityManager manager) {
        mHUDConnectivityManager = manager;
    }
    public class CheckedResortSelectionItem extends StandardCarouselItem {
        String value;
        Location location;
        public CheckedResortSelectionItem(String title,String value, Location loc) {
            super(title);
            this.value = value;
            this.location = loc;
        }

        public  String getPath() {
            return this.value;
        }

        public Location getLocation() {
            return this.location;
        }

        @Override
        public void updateView(View view) {
            super.updateView(view);
            view.findViewById(R.id.checkmark).setVisibility(value.equals(current_resort.getPath()) ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public int getLayoutId() {
            return R.layout.carousel_item_checkmark;
        }

    }
    private class GetMetaData extends AsyncTask<Void, Void, String> {

        String mUrl;
        String mComment;

        public GetMetaData(String url) {
            mUrl = url;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result = "";
            try {
                //Get Request
                HUDHttpRequest request = new HUDHttpRequest(HUDHttpRequest.RequestMethod.GET, mUrl);
                HUDHttpResponse response = mHUDConnectivityManager.sendWebRequest(request);
                if (response.hasBody()) {
                    result = new String(response.getBody());
                }
            } catch (Exception e) {
                mComment = "failed to download file: " + e.getMessage();
                e.printStackTrace();
                return "";
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            Object json;
            try {
                json = new JSONTokener(result).nextValue();
                if (json instanceof JSONArray) {
                    //we have the resortdata, save to file
                    SaveResortDataToFile(result);
                } else if (json instanceof JSONObject) {
                    SaveLiftDataToFile(result);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    public void SaveLiftDataToFile(String data) {
        FileOutputStream fos;
        try {
            fos = activity.openFileOutput(current_resort.getPath()+".json", Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            fos.close();
            setLastUpdate(Calendar.getInstance().getTimeInMillis());
            lifts = this.Lifts();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private StandardListItem[] Lifts() throws Exception {
        lifts = new StandardListItem[0];
        if (!current_resort.getPath().equals("")) {
            File fl = new File(activity.getFilesDir()+"/"+current_resort.getPath()+".json");
            int i = 0;
            if (fl.exists()) {
                FileInputStream fin;
                fin = new FileInputStream(fl);
                String lifts_string = this.convertStreamToString(fin);
                fin.close();
                JSONObject obj = new JSONObject(lifts_string);
    //            double lat = (Double) obj.getJSONArray("ll").get(1);
    //            double lon = (Double) obj.getJSONArray("ll").get(0);
    //            resort_location.setLatitude(lat);
    //            resort_location.setLongitude(lon);
    //            resort_location.setLatitude(47.44225);
    //            resort_location.setLongitude(12.39000);
                JSONObject json_lifts = obj.getJSONObject("lifts").optJSONObject("status");
                lifts = new StandardListItem[json_lifts.length()];
                if (json_lifts.length() > 0) {
                    Iterator<?> keysItr = json_lifts.keys();
                    while (keysItr.hasNext()) {
                        String name = (String) keysItr.next();
                        String status = (String) json_lifts.get(name);
                        Integer icnStatus;
                        if (status.equals("open")) {
                            icnStatus = R.drawable.open;
                        } else if (status.equals("closed")) {
                            icnStatus = R.drawable.closed;
                        } else {
                            icnStatus = R.drawable.scheduled;
                        }
                        lifts[i] = new StandardListItem(name, null, null, icnStatus);
                        i++;
                    }
                }
            }
        }
        return lifts;
    }
    private void SaveResortDataToFile(String data) {
        FileOutputStream fos;
        try {
            fos = activity.openFileOutput(resorts_file, Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            fos.close();
            setLastRefresh(Calendar.getInstance().getTimeInMillis());
            resorts = this.Resorts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void UpdateResortFile() {
        try {
            resorts = this.Resorts();
            current_resort = this.getResort();
            lifts = this.Lifts();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long diff = Calendar.getInstance().getTimeInMillis() - getLastRefresh();
        String path = activity.getFilesDir()+"/"+resorts_file;
        File file = new File(path);
        //get resort file only once a day
        if (!file.exists() || diff >= (60000 * 60 * 24) ) {//60000 sec * 60 min * 24 hours = 1 day
            String url = "https://liftie.info/api/meta";
            new GetMetaData(url).execute();
        }
    }
    private CarouselItem[] Resorts() throws Exception {
        if (resorts == null) {
            //read the resort file
            File fl = new File(activity.getFilesDir()+"/"+resorts_file);
            if (fl.exists() ) {
                FileInputStream fin;
                    fin = new FileInputStream(fl);
                    String resort_string = this.convertStreamToString(fin);
                    fin.close();
                    JSONArray resorts_object = new JSONArray(resort_string);
                    resorts = new CarouselItem[resorts_object.length()];
                    for (int i = 0; i < resorts_object.length(); i++) {
                        String name = resorts_object.getJSONObject(i).getString("name");
                        String path = resorts_object.getJSONObject(i).getString("id");
                        double lat = (Double) resorts_object.getJSONObject(i).getJSONArray("ll").get(1);
                        double lon = (Double) resorts_object.getJSONObject(i).getJSONArray("ll").get(0);
                        Location resort_loc = new Location("");
                        resort_loc.setLatitude(lat);
                        resort_loc.setLongitude(lon);
                        resorts[i] = new CheckedResortSelectionItem(name,path,resort_loc);
                    }
            }
        }
        return resorts;
    }
    protected String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
    public int getRefreshRate()  {
        return sharedPreferences.getInt("refreshrate", 0);
    }
    public void setRefreshRate(int rate)  {
        sharedPreferences.edit().putInt("refreshrate", rate).apply();
    }
    public void setLastUpdate(long ct) {
        sharedPreferences.edit().putLong("last_update", ct).apply();
    }
    public long getLastUpdate() {
        return sharedPreferences.getLong("last_update", 0);
    }
    public long getLastRefresh() {
        return sharedPreferences.getLong("last_refresh", 0);
    }
    public void setLastRefresh(long lr) {
        sharedPreferences.edit().putLong("last_refresh", lr).apply();
    }
    public int getRange() {
        return sharedPreferences.getInt("range", 0);
    }
    public void setRange(int range) {
        sharedPreferences.edit().putInt("range", range).apply();
    }
    public String getSavedResort () {
        return sharedPreferences.getString("resort", "");
    }
    public void setSavedResort (String resort) {
        sharedPreferences.edit().putString("resort", resort).apply();
    }
    private CheckedResortSelectionItem getResort() {
        //get the resort as string, then get the list item since position can change
        String r = this.getSavedResort();
        CheckedResortSelectionItem found = new CheckedResortSelectionItem("","",null);
        if (this.resorts == null) {
            return found;
        }
        for (CarouselItem resort : this.resorts) {
            CheckedResortSelectionItem res = (CheckedResortSelectionItem) resort;
            if (res.getPath().equals(r)) {
                found = res;
                break;
            }
        }
        return found;
    }
}
