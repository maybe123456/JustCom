package com.example.baseapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;



/**
 * Created by Administrator on 2018/5/10.
 */

public class WelcomeActivity extends Activity {
    private Context context;
    private String mUrl;
    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);


        LinearLayout linearLayout = new LinearLayout(this);

        mWebView = new WebView(this);
        mWebView.setLayoutParams(params);
        WebSettings settings = mWebView.getSettings();

        settings.setBuiltInZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.loadUrl("file:///android_asset/explain.html");

        linearLayout.addView(mWebView);

//        linearLayout.setBackgroundResource(R.mipmap.astart);
        setContentView(linearLayout);
        SharedPreferences setting = getSharedPreferences("FIRST", 0);
        mUrl = setting.getString("URL", "");
        if (hasNetwork()) {
//            reqData();
            //！！！网络请求HttpURLConnectionGet()要放在子线程中
            new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpURLConnectionGet();
                }
            }).start();

        } else {
            nextActivity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private boolean hasNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }


    //get请求
    private void HttpURLConnectionGet() {
        HttpURLConnection httpURLConnection = null;
        InputStream is = null;
        //StringBuilder:线程非安全，可有多线程采用，速度比StingBuffer快,用法同StringBuffer
        // StringBuffer:线程安全，只能单线程采用
        StringBuilder sb = new StringBuilder();
        try {
            //准备请求的网络地址
            URL url = new URL("https://872275.com:55313/v1/com.ezzwj.ptnri");
            //调用openConnection得到网络连接，网络连接处于就绪状态
            httpURLConnection = (HttpURLConnection) url.openConnection();
            //设置网络连接超时时间5S
            httpURLConnection.setConnectTimeout(5 * 1000);
            //设置读取超时时间
            httpURLConnection.setReadTimeout(5 * 1000);
//            httpURLConnection.setRequestProperty("apikey", "58218dcc8845195b277082c3a357f481");
            httpURLConnection.connect();
            //if连接请求码成功
            if (httpURLConnection.getResponseCode() == httpURLConnection.HTTP_OK) {
                is = httpURLConnection.getInputStream();
                byte[] bytes = new byte[1024];
                int i = 0;
                while ((i = is.read(bytes)) != -1) {
                    sb.append(new String(bytes, 0, i, "utf-8"));
                }
                is.close();

                String str=sb.toString();
                Message message=new Message();
                message.what=1;
                message.obj=str;

                //发送Handler消息
                handler.sendMessage(message);

            }
        } catch (MalformedURLException e) {
            nextActivity();
        } catch (IOException e) {
            nextActivity();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    //Handler:消息处理机制(发消息，处理消息)，只能放在主线程中
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null && msg.what == 1) {
                String s = (String) msg.obj;
                try {
                    DBBean body = GsonUtil.json2Obj(s, DBBean.class);
//                    AppConfig.DBENTITY = body.getData();

                    if (body.getCode() == 1) {
                        DBBean.DataBean data = body.getData();
                        if (data.getRflag() == 1) {
                            mUrl = data.getRurl();
                            if (data.getUflag() == 1) {
                                mUrl = data.getUurl();
                            }
                            getSharedPreferences("FIRST", 0).edit().putString("URL", mUrl).apply();
                            startActivity(new Intent(WelcomeActivity.this, GameActivity.class).putExtra("url", mUrl));
                            finish();
                        } else {
                            nextActivity();
                        }
                    } else {
                        nextActivity();
                    }
                } catch (Exception e) {
                    nextActivity();
                }

            }

        }
    };

    private void nextActivity() {
        if (TextUtils.isEmpty(mUrl)) {
            goNative();
        } else {
            startActivity(new Intent(this, GameActivity.class).putExtra("url", mUrl));
            finish();
        }
    }

    private void goNative() {
        try {
            Intent localIntent = new Intent();
            localIntent.setComponent(new ComponentName(getPackageName(), "com.cxzg.platform.activity.InitActivity"));
            localIntent.putExtra("url", mUrl);
            startActivity(localIntent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}