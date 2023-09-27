package nl.onrequest.snow2liftie;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class LiftieDataService extends IntentService {
    private static final String ACTION_GET_LIFTDATA = "nl.onrequest.snow2liftie.action.GET_LIFTDATA";
    private static final String ACTION_GET_RESORTS = "nl.onrequest.snow2liftie.action.GET_RESORTS";
    public static final String RESULT_LIFTDATA = "";

    public LiftieDataService() {
        super("LiftieDataService");
    }

    /**
     * Starts this service to perform action GetLiftData with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionGetLiftData(Context context) {
        Intent intent = new Intent(context, LiftieDataService.class);
        intent.setAction(ACTION_GET_LIFTDATA);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action GetResorts with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionGetResorts(Context context) {
        Intent intent = new Intent(context, LiftieDataService.class);
        intent.setAction(ACTION_GET_RESORTS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_GET_LIFTDATA.equals(action)) {
                handleActionGetLiftData();
            } else if (ACTION_GET_RESORTS.equals(action)) {
                handleActionGetResorts();
            }
        }
    }

    /**
     * Handle action GetLiftData in the provided background thread with the provided
     * parameters.
     */
    private void handleActionGetLiftData() {
        String result = "";
        try {
            //Get Request
            HUDHttpRequest request = new HUDHttpRequest(HUDHttpRequest.RequestMethod.GET, "https://liftie.info/api/resort/" + Helper.getInstance().current_resort.getPath());
            HUDHttpResponse response = Helper.getInstance().mHUDConnectivityManager.sendWebRequest(request);
            if (response.hasBody()) {
                result = new String(response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = "";
        }
        // processing done here….
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LiftsActivity.ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESULT_LIFTDATA, result);
        sendBroadcast(broadcastIntent);
    }

    /**
     * Handle action GetResorts in the provided background thread with the provided
     * parameters.
     */
    private void handleActionGetResorts() {
    }
}
