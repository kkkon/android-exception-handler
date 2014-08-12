/*
 * The MIT License
 * 
 * Copyright (C) 2014 Kiyofumi Kondoh
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jp.ne.sakura.kkkon.android.exceptionhandler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;

/**
 *
 * @author Kiyofumi Kondoh
 */
public class DefaultUploaderWeb
{
    private static final String TAG = "kk";
    private static final boolean DEBUG = false;
    private static final boolean USE_DIALOG = true;

    private static Thread thread = null;

    @Override
    protected void finalize() throws Throwable {
        try
        {
            terminate();
        }
        finally
        {
            super.finalize();
        }
    }


    public static void terminate()
    {
        if ( null != thread )
        {
            try
            {
                while ( thread.isAlive() )
                {
                    thread.join( 1 * 1000 );
                    if ( thread.isAlive() )
                    {
                        thread.interrupt();
                        thread.join();
                    }
                }
            }
            catch ( InterruptedException e )
            {
                Log.d( TAG, "got Exception", e );
            }
            finally
            {
                thread = null;
            }
        }
    }

    public static AlertDialog.Builder setupAlertDialog( final Context context )
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder( context );
        final Locale defaultLocale = Locale.getDefault();

        String title = "";
        String message = "";
        String positive = "";
        String negative = "";

        boolean needDefaultLang = true;
        if ( null != defaultLocale )
        {
            if ( defaultLocale.equals( Locale.JAPANESE ) || defaultLocale.equals( Locale.JAPAN ) )
            {
                title = "情報";
                message = "エラー送信中です。キャンセルしますか？";
                positive = "待つ";
                negative = "キャンセル";
                needDefaultLang = false;
            }
        }
        if ( needDefaultLang )
        {
            title = "INFO";
            message = "Now uploading error information. Cancel upload?";
            positive = "Wait";
            negative = "Cancel";
        }
        alertDialog.setTitle( title );
        alertDialog.setMessage( message );
        alertDialog.setPositiveButton( positive, null);
        alertDialog.setNegativeButton( negative, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface di, int i) {
                if ( thread.isAlive() )
                {
                    Log.d( TAG, "request interrupt" );
                    thread.interrupt();
                }
                else
                {
                    // nothing
                }
            }
        } );

        return alertDialog;
    }

    public static void upload( final Context context, final File file, final String url )
    {
        terminate();

        thread = new Thread( new Runnable() {

            @Override
            public void run() {
                Log.d( TAG, "upload thread tid=" + android.os.Process.myTid() );
                try
                {
                    //$(BRAND)/$(PRODUCT)/$(DEVICE)/$(BOARD):$(VERSION.RELEASE)/$(ID)/$(VERSION.INCREMENTAL):$(TYPE)/$(TAGS)
                    Log.d( TAG, "fng=" + Build.FINGERPRINT );
                    final List<NameValuePair> list = new ArrayList<NameValuePair>(16);
                    list.add( new BasicNameValuePair( "fng", Build.FINGERPRINT ) );

                    HttpPost    httpPost = new HttpPost( url );
                    //httpPost.getParams().setParameter( CoreConnectionPNames.SO_TIMEOUT, new Integer(5*1000) );
                    httpPost.setEntity( new UrlEncodedFormEntity( list, HTTP.UTF_8 ) );
                    DefaultHttpClient   httpClient = new DefaultHttpClient();
                    Log.d( TAG, "socket.timeout=" + httpClient.getParams().getIntParameter( CoreConnectionPNames.SO_TIMEOUT, -1) );
                    Log.d( TAG, "connection.timeout=" + httpClient.getParams().getIntParameter( CoreConnectionPNames.CONNECTION_TIMEOUT, -1) );
                    httpClient.getParams().setParameter( CoreConnectionPNames.SO_TIMEOUT, new Integer(5*1000) );
                    httpClient.getParams().setParameter( CoreConnectionPNames.CONNECTION_TIMEOUT, new Integer(5*1000) );
                    Log.d( TAG, "socket.timeout=" + httpClient.getParams().getIntParameter( CoreConnectionPNames.SO_TIMEOUT, -1) );
                    Log.d( TAG, "connection.timeout=" + httpClient.getParams().getIntParameter( CoreConnectionPNames.CONNECTION_TIMEOUT, -1) );
                    // <uses-permission android:name="android.permission.INTERNET"/>
                    // got android.os.NetworkOnMainThreadException, run at UI Main Thread
                    HttpResponse response = httpClient.execute( httpPost );
                    Log.d( TAG, "response=" + response.getStatusLine().getStatusCode() );
                }
                catch ( Exception e )
                {
                    Log.d( TAG, "got Exception. msg=" + e.getMessage(), e );
                }
                finally
                {
                    ExceptionHandler.clearReport();
                }
                Log.d( TAG, "upload finish" );
            }
        });
        thread.setName("upload crash");

        thread.start();
        if ( USE_DIALOG )
        {
            final AlertDialog.Builder alertDialogBuilder = setupAlertDialog( context );
            final AlertDialog alertDialog = (null==alertDialogBuilder)?(null):(alertDialogBuilder.show());

            while ( thread.isAlive() )
            {
                Log.d( TAG, "thread tid=" + android.os.Process.myTid() + ",state=" + thread.getState() );
                if ( ! thread.isAlive() )
                {
                    break;
                }

                {
                    try
                    {
                        Thread.sleep( 1 * 1000 );
                    }
                    catch ( InterruptedException e )
                    {
                        Log.d( TAG, "got exception", e );
                    }
                }

                if ( null != alertDialog )
                {
                    if ( alertDialog.isShowing() )
                    {
                    }
                    else
                    {
                        if ( ! thread.isAlive() )
                        {
                            break;
                        }
                        alertDialog.show();
                    }
                }

                if ( ! Thread.State.RUNNABLE.equals(thread.getState()) )
                {
                    break;
                }

            }

            if ( null != alertDialog )
            {
                alertDialog.dismiss();
            }

            try
            {
                thread.join(); // must call. leak handle...
                thread = null;
            }
            catch ( InterruptedException e )
            {
                Log.d( TAG, "got Exception", e );
            }
        }

    }
}
