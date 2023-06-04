package nl.onrequest.snow2liftie;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.reconinstruments.os.HUDOS;
import com.reconinstruments.os.connectivity.HUDConnectivityManager;
import com.reconinstruments.ui.carousel.CarouselActivity;
import com.reconinstruments.ui.carousel.StandardCarouselItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;


public class MainActivity extends CarouselActivity {

    private static final String mac_file = "mac.json";

    HUDWebService mHUDWebService;

    public static boolean phoneConnected = false;
    public static String phoneAddress = "";

    static class ImageCarouselItem extends StandardCarouselItem {
        public ImageCarouselItem(String title, Integer icon) {
            super(title, icon);
        }
        @Override
        public int getLayoutId() {
            return R.layout.carousel_item_title_icon_column;
        }
        @Override
        public void onClick(Context context) {
            if (this.getTitle().equals("Lifts")) {
                context.startActivity(new Intent(context, LiftsActivity.class));
            }
            if (this.getTitle().equals("Settings")) {
                context.startActivity(new Intent(context, SettingsActivity.class));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Helper.getInstance().setActivity(this);
        Helper.getInstance().setSharedPreferences(this.getSharedPreferences("nl.onrequest.snow2liftie", Context.MODE_PRIVATE));
        Helper.getInstance().setHUDConnectivityManager((HUDConnectivityManager) HUDOS.getHUDService(HUDOS.HUD_CONNECTIVITY_SERVICE));
        Helper.getInstance().UpdateResortFile();
        setContentView(R.layout.activity_main);
        getCarousel().setPageMargin(30);
        getCarousel().setContents(
                new ImageCarouselItem("Lifts", R.drawable.carousel_icon_lifts),
                new ImageCarouselItem("Settings", R.drawable.carousel_icon_settings));

        //Recom3: disabled load of binary library
        //It is not abailable in old systems Snow2/Recon Jet
        //Thought available sometimes the sendWebRequest method is not
        /*
        System.load("/system/lib/libreconinstruments_jni.so");
        */

        //Recom3: instead a local HUDWebService is started
        Intent intent = new Intent(this, HUDWebService.class);
        bindService(intent, this.hudWebSrvConn, Context.BIND_AUTO_CREATE);

        //Recom3: receiver are register to hear to BT activity
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);

        //Recom3: load default mac address
        loadMacAddres();
    }

    private void loadMacAddres()
    {
        File fl = new File(this.getFilesDir()+"/"+mac_file);
        if (fl.exists() ) {
            FileInputStream fin;
            try {
                fin = new FileInputStream(fl);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
                StringBuilder sb = new StringBuilder();
                String line="";
                try {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);//
                    }
                    reader.close();
                    phoneAddress=sb.toString();
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private ServiceConnection hudWebSrvConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName param1ComponentName, IBinder param1IBinder) {

            Log.i("HUDWebService", "onServiceConnected");

            HUDWebService.LocalBinder binder = (HUDWebService.LocalBinder) param1IBinder;
            MainActivity.this.mHUDWebService = binder.getService();

            Helper.getInstance().setHUDConnectivityManager(MainActivity.this.mHUDWebService.hudConnectivityManager);
        }

        @Override
        public void onServiceDisconnected(ComponentName param1ComponentName) {
            MainActivity.this.mHUDWebService = null;
        }
    };
}
