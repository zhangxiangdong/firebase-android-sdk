package com.example.e2e_transport;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.datatransport.Encoding;
import com.google.android.datatransport.Event;
import com.google.android.datatransport.Transport;
import com.google.android.datatransport.cct.CCTDestination;
import com.google.android.datatransport.runtime.TransportRuntime;
import com.google.android.datatransport.runtime.firebase.transport.ClientMetrics;
import com.google.firebase.testapps.firestore.proto.Test;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    CCTDestination destination = CCTDestination.LEGACY_INSTANCE;

    TransportRuntime.initialize(this.getApplicationContext());
    Transport<Test> transportRuntime =
            TransportRuntime.getInstance()
            .newFactory(destination)
            .getTransport("1018", Test.class, Encoding.of("proto"), Test::toByteArray);

    transportRuntime.send(Event.ofData(
        12, Test.newBuilder().setHello("_p_r_o_t_o_FirebaseLEGACY_INSTANCE").build()));
  }
}