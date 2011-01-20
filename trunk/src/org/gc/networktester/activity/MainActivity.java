/*
 *
 * Copyright (C) 2010 Guillaume Cottenceau.
 *
 * Android Network Tester is licensed under the Apache 2.0 license.
 *
 */

package org.gc.networktester.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gc.networktester.R;
import org.gc.networktester.tester.Download100kbTester;
import org.gc.networktester.tester.Download10kbTester;
import org.gc.networktester.tester.Download1mbTester;
import org.gc.networktester.tester.HostResolutionTester;
import org.gc.networktester.tester.RealWebTester;
import org.gc.networktester.tester.TcpConnectionTester;
import org.gc.networktester.tester.Tester;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private Button buttonStartStop;
    
    private List<Tester> testers;
    
    private volatile boolean running = false;
    private volatile boolean wantStop = false;
    
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        
        super.onCreate( savedInstanceState );
        
        testers = new ArrayList<Tester>();
        testers.add( new HostResolutionTester() );
        testers.add( new TcpConnectionTester() );
        testers.add( new RealWebTester() );
        testers.add( new Download10kbTester() );
        testers.add( new Download100kbTester() );
        testers.add( new Download1mbTester() );

        setContentView( R.layout.main );
        setupViews();
        
        // disable java level DNS caching
        System.setProperty( "networkaddress.cache.ttl", "0" );
        System.setProperty( "networkaddress.cache.negative.ttl", "0" );

        // display network type      
        NetworkInfo netinfo
            = ( (ConnectivityManager) getSystemService( Context.CONNECTIVITY_SERVICE ) ).getActiveNetworkInfo();
        String type = netinfo == null ? "unknown"
                                      : netinfo.getSubtypeName().length() == 0 ? netinfo.getTypeName()
                                                                               : netinfo.getTypeName() + "/" + netinfo.getSubtypeName();
        String str = String.format( getResources().getString( R.string.network_type ), type );
        ( (TextView) findViewById( R.id.main__text_network_type ) ).setText( str );        
    }

    private void setupViews() {
        buttonStartStop = (Button) findViewById( R.id.main__button_startstop );
        buttonStartStop.setOnClickListener( new OnClickListener() {
            public void onClick( View v ) {
                if ( running ) {
                    wantStop = true;
                } else {
                    running = true;
                    wantStop = false;
                    launch();
                }
            }
        } );
        
        for ( Tester tester : testers ) {
            tester.setupViews( this );
        }
        
    }
    
    private void launch() {
        final Map<Tester, Boolean> areActive = new HashMap<Tester, Boolean>();
        for ( Tester tester : testers ) {
            areActive.put( tester, tester.prepareTestAndIsActive() );
        }
        buttonStartStop.setText( R.string.stop_tests );
        new Thread() {
            @Override
            public void run() {
                try {
                    for ( Tester tester : testers ) {
                        if ( areActive.get( tester ) ) {
                            Log.d( this.toString(), "Launch test " + tester );
                            if ( ! tester.performTest() || wantStop ) {
                                return;
                            }
                        }
                    }
                } finally {
                    runOnUiThread( new Thread() { public void run() {
                        running = false;
                        for ( Tester tester : testers ) {
                            tester.cleanupTests();
                        }
                        buttonStartStop.setText( R.string.start_tests );
                    } } );
                }
            }
        }.start();
    }
 
    public void onPause() {
        wantStop = true;
        super.onPause();
    }

    public void onResume() {
        for ( Tester tester : testers ) {
            tester.setupViews( this );
        }
        super.onResume();
    }
        
    public boolean isWantStop() {
        return wantStop;
    }
    
}