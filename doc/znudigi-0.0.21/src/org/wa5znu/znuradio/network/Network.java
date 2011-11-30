/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.network;

import org.wa5znu.znuradio.audio.AudioUtils;
import org.wa5znu.znuradio.receiver.Receiver;
import org.wa5znu.znuradio.receiver.Controller;
import org.wa5znu.znuradio.receiver.ReceiverHandler;
import org.wa5znu.znuradio.dsp.Complex;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Network implements Runnable, ReceiverHandler, Controller {
  private static final int PORT_NUMBER=3125;
  Receiver receiver;
  ServerSocket serverSocket;
  ConcurrentLinkedQueue<ClientConnection> networkServers;

  public Network() {
    networkServers = new ConcurrentLinkedQueue<ClientConnection>();
  }

  public static void main(String args[]) {
    int deviceCount = AudioUtils.getMixerCount();
    Network network = new Network();
    network.receiver = new Receiver(network);

    if (args.length == 0) {
      System.out.println("[Trying all sound devices in numeric order.]");
      for (int deviceno = 0; deviceno<deviceCount; deviceno++) {
	System.out.println("[Trying device " + deviceno + "]");
	if (network.receiver.startReceiver(AudioUtils.getMixerName(deviceno))) {
	  System.out.println("[Using device " + deviceno + "]");
	  network.run();
	  break;
	}
      }
    } else if (args.length == 1) {
      if (args[0].equals("--list")) {
	System.out.println("[Listing devices]");
	for (int deviceno = 0; deviceno < deviceCount; deviceno++) {
	  System.out.println("[Device " + deviceno + ": " + AudioUtils.getMixerName(deviceno)+"]");
	}
      } else {
	int deviceno = Integer.parseInt(args[0]);
	System.out.println("[Trying device " + deviceno + "]");
	if (network.receiver.startReceiver(AudioUtils.getMixerName(deviceno))) {
	  System.out.println("[Using device " + deviceno + "]");
	  network.run();
	}
      }
    } else {
      System.err.println("usage: specify no options to search sound devices, a number to use that sound device, or --list to list all devices");
    }
  }

      

  public void run() {
    initNetwork();
    try {
      while (true) {
	System.out.println("[Listening on network " + serverSocket +"]");
	Socket connection = serverSocket.accept();
	System.out.println("Got " + connection);
	networkServers.offer(new ClientConnection(this, receiver, connection));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
	if (serverSocket != null)
	  serverSocket.close();
      } catch (IOException e) {
	e.printStackTrace();
      }
    }
  }

  void closeServer(ClientConnection server) {
    networkServers.remove(server);
    System.out.println("[Network: Server count: " + networkServers.size()+ "]");
  }

  private void initNetwork() {
    try {
      serverSocket = new ServerSocket(PORT_NUMBER);
    } catch (IOException e) {
      throw new RuntimeException("initNetwork", e);
    }
  }

  public void handleSpectrum(int frame, double data[], int length)  {
    for (ClientConnection n : networkServers)
      n.handleSpectrum(frame, data, length);
  }

  public void handleStage(int frame, double data[], int length) {
    for (ClientConnection n : networkServers)
      n.handleStage(frame, data, length);
  }

  public void handleStage(int frame, Complex data[], int length) {
    for (ClientConnection n : networkServers)
      n.handleStage(frame, data, length);
  }

  public void handlePhase(int frame, double phi, boolean dcd) {
    for (ClientConnection n : networkServers)
      n.handlePhase(frame, phi, dcd);
  }

  public void handleText(int frame, String text) {
    for (ClientConnection n : networkServers)
      n.handleText(frame, text);
  }

  public void setSampleRate(int r) {
    for (ClientConnection n : networkServers)
      n.setSampleRate(r);
  }

  public void setFrequency(double f) {
    setFrequency(f, false); 
  }

  public void setFrequency(double f, boolean userClick) {
    for (ClientConnection n : networkServers)
      n.setFrequency(f);
    if (receiver != null)
      receiver.setFrequency(f);
  }

  public void showNextStage() {
    receiver.showNextStage();
  }

}
