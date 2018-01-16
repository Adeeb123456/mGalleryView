package com.mgalleryview.mgalleryview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

public class CustomGalleryActivity extends Activity {

    GridView gridGallery;
    Handler handler;
    GalleryAdapter adapter;
    ImageView imgNoMedia, btnGalleryOk;
    String action;
    TextView textViewTotalPhotos, btnLockedPhotos;
    private ImageLoader imageLoader;
    File myDirectory;
    TextView textTotal;
    ImageView imageViewHome;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);
        action = getIntent().getAction();
        if (action == null) {
          //  finish();
        }
        initImageLoader();
        init();
    }

    private void initImageLoader() {
        try {
            String CACHE_DIR = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/.temp_tmp";
            new File(CACHE_DIR).mkdirs();

            File cacheDir = StorageUtils.getOwnCacheDirectory(getBaseContext(),
                    CACHE_DIR);

            DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                    .cacheOnDisc(true).imageScaleType(ImageScaleType.EXACTLY)
                    .bitmapConfig(Bitmap.Config.RGB_565).build();
            ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(
                    getBaseContext())
                    .defaultDisplayImageOptions(defaultOptions)
                    .discCache(new UnlimitedDiscCache(cacheDir))
                    .memoryCache(new WeakMemoryCache());

            ImageLoaderConfiguration config = builder.build();
            imageLoader = ImageLoader.getInstance();
            imageLoader.init(config);

        } catch (Exception e) {

        }
    }


    private void init() {

        handler = new Handler();
        gridGallery = (GridView) findViewById(R.id.gridGallery);
        textViewTotalPhotos = (TextView) findViewById(R.id.textView_total_photos);
        textTotal = (TextView) findViewById(R.id.textView_textTotal);
        btnLockedPhotos = (TextView) findViewById(R.id.btnGalleryPick);
        imageViewHome = (ImageView) findViewById(R.id.home_screen);


        imageViewHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //  startActivity(new Intent(CustomGalleryActivity.this, LockerView.class));
                finish();
            }
        });

            textTotal.setText("Total Photos:");
            btnLockedPhotos.setText("Locked Photos");



        btnLockedPhotos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getIntent().getExtras().getString("videolocker").equals("videos")) {
                  //  startActivity(new Intent(CustomGalleryActivity.this, VideoLocker.class));
                    finish();
                } else {
                   // startActivity(new Intent(CustomGalleryActivity.this, PhotoLocker.class));
                    finish();
                }

            }
        });
        gridGallery.setFastScrollEnabled(true);

        adapter = new GalleryAdapter(CustomGalleryActivity.this, imageLoader);
        PauseOnScrollListener listener = new PauseOnScrollListener(imageLoader,
                true, true);
        gridGallery.setOnScrollListener(listener);

        findViewById(R.id.llBottomContainer).setVisibility(View.VISIBLE);
        gridGallery.setOnItemClickListener(mItemMulClickListener);
        adapter.setMultiplePick(true);

        gridGallery.setAdapter(adapter);
        imgNoMedia = (ImageView) findViewById(R.id.imgNoMedia);

        btnGalleryOk = (ImageView) findViewById(R.id.btnGalleryOk);
        btnGalleryOk.setOnClickListener(mOkClickListener);

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (getIntent().getExtras()!=null&&getIntent().getExtras().getString("videolocker").equals("videos")) {
                            adapter.addAll(getGalleryVideos(), false);

                        } else {
                            adapter.addAll(getGalleryPhotos(), true);
                        }

                        checkImageStatus();


                    }
                });
                Looper.loop();
            }

            ;

        }.start();

    }

    private void checkImageStatus() {
        if (adapter.isEmpty()) {
            imgNoMedia.setVisibility(View.VISIBLE);
        } else {
            imgNoMedia.setVisibility(View.GONE);
        }
    }

    ArrayList<CustomGallery> selected;
    View.OnClickListener mOkClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            selected = adapter.getSelected();

            if (selected.size() > 0) {

            } else {
               // showToast("Nothing is selected", "short");
            }
        }
    };
    AdapterView.OnItemClickListener mItemMulClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> l, View v, int position, long id) {
            adapter.changeSelection(v, position);
            Toast.makeText(getApplicationContext(),"chked",Toast.LENGTH_SHORT).show();
        }
    };

    AdapterView.OnItemClickListener mItemSingleClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> l, View v, int position, long id) {
            CustomGallery item = adapter.getItem(position);
            Intent data = new Intent().putExtra("single_path", item.sdcardPath);
            setResult(RESULT_OK, data);
            finish();
        }
    };

    private ArrayList<CustomGallery> getGalleryPhotos() {
        ArrayList<CustomGallery> galleryList = new ArrayList<CustomGallery>();
        try {
            final String[] columns = {MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media._ID};
            final String orderBy = MediaStore.Images.Media._ID;
            Cursor imagecursor = managedQuery(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
                    null, null, orderBy);
            if (imagecursor != null && imagecursor.getCount() > 0) {
                while (imagecursor.moveToNext()) {
                    CustomGallery item = new CustomGallery();
                    int dataColumnIndex = imagecursor
                            .getColumnIndex(MediaStore.Images.Media.DATA);
                    item.sdcardPath = imagecursor.getString(dataColumnIndex);
                    galleryList.add(item);
                }
                textViewTotalPhotos.setText(String.valueOf(galleryList.size()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        // show newest photo at beginning of the list
        Collections.reverse(galleryList);
        return galleryList;
    }

    private ArrayList<CustomGallery> getGalleryVideos() {
        ArrayList<CustomGallery> galleryList = new ArrayList<CustomGallery>();
        try {
            final String[] columns = {MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media._ID};
            final String orderBy = MediaStore.Video.Media._ID;
            Cursor videoCursor = managedQuery(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns,
                    null, null, orderBy);
            if (videoCursor != null && videoCursor.getCount() > 0) {
                while (videoCursor.moveToNext()) {
                    CustomGallery item = new CustomGallery();
                    int dataColumnIndex = videoCursor
                            .getColumnIndex(MediaStore.Images.Media.DATA);
                    item.sdcardPath = videoCursor.getString(dataColumnIndex);
                    galleryList.add(item);
                }
                textViewTotalPhotos.setText(String.valueOf(galleryList.size()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        // show newest photo at beginning of the list
        Collections.reverse(galleryList);
        return galleryList;
    }




    @SuppressLint("NewApi")
    public static Bitmap retriveVideoFrameFromVideo(String videoPath)
            throws Throwable {
        Bitmap bitmap = null;
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(videoPath);
            bitmap = mediaMetadataRetriever.getFrameAtTime();
        } catch (Exception e) {
            throw new Throwable(
                    "Exception in retriveVideoFrameFromVideo(String videoPath)"
                            + e.getMessage());
        } finally {
            if (mediaMetadataRetriever != null) {
                mediaMetadataRetriever.release();
            }
        }
        return bitmap;
    }


}
