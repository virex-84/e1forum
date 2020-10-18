package com.virex.e1forum;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.virex.e1forum.common.PrefCookieJar;
import com.virex.e1forum.network.ForumWebService;
import com.virex.e1forum.network.PostWebService;
import com.virex.e1forum.network.TopicWebService;
import com.virex.e1forum.network.UserAgentInterceptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;

import okhttp3.Cookie;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class App extends Application {

    private PrefCookieJar prefCookieJar;

    private static ForumWebService forumApi;
    private static TopicWebService topicApi;
    private static PostWebService postApi;
    //private static AnketaWebService anketaApi;

    public static ForumWebService getForumApi() {
        return forumApi;
    }
    public static TopicWebService getTopicApi() {
        return topicApi;
    }
    public static PostWebService getPostApi() {
        return postApi;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //отлавливаем необработанные исключения
        final Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                handleUncaughtException(thread, throwable);
                handler.uncaughtException(thread, throwable);
            }
        });
        
        
        //устанавливаем контроль за куками
        prefCookieJar=new PrefCookieJar(getSharedPreferences("APPP", MODE_PRIVATE), new PrefCookieJar.ChangeListener() {
            @Override
            public void onChange(List<Cookie> cookies) {
                Log.i("COOKIES  change","==START==\n");
                Log.i("","\n"+TextUtils.join("\n", cookies));
                Log.i("COOKIES  change","==END==\n");
            }
        });

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        httpClient.addInterceptor(new UserAgentInterceptor());//добавим UserAgent
        //httpClient.addInterceptor(new AddCookiesInterceptor(getApplicationContext()));//выдача куков на сайт - заменено PrefCookieJar
        //httpClient.addInterceptor(new ReceivedCookiesInterceptor(getApplicationContext()));//сохранение куков с сайта - заменено PrefCookieJar

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext;
        SSLSocketFactory sslSocketFactory=null;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        OkHttpClient.Builder builder=new OkHttpClient.Builder();
        builder
                .followRedirects(true)
                .followSslRedirects(true)
                //куки
                .cookieJar(prefCookieJar)
                //доверяем любым ssl сертификатам
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                    }
                });
        //пробуем создать клиента с sslSocketFactory
        if (sslSocketFactory!=null)
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);

        OkHttpClient client=builder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://e1.ru/talk/") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты при POST запросах
                .client(client)
                .build();

        //создаем объект, при помощи которого будем выполнять запросы
        forumApi = retrofit.create(ForumWebService.class);
        topicApi = retrofit.create(TopicWebService.class);
        postApi = retrofit.create(PostWebService.class);
    }



    //очистка всех куков
    public void clearCookies(){
        prefCookieJar.clearCookies();
    }

    //узнаем залогинены мы или нет
    public boolean isLogin(){
        try {
            for (Cookie cookie : prefCookieJar.getCookies()) {
                //гарантия того что мы залогинены - этот хитрый кук
                if (cookie.name().equals("e1_ttq") && !TextUtils.isEmpty(cookie.value())) return true;
            }
        } catch(NullPointerException e){

        }
        return false;
    }


    public boolean initIsLogin(){
        for (String cookie : prefCookieJar.loadCookies()){
            //гарантия того что мы залогинены - этот хитрый кук
            if (cookie.contains("e1_ttq") && cookie.contains("key")){
                    return true;
            }
        }
        return false;
    }

    //выцепляем из всего стека записи о своих модулях (.java файлах)
    private String extractSelfCause(Throwable cause, String searchPackage){
        StackTraceElement[] items=cause.getStackTrace();
        String result="";

        for (int i = 0; i < items.length; i++) {
            StackTraceElement item = items[i];
            if (item.getClassName().contains(searchPackage)){
                result=result.concat("\n");
                result=result.concat(String.format("filename=%s\n", item.getFileName()));
                result=result.concat(String.format("class=%s\n", item.getClassName()));
                result=result.concat(String.format("method=%s\n", item.getMethodName()));
                result=result.concat(String.format("line=%s\n", item.getLineNumber()));
            }
        }

        return result;
    }

    private void handleUncaughtException(Thread thread, Throwable exception) {
        exception.printStackTrace();

        String LINE_SEPARATOR = "\n";

        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        StringBuilder errorReport = new StringBuilder();

        errorReport.append("************ CAUSE OF ERROR ************");
        errorReport.append(LINE_SEPARATOR);
        errorReport.append(stackTrace.toString());

        Throwable cause=exception.getCause();
        if(cause==null) cause=exception;

        errorReport.append(extractSelfCause(cause,  "virex"));
        errorReport.append(LINE_SEPARATOR);

        errorReport.append(String.format("error=%s\n", cause.getLocalizedMessage()));
        errorReport.append(LINE_SEPARATOR);

        errorReport.append("************ DEVICE INFORMATION ***********");
        errorReport.append(LINE_SEPARATOR);

        errorReport.append(String.format("Brand: %s\n", Build.BRAND));
        errorReport.append(String.format("Device: %s\n",Build.DEVICE));
        errorReport.append(String.format("Model: %s\n",Build.MODEL));
        errorReport.append(String.format("ID: %s\n",Build.ID));
        errorReport.append(String.format("Product: %s\n",Build.PRODUCT));
        errorReport.append(LINE_SEPARATOR);

        errorReport.append("************ FIRMWARE ************");
        errorReport.append(LINE_SEPARATOR);
        errorReport.append(String.format("SDK: %s\n",Build.VERSION.SDK));
        errorReport.append(String.format("Release: %s\n",Build.VERSION.RELEASE));
        errorReport.append(String.format("Incremental: %s\n",Build.VERSION.INCREMENTAL));

        //запуск своего окна ошибки
        Intent intent = new Intent(getApplicationContext(), AppCrashActivity.class);
        intent.putExtra("error_message", errorReport.toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TOP); //FLAG_ACTIVITY_CLEAR_TOP - при resume открывает главное окно
        startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
