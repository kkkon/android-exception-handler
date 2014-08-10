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
package jp.ne.sakura.kkkon.android.exceptionhandler.testapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import jp.ne.sakura.kkkon.android.exceptionhandler.ExceptionHandler;
import jp.ne.sakura.kkkon.android.exceptionhandler.SettingsCompat;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

/**
 *
 * @author Kiyofumi Kondoh
 */
public class ExceptionHandlerTestApp extends Activity
{
    public static final String TAG = "appKK";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        final Context context = this.getApplicationContext();

        {
            ExceptionHandler.initialize( context );
            if ( ExceptionHandler.needReport() )
            {
                final String fileName = ExceptionHandler.getBugReportFileAbsolutePath();
                File file = new File( fileName );
                if ( file.exists() )
                {
                    Log.d( TAG, file.getAbsolutePath() );
                    InputStream inStream = null;
                    ZipOutputStream outStream = null;
                    try
                    {
                        inStream = new FileInputStream( file );
                        String strFileZip = file.getAbsolutePath();
                        {
                            int index = strFileZip.lastIndexOf( '.' );
                            if ( 0 < index )
                            {
                                strFileZip = strFileZip.substring( 0, index );
                                strFileZip += ".zip";
                            }
                        }
                        Log.d( TAG, strFileZip );
                        String strFileName = file.getAbsolutePath();
                        {
                            int index = strFileName.lastIndexOf( File.separatorChar );
                            if ( 0 < index )
                            {
                                strFileName = strFileName.substring( index+1 );
                            }
                        }
                        Log.d( TAG, strFileName );

                        outStream = new ZipOutputStream( new FileOutputStream(strFileZip) );
                        byte[] buff = new byte[8124];
                        {
                            ZipEntry entry = new ZipEntry( strFileName );
                            outStream.putNextEntry( entry );

                            int len = 0;
                            while ( 0 < (len = inStream.read( buff ) ) )
                            {
                                outStream.write( buff, 0, len );
                            }
                            outStream.closeEntry();
                        }
                        outStream.finish();
                        outStream.flush();

                    }
                    catch ( IOException e )
                    {
                        Log.e( TAG, "got exception", e );
                    }
                    finally
                    {
                        if ( null != outStream )
                        {
                            try { outStream.close(); } catch( Exception e ) { }
                        }
                        outStream = null;

                        if ( null != inStream )
                        {
                            try { inStream.close(); } catch( Exception e ) { }
                        }
                        inStream = null;
                    }
                    Log.i( TAG, "zip created" );
                }

                if ( file.exists() )
                {
                    // upload or send e-mail
                    InputStream inStream = null;
                    StringBuilder sb = new StringBuilder();
                    try
                    {
                        inStream = new FileInputStream( file );
                        byte[] buff = new byte[8124];
                        int readed = 0;
                        do
                        {
                            readed = inStream.read(buff);
                            for ( int i = 0; i < readed; i++ )
                            {
                                sb.append( (char)buff[i] );
                            }
                        } while( readed >= 0);

                        final String str = sb.toString();
                        Log.i( TAG, str );
                    }
                    catch ( IOException e )
                    {
                        Log.e( TAG, "got exception", e );
                    }
                    finally
                    {
                        if ( null != inStream )
                        {
                            try { inStream.close(); } catch( Exception e ) { }
                        }
                        inStream = null;
                    }

                    AlertDialog.Builder alertDialog = new AlertDialog.Builder( this );
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
                            title = "エラー";
                            message = "エラーを検出しました。エラー情報を送信しますか？";
                            positive = "送信";
                            negative = "キャンセル";
                            needDefaultLang = false;
                        }
                    }
                    if ( needDefaultLang )
                    {
                        title = "ERROR";
                        message = "Got unexpected error. Do you want to send information of error.";
                        positive = "Send";
                        negative = "Cancel";
                    }
                    alertDialog.setTitle( title );
                    alertDialog.setMessage( message );
                    alertDialog.setPositiveButton( positive, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface di, int i) {
                            // TODO
                        }
                    } );
                    alertDialog.setNegativeButton( negative, null );
                    alertDialog.show();
                }
            }
            ExceptionHandler.clearReport();
            ExceptionHandler.registHandler();
        }

        super.onCreate(savedInstanceState);

        /* Create a TextView and set its content.
         * the text is retrieved by calling a native
         * function.
         */
        LinearLayout layout = new LinearLayout( this );
        layout.setOrientation( LinearLayout.VERTICAL );

        TextView  tv = new TextView(this);
        tv.setText( "ExceptionHandler" );
        layout.addView( tv );

        Button btn1 = new Button( this );
        btn1.setText( "invoke Exception" );
        btn1.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                final int count = 2;
                int[] array = new int[count];
                int value = array[count]; // invoke IndexOutOfBOundsException
            }
        } );
        layout.addView( btn1 );

        Button btn2 = new Button( this );
        btn2.setText( "reinstall apk" );
        btn2.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                boolean foundApk = false;
                {
                    final String apkPath = context.getPackageCodePath(); // API8
                    Log.d( TAG, "PackageCodePath: " + apkPath );
                    final File fileApk = new File(apkPath);
                    if ( fileApk.exists() )
                    {
                        foundApk = true;

                        Intent promptInstall = new Intent(Intent.ACTION_VIEW);
                        promptInstall.setDataAndType(Uri.fromFile( fileApk ), "application/vnd.android.package-archive");
                        promptInstall.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                        context.startActivity( promptInstall );
                    }
                }

                if ( false == foundApk )
                {
                    for ( int i = 0; i < 10; ++i )
                    {
                        File fileApk = new File("/data/app/" + context.getPackageName() + "-" + i + ".apk" );
                        Log.d( TAG, "check apk:" + fileApk.getAbsolutePath() );
                        if ( fileApk.exists() )
                        {
                            Log.i( TAG, "apk found. path=" + fileApk.getAbsolutePath() );
                            /*
                             * // require parmission
                            {
                                final String strCmd = "pm install -r " + fileApk.getAbsolutePath();
                                try
                                {
                                    Runtime.getRuntime().exec( strCmd );
                                }
                                catch ( IOException e )
                                {
                                    Log.e( TAG, "got exception", e );
                                }
                            }
                            */
                            Intent promptInstall = new Intent(Intent.ACTION_VIEW);
                            promptInstall.setDataAndType(Uri.fromFile( fileApk ), "application/vnd.android.package-archive");
                            promptInstall.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                            context.startActivity( promptInstall );
                            break;
                        }
                    }
                }
            }
        } );
        layout.addView( btn2 );

        Button btn3 = new Button( this );
        btn3.setText( "check apk" );
        btn3.setOnClickListener( new View.OnClickListener() {
            private boolean checkApk( final File fileApk, ZipEntryFilter filter )
            {
                boolean result = false;

                if ( fileApk.exists() )
                {
                    ZipFile zipFile = null;
                    try
                    {
                        zipFile = new ZipFile( fileApk );
                        List<ZipEntry> list = new ArrayList<ZipEntry>( zipFile.size() );
                        for ( Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements(); )
                        {
                            ZipEntry ent = e.nextElement();
                            Log.d( TAG, ent.getName() );
                            final boolean accept = filter.accept( ent );
                            if ( accept )
                            {
                                list.add( ent );
                            }
                        }
                    }
                    catch ( IOException e )
                    {
                        Log.d( TAG, "got exception", e );
                    }
                    finally
                    {
                        if ( null != zipFile )
                        {
                            try { zipFile.close(); } catch ( Exception e ) { }
                        }
                    }
                }

                return result;
            }

            @Override
            public void onClick(View view)
            {
                boolean foundApk = false;
                {
                    final String apkPath = context.getPackageCodePath(); // API8
                    Log.d( TAG, "PackageCodePath: " + apkPath );
                    final File fileApk = new File(apkPath);
                    this.checkApk( fileApk, new ZipEntryFilter() {
                        @Override
                        public boolean accept( ZipEntry entry )
                        {
                            if ( entry.isDirectory() )
                            {
                                return false;
                            }

                            final String filename = entry.getName();
                            if ( filename.startsWith("lib/") )
                            {
                                return true;
                            }

                            return false;
                        }
                    } );
                }

            }
        } );
        layout.addView( btn3 );

        Button btn4 = new Button( this );
        btn4.setText( "print dir and path" );
        btn4.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                {
                    final File file = context.getCacheDir();
                    Log.d( TAG, "Ctx.CacheDir=" + file.getAbsoluteFile() );
                }
                {
                    final File file = context.getExternalCacheDir(); // API 8
                    Log.d( TAG, "Ctx.ExternalCacheDir=" + file.getAbsolutePath() );
                }
                {
                    final File file = context.getFilesDir();
                    Log.d( TAG, "Ctx.FilesDir=" + file.getAbsolutePath() );
                }
                {
                    final String value = context.getPackageResourcePath();
                    Log.d( TAG, "Ctx.PackageResourcePath=" + value );
                }
                {
                    final String[] files = context.fileList();
                    if ( null == files )
                    {
                        Log.d( TAG, "Ctx.fileList=" + files );
                    }
                    else
                    {
                        for ( final String filename : files )
                        {
                            Log.d( TAG, "Ctx.fileList=" + filename );
                        }
                    }
                }


                {
                    final File file = Environment.getDataDirectory();
                    Log.d( TAG, "Env.DataDirectory=" + file.getAbsolutePath() );
                }
                {
                    final File file = Environment.getDownloadCacheDirectory();
                    Log.d( TAG, "Env.DownloadCacheDirectory=" + file.getAbsolutePath() );
                }
                {
                    final File file = Environment.getExternalStorageDirectory();
                    Log.d( TAG, "Env.ExternalStorageDirectory=" + file.getAbsolutePath() );
                }
                {
                    final File file = Environment.getRootDirectory();
                    Log.d( TAG, "Env.RootDirectory=" + file.getAbsolutePath() );
                }
                {
                    final ApplicationInfo appInfo = context.getApplicationInfo();
                    Log.d( TAG, "AppInfo.dataDir=" + appInfo.dataDir );
                    Log.d( TAG, "AppInfo.nativeLibraryDir=" + appInfo.nativeLibraryDir ); // API 9
                    Log.d( TAG, "AppInfo.publicSourceDir=" + appInfo.publicSourceDir );
                    {
                        final String[] sharedLibraryFiles = appInfo.sharedLibraryFiles;
                        if ( null == sharedLibraryFiles )
                        {
                            Log.d( TAG, "AppInfo.sharedLibraryFiles=" + sharedLibraryFiles );
                        }
                        else
                        {
                            for ( final String fileName : sharedLibraryFiles )
                            {
                                Log.d( TAG, "AppInfo.sharedLibraryFiles=" + fileName );
                            }
                        }
                    }
                    Log.d( TAG, "AppInfo.sourceDir=" + appInfo.sourceDir );
                }
                {
                    Log.d( TAG, "System.Properties start" );
                    final Properties properties = System.getProperties();
                    if ( null != properties )
                    {
                        for ( final Object key: properties.keySet() )
                        {
                            String value = properties.getProperty( (String)key );
                            Log.d( TAG, " key=" + key + ",value=" + value );
                        }
                    }
                    Log.d( TAG, "System.Properties end" );
                }
                {
                    Log.d( TAG, "System.getenv start" );
                    final Map<String,String>  mapEnv = System.getenv();
                    if ( null != mapEnv )
                    {
                        for ( final Map.Entry<String,String> entry : mapEnv.entrySet() )
                        {
                            final String key = entry.getKey();
                            final String value = entry.getValue();
                            Log.d( TAG, " key=" + key + ",value=" + value );
                        }
                    }
                    Log.d( TAG, "System.getenv end" );
                }
            }
        } );
        layout.addView( btn4 );

        Button btn5 = new Button( this );
        btn5.setText( "check INSTALL_NON_MARKET_APPS" );
        btn5.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                SettingsCompat.initialize( context );
                if ( SettingsCompat.isAllowedNonMarketApps() )
                {
                    Log.d( TAG, "isAllowdNonMarketApps=true" );
                }
                else
                {
                    Log.d( TAG, "isAllowdNonMarketApps=false" );
                }
            }
        } );
        layout.addView( btn5 );

        Button btn6 = new Button( this );
        btn6.setText( "send email" );
        btn6.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                Intent mailto = new Intent();
                mailto.setAction( Intent.ACTION_SENDTO );
                mailto.setType( "message/rfc822" );
                mailto.setData( Uri.parse("mailto:") );
                mailto.putExtra( Intent.EXTRA_EMAIL, new String[] { "" } );
                mailto.putExtra( Intent.EXTRA_SUBJECT, "[BugReport] " + context.getPackageName() );
                mailto.putExtra( Intent.EXTRA_TEXT, "body text" );
                //mailto.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                //context.startActivity( mailto );
                Intent intent = Intent.createChooser( mailto, "Send Email" );
                if ( null != intent )
                {
                    intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                    try
                    {
                        context.startActivity( intent );
                    }
                    catch ( android.content.ActivityNotFoundException e )
                    {
                        Log.d( TAG, "got Exception", e );
                    }
                }
            }
        } );
        layout.addView( btn6 );

        Button btn7 = new Button( this );
        btn7.setText( "upload http thread" );
        btn7.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                Log.d( TAG, "brd=" + Build.BRAND );
                Log.d( TAG, "prd=" + Build.PRODUCT );

                //$(BRAND)/$(PRODUCT)/$(DEVICE)/$(BOARD):$(VERSION.RELEASE)/$(ID)/$(VERSION.INCREMENTAL):$(TYPE)/$(TAGS)
                Log.d( TAG, "fng=" + Build.FINGERPRINT );
                final List<NameValuePair> list = new ArrayList<NameValuePair>(16);
                list.add( new BasicNameValuePair( "fng", Build.FINGERPRINT ) );

                final Thread thread = new Thread( new Runnable() {

                    @Override
                    public void run() {
                        Log.d( TAG, "upload thread id=" + Thread.currentThread().getId() );
                        try
                        {
                            HttpPost    httpPost = new HttpPost( "http://kkkon.sakura.ne.jp/android/bug" );
                            httpPost.setEntity( new UrlEncodedFormEntity( list, HTTP.UTF_8 ) );
                            DefaultHttpClient   httpClient = new DefaultHttpClient();
                            // <uses-permission android:name="android.permission.INTERNET"/>
                            // got android.os.NetworkOnMainThreadException, run at UI Main Thread
                            HttpResponse response = httpClient.execute( httpPost );
                            Log.d( TAG, "response=" + response.getStatusLine().getStatusCode() );
                        }
                        catch ( Exception e )
                        {
                            Log.d( TAG, "got Exception", e );
                        }
                        Log.d( TAG, "upload finish" );
                    }
                });
                thread.setName("upload crash");

                thread.start();
                /*
                while ( thread.isAlive() )
                {
                    Log.d( TAG, "thread.id=" + thread.getId() + ",state=" + thread.getState() );
                    if ( ! thread.isAlive() )
                    {
                        break;
                    }
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder( ExceptionHandlerTestApp.this );
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

                    if ( ! thread.isAlive() )
                    {
                        break;
                    }

                    alertDialog.show();

                    if ( ! Thread.State.RUNNABLE.equals(thread.getState()) )
                    {
                        break;
                    }

                }
                */

                /*
                try
                {
                    thread.join(); // must call. leak handle...
                }
                catch ( InterruptedException e )
                {
                    Log.d( TAG, "got Exception", e );
                }
                */
            }
        } );
        layout.addView( btn7 );

        setContentView( layout );
    }
}
