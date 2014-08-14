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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import jp.ne.sakura.kkkon.android.exceptionhandler.ExceptionHandler;

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
                Intent reportIntent = new Intent( context, ExceptionHandlerReportApp.class );
                try
                {
                    this.startActivity( reportIntent );
                }
                catch ( ActivityNotFoundException e )
                {
                    Log.d( TAG, "need add Activity\n" + "<activity android:name=\"" + ExceptionHandlerReportApp.class.getName()  + "\"/>\n" );
                }
            }
            Log.d( TAG, "pre registHandler" );
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
        btn2.setText( "switch Activity" );
        btn2.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                Intent reportIntent = new Intent( context, ExceptionHandlerReportApp.class );
                try
                {
                    startActivity( reportIntent );
                }
                catch ( ActivityNotFoundException e )
                {
                    Log.d( TAG, "need add Activity\n" + "<activity android:name=\"" + ExceptionHandlerReportApp.class.getName()  + "\"/>\n", e );
                }
            }
        } );
        layout.addView( btn2 );

        setContentView( layout );
    }
}
