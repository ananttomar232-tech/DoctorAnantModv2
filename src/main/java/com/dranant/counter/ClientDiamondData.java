package com.dranant.counter;

import com.dranant.network.BankPayload;
import net.minecraft.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Client-side mirror of the Diamond Bank (plain Java, safe to reference from common packet handlers). */
public final class ClientDiamondData {
    public static final long POPUP_MILLIS = 1800L;

    private static long balance;
    private static double rate;
    private static long syncMillis;
    private static boolean running;
    private static boolean visible;
    private static double displayed;
    private static long lastFrameMillis;
    private static long pulseMillis;
    private static final List<Popup> POPUPS = new ArrayList<>();

    public record Popup(long amount, long startMillis, float xJitter) {}

    public static synchronized void update(BankPayload p) {
        balance = p.balance();
        rate = p.ratePerSec();
        running = p.running();
        visible = p.visible();
        syncMillis = Util.getMillis();
        if (p.delta() != 0) {
            POPUPS.add(new Popup(p.delta(), syncMillis, (float) (Math.random() * 30.0 - 15.0)));
            if (POPUPS.size() > 10) POPUPS.remove(0);
            pulseMillis = syncMillis;
        }
        if (displayed == 0 || Math.abs(displayed - balance) > Math.max(1_000_000, balance * 0.5)) displayed = balance;
    }

    /** Smoothly rolling number: extrapolates passive growth between packets and eases toward the target. */
    public static synchronized long displayValue() {
        long now = Util.getMillis();
        double target = balance + (running ? rate * Math.min(500L, now - syncMillis) / 1000.0 : 0.0);
        double dt = lastFrameMillis == 0 ? 0.016 : Math.min(0.1, (now - lastFrameMillis) / 1000.0);
        lastFrameMillis = now;
        displayed += (target - displayed) * Math.min(1.0, dt * 10.0);
        if (Math.abs(target - displayed) < 1.0) displayed = target;
        return Math.max(0L, Math.round(displayed));
    }

    /** 0..1 pulse strength right after a transaction. */
    public static synchronized float pulse() {
        long age = Util.getMillis() - pulseMillis;
        return age > 400 ? 0.0F : 1.0F - age / 400.0F;
    }

    public static synchronized double getRate() {
        return running ? rate : 0.0;
    }

    public static synchronized boolean isRunning() {
        return running;
    }

    public static synchronized boolean isVisible() {
        return visible;
    }

    public static synchronized List<Popup> getActivePopups() {
        long now = Util.getMillis();
        Iterator<Popup> it = POPUPS.iterator();
        while (it.hasNext()) if (now - it.next().startMillis() > POPUP_MILLIS) it.remove();
        return new ArrayList<>(POPUPS);
    }

    private ClientDiamondData() {}
}
