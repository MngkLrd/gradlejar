package com.kirill.simplesorter.network;
public final class ClientNetwork {
 private ClientNetwork(){}
 public static void send(WandSettingsPayload p){ModNetworking.CHANNEL.sendToServer(p);}
 public static void send(WandActionPayload p){ModNetworking.CHANNEL.sendToServer(p);}
}
