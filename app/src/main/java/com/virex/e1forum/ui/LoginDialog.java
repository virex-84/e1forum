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

public class LoginDialog extends DialogFragment {

    private EditText input_login;
    private EditText input_password;
    private TextView tv_error;
    private ProgressBar progressBar;

    OnDialogClickListener onDialogClickListener;

    public interface OnDialogClickListener {
        void onOkClick(String login, String password);
    }

    public LoginDialog(OnDialogClickListener onDialogClickListener){
        this.onDialogClickListener=onDialogClickListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View rootview = getActivity().getLayoutInflater().inflate(R.layout.login_dialog, null);
        input_login = rootview.findViewById(R.id.input_login);
        input_password = rootview.findViewById(R.id.input_password);
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
                if (TextUtils.isEmpty(input_login.getText())){
                    input_login.requestFocus();
                    input_login.setError(getString(R.string.error_empty_text));
                    return;
                }
                if (TextUtils.isEmpty(input_password.getText())){
                    input_password.requestFocus();
                    input_password.setError(getString(R.string.error_empty_text));
                    return;
                }
                tv_error.setVisibility(View.GONE);
                onDialogClickListener.onOkClick(input_login.getText().toString(), input_password.getText().toString());
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
