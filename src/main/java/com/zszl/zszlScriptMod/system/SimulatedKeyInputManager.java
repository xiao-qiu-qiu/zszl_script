package com.zszl.zszlScriptMod.system;

import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimulatedKeyInputManager {
    public static final SimulatedKeyInputManager INSTANCE = new SimulatedKeyInputManager();
    private static final int PRESS_HOLD_TICKS = 10;

    public static final class SimulatedPressEvent {
        private final int keyCode;
        private final Set<Integer> modifiers;

        private SimulatedPressEvent(int keyCode, Set<Integer> modifiers) {
            this.keyCode = keyCode;
            this.modifiers = modifiers;
        }

        public int getKeyCode() {
            return keyCode;
        }

        public Set<Integer> getModifiers() {
            return modifiers;
        }
    }

    private final Set<Integer> heldKeys = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    private final Set<Integer> managedKeys = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    private final Map<Integer, Integer> pendingReleaseTicks = new ConcurrentHashMap<>();
    private final Queue<SimulatedPressEvent> pendingPressEvents = new ConcurrentLinkedQueue<>();

    private SimulatedKeyInputManager() {
    }

    public static void simulateKey(String keyName, String state) {
        int keyCode = ModUtils.resolveLwjglKeyCode(keyName);
        if (keyCode == Keyboard.KEY_NONE) {
            zszlScriptMod.LOGGER.warn("忽略未知模拟按键: {}", keyName);
            return;
        }

        simulateKeyCode(keyCode, state);
    }

    public static void simulateKeyCode(int keyCode, String state) {
        if (keyCode == Keyboard.KEY_NONE) {
            return;
        }

        String normalizedState = ModUtils.normalizeSimulatedKeyState(state);
        runOnClientThread(() -> INSTANCE.applyStateChange(keyCode, normalizedState));
    }

    public static boolean isKeyDown(int keyCode) {
        return Keyboard.isKeyDown(keyCode) || INSTANCE.isSimulatedKeyDown(keyCode);
    }

    public static boolean isEitherKeyDown(int primaryKeyCode, int secondaryKeyCode) {
        return isKeyDown(primaryKeyCode) || isKeyDown(secondaryKeyCode);
    }

    public SimulatedPressEvent pollPressedKey() {
        return pendingPressEvents.poll();
    }

    public void reset() {
        runOnClientThread(this::resetInternal);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        syncManagedKeyStates();
        advancePendingReleases();
    }

    private void applyStateChange(int keyCode, String state) {
        managedKeys.add(keyCode);
        switch (state) {
            case "Down":
                pressKey(keyCode, false);
                break;
            case "Up":
                releaseKey(keyCode);
                break;
            default:
                tapKey(keyCode);
                break;
        }
        zszlScriptMod.LOGGER.info("成功模拟实例内按键: {} ({})", Keyboard.getKeyName(keyCode), state);
    }

    private void pressKey(int keyCode, boolean syntheticTap) {
        boolean wasHeld = heldKeys.contains(keyCode);
        heldKeys.add(keyCode);
        if (syntheticTap) {
            pendingReleaseTicks.put(keyCode, PRESS_HOLD_TICKS);
        } else {
            pendingReleaseTicks.remove(keyCode);
        }

        syncKeyBindingState(keyCode);
        if (!wasHeld || syntheticTap) {
            KeyBinding.onTick(keyCode);
            pendingPressEvents.add(new SimulatedPressEvent(keyCode, snapshotActiveModifiers()));
        }
    }

    private void tapKey(int keyCode) {
        if (heldKeys.contains(keyCode) && !pendingReleaseTicks.containsKey(keyCode)) {
            syncKeyBindingState(keyCode);
            KeyBinding.onTick(keyCode);
            pendingPressEvents.add(new SimulatedPressEvent(keyCode, snapshotActiveModifiers()));
            return;
        }
        pressKey(keyCode, true);
    }

    private void releaseKey(int keyCode) {
        heldKeys.remove(keyCode);
        pendingReleaseTicks.remove(keyCode);
        syncKeyBindingState(keyCode);
    }

    private void syncManagedKeyStates() {
        if (managedKeys.isEmpty()) {
            return;
        }

        for (Integer keyCode : new HashSet<>(managedKeys)) {
            syncKeyBindingState(keyCode);
        }
    }

    private void advancePendingReleases() {
        if (pendingReleaseTicks.isEmpty()) {
            return;
        }

        Set<Integer> releaseKeys = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : pendingReleaseTicks.entrySet()) {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                releaseKeys.add(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }

        for (Integer keyCode : releaseKeys) {
            releaseKey(keyCode);
        }
    }

    private void resetInternal() {
        Set<Integer> keysToReset = new HashSet<>(managedKeys);
        keysToReset.addAll(heldKeys);
        keysToReset.addAll(pendingReleaseTicks.keySet());

        pendingPressEvents.clear();
        heldKeys.clear();
        pendingReleaseTicks.clear();
        managedKeys.clear();

        for (Integer keyCode : keysToReset) {
            KeyBinding.setKeyBindState(keyCode, Keyboard.isKeyDown(keyCode));
        }
    }

    private void syncKeyBindingState(int keyCode) {
        KeyBinding.setKeyBindState(keyCode, Keyboard.isKeyDown(keyCode) || isSimulatedKeyDown(keyCode));
    }

    private boolean isSimulatedKeyDown(int keyCode) {
        if (keyCode == Keyboard.KEY_NONE) {
            return false;
        }
        if (heldKeys.contains(keyCode)) {
            return true;
        }

        switch (keyCode) {
            case Keyboard.KEY_LCONTROL:
                return heldKeys.contains(Keyboard.KEY_RCONTROL);
            case Keyboard.KEY_RCONTROL:
                return heldKeys.contains(Keyboard.KEY_LCONTROL);
            case Keyboard.KEY_LSHIFT:
                return heldKeys.contains(Keyboard.KEY_RSHIFT);
            case Keyboard.KEY_RSHIFT:
                return heldKeys.contains(Keyboard.KEY_LSHIFT);
            case Keyboard.KEY_LMENU:
                return heldKeys.contains(Keyboard.KEY_RMENU);
            case Keyboard.KEY_RMENU:
                return heldKeys.contains(Keyboard.KEY_LMENU);
            default:
                return false;
        }
    }

    private Set<Integer> snapshotActiveModifiers() {
        Set<Integer> modifiers = new HashSet<>();
        if (isKeyDown(Keyboard.KEY_LCONTROL) || isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers.add(Keyboard.KEY_LCONTROL);
        }
        if (isKeyDown(Keyboard.KEY_LSHIFT) || isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers.add(Keyboard.KEY_LSHIFT);
        }
        if (isKeyDown(Keyboard.KEY_LMENU) || isKeyDown(Keyboard.KEY_RMENU)) {
            modifiers.add(Keyboard.KEY_LMENU);
        }
        return modifiers;
    }

    private static void runOnClientThread(Runnable task) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        if (mc.isCallingFromMinecraftThread()) {
            task.run();
            return;
        }
        mc.addScheduledTask(task);
    }
}
