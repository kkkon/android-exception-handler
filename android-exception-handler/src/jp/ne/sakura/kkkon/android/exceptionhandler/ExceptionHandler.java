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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 *
 * @author Kiyofumi Kondoh
 */
public class ExceptionHandler
{
    private static final String TAG = "kk";
    private static final boolean DEBUG = false;

    private static File fileBugReport;

    public static void LogD( final String msg )
    {
        if ( DEBUG )
        {
            Log.d( TAG, msg );
        }
    }

    public static void LogD( final String msg, Throwable throwable )
    {
        if ( DEBUG )
        {
            Log.d( TAG, msg, throwable );
        }
    }

    public static void LogI( final String msg )
    {
        if ( DEBUG )
        {
            Log.i( TAG, msg );
        }
    }

    public static void LogE( final String msg )
    {
        if ( DEBUG )
        {
            Log.e( TAG, msg );
        }
    }

    public static boolean initialize( final Context context )
    {
        boolean result = false;

        try
        {
            final int pid = android.os.Process.myPid();
            LogD( "pid=" + pid );

            // android.os.Build.VERSION.SDK_INT: API 4
            LogD( "android SDK=" + android.os.Build.VERSION.SDK );
            final int android_sdk_value = Integer.valueOf(android.os.Build.VERSION.SDK);

           if ( null == context )
            {
                LogE( "context is null" );
            }
            else
            {
                {
                    packageName = context.getPackageName();
                    LogD( "packageName=" + packageName );
                }

                {
                    final PackageManager pm = context.getPackageManager();
                    if ( null == pm )
                    {
                        LogE( "PackageManager is null" );
                    }
                    else
                    {
                        final PackageInfo packageInfo = pm.getPackageInfo( packageName, 0 );
                        if ( null == packageInfo )
                        {
                            LogE( "PackageInfo is null" );
                        }
                        else
                        {
                            versionName = packageInfo.versionName;
                            LogD( "versionName=" + versionName );
                        }
                    }
                }

                {
                    if ( null == packageName )
                    {
                        packageName = "unknown package";
                    }
                    if ( null == versionName )
                    {
                        versionName = "unknown ver";
                    }
                }

                //context.getSharedPreferences(TAG, pid)
                if ( 9 <= android_sdk_value )
                {
                    final File filesDir = context.getFilesDir();
                    LogD( filesDir.getPath() );
                    final long freeSpaceBytes;
                    freeSpaceBytes = filesDir.getFreeSpace(); // API 9
                    LogD( "freeSpaceBytes=" + freeSpaceBytes );
                    {
                        long value = freeSpaceBytes;
                        long disp = 0;
                        int index = 0;
                        while( 10*1024 < value )
                        {
                            disp = value % 1024;
                            value = value / 1024;
                            index += 1;
                        }
                        final String[] str = { "", "KiB", "MiB", "GiB" };
                        if ( index < str.length )
                        {
                            LogD( " freeSpace=" + value + "." + disp +  str[index] );
                        }
                    }

                    // TODO check free space
                }

                {
                    final String fileName;
                    {
                        final File dir = context.getCacheDir();
                        if ( null == dir )
                        {
                            fileName = "/data/local/tmp/" + "crash-log" + "_" + packageName + "_" + versionName;
                        }
                        else
                        {
                            if ( dir.getAbsolutePath().endsWith("/") )
                            {
                                fileName = dir.getAbsolutePath() + "" + "crash-log";
                            }
                            else
                            {
                                fileName = dir.getAbsolutePath() + "/" + "crash-log";
                            }
                        }
                    }
                    fileBugReport = new File( fileName + ".txt" );
                    LogD( fileBugReport.getAbsolutePath() );
                }

                // /data/app/*.apk
                {
                    File fileDataApp = new File("/data/app");
                    // doesn't work. listing not allowed for normal user
                    {
                        LogD( "apk list start" );
                        File[] fileList = fileDataApp.listFiles();
                        if ( null != fileList )
                        {
                            for ( final File file : fileList )
                            {
                                if ( null != file )
                                {
                                    LogD( file.getName() );
                                }
                            }
                        }
                        LogD( "apk list end" );
                    }

                    for ( int i = 0; i < 10; ++i )
                    {
                        File fileApk = new File("/data/app/" + packageName + "-" + i + ".apk" );
                        LogD( "check apk:" + fileApk.getAbsolutePath() );
                        if ( fileApk.exists() )
                        {
                            LogI( "apk found. path=" + fileApk.getAbsolutePath() );
                            // require permission INSTALL_PACKAGES 
                            //final String strCmd = "pm install -r " + fileApk.getAbsolutePath();
                            //Runtime.getRuntime().exec( strCmd );
                            break;
                        }
                    }
                }
            }
        }
        catch ( Exception e )
        {
            LogD( "got exception:", e );
            result = false;
        }

        return result;
    }

    public static String getBugReportFileAbsolutePath()
    {
        if ( null == fileBugReport )
        {
            LogE( "please call initialize" );
            return "";
        }

        return fileBugReport.getAbsolutePath();
    }

    public static boolean needReport()
    {
        boolean need = false;

        if ( null != fileBugReport )
        {
            LogD( fileBugReport.getAbsolutePath() );
        }

        if ( null == fileBugReport )
        {
            return false;
        }
        else
        {
            if ( fileBugReport.exists() )
            {
                LogD( "Crash Bug Report found" );
                need = true;
            }
        }

        return need;
    }

    public static boolean clearReport()
    {
        boolean clear = true;

        if ( null != fileBugReport )
        {
            LogD( fileBugReport.getAbsolutePath() );
        }

        if ( null == fileBugReport )
        {
            return true;
        }
        else
        {
            if ( fileBugReport.exists() )
            {
                fileBugReport.delete();
                LogD( "Crash Bug Report deleted" );
                clear = true;
            }
        }

        return clear;
    }

    public static boolean registHandler()
    {
        clearReport();

        boolean result = true;

        try
        {
            final int pid = android.os.Process.myPid();
            LogD( "pid=" + pid );


            Thread thread = Thread.currentThread();
            if ( null == thread )
            {
                {
                    oldHandler = Thread.getDefaultUncaughtExceptionHandler();

                    Thread.setDefaultUncaughtExceptionHandler( myHandler );
                }
            }
            else
            {
                synchronized( thread )
                {
                    oldHandler = Thread.getDefaultUncaughtExceptionHandler();

                    Thread.setDefaultUncaughtExceptionHandler( myHandler );
                }
            }
        }
        catch ( Exception e )
        {
            LogD( "got exception:", e );
            result = false;
        }

        return result;
    }

    private static Thread.UncaughtExceptionHandler oldHandler;
    private static Thread.UncaughtExceptionHandler myHandler = new MyHandler();

    private static String packageName;
    private static String versionName;

    public static class MyHandler implements Thread.UncaughtExceptionHandler
    {
        private volatile boolean inCrashing = false;

        @Override
        public void uncaughtException(Thread t, Throwable exception )
        {
            try
            {
                if ( inCrashing )
                {
                    return;
                }
                inCrashing = true;

                LogD( "uncaughtException" );
                if ( null == fileBugReport )
                {
                }
                else
                {
                    if ( fileBugReport.exists() )
                    {
                        fileBugReport.delete();
                    }

                    try
                    {
                        if ( fileBugReport.createNewFile() )
                        {
                            ;
                        }
                    }
                    catch ( IOException e )
                    {

                    }
                }

                OutputStream outputStream = null;
                try
                {
                    try
                    {
                        outputStream = new FileOutputStream( fileBugReport, false );
                    }
                    catch ( FileNotFoundException e )
                    {

                    }

                    if ( null != outputStream )
                    {
                        outputStream.write( packageName.getBytes() );
                        outputStream.write( "\n".getBytes() );
                        outputStream.write( versionName.getBytes() );
                        outputStream.write( "\n".getBytes() );

                        final String str = Log.getStackTraceString( exception );
                        outputStream.write( str.getBytes() );
                        outputStream.write( "\n".getBytes() );
                    }
                    if ( null != outputStream )
                    {
                        final Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
                        if ( null != map )
                        {
                            for ( final Map.Entry<Thread, StackTraceElement[]> entry : map.entrySet() )
                            {
                                outputStream.write( "\n".getBytes() );
                                if ( null != entry )
                                {
                                    final Thread thread = entry.getKey();
                                    final StackTraceElement[] stackArray = entry.getValue();
                                    if ( null == thread )
                                    {

                                    }
                                    else
                                    {
                                        final StringBuilder sb = new StringBuilder();
                                        sb.append( thread.getId() );
                                        sb.append( " " );
                                        sb.append( thread.getName() );
                                        String strState = "";
                                        {
                                            final Thread.State state = thread.getState();
                                            if ( null != state )
                                            {
                                                strState = state.name();
                                            }
                                        }
                                        sb.append( " " );
                                        sb.append( strState );
                                        sb.append( "\n" );
                                        final String str = sb.toString();
                                        LogD( str );
                                        outputStream.write( str.getBytes() );
                                    }
                                    if ( null == stackArray )
                                    {

                                    }
                                    else
                                    {
                                        for ( final StackTraceElement stack : stackArray )
                                        {
                                            final String str = "\tat " + stack.toString() + "\n";
                                            LogD( str );
                                            outputStream.write( str.getBytes() );
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch ( IOException e )
                {
                    LogD( e.toString() );
                }
                finally
                {
                    if ( null != outputStream )
                    {
                        try { outputStream.flush(); } catch ( Exception e ) {  }
                        try { outputStream.close(); } catch ( Exception e ) {  }
                    }
                    outputStream = null;
                }
            }
            catch ( Throwable throwable )
            {
                
            }

            if ( DEBUG )
            {
                Runtime.getRuntime().exit( 0 );
            }

            if ( null == oldHandler )
            {
                Runtime.getRuntime().exit( 0 );
            }
            else
            {
                oldHandler.uncaughtException( t, exception );
            }
        }

    }
}
