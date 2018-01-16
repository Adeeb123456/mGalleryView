package com.mgalleryview.mgalleryview;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;

public class GalleryAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater infalter;
    private ArrayList<CustomGallery> data = new ArrayList<CustomGallery>();
    ImageLoader imageLoader;
    boolean statusPhoto;
    private boolean isActionMultiplePick;
    ArrayList<Bitmap> arrayListBitmap;

    public GalleryAdapter(Context c, ImageLoader imageLoader) {
        infalter = (LayoutInflater) c
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = c;
        this.imageLoader = imageLoader;
        // clearCache();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public CustomGallery getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setMultiplePick(boolean isMultiplePick) {
        this.isActionMultiplePick = isMultiplePick;
    }

    public void selectAll(boolean selection) {
        for (int i = 0; i < data.size(); i++) {
            data.get(i).isSeleted = selection;

        }
        notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        boolean isAllSelected = true;

        for (int i = 0; i < data.size(); i++) {
            if (!data.get(i).isSeleted) {
                isAllSelected = false;
                break;
            }
        }

        return isAllSelected;
    }

    public boolean isAnySelected() {
        boolean isAnySelected = false;

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).isSeleted) {
                isAnySelected = true;
                break;
            }
        }

        return isAnySelected;
    }

    public ArrayList<CustomGallery> getSelected() {
        ArrayList<CustomGallery> dataT = new ArrayList<CustomGallery>();

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).isSeleted) {
                dataT.add(data.get(i));
            }
        }

        return dataT;
    }

    public void addAll(ArrayList<CustomGallery> files, boolean statusPhoto) {

        this.statusPhoto = statusPhoto;

        try {
            this.data.clear();
            this.data.addAll(files);

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!statusPhoto) {
            new BitmapFiles().execute();
        }
    }

    public void changeSelection(View v, int position) {

        if (data.get(position).isSeleted) {
            data.get(position).isSeleted = false;
        } else {
            data.get(position).isSeleted = true;
        }

        ((ViewHolder) v.getTag()).imgQueueMultiSelected.setSelected(data
                .get(position).isSeleted);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        if (convertView == null) {

            convertView = infalter.inflate(R.layout.gallery_item, null);
            holder = new ViewHolder();
            holder.imgQueue = (ImageView) convertView
                    .findViewById(R.id.imgQueue);

            holder.imgQueueMultiSelected = (ImageView) convertView
                    .findViewById(R.id.imgQueueMultiSelected);

            if (isActionMultiplePick) {
                holder.imgQueueMultiSelected.setVisibility(View.VISIBLE);
            } else {
                holder.imgQueueMultiSelected.setVisibility(View.GONE);
            }

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.imgQueue.setTag(position);

        try {
            String bitmap = ("file://" + Uri.decode(data.get(position).sdcardPath));

            if (statusPhoto) {
                imageLoader.displayImage("file://" + Uri.decode(data.get(position).sdcardPath),
                        holder.imgQueue, new SimpleImageLoadingListener() {
                            @Override
                            public void onLoadingStarted(String imageUri, View view) {
                                holder.imgQueue
                                        .setImageResource(R.drawable.no_media);
                                super.onLoadingStarted(imageUri, view);
                            }
                        });
            } else if (!statusPhoto) {

                holder.imgQueue.setImageBitmap(arrayListBitmap.get(position));

            }


            if (isActionMultiplePick) {

                holder.imgQueueMultiSelected
                        .setSelected(data.get(position).isSeleted);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return convertView;
    }

    public class ViewHolder {
        ImageView imgQueue;
        ImageView imgQueueMultiSelected;
    }

    public void clearCache() {
        imageLoader.clearDiscCache();
        imageLoader.clearMemoryCache();
    }

    public void clear() {
        data.clear();
        notifyDataSetChanged();
    }

    static ProgressDialog progressDialog;

    class BitmapFiles extends AsyncTask<Void, Void, Void> {

        String uri;
        int position;

        String encodedString = null;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage("Please wait.");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            arrayListBitmap = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                Bitmap bmThumbnail;
                bmThumbnail = ThumbnailUtils.createVideoThumbnail(data.get(i).sdcardPath, MediaStore.Video.Thumbnails.MICRO_KIND);
                arrayListBitmap.add(bmThumbnail);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            notifyDataSetChanged();
        }
    }


}
