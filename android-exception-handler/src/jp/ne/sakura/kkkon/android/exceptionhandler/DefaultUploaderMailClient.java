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
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 *
 * @author Kiyofumi Kondoh
 */
public class DefaultUploaderMailClient
{
    private static final String TAG = "kk";

    public static void upload( final Context context, final File file, final String[] mailAddrs )
    {
        StringBuilder sb = new StringBuilder();
        {
            InputStream inStream = null;
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

                Log.i( TAG, sb.toString() );
            }
            catch ( Exception e )
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
        }

        {
            final Intent mailto = new Intent();
            mailto.setAction( Intent.ACTION_SENDTO );
            mailto.setType( "message/rfc822" );
            mailto.setData( Uri.parse("mailto:") );
            mailto.putExtra( Intent.EXTRA_EMAIL, mailAddrs );
            mailto.putExtra( Intent.EXTRA_SUBJECT, "[BugReport] " + context.getPackageName() );
            mailto.putExtra( Intent.EXTRA_TEXT, sb.toString() );
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

        ExceptionHandler.clearReport();
    }

}
