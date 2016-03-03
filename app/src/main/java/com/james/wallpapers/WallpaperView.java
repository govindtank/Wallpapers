package com.james.wallpapers;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;


public class WallpaperView extends ActionBarActivity {

    String path, title, author, up;
    int num, position;
    boolean fav;

    BroadcastReceiver complete;

    Toolbar toolbar;
    ImageView imageee;
    TextView wall, auth;
    LinearLayout bg;

    Drawable transition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_view);

        final android.support.design.widget.FloatingActionButton fab = (android.support.design.widget.FloatingActionButton) findViewById(R.id.fab);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.inflateMenu(R.menu.menu_wallpaper_view);

        imageee = (ImageView) findViewById(R.id.imageee);
        wall = (TextView) findViewById(R.id.wall);
        auth = (TextView) findViewById(R.id.auth);
        bg = (LinearLayout) findViewById(R.id.back);

        transition = new ColorDrawable(Color.TRANSPARENT);

        byte[] b = getIntent().getByteArrayExtra("preload");
        if (b != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
            imageee.setImageBitmap(bmp);
            transition = new BitmapDrawable(getResources(), bmp);
        }

        position = 0;

        final SharedPreferences prefs = getSharedPreferences("com.james.wallpapers", 0);

        final String[] arrayname = getResources().getStringArray(getIntent().getIntExtra("array", 0));
        final String[] arrayurl = getResources().getStringArray(getIntent().getIntExtra("arrays", 0));

        int nameid = getIntent().getIntExtra("array", 0);

        num = getIntent().getIntExtra("num", 0) ;
        position = num;
        author = getIntent().getStringExtra("auth");
        up = getIntent().getStringExtra("up");
        title = arrayname[num];
        path = arrayurl[num];

        String mName = title.replace("*", "");
        getSupportActionBar().setTitle(mName);

        wall.setText(mName);
        auth.setText(author);

        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.orange)));
        fav = prefs.getBoolean(path, false);
        if(fav){
            fab.setImageResource(R.mipmap.fav_added);
        }

        new Thread() {
            @Override
            public void run() {
                final BitmapDrawable bmp;
                if (Cache.dir(author.toLowerCase().replace(" ", "_"), WallpaperView.this)) bmp = (BitmapDrawable) Cache.getDrawable(author.toLowerCase().replace(" ", "_"), path.replace("/", "").replace(":", "").replace(".", ""), path, WallpaperView.this);
                else bmp = (BitmapDrawable) Cache.downloadDrawable(path, WallpaperView.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TransitionDrawable td = new TransitionDrawable(new Drawable[]{transition, bmp});
                        imageee.setImageDrawable(td);
                        td.startTransition(200);
                        findViewById(R.id.progressBar).setVisibility(View.GONE);

                        int color = getDominantColor(bmp.getBitmap());

                        toolbar.setBackgroundColor(color);
                        findViewById(R.id.fab).setBackgroundTintList(ColorStateList.valueOf(color));
                        bg.setBackgroundColor(color);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            getWindow().setStatusBarColor(darkColor(color));
                        };

                        if(fav){
                            fab.setImageResource(R.mipmap.fav_added_white);
                        }else{
                            fab.setImageResource(R.mipmap.fav_add_white);
                        }
                    }
                });
            }
        }.start();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fav) {
                    fab.setImageResource(R.mipmap.fav_add_white);
                    prefs.edit().putBoolean(path, false).apply();
                    fav = false;
                } else {
                    fab.setImageResource(R.mipmap.fav_added_white);
                    prefs.edit().putBoolean(path, true).apply();
                    fav = true;
                }
            }
        });

        final TypedArray tab_names = getResources().obtainTypedArray(R.array.wp_names);
        final TypedArray tab_urls = getResources().obtainTypedArray(R.array.wp_urls);

        int authnum = -1;
        for(int i = 0; i < tab_names.length(); i++) {
            if (tab_names.getResourceId(i, -1) == nameid) {
                authnum = i;
            }
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.more);
        GridLayoutManager grid = new GridLayoutManager(this, 1);
        grid.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(grid);
        recyclerView.setAdapter(new ListAdapter(tab_names.getResourceId(authnum, -1), tab_urls.getResourceId(authnum, -1), WallpaperView.this, author));
        recyclerView.setHasFixedSize(true);

        tab_names.recycle();
        tab_urls.recycle();

        RecyclerView recyclerView2 = (RecyclerView) findViewById(R.id.similar);
        GridLayoutManager grid2 = new GridLayoutManager(this, 1);
        grid2.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView2.setLayoutManager(grid2);
        SearchAdapter adapter = new SearchAdapter(WallpaperView.this);
        recyclerView2.setAdapter(adapter);
        recyclerView2.setHasFixedSize(true);

        adapter.filter(mName);

        complete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new MaterialDialog.Builder(WallpaperView.this)
                        .title("Download Complete")
                        .content("Your wallpaper has been downloaded.")
                        .positiveText("Downloads")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                            }
                        })
                        .show();
            }
        };

    }

    Target imageLoad = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            int color = getDominantColor(bitmap);
            ImageView imageee = (ImageView) findViewById(R.id.imageee);
            imageee.setImageBitmap(bitmap);
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            android.support.design.widget.FloatingActionButton fab = (android.support.design.widget.FloatingActionButton) findViewById(R.id.fab);
            fab.setBackgroundTintList(ColorStateList.valueOf(getDominantColor(bitmap)));
            if(isColorDark(color)){
                if(fav){
                    fab.setImageResource(R.mipmap.fav_added_white);
                }else{
                    fab.setImageResource(R.mipmap.fav_add_white);
                }
            } else {
                if(fav) {
                    fab.setImageResource(R.mipmap.fav_added);
                } else {
                    fab.setImageResource(R.mipmap.fav_add);
                }
            }
            toolbar.setBackgroundColor(color);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(darkColor(color));
            };
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            new MaterialDialog.Builder(WallpaperView.this)
                    .title("No Connection")
                    .content("Internet access is needed to view wallpapers.")
                    .positiveText("Wifi Settings")
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    })
                    .show();
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wallpaper_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {


            if(title.contains("*")){
                new MaterialDialog.Builder(this)
                        .title("Credit Required")
                        .content("Credit is required for this wallpaper. Make sure you check the about section for who made this wallpaper so you can give them credit.")
                        .positiveText("Start Download")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(path));
                                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, String.valueOf(getTitle() + ".png"));
                                r.allowScanningByMediaScanner();
                                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(r);
                                download();
                            }
                        })
                        .show();
            }else{
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(path));
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title + ".png");
                r.allowScanningByMediaScanner();
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(r);
                download();
            }

            registerReceiver(complete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            return true;
        }

        if(id == R.id.action_set) {
            setWallpaperURL(path, author);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    @Override
    public Intent getParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    private Intent getParentActivityIntentImpl() {
        Intent i = null;

        // Here you need to do some logic to determine from which Activity you came.
        // example: you could pass a variable through your Intent extras and check that.
        if (up.matches("Flat")) {
            i = new Intent(this, Flat.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else if(up.matches("Fav")) {
            i = new Intent(this, Fav.class);
            // same comments as above
        } else if(up.matches("Search")){
            i = new Intent(this, SearchActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        System.gc();
        Runtime.getRuntime().gc();

        return i;
    }

    public static int getDominantColor(Bitmap bitmap) {
        if (null == bitmap) return Color.TRANSPARENT;

        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        int alphaBucket = 0;

        boolean hasAlpha = bitmap.hasAlpha();
        int pixelCount = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[pixelCount];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int y = 0, h = bitmap.getHeight(); y < h; y++) {
            for (int x = 0, w = bitmap.getWidth(); x < w; x++) {
                int color = pixels[x + y * w]; // x + y * width
                redBucket += (color >> 16) & 0xFF; // Color.red
                greenBucket += (color >> 8) & 0xFF; // Color.green
                blueBucket += (color & 0xFF); // Color.blue
                if (hasAlpha) alphaBucket += (color >>> 24); // Color.alpha
            }
        }

        return Color.argb(
                (hasAlpha) ? (alphaBucket / pixelCount) : 255,
                redBucket / pixelCount,
                greenBucket / pixelCount,
                blueBucket / pixelCount);
    }

    public boolean isColorDark(int color){
        double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114* Color.blue(color))/255;
        return darkness >= 0.5;
    }

    public void download(){
        final ProgressDialog progressBarDialog = new ProgressDialog(this);
        progressBarDialog.setTitle("Downloading...");

        progressBarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressBarDialog.setProgress(0);

        new Thread(new Runnable() {

            @Override
            public void run() {
                boolean downloading = true;
                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                for (int downloadinger = 0; downloading; downloadinger++) {

                    DownloadManager.Query q = new DownloadManager.Query();
                    Cursor cursor = manager.query(q);
                    cursor.moveToFirst();

                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        progressBarDialog.dismiss();
                        downloading = false;
                    }

                    final int dl_progress = downloadinger;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBarDialog.setProgress(dl_progress);
                        }
                    });

                    cursor.close();
                }
            }
        }).start();
        progressBarDialog.show();
    }

    public int darkColor(int color){
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    public void setWallpaperURL(final String src, final String folder) {
        if(Cache.dir(folder.toLowerCase().replace(" ", "_"), this)) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        WallpaperManager myWallpaperManager = WallpaperManager.getInstance(WallpaperView.this);
                        Bitmap overlay = Bitmap.createBitmap(myWallpaperManager.getDesiredMinimumWidth(), myWallpaperManager.getDesiredMinimumHeight(), Bitmap.Config.ARGB_8888);
                        Bitmap wall = ((BitmapDrawable) Cache.getDrawable(folder.toLowerCase().replace(" ", "_"), src.replace("/", "").replace(":", "").replace(".", ""), src, WallpaperView.this)).getBitmap();

                        if (wall.getHeight() >= overlay.getHeight() && wall.getWidth() >= overlay.getWidth()) {
                            Canvas canvas = new Canvas(overlay);
                            canvas.drawBitmap(overlay, new Matrix(), null);
                            canvas.drawBitmap(wall, (overlay.getWidth()/2)-(wall.getWidth()/2), (overlay.getHeight()/2)-(wall.getHeight()/2), null);

                            myWallpaperManager.setBitmap(overlay);
                        } else {
                            myWallpaperManager.setBitmap(wall);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WallpaperView.this, "Enjoy your new wallpaper :)", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }.start();
        } else {
            new Thread() {
                @Override
                public void run() {
                    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
                    try {
                        java.net.URL url = new java.net.URL(src);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap bmp = BitmapFactory.decodeStream(input);
                        connection.disconnect();
                        WallpaperManager myWallpaperManager = WallpaperManager.getInstance(WallpaperView.this);
                        myWallpaperManager.setBitmap(bmp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WallpaperView.this, "Enjoy your new wallpaper :)", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        imageee.setImageDrawable(null);
    }
}
