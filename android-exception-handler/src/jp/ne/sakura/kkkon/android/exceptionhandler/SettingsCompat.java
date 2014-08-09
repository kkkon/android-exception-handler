/*
 * The MIT License
 *
 * Copyright 2014 Kiyofumi Kondoh
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

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Kiyofumi Kondoh
 */
public class SettingsCompat
{
    private static final String TAG = "kk";
    private static final boolean DEBUG = false;

    private static final String INSTALL_NON_MARKET_APPS = "INSTALL_NON_MARKET_APPS";

    private static ContentResolver contentResolver = null;
    private static Class<?>    settingsSystem = null;  // API 1-2
    private static Class<?>    settingsSecure = null;  // API 3-16
    private static Class<?>    settingsGlobal = null;  // API 17-

    static public boolean initialize( Context context )
    {
        boolean result = true;

        contentResolver = null;
        settingsSystem = null;
        settingsSecure = null;
        settingsGlobal = null;

        if ( null == context )
        {
            result = false;
        }
        else
        {
            contentResolver = context.getContentResolver();
        }

        try
        {
            final Class<?>    settings = Class.forName("android.provider.Settings");
            if ( null != settings )
            {
                final Class<?>[]  settingsClasses = settings.getClasses();
                if ( null != settingsClasses )
                {
                    for ( final Class<?> clazz : settingsClasses )
                    {
                        if ( null == clazz )
                        {
                            continue;
                        }

                        if ( "android.provider.Settings$Global".equals( clazz.getName() ) )
                        {
                            settingsGlobal = clazz;
                        }
                        else if ( "android.provider.Settings$Secure".equals( clazz.getName() ) )
                        {
                            settingsSecure = clazz;
                        }
                        else if ( "android.provider.Settings$System".equals( clazz.getName() ) )
                        {
                            settingsSystem = clazz;
                        }
                    }
                }
            }
        }
        catch ( ClassNotFoundException e )
        {
            Log.e( TAG, "got Exception:", e );
            result = false;
        }

        return result;
    }

    static public boolean isAllowedNonMarketApps()
    {
        final int apiLevel;
        try
        {
            apiLevel = Integer.valueOf( Build.VERSION.SDK );
        }
        catch ( NumberFormatException e)
        {
            Log.d( TAG, "got Exception:", e );
        }

        boolean allowed = false;
        //if ( 17 <= apiLevel )
        {
            if ( null != settingsGlobal )
            {
                try
                {
                    final java.lang.reflect.Method method_getInt = settingsGlobal.getMethod( "getInt", new Class[] { ContentResolver.class, String.class, int.class } );
                    final java.lang.reflect.Field field_INSTALL_NON_MARKET_APPS = settingsGlobal.getField( INSTALL_NON_MARKET_APPS );
                    Log.d( TAG, "" + method_getInt );
                    Log.d( TAG, "" + field_INSTALL_NON_MARKET_APPS );
                    if ( null != method_getInt && null != field_INSTALL_NON_MARKET_APPS )
                    {
                        try
                        {
                            final String constValue = (String) field_INSTALL_NON_MARKET_APPS.get(null);
                            final Object ret = method_getInt.invoke( null, contentResolver, constValue, 0 );
                            Log.d( TAG, "Global.constValue=" + constValue );
                            Log.d( TAG, "Global.ret=" + ret );
                            if ( null != ret )
                            {
                                if ( 0 != ((Integer)ret).intValue() )
                                {
                                    allowed = true;
                                }
                            }
                        }
                        catch ( IllegalAccessException e )
                        {
                            Log.e( TAG, "got Exception:", e );
                        }
                        catch ( InvocationTargetException e )
                        {
                            Log.e( TAG, "got Exception:", e );
                        }
                        catch ( IllegalArgumentException e )
                        {
                            Log.e( TAG, "got Exception:", e );
                        }
                    }
                }
                catch ( NoSuchMethodException e )
                {
                    Log.e( TAG, "got Exception:", e );
                }
                catch ( NoSuchFieldException e )
                {
                    Log.e( TAG, "got Exception:", e );
                }
            }
        }

        //if ( 3 <= apiLevel )
        {
            if ( null != settingsSecure )
            {
                try
                {
                    final java.lang.reflect.Method method_getInt = settingsSecure.getMethod( "getInt", new Class[] { ContentResolver.class, String.class, int.class } );
                    final java.lang.reflect.Field field_INSTALL_NON_MARKET_APPS = settingsSecure.getField( INSTALL_NON_MARKET_APPS );
                    Log.d( TAG, "" + method_getInt );
                    Log.d( TAG, "" + field_INSTALL_NON_MARKET_APPS );
                    if ( null != method_getInt && null != field_INSTALL_NON_MARKET_APPS )
                    {
                        try
                        {
                            final String constValue = (String) field_INSTALL_NON_MARKET_APPS.get(null);
                            final Object ret = method_getInt.invoke( null, contentResolver, constValue, 0 );
                            Log.d( TAG, "Secure.constValue=" + constValue );
                            Log.d( TAG, "Secure.ret=" + ret );
                            if ( null != ret )
                            {
                                if ( 0 != ((Integer)ret).intValue() )
                                {
                                    allowed = true;
                                }
                            }
                        }
                        catch ( IllegalAccessException e )
                        {
                            Log.e( TAG, "got Exception:", e );
                        }
                        catch ( InvocationTargetException e )
                        {
                            Log.e( TAG, "got Exception:", e );
                        }
                        catch ( IllegalArgumentException e )
                        {
                            Log.e( TAG, "got Exception:", e );
                        }
                    }
                }
                catch ( NoSuchMethodException e )
                {
                    Log.e( TAG, "got Exception:", e );
                }
                catch ( NoSuchFieldException e )
                {
                    Log.e( TAG, "got Exception:", e );
                }
            }
        }

        //if ( 1 <= apiLevel )
        {
            if ( null != settingsSystem )
            {
                try
                {
                    final java.lang.reflect.Method method_getInt = settingsSystem.getMethod( "getInt", new Class[] { ContentResolver.class, String.class, int.class } );
                    final java.lang.reflect.Field field_INSTALL_NON_MARKET_APPS = settingsSystem.getField( INSTALL_NON_MARKET_APPS );
                    Log.d( TAG, "" + method_getInt );
                    Log.d( TAG, "" + field_INSTALL_NON_MARKET_APPS );
                    if ( null != method_getInt && null != field_INSTALL_NON_MARKET_APPS )
                    {
                        try
                        {
                            final String constValue = (String) field_INSTALL_NON_MARKET_APPS.get(null);
                            final Object ret = method_getInt.invoke( null, contentResolver, constValue, 0 );
                            Log.d( TAG, "System.constValue=" + constValue );
                            Log.d( TAG, "System.ret=" + ret );
                            if ( null != ret )
                            {
                                if ( 0 != ((Integer)ret).intValue() )
                                {
                                    allowed = true;
                                }
                            }
                        }
                        catch ( IllegalAccessException e )
                        {
                            Log.e( TAG, "got Exception:", e );
                        }
                        catch ( InvocationTargetException e )
                        {
                            Log.e( TAG, "got Exception:", e );
                        }
                        catch ( IllegalArgumentException e )
                        {
                            Log.e( TAG, "got Exception:", e );
                        }
                    }
                }
                catch ( NoSuchMethodException e )
                {
                    Log.e( TAG, "got Exception:", e );
                }
                catch ( NoSuchFieldException e )
                {
                    Log.e( TAG, "got Exception:", e );
                }
            }
        }

        return allowed;
    }

}
