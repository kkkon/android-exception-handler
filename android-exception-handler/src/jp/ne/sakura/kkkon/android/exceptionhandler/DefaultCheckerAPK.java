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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 * @author Kiyofumi Kondoh
 */
public class DefaultCheckerAPK
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



    private static AlertDialog.Builder setupAlertDialog( final Context context )
    {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder( context );
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
                message = "インストール状態をチェック中です。キャンセルしますか？";
                positive = "待つ";
                negative = "キャンセル";
                needDefaultLang = false;
            }
        }
        if ( needDefaultLang )
        {
            title = "INFO";
            message = "Now checking installation. Cancel check?";
            positive = "Wait";
            negative = "Cancel";
        }
        alertDialog.setTitle( title );
        alertDialog.setMessage( message );
        alertDialog.setPositiveButton( positive, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface di, int i) {
                if ( null != thread )
                {
                    if ( thread.isAlive() )
                    {
                        alertDialog.show();
                    }
                    else
                    {
                        terminate();
                    }
                }
            }
        });

        alertDialog.setNegativeButton( negative, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface di, int i) {
                if ( null != thread )
                {
                    if ( thread.isAlive() )
                    {
                        Log.d( TAG, "request interrupt" );
                        terminate();
                    }
                    else
                    {
                        terminate();
                    }
                }
            }
        } );

        alertDialog.setCancelable( false );

        return alertDialog;
    }

    private static final ZipEntryFilter defaultFilter = new ZipEntryFilter() {
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
    };


    public static boolean checkAPK( final Context context, final ZipEntryFilter filterZipEntry )
    {
        terminate();

        final String apkPath = context.getPackageCodePath(); // API8
        Log.d( TAG, "PackageCodePath: " + apkPath );
        final File fileApk = new File(apkPath);

        final boolean[] result = new boolean[1];
        result[0] = true;

        thread = new Thread( new Runnable() {

            @Override
            public void run() {
                ZipEntryFilter filter = filterZipEntry;
                if ( null == filter )
                {
                    filter = defaultFilter;
                }

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
                            Log.d( TAG, "" + ent.getSize() );
                            final boolean accept = filter.accept( ent );
                            if ( accept )
                            {
                                list.add( ent );
                            }
                        }

                        Log.d( TAG, Build.CPU_ABI );    // API 4
                        Log.d( TAG, Build.CPU_ABI2 );   // API 8

                        final String[] abiArray = {
                            Build.CPU_ABI       // API 4
                            , Build.CPU_ABI2    // API 8
                        };

                        String abiMatched = null;
                        {
                            boolean foundMatched = false;
                            for ( final String abi : abiArray )
                            {
                                if ( null == abi )
                                {
                                    continue;
                                }
                                if ( 0 == abi.length() )
                                {
                                    continue;
                                }

                                for ( final ZipEntry entry : list )
                                {
                                    Log.d( TAG, entry.getName() );

                                    final String prefixABI = "lib/" + abi + "/";
                                    if ( entry.getName().startsWith( prefixABI ) )
                                    {
                                        abiMatched = abi;
                                        foundMatched = true;
                                        break;
                                    }
                                }

                                if ( foundMatched )
                                {
                                    break;
                                }
                            }
                        }
                        Log.d( TAG, "matchedAbi=" + abiMatched );

                        if ( null != abiMatched )
                        {
                            boolean needReInstall = false;

                            for ( final ZipEntry entry : list )
                            {
                                Log.d( TAG, entry.getName() );

                                final String prefixABI = "lib/" + abiMatched + "/";
                                if ( entry.getName().startsWith( prefixABI ) )
                                {
                                    final String jniName = entry.getName().substring( prefixABI.length() );
                                    Log.d( TAG, "jni=" + jniName );

                                    final String strFileDst = context.getApplicationInfo().nativeLibraryDir + "/" + jniName;
                                    Log.d( TAG, strFileDst );
                                    final File fileDst = new File( strFileDst );
                                    if ( ! fileDst.exists() )
                                    {
                                        Log.w( TAG, "needReInstall: content missing " + strFileDst );
                                        needReInstall = true;
                                    }
                                    else
                                    {
                                        assert( entry.getSize() <= Integer.MAX_VALUE );
                                        if ( fileDst.length() != entry.getSize() )
                                        {
                                            Log.w( TAG, "needReInstall: size broken " + strFileDst );
                                            needReInstall = true;
                                        }
                                        else
                                        {
                                            //org.apache.commons.io.IOUtils.contentEquals( zipFile.getInputStream( entry ), new FileInputStream(fileDst) );

                                            final int size = (int)entry.getSize();
                                            byte[] buffSrc = new byte[size];

                                            {
                                                InputStream inStream = null;
                                                try
                                                {
                                                    inStream = zipFile.getInputStream( entry );
                                                    int pos = 0;
                                                    {
                                                        while( pos < size )
                                                        {
                                                            final int ret = inStream.read( buffSrc, pos, size - pos );
                                                            if ( ret <= 0 )
                                                            {
                                                                break;
                                                            }
                                                            pos += ret;
                                                        }
                                                    }
                                                }
                                                catch ( IOException e )
                                                {
                                                    Log.d( TAG, "got exception", e );
                                                }
                                                finally
                                                {
                                                    if ( null != inStream )
                                                    {
                                                        try { inStream.close(); } catch ( Exception e ) { }
                                                    }
                                                }
                                            }
                                            byte[] buffDst = new byte[(int)fileDst.length()];
                                            {
                                                InputStream inStream = null;
                                                try
                                                {
                                                    inStream = new FileInputStream( fileDst );
                                                    int pos = 0;
                                                    {
                                                        while( pos < size )
                                                        {
                                                            final int ret = inStream.read( buffDst, pos, size - pos );
                                                            if ( ret <= 0 )
                                                            {
                                                                break;
                                                            }
                                                            pos += ret;
                                                        }
                                                    }
                                                }
                                                catch ( IOException e )
                                                {
                                                    Log.d( TAG, "got exception", e );
                                                }
                                                finally
                                                {
                                                    if ( null != inStream )
                                                    {
                                                        try { inStream.close(); } catch ( Exception e ) { }
                                                    }
                                                }
                                            }

                                            if( Arrays.equals( buffSrc, buffDst ) )
                                            {
                                                Log.d( TAG, " content equal " + strFileDst );
                                                // OK
                                            }
                                            else
                                            {
                                                Log.w( TAG, "needReInstall: content broken " + strFileDst );
                                                needReInstall = true;
                                            }
                                        }

                                    }

                                }
                            } // for ZipEntry

                            if ( needReInstall )
                            {
                                // need call INSTALL APK
                                Log.w( TAG, "needReInstall apk" );
                                result[0] = false;
                            }
                            else
                            {
                                Log.d( TAG, "no need ReInstall apk" );
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
            }
        });
        thread.setName( "check jni so" );

        thread.start();
        if ( USE_DIALOG )
        {
            final AlertDialog.Builder alertDialogBuilder = setupAlertDialog( context );
            if ( null != alertDialogBuilder )
            {
                alertDialogBuilder.setCancelable( false );

                final AlertDialog alertDialog = alertDialogBuilder.show();

                /*
                while ( thread.isAlive() )
                {
                    Log.d( TAG, "check thread tid=" + android.os.Process.myTid() + ",state=" + thread.getState() );
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
                            alertDialog.dismiss();
                            alertDialog = alertDialogBuilder.show();
                        }
                    }

                    if ( Thread.State.TERMINATED.equals(thread.getState()) )
                    {
                        break;
                    }

                }

                if ( null != alertDialog )
                {
                    alertDialog.dismiss();
                }
                */
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

        return result[0];
    }

}
