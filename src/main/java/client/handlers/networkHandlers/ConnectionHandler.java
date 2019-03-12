package client.handlers.networkHandlers;

import client.main.Client;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import shared.packets.Packet;
import shared.packets.PacketJoin;

public class ConnectionHandler extends Thread {

  public BlockingQueue received;

  private byte[] buffer;
  private String address;
  private int port;
  private boolean connected;
  private DatagramSocket clientSocket;
  private Socket socket;
  private PrintWriter out;

  public ConnectionHandler(String address) {
    connected = true;
    port = 4446;
    received = new LinkedBlockingQueue<String>();
    this.address = address;
    try {
      clientSocket = new DatagramSocket(port);
      socket = new Socket(this.address, 4445);
      out = new PrintWriter(socket.getOutputStream(), true);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void run() {
    Packet joinPacket =
        new PacketJoin(
            Client.levelHandler.getClientPlayer().getUUID(),
            Client.settings.getUsername(),
            Client.levelHandler.getClientPlayer().getX(),
            Client.levelHandler.getClientPlayer().getY());
    send(joinPacket.getString());

    Client.multiplayer = true;
    while (connected) {
      buffer = new byte[3200];
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      try {
        clientSocket.receive(packet);
        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
        received.add(msg.trim());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void end() {
    connected = false;
    out.close();
    try {
      socket.close();
      clientSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void send(String data) {
    out.println(data);
  }
}