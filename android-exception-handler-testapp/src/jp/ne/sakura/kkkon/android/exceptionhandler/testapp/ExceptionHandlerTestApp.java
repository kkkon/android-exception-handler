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

import jp.ne.sakura.kkkon.android.exceptionhandler.ExceptionHandler;
import jp.ne.sakura.kkkon.android.exceptionhandler.SettingsCompat;

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
                        Log.i( "appKK", str );
                    }
                    catch ( IOException e )
                    {
                        Log.e( "appKK", "got exception", e );
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

        setContentView( layout );
    }
}
