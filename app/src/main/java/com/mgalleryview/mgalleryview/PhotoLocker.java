package com.mgalleryview.mgalleryview;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class PhotoLocker extends Activity implements AdapterView.OnItemClickListener {

    GridView gridGallery;
    Handler handler;
    GalleryAdapter adapter;

    ImageView imgSinglePick;
    TextView btnGalleryPick;
    TextView btnAddMore, textViewLockedPhotos;

    String action;
    ViewSwitcher viewSwitcher;
    ImageLoader imageLoader;
    ArrayList<CustomGallery> dataT;
    File myDirectory;
    public int screen_width, screen_height;
    public DisplayMetrics dm;
    int position;
    String[] allSelectedPath;
    String photoFolderPath;
    ImageView imageViewHome;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_locker);
        initImageLoader();
        init();
    }

    private void initImageLoader() {
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc().imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                .bitmapConfig(Bitmap.Config.RGB_565).build();
        ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(
                this).defaultDisplayImageOptions(defaultOptions).memoryCache(
                new WeakMemoryCache());

        ImageLoaderConfiguration config = builder.build();
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(config);
    }

    private void init() {

        handler = new Handler();
        gridGallery = (GridView) findViewById(R.id.gridGallery);
        gridGallery.setFastScrollEnabled(true);
        adapter = new GalleryAdapter(getApplicationContext(), imageLoader);
        imageViewHome = (ImageView) findViewById(R.id.home_screen);
        imageViewHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // startActivity(new Intent(PhotoLocker.this, LockerView.class));
                finish();
            }
        });
        adapter.setMultiplePick(false);
        gridGallery.setAdapter(adapter);
        gridGallery.setOnItemClickListener(this);
        viewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);
        viewSwitcher.setDisplayedChild(1);
        dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        screen_width = dm.widthPixels;
        screen_height = dm.heightPixels;
        imgSinglePick = (ImageView) findViewById(R.id.imgSinglePick);
        textViewLockedPhotos = (TextView) findViewById(R.id.textView_locked_Photos);
        btnGalleryPick = (TextView) findViewById(R.id.btnGalleryPick);

        photoFolderPath = Environment.getExternalStorageDirectory().toString() + "/.Photos Locker";
        File f = new File(photoFolderPath);
        if (!f.exists()) {
            f.mkdir();
        }
        File file[] = f.listFiles();
        if (file.length > 0) {
            allSelectedPath = new String[file.length];
            for (int i = 0; i < file.length; i++) {
                allSelectedPath[i] = String.valueOf(file[i]);
            }
            dataT = new ArrayList<CustomGallery>();
            for (String string : allSelectedPath) {
                CustomGallery item = new CustomGallery();
                item.sdcardPath = string;
                dataT.add(item);
            }
            viewSwitcher.setDisplayedChild(0);
            adapter.addAll(dataT, true);
            textViewLockedPhotos.setText((String.valueOf(dataT.size())));
        }

        btnAddMore = (TextView) findViewById(R.id.btnGalleryPickMul);
        btnAddMore.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(Action.ACTION_MULTIPLE_PICK);
                i.putExtra("videolocker", "photos");
                startActivityForResult(i, 200);
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            adapter.clear();
            viewSwitcher.setDisplayedChild(1);
            String single_path = data.getStringExtra("single_path");
            imageLoader.displayImage("file://" + single_path, imgSinglePick);

        } else if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            allSelectedPath = data.getStringArrayExtra("all_path");
            dataT = new ArrayList<CustomGallery>();
            for (String string : allSelectedPath) {
                CustomGallery item = new CustomGallery();
                item.sdcardPath = string;
                dataT.add(item);
            }
            viewSwitcher.setDisplayedChild(0);
            adapter.addAll(dataT, true);
            textViewLockedPhotos.setText((String.valueOf(dataT.size())));


        }

    }

    void copyFile(File sourceLocation, File targtLocation) throws IOException {
        if (sourceLocation.exists()) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(sourceLocation);
                new File(String.valueOf(targtLocation)).delete();
                out = new FileOutputStream(targtLocation);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            sourceLocation.delete();
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(sourceLocation));
            sendBroadcast(scanIntent);
            Log.e("Success", "Copy file successful.");

        } else {
            Log.v("not success", "Copy file failed. Source file missing.");
        }
    }

    public void deletePath(String path) {
        boolean deleted = false;
        File file = new File(path);
        if (file.exists()) {
            deleted = file.delete();
        }
        Log.e("Ali", "Status of delete Pic\n" + deleted);
        // request scan
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(file));
        sendBroadcast(scanIntent);
    }




    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        this.position = position;
      //  showMyDialog();
    }

    public void makeVideoDir(String folderPath) {
        myDirectory = new File(folderPath);
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }

    }

    public class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

        private MediaScannerConnection mMs;
        private File mFile;

        public SingleMediaScanner(Context context, File f) {
            mFile = f;
            mMs = new MediaScannerConnection(context, this);
            mMs.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            mMs.scanFile(mFile.getAbsolutePath(), null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            mMs.disconnect();
        }

    }

}
