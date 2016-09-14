package com.staysafe.staysafe.util;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by User on 14/09/20161
 */

public class JsonAsyncTask extends AsyncTask<String, String, String> {

    private OnJsonDownloadListener listener;

    public JsonAsyncTask() {}

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (listener != null)
            listener.OnPreExecute();
    }

    public void setOnJsonListener(OnJsonDownloadListener listener){
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... uri) {
        String responseString = null;
        InputStream inputStream = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(uri[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(15000 /* milliseconds */);
            connection.setConnectTimeout(15000 /* milliseconds */);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);

            //start connection
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK)
                throw new IOException("Error while connecting! Error code: "+responseCode);
            inputStream = connection.getInputStream();

            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            responseString = total.toString();
        } catch (IllegalStateException | IOException e1) {
            e1.printStackTrace();
            return null;
        } finally {
            if (connection != null)
                connection.disconnect();
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (listener != null)
            listener.OnPostExecute(s);
    }



    public interface OnJsonDownloadListener {
        void OnPreExecute();
        void OnPostExecute(String result);
    }

}
