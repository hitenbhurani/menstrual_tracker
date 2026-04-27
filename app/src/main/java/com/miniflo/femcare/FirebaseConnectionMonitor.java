package com.miniflo.femcare;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseConnectionMonitor {
    
    public static void setupConnectionMonitoring(@NonNull Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        
        database.getReference(".info/connected").addValueEventListener(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean isConnected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    
                    if (isConnected) {
                        Log.d("FIREBASE_CONNECTION", "Connected to Firebase");
                        FirebaseAuthState.clearAuthError(context);
                    } else {
                        Log.w("FIREBASE_CONNECTION", "Disconnected from Firebase");
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FIREBASE_CONNECTION", "Connection check failed: " + error.getMessage());
                }
            }
        );
    }
}
