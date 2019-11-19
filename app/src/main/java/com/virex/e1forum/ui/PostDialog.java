package com.virex.e1forum.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

import com.virex.e1forum.R;

public class PostDialog extends DialogFragment {

    private EditText input_title;
    private EditText input_text;
    private TextView tv_error;
    private ProgressBar progressBar;

    private OnDialogClickListener onDialogClickListener;

    public interface OnDialogClickListener {
        void onOkClick(String subject, String body);
    }

    public PostDialog(OnDialogClickListener onDialogClickListener){
        this.onDialogClickListener=onDialogClickListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View rootview = getActivity().getLayoutInflater().inflate(R.layout.post_dialog, null);
        input_title = rootview.findViewById(R.id.input_title);
        input_text = rootview.findViewById(R.id.input_text);
        tv_error = rootview.findViewById(R.id.tv_error);
        progressBar = rootview.findViewById(R.id.progressBar);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false)
                .setView(rootview)
                .setCancelable(false)
                .setPositiveButton(R.string.Ok,null)
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onStart() {
        //перехватываем нажатие кнопки "ок"
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(input_title.getText())){
                    input_title.requestFocus();
                    input_title.setError(getString(R.string.error_empty_text));
                    return;
                }
                if (TextUtils.isEmpty(input_text.getText())){
                    input_text.requestFocus();
                    input_text.setError(getString(R.string.error_empty_text));
                    return;
                }
                tv_error.setVisibility(View.GONE);
                onDialogClickListener.onOkClick(input_title.getText().toString(), input_text.getText().toString());
            }
        });

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

    public void setError(String message){
        tv_error.setVisibility(View.VISIBLE);
        tv_error.setText(HtmlCompat.fromHtml(message,HtmlCompat.FROM_HTML_MODE_COMPACT).toString());
    }

    public void setStartLoading(){
        progressBar.setVisibility(View.VISIBLE);
    }

    public void setFinishLoading(){
        progressBar.setVisibility(View.GONE);
    }
}
