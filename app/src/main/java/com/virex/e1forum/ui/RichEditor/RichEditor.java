package com.virex.e1forum.ui.RichEditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

public class RichEditor extends WebView {

    public interface StateListener {
        /*
        void onBold(boolean value);
        void onItalic(boolean value);
        void onUnderline(boolean value);
        void onStrike(boolean value);
        */
        void onUpdateToolbar(REStatus toolbarStatus);
        void onLoaded();
    }

    private static final String SETUP_HTML = "file:///android_asset/editor.html";
    private String source;
    private boolean isReady = false;
    private StateListener stateListener;
    private REStatus toolbarStatus=new REStatus();


    public RichEditor(Context context) {
        this(context, null);
    }

    public RichEditor(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.webViewStyle);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public RichEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addJavascriptInterface(this, "android");

        //setInitialScale(1);
        //setPadding(0, 0, 0, 0);
        //setInitialScale((int) getScale());
        //getSettings().setUseWideViewPort(true);
        //setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        //setScrollbarFadingEnabled(false);
        //getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);

        getSettings().setLoadWithOverviewMode(true);
        getSettings().setUseWideViewPort(true);

        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);

        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isReady = url.equalsIgnoreCase(SETUP_HTML);
            }
        });
        loadUrl(SETUP_HTML);
    }

    public void setValue(@NonNull String value){
        exec(String.format("javascript:sceditor.instance(document.getElementById('example')).execCommand('%s');",value));
    }

    public void addQuote(@NonNull String quote){
        //exec(String.format("javascript:sceditor.instance(document.getElementById('example')).wysiwygEditorInsertHtml( '<br><quote>%s<br></quote><br>');",quote));
        exec(String.format("javascript:sceditor.instance(document.getElementById('example')).insert( '[quote]%s[/quote]');",quote));
    }

    public void addQuote(@NonNull String nickname, @NonNull String quote){
        //exec(String.format("javascript:sceditor.instance(document.getElementById('example')).wysiwygEditorInsertHtml( '<br><quote><nickname>%s</nickname><br>%s</quote><br>');",nickname,quote));
        exec(String.format("javascript:sceditor.instance(document.getElementById('example')).insert( '[quote=%s]%s[/quote] ');",nickname,quote));
    }

    /*
    public void undo(){
        exec("javascript:sceditor.instance(document.getElementById('example')).undo();");
    }
     */

    public void insertImage(@NonNull String value){
        exec(String.format("javascript:sceditor.instance(document.getElementById('example')).wysiwygEditorInsertHtml( '<img src=\"%s\"/>');",value));
    }

    public void insertLink(@NonNull String value){
        exec(String.format("javascript:sceditor.instance(document.getElementById('example')).wysiwygEditorInsertHtml( '<a href=\"%s\">%s</a>');",value,value));
    }

    public void insertCode(@NonNull String value){
        exec(String.format("javascript:sceditor.instance(document.getElementById('example')).wysiwygEditorInsertHtml( '<br><code>%s</code><br>');",value));
    }

    public void sourceMode(){
        exec("javascript:sceditor.instance(document.getElementById('example')).toggleSourceMode();");
    }

    public String getBBCode() {
        //exec("javascript:SCEditor.getText;");
        //return "SCEditor";
        return source;
    }

    protected void exec(final String trigger) {
        if (isReady) {
            load(trigger);
        } else {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    exec(trigger);
                }
            }, 100);
        }
    }

    private void load(String trigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript(trigger, null);
        } else {
            loadUrl(trigger);
        }
    }

    @JavascriptInterface
    public void onData(String value) {
        //сюда приходит содержимое для отправки
        source=value;
    }

    @JavascriptInterface
    public void onUpdateStatus(final String name, final String value) {
        //здесь мы обновляем статус текста (подчеркнутый, зачеркнутый и т.д.)
        if (name.contains("bold")) toolbarStatus.isBold=value.contains("1");
        if (name.contains("italic")) toolbarStatus.isItalic=value.contains("1");
        if (name.contains("underline")) toolbarStatus.isUnderline=value.contains("1");
        if (name.contains("strikethrough")) toolbarStatus.isStrike=value.contains("1");
        if (name.contains("code")) toolbarStatus.isCode=value.contains("1");
    }

    @JavascriptInterface
    public void onUpdateToolbar() {
        //тут мы обновляем тулбар
        if (stateListener!=null)
            stateListener.onUpdateToolbar(toolbarStatus);
    }

    @JavascriptInterface
    public void onLoaded() {
        //тут js едитор готов
        if (stateListener!=null)
            stateListener.onLoaded();
    }

    public void setStateListener(StateListener stateListener){
        this.stateListener=stateListener;
    }

}