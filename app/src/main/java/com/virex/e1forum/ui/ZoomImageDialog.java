package com.virex.e1forum.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.virex.e1forum.R;
import com.virex.e1forum.common.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class ZoomImageDialog  extends DialogFragment {

    private Drawable drawable;
    private EditText et_filename;
    private ImageButton iv_download;
    private String filename;

    public ZoomImageDialog(@NonNull Drawable drawable, String filename){
        this.drawable=drawable;
        this.filename=filename;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View rootview = getActivity().getLayoutInflater().inflate(R.layout.zoom_dialog, null);
        et_filename= rootview.findViewById(R.id.et_filename);
        iv_download = rootview.findViewById(R.id.iv_download);

        if (this.filename!=null)
            et_filename.setText(this.filename);

        iv_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        ZoomImageView zw_zoom = rootview.findViewById(R.id.zw_zoom);
        zw_zoom.setImageDrawable(drawable);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false)
                .setView(rootview)
                .setCancelable(true)
                .setPositiveButton(R.string.Save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();

                        File sdcard = Environment.getExternalStorageDirectory();
                        if (sdcard != null) {
                            File mediaDir = new File(sdcard, "DCIM/Camera");
                            if (!mediaDir.exists()) {
                                mediaDir.mkdirs();
                            }
                        }

                        String path = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap, "Image Description", null);
                        Uri uri = Uri.parse(path);

                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("image/*");
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.putExtra(Intent.EXTRA_TEXT, "Hello!");

// (Optional) Here we're setting the title of the content
                        intent.putExtra(Intent.EXTRA_TITLE, "Send message");

// (Optional) Here we're passing a content URI to an image to be displayed
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivity(Intent.createChooser(intent, "Share Via"), null);

                        /*
                        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] bitMapData = stream.toByteArray();

                        Utils.writeFileOnInternalStorage(iv_download.getContext(),et_filename.getText().toString(),bitMapData);

                         */
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.Close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //сохраняем состояние при перевороте экрана
        setRetainInstance(true);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
