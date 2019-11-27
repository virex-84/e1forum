package com.virex.e1forum.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

import com.virex.e1forum.R;
import com.virex.e1forum.ui.RichEditor.REStatus;
import com.virex.e1forum.ui.RichEditor.RichEditor;

public class PostDialog extends DialogFragment {

    private EditText input_title;

    private RichEditor input_text;

    private TextView tv_error;
    private ProgressBar progressBar;

    private OnDialogClickListener onDialogClickListener;
    private View.OnClickListener onClickListener;
    private String title;

    public interface OnDialogClickListener {
        void onOkClick(String subject, String body);
    }

    public interface OnValueClickListener {
        void onClick(String value);
    }

    public PostDialog(String title, OnDialogClickListener onDialogClickListener){
        this.onDialogClickListener=onDialogClickListener;
        this.title=title;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final View rootview = getActivity().getLayoutInflater().inflate(R.layout.post_dialog, null);
        input_title = rootview.findViewById(R.id.input_title);
        if (!TextUtils.isEmpty(title))
            input_title.setText(title);

        tv_error = rootview.findViewById(R.id.tv_error);
        progressBar = rootview.findViewById(R.id.progressBar);

        input_text = rootview.findViewById(R.id.input_text);
        input_text.requestFocus();

        onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getId()==R.id.action_bold) input_text.setValue("bold");
                if (view.getId()==R.id.action_italic) input_text.setValue("italic");
                if (view.getId()==R.id.action_underline) input_text.setValue("underline");
                if (view.getId()==R.id.action_strikethrough) input_text.setValue("strikethrough");

                if (view.getId()==R.id.action_code) {
                    ShowInsertDialog(getString(R.string.insert_code),new OnValueClickListener() {
                        @Override
                        public void onClick(String value) {
                            input_text.insertCode(value);
                        }
                    });
                }
                if (view.getId()==R.id.action_image) {
                    ShowInsertDialog(getString(R.string.insert_url),new OnValueClickListener() {
                        @Override
                        public void onClick(String value) {
                            input_text.insertImage(value);
                        }
                    });
                }
                if (view.getId()==R.id.action_link) {
                    ShowInsertDialog(getString(R.string.insert_url),new OnValueClickListener() {
                        @Override
                        public void onClick(String value) {
                            input_text.insertLink(value);
                        }
                    });
                }
                if (view.getId()==R.id.action_source) input_text.sourceMode();

            }
        };

        rootview.findViewById(R.id.action_bold).setOnClickListener(onClickListener);
        rootview.findViewById(R.id.action_italic).setOnClickListener(onClickListener);
        rootview.findViewById(R.id.action_underline).setOnClickListener(onClickListener);
        rootview.findViewById(R.id.action_strikethrough).setOnClickListener(onClickListener);
        rootview.findViewById(R.id.action_code).setOnClickListener(onClickListener);
        rootview.findViewById(R.id.action_image).setOnClickListener(onClickListener);
        rootview.findViewById(R.id.action_link).setOnClickListener(onClickListener);
        rootview.findViewById(R.id.action_source).setOnClickListener(onClickListener);

        input_text.setStateListener(new RichEditor.StateListener() {
            @Override
            public void onUpdateToolbar(final REStatus toolbarStatus) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rootview.findViewById(R.id.action_bold).setBackgroundResource(toolbarStatus.isBold ? R.color.colorPrimary : R.color.colorAccent);
                        rootview.findViewById(R.id.action_italic).setBackgroundResource(toolbarStatus.isItalic ? R.color.colorPrimary : R.color.colorAccent);
                        rootview.findViewById(R.id.action_underline).setBackgroundResource(toolbarStatus.isUnderline ? R.color.colorPrimary : R.color.colorAccent);
                        rootview.findViewById(R.id.action_strikethrough).setBackgroundResource(toolbarStatus.isStrike ? R.color.colorPrimary : R.color.colorAccent);
                    }
                });
            }
        });


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
                String title=input_title.getText().toString();
                String body=input_text.getBBCode();

                if (TextUtils.isEmpty(title)){
                    input_title.requestFocus();
                    input_title.setError(getString(R.string.error_empty_text));
                    return;
                }
                if (TextUtils.isEmpty(body)){
                    input_text.requestFocus();
                    //input_text.setError(getString(R.string.error_empty_text));
                    Toast.makeText(getContext(), getString(R.string.error_empty_text), Toast.LENGTH_SHORT).show();
                    return;
                }

                tv_error.setVisibility(View.GONE);
                onDialogClickListener.onOkClick(title, body);
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

    private void ShowInsertDialog(String title, final OnValueClickListener onValueClickListener){
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.value_dialog, null);
        ((EditText)dialogView.findViewById(R.id.value)).setHint(title);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton(R.string.Ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (onValueClickListener!=null)
                            onValueClickListener.onClick(((EditText)dialogView.findViewById(R.id.value)).getText().toString());
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

}
