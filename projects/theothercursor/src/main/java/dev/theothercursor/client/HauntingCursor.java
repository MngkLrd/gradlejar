package dev.theothercursor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Random;

final class HauntingCursor {
    private enum Mode {
        WATCHING, SEEKING, INSPECTING, STARTLED, FALSE_DRAG, DISAPPEARING
    }

    private final Random random = new Random();
    private AbstractContainerScreen<?> screen;
    private Mode mode = Mode.WATCHING;
    private Slot watchedSlot;
    private ItemStack pretendItem = ItemStack.EMPTY;
    private long lastFrameNs;
    private double x;
    private double y;
    private double destinationX;
    private double destinationY;
    private double modeSeconds;
    private double sessionSeconds;
    private double duration;
    private double retreatCooldown;
    private double dragDestinationX;
    private double dragDestinationY;

    void reset() {
        screen = null;
        lastFrameNs = 0L;
        watchedSlot = null;
        pretendItem = ItemStack.EMPTY;
    }

    void render(AbstractContainerScreen<?> current, GuiGraphics graphics, int mouseX, int mouseY) {
        long now = System.nanoTime();
        if (screen != current || lastFrameNs == 0L) {
            open(current, now);
        }

        double dt = Math.max(0.0, Math.min(0.075, (now - lastFrameNs) * 1.0e-9));
        lastFrameNs = now;
        modeSeconds += dt;
        sessionSeconds += dt;
        retreatCooldown = Math.max(0.0, retreatCooldown - dt);

        if (mode != Mode.STARTLED && mode != Mode.DISAPPEARING
                && retreatCooldown == 0.0
                && distance(x, y, mouseX, mouseY) < 23.0) {
            recoil(mouseX, mouseY);
        }

        switch (mode) {
            case WATCHING -> {
                if (modeSeconds >= duration) {
                    chooseSomethingToWatch();
                }
            }
            case SEEKING -> {
                if (watchedSlot != null && watchedSlot.hasItem() && watchedSlot.isActive()) {
                    destinationX = slotX(watchedSlot);
                    destinationY = slotY(watchedSlot);
                }
                travelTo(destinationX, destinationY, 78.0, dt);
                if (distance(x, y, destinationX, destinationY) < 4.0) {
                    change(Mode.INSPECTING, 2.3 + random.nextDouble() * 3.6);
                }
            }
            case INSPECTING -> {
                // Tiny, intentional micro-movements rather than noisy random jitter.
                x += Math.sin(sessionSeconds * 3.1) * dt * 1.7;
                y += Math.cos(sessionSeconds * 2.7) * dt * 1.2;
                if (modeSeconds >= duration) {
                    if (watchedSlot != null && watchedSlot.hasItem()
                            && sessionSeconds > 9.0 && random.nextDouble() < 0.095) {
                        beginFakeDrag();
                    } else {
                        change(Mode.WATCHING, 0.8 + random.nextDouble() * 2.4);
                    }
                }
            }
            case STARTLED -> {
                travelTo(destinationX, destinationY, 210.0, dt);
                if (modeSeconds >= duration) {
                    change(Mode.WATCHING, 1.1 + random.nextDouble() * 2.3);
                }
            }
            case FALSE_DRAG -> {
                travelTo(dragDestinationX, dragDestinationY, 105.0, dt);
                if (modeSeconds >= duration
                        || distance(x, y, dragDestinationX, dragDestinationY) < 6.0) {
                    pretendItem = ItemStack.EMPTY;
                    destinationX = random.nextBoolean() ? 9.0 : screen.width - 22.0;
                    destinationY = random.nextBoolean() ? 9.0 : screen.height - 22.0;
                    change(Mode.DISAPPEARING, 1.0 + random.nextDouble() * 1.0);
                }
            }
            case DISAPPEARING -> {
                travelTo(destinationX, destinationY, 125.0, dt);
                if (modeSeconds >= duration) {
                    change(Mode.WATCHING, 2.0 + random.nextDouble() * 4.0);
                }
            }
        }

        x = clamp(x, 2.0, screen.width - 13.0);
        y = clamp(y, 2.0, screen.height - 18.0);

        if (mode == Mode.FALSE_DRAG && !pretendItem.isEmpty()) {
            // Only renders a copy. The actual Slot, menu and carried stack are never touched.
            graphics.renderFakeItem(pretendItem, (int) Math.round(x + 5), (int) Math.round(y + 2));
        }
        GhostCursorArt.draw(graphics, (int) Math.round(x), (int) Math.round(y),
                mode == Mode.FALSE_DRAG, (float) sessionSeconds);
    }

    private void open(AbstractContainerScreen<?> current, long now) {
        screen = current;
        lastFrameNs = now;
        sessionSeconds = 0.0;
        modeSeconds = 0.0;
        retreatCooldown = 0.7;
        watchedSlot = null;
        pretendItem = ItemStack.EMPTY;
        x = random.nextBoolean() ? 11.0 : Math.max(11.0, current.width - 26.0);
        y = random.nextBoolean() ? 11.0 : Math.max(11.0, current.height - 28.0);
        change(Mode.WATCHING, 1.4 + random.nextDouble() * 2.8);
    }

    private void change(Mode next, double seconds) {
        mode = next;
        modeSeconds = 0.0;
        duration = seconds;
    }

    private void chooseSomethingToWatch() {
        Slot candidate = null;
        double best = -1.0;
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || !slot.hasItem()) continue;
            double sx = slotX(slot);
            double sy = slotY(slot);
            if (sx < 4 || sy < 4 || sx > screen.width - 5 || sy > screen.height - 5) continue;

            double score = random.nextDouble() * 5.5 + valueScore(slot.getItem());
            if (score > best) {
                candidate = slot;
                best = score;
            }
        }
        watchedSlot = candidate;
        if (candidate != null) {
            destinationX = slotX(candidate) - 2.0;
            destinationY = slotY(candidate) - 2.0;
        } else {
            destinationX = 15.0 + random.nextDouble() * Math.max(1.0, screen.width - 44.0);
            destinationY = 15.0 + random.nextDouble() * Math.max(1.0, screen.height - 44.0);
        }
        change(Mode.SEEKING, 12.0);
    }

    private double valueScore(ItemStack stack) {
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        double score = stack.getRarity().ordinal() * 1.8;
        if (stack.isEnchanted()) score += 3.5;
        if (name.contains("sword")) score += 5.0;
        if (name.contains("diamond") || name.contains("netherite")) score += 5.0;
        if (name.contains("totem") || name.contains("elytra")
                || name.contains("star") || name.contains("artifact")) score += 5.5;
        if (name.contains("gem") || name.contains("rare") || name.contains("legendary")) score += 3.0;
        return score;
    }

    private void recoil(int mouseX, int mouseY) {
        double awayX = x - mouseX;
        double awayY = y - mouseY;
        double d = Math.max(0.0001, Math.sqrt(awayX * awayX + awayY * awayY));
        if (d < 1.0) {
            awayX = random.nextBoolean() ? -1.0 : 1.0;
            awayY = random.nextBoolean() ? -0.7 : 0.7;
            d = Math.sqrt(awayX * awayX + awayY * awayY);
        }

        double jump = 39.0 + random.nextDouble() * 43.0;
        x = clamp(x + awayX / d * jump, 3.0, screen.width - 14.0);
        y = clamp(y + awayY / d * jump, 3.0, screen.height - 19.0);
        destinationX = clamp(x + awayX / d * 52.0, 3.0, screen.width - 14.0);
        destinationY = clamp(y + awayY / d * 52.0, 3.0, screen.height - 19.0);
        pretendItem = ItemStack.EMPTY;
        watchedSlot = null;
        retreatCooldown = 1.65;
        change(Mode.STARTLED, 0.55 + random.nextDouble() * 0.3);
    }

    private void beginFakeDrag() {
        if (watchedSlot == null || !watchedSlot.hasItem()) {
            change(Mode.WATCHING, 1.4);
            return;
        }
        pretendItem = watchedSlot.getItem().copy();
        pretendItem.setCount(1);
        Slot vacant = null;
        double best = -1.0;
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || slot.hasItem() || slot == watchedSlot) continue;
            double d = distance(slotX(slot), slotY(slot), x, y);
            if (d > 32.0 && d < 230.0 && random.nextDouble() > best) {
                vacant = slot;
                best = random.nextDouble();
            }
        }
        if (vacant != null) {
            dragDestinationX = slotX(vacant);
            dragDestinationY = slotY(vacant);
        } else {
            dragDestinationX = clamp(x + (random.nextBoolean() ? 75.0 : -75.0), 8.0, screen.width - 20.0);
            dragDestinationY = clamp(y + (random.nextBoolean() ? 38.0 : -38.0), 8.0, screen.height - 20.0);
        }
        retreatCooldown = 1.5;
        change(Mode.FALSE_DRAG, 2.0);
    }

    private void travelTo(double toX, double toY, double pixelsPerSecond, double dt) {
        double deltaX = toX - x;
        double deltaY = toY - y;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (distance < 0.001) return;
        double step = Math.min(distance, pixelsPerSecond * dt);
        x += deltaX * (step / distance);
        y += deltaY * (step / distance);
    }

    private double slotX(Slot slot) {
        return screen.getGuiLeft() + slot.x + 8.0;
    }

    private double slotY(Slot slot) {
        return screen.getGuiTop() + slot.y + 8.0;
    }

    private static double distance(double x0, double y0, double x1, double y1) {
        return Math.hypot(x1 - x0, y1 - y0);
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
