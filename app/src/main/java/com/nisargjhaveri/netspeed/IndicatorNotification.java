package com.nisargjhaveri.netspeed;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.nisargjhaveri.netspeed.settings.Settings;
import com.nisargjhaveri.netspeed.settings.SettingsActivity;

import java.util.Locale;

final class IndicatorNotification {
    private static final int NOTIFICATION_ID = 1;

    private Context mContext;

    private Paint mIconSpeedPaint, mIconUnitPaint;
    private Bitmap mIconBitmap;
    private Canvas mIconCanvas;

    private RemoteViews mNotificationContentView;

    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;

    private int mNotificationPriority;
    private String mSpeedToShow = "total";

    IndicatorNotification(Context context) {
        mContext = context;

        setup();
    }

    void start(Service serviceContext) {
        serviceContext.startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    void stop(Service serviceContext) {
        serviceContext.stopForeground(true);
    }

    void hideNotification() {
        mNotificationBuilder.setPriority(Notification.PRIORITY_MIN);
    }

    void showNotification() {
        mNotificationBuilder.setPriority(mNotificationPriority);
    }

    void updateNotification(Speed speed) {
        Speed.HumanSpeed speedToShow = speed.getHumanSpeed(mSpeedToShow);
        mNotificationBuilder.setSmallIcon(
                getIndicatorIcon(speedToShow.speedValue, speedToShow.speedUnit)
        );

        RemoteViews contentView = mNotificationContentView.clone();

        contentView.setTextViewText(
                R.id.notificationSpeedValue,
                speedToShow.speedValue
        );

        contentView.setTextViewText(
                R.id.notificationSpeedUnit,
                speedToShow.speedUnit
        );

        contentView.setTextViewText(
                R.id.notificationText,
                String.format(
                        Locale.ENGLISH, mContext.getString(R.string.notif_up_down_speed),
                        speed.down.speedValue, speed.down.speedUnit,
                        speed.up.speedValue, speed.up.speedUnit
                )
        );

        mNotificationBuilder.setContent(contentView);

        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    void handleConfigChange(Bundle extras) {
        // Which speed to show in indicator icon
        mSpeedToShow = extras.getString(Settings.KEY_INDICATOR_SPEED_TO_SHOW, "total");

        // Show/Hide settings button
        if (extras.getBoolean(Settings.KEY_SHOW_SETTINGS_BUTTON, false)) {
            mNotificationContentView.setViewVisibility(R.id.notificationSettings, View.VISIBLE);
        } else {
            mNotificationContentView.setViewVisibility(R.id.notificationSettings, View.GONE);
        }

        // Notification priority
        switch (extras.getString(Settings.KEY_NOTIFICATION_PRIORITY, "max")) {
            case "low":
                mNotificationPriority = Notification.PRIORITY_LOW;
                break;
            case "default":
                mNotificationPriority = Notification.PRIORITY_DEFAULT;
                break;
            case "high":
                mNotificationPriority = Notification.PRIORITY_HIGH;
                break;
            case "max":
                mNotificationPriority = Notification.PRIORITY_MAX;
                break;
        }
        mNotificationBuilder.setPriority(mNotificationPriority);

        // Show/Hide on lock screen
        if (extras.getBoolean(Settings.KEY_NOTIFICATION_ON_LOCK_SCREEN, false)) {
            mNotificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        } else {
            mNotificationBuilder.setVisibility(Notification.VISIBILITY_SECRET);
        }
    }

    private void setup() {
        setupIndicatorIconGenerator();

        mNotificationContentView = new RemoteViews(mContext.getPackageName(), R.layout.view_indicator_notification);

        PendingIntent openSettingsIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext, SettingsActivity.class), 0);
        mNotificationContentView.setOnClickPendingIntent(R.id.notificationSettings, openSettingsIntent);

        mNotificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new Notification.Builder(mContext)
                .setSmallIcon(getIndicatorIcon("", ""))
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setContent(mNotificationContentView)
                .setOngoing(true)
                .setLocalOnly(true);
    }

    private void setupIndicatorIconGenerator() {
        mIconSpeedPaint = new Paint();
        mIconSpeedPaint.setColor(Color.WHITE);
        mIconSpeedPaint.setAntiAlias(true);
        mIconSpeedPaint.setTextSize(65);
        mIconSpeedPaint.setTextAlign(Paint.Align.CENTER);
        mIconSpeedPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));

        mIconUnitPaint = new Paint();
        mIconUnitPaint.setColor(Color.WHITE);
        mIconUnitPaint.setAntiAlias(true);
        mIconUnitPaint.setTextSize(40);
        mIconUnitPaint.setTextAlign(Paint.Align.CENTER);
        mIconUnitPaint.setTypeface(Typeface.DEFAULT_BOLD);

        mIconBitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);

        mIconCanvas = new Canvas(mIconBitmap);
    }

    private Icon getIndicatorIcon(String speedValue, String speedUnit) {
        mIconCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mIconCanvas.drawText(speedValue, 48, 52, mIconSpeedPaint);
        mIconCanvas.drawText(speedUnit, 48, 95, mIconUnitPaint);

        return Icon.createWithBitmap(mIconBitmap);
    }
}
