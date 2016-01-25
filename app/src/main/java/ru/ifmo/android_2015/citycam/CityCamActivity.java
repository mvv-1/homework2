package ru.ifmo.android_2015.citycam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import ru.ifmo.android_2015.citycam.model.City;
import ru.ifmo.android_2015.citycam.webcams.Webcams;

/**
 * Экран, показывающий веб-камеру одного выбранного города.
 * Выбранный город передается в extra параметрах.
 */
public class CityCamActivity extends AppCompatActivity {

    /**
     * Обязательный extra параметр - объект City, камеру которого надо показать.
     */
    public static final String EXTRA_CITY = "city";

    private City city;

    public ImageView camImageView;
    public ProgressBar progressView;
    public TextView clockView;
    public TextView titleView;

    private WebcamGetter getter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        city = getIntent().getParcelableExtra(EXTRA_CITY);
        if (city == null) {
            Log.w(TAG, "City object not provided in extra parameter: " + EXTRA_CITY);
            finish();
        }

        setContentView(R.layout.activity_city_cam);
        camImageView = (ImageView) findViewById(R.id.cam_image);
        progressView = (ProgressBar) findViewById(R.id.progress);
        clockView = (TextView) findViewById(R.id.clock);
        titleView = (TextView) findViewById(R.id.title);

        getSupportActionBar().setTitle(city.name);

        progressView.setVisibility(View.VISIBLE);

        // Здесь должен быть код, инициирующий асинхронную загрузку изображения с веб-камеры
        // в выбранном городе.

        getter = (WebcamGetter) getLastCustomNonConfigurationInstance();
        if (getter == null) {
            getter = new WebcamGetter();
            getter.execute(city);
        }
        getter.attach(this);
    }

    private static final String TAG = "CityCam";

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return getter;
    }
}

class WebcamInfo {
    public final Bitmap preview;
    public final String title;
    public final Date lastUpdate;

    public WebcamInfo(Bitmap preview, String title, Date lastUpdate) {
        this.preview = preview;
        this.title = title;
        this.lastUpdate = lastUpdate;
    }
}

class WebcamGetter extends AsyncTask<City, Void, WebcamInfo> {
    private byte[] fetch(URL url) {
        HttpURLConnection connection = null;
        InputStream tmp = null;
        byte[] res = null;
        Log.d("url", url.toString());
        try {
            connection = (HttpURLConnection)url.openConnection();
            Log.d("responce", connection.getResponseMessage());
            tmp = connection.getInputStream();
            Log.d("available", Integer.toString(tmp.available()));
            //Thread.sleep(1000);
            res = new byte[tmp.available()];
            tmp.read(res, 0, res.length);
        } catch (IOException e) {
            //e.printStackTrace();
            res = null;
//        } catch (InterruptedException e) {
//            e.printStackTrace();
        } finally {
            try {
                if (tmp != null) tmp.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (connection != null) connection.disconnect();
        }
        return res;
    }

    @Override
    protected WebcamInfo doInBackground(City... params) {
        City city = params[0];
        String json = null;
        try {
            byte[] t = fetch(Webcams.createNearbyUrl(city.latitude, city.longitude));
            if (t != null)
                json = new String(t);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (json == null) {
            return null;
        }
        Log.d("json", json);
//        JsonReader reader = new JsonReader(new InputStreamReader(stream));
//        try {
//            reader.beginObject();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        } finally {
//            try {
//                reader.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        String title;
        Date lastUpdate;
        URL picURL;
        try {
            JSONObject root = new JSONObject(json);
            if (!root.getString("status").equals("ok"))
                return null;
            Log.d("Status", "ok");
            root = root.getJSONObject("webcams");
            if (root.getString("count").equals("0"))
                return null;
            Log.d("Count", "> 0");
            root = root.getJSONArray("webcam").getJSONObject(0);
            title = root.getString("title");
            lastUpdate = new Date(Long.parseLong(root.getString("last_update")));
            picURL = new URL(root.getString("preview_url"));
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        byte[] bytes = fetch(picURL);
        Bitmap pic = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return new WebcamInfo(pic, title, lastUpdate);
    }

    private CityCamActivity activity;

    public void attach(CityCamActivity activity) {
        this.activity = activity;
    }

    @Override
    protected void onPostExecute(WebcamInfo webcamInfo) {
        super.onPostExecute(webcamInfo);
        if (webcamInfo == null) {
            Toast.makeText(activity.getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
            activity.finish();
        } else {
            activity.camImageView.setImageBitmap(webcamInfo.preview);
            activity.titleView.setText(webcamInfo.title);
            activity.clockView.setText(webcamInfo.lastUpdate.toString());
        }
    }
}
