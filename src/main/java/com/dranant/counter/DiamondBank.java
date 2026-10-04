package com.dranant.counter;

import com.dranant.config.DrAnantConfig;
import com.dranant.network.BankPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Diamond Counter, now a real bank (saved per world).
 * <ul>
 *   <li>Earned diamonds (pharmacist sales, lab payments picked up, boss rewards, any diamond you pick up while running)
 *       vanish from the inventory and are added here.</li>
 *   <li>While running, the counter also grows non-stop on an accelerating curve: 1,000,000 diamonds after 5 minutes
 *       (configurable), and it keeps accelerating after that.</li>
 *   <li>Shop purchases are paid from this balance.</li>
 * </ul>
 */
public class DiamondBank extends SavedData {
    private static final String NAME = "dranant_diamond_bank";

    private long balance;
    private boolean running;
    private boolean visible;
    private long runTicks;
    private double passiveCarry;
    private int syncTimer;

    public static DiamondBank get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DiamondBank::new, DiamondBank::load, null), NAME);
    }

    private static DiamondBank load(CompoundTag tag, HolderLookup.Provider registries) {
        DiamondBank bank = new DiamondBank();
        bank.balance = tag.getLong("balance");
        bank.running = tag.getBoolean("running");
        bank.visible = tag.getBoolean("visible");
        bank.runTicks = tag.getLong("runTicks");
        return bank;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("balance", balance);
        tag.putBoolean("running", running);
        tag.putBoolean("visible", visible);
        tag.putLong("runTicks", runTicks);
        return tag;
    }

    /** Passive income: cumulative(t) = target * (t / T)^2, so it reaches the target after T and keeps accelerating. */
    private static double passiveForTick(long t) {
        double target = DrAnantConfig.PASSIVE_TARGET.get();
        double totalTicks = DrAnantConfig.PASSIVE_TARGET_SECONDS.get() * 20.0D;
        return target * (2.0D * t + 1.0D) / (totalTicks * totalTicks);
    }

    public double currentRatePerSecond() {
        return running ? passiveForTick(runTicks) * 20.0D : 0.0D;
    }

    /** Called every server tick. */
    public void tick(MinecraftServer server) {
        if (running && DrAnantConfig.PASSIVE_INCOME.get()) {
            passiveCarry += passiveForTick(runTicks);
            runTicks++;
            long whole = (long) passiveCarry;
            if (whole > 0) {
                balance += whole;
                passiveCarry -= whole;
            }
            if (runTicks % 100 == 0) setDirty();
        }
        if (++syncTimer >= 5) {
            syncTimer = 0;
            if (visible) PacketDistributor.sendToAllPlayers(snapshot(0));
        }
    }

    private BankPayload snapshot(long delta) {
        return new BankPayload(balance, delta, currentRatePerSecond(), running, visible);
    }

    /** Adds earned diamonds (only while the counter is running). @return true if counted */
    public boolean earn(MinecraftServer server, long amount) {
        if (!running || amount <= 0) return false;
        balance += amount;
        setDirty();
        PacketDistributor.sendToAllPlayers(snapshot(amount));
        return true;
    }

    public long getBalance() {
        return balance;
    }

    /** Removes up to {@code amount}; @return how much was actually taken. */
    public long withdraw(MinecraftServer server, long amount) {
        long taken = Math.min(balance, Math.max(0, amount));
        if (taken > 0) {
            balance -= taken;
            setDirty();
            PacketDistributor.sendToAllPlayers(snapshot(-taken));
        }
        return taken;
    }

    public void setRunning(MinecraftServer server, boolean run) {
        running = run;
        if (run) visible = true;
        setDirty();
        PacketDistributor.sendToAllPlayers(snapshot(0));
    }

    public void reset(MinecraftServer server) {
        balance = 0;
        runTicks = 0;
        passiveCarry = 0;
        setDirty();
        PacketDistributor.sendToAllPlayers(snapshot(0));
    }

    public void set(MinecraftServer server, long value) {
        balance = Math.max(0, value);
        visible = true;
        setDirty();
        PacketDistributor.sendToAllPlayers(snapshot(0));
    }

    public void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, snapshot(0));
    }

    public boolean isRunning() {
        return running;
    }
}
