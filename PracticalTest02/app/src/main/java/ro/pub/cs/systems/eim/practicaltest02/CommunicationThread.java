package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

public class CommunicationThread extends Thread{
    private ServerThread serverThread;
    private Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            if (bufferedReader == null || printWriter == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                return;
            }

            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client");
            String moneda = bufferedReader.readLine();
            if (moneda == null || moneda.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client");
                return;
            }

            String currency = serverThread.getCurrency();
            String newCurrency = null;
            if (currency != null && moneda.equals(serverThread.getCurrencyType())) {
                Log.i(Constants.TAG,
                        "[COMMUNICATION THREAD] Getting the information from the cache...");
                newCurrency = currency;
                String final_result = moneda + " " + newCurrency;
                printWriter.println(final_result);
                printWriter.flush();
            } else {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                HttpClient httpClient = new DefaultHttpClient();
                String url = Constants.BASE_URL +
                        moneda + ".json";

                HttpGet httpGet = new HttpGet(url);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String content = httpClient.execute(httpGet, responseHandler);
                JSONObject result = new JSONObject(content);

                JSONObject time = result.getJSONObject("time");
                String last_update = time.getString("updated");
                JSONObject bpi = result.getJSONObject("bpi");
                JSONObject currency_info = bpi.getJSONObject("EUR");
                String rate = currency_info.getString("rate");

                newCurrency = rate;
                serverThread.setCurrency(newCurrency);
                serverThread.setCurrencyType(moneda);
                serverThread.setTime(last_update);
                String final_result = moneda + " " + newCurrency;
                printWriter.println(final_result);
                printWriter.flush();
            }
        } catch (IOException exception) {
            if (Constants.DEBUG) {
                exception.printStackTrace();
            }
        } catch (JSONException exception) {
            if (Constants.DEBUG) {
                exception.printStackTrace();
            }
        }
    }
}
