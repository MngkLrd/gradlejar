package dev.minemods.sleepdebt.client;
import dev.minemods.sleepdebt.network.SleepStatusPayload;
public final class ClientState {
 public static volatile int debt,maskTicks,crashTicks,caffeineLoad;
 public static void update(SleepStatusPayload p){
  debt=p.debt();maskTicks=p.maskTicks();crashTicks=p.crashTicks();caffeineLoad=p.caffeineLoad();
 }
 private ClientState(){}
}
