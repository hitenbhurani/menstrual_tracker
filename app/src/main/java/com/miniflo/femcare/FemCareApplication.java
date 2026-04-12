package com.miniflo.femcare;

import android.app.Application;

public class FemCareApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BackgroundTaskScheduler.scheduleAll(this);
    }
}
