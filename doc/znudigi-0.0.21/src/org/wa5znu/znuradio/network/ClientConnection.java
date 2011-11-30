/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.network;

import org.wa5znu.znuradio.receiver.Receiver;
import org.wa5znu.znuradio.receiver.Demodulator;
import org.wa5znu.znuradio.receiver.Controller;
import org.wa5znu.znuradio.receiver.ReceiverHandler;
import org.wa5znu.znuradio.dsp.Complex;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientConnection implements ReceiverHandler, Controller {
  final static int SPECTRUM_WIDTH=2048; // crock
  /**
   * After MAX_QUEUE_SIZE queued responses to go out the client, start dropping records.
   */
  public static final int MAX_QUEUE_SIZE=100;

  /**
   * After QUEUE_TIMEOUT_MS milliseconds of trying to add to a full queue, drop the record.
   */
  public static final int QUEUE_TIMEOUT_MS=500;
  private Receiver receiver;
  private Demodulator demodulator;
  private Socket connection;
  private int sampleRate;
  private Controller controller;
  private ArrayBlockingQueue<Runnable> queue;
  private PhaseRecord phaseRecord = new PhaseRecord(this);
  private InputStream inputStream;
  private BufferedOutputStream bufferedOutputStream;
  private OutputStream outputStream;
  private Requester requester;
  private Responder responder;
  private boolean done = false;
  private final Object writeLock = new Object();
  private boolean clientWantsSpectrumNybbles=false;

  public ClientConnection(Controller controller, Receiver receiver, Socket connection) {
    super();
    queue = new ArrayBlockingQueue<Runnable>(MAX_QUEUE_SIZE);
    this.controller=controller;
    this.connection=connection;
    this.receiver=receiver;	// crock
    System.out.println("Received connection from " + connection);
    setSampleRate(receiver.getSampleRate());
    {
      try {
	outputStream = connection.getOutputStream();
	bufferedOutputStream = new BufferedOutputStream(outputStream);
	inputStream = connection.getInputStream();
	writeMode("BPSK31");
      } catch (IOException e) {
	e.printStackTrace();
	closeDown();
      }
    }

    demodulator = receiver.addDemodulator(this, this);

    requester = new Requester();
    responder = new Responder();
    requester.start();
    responder.start();
  }

  private void closeDown() {
    System.out.println("[Closing down connection " + connection + "]");
    ((Network)controller).closeServer(this); // cast crock
    bufferedOutputStream=null;
    outputStream=null;
    inputStream=null;
    try {
      connection.close();
    } catch (IOException e) {
      connection=null;
      e.printStackTrace();
    }
    System.out.println("[Closed down connection " + this + "]");
    connection=null;
  }

  int lastSpectrumFrame = -1;
  int lastSpectrumFrameAcked = -1;
  final static int MAX_UNACKED_SPECTRUM_FRAMES = 25+1;
  boolean droppingFrames = false;
  public void handleSpectrum(int frame, double data[], int length)  {
    lastSpectrumFrame = frame;
    if ((lastSpectrumFrame - lastSpectrumFrameAcked) > MAX_UNACKED_SPECTRUM_FRAMES) {
      if (! droppingFrames) {
	droppingFrames = true;
	System.out.println("[Dropped spectrum frame " + frame + " because client has not acked "+MAX_UNACKED_SPECTRUM_FRAMES+" for " + this + "]");
      }
    } else {
      if (droppingFrames) {
	droppingFrames = false;
	System.out.println("[Resuming at spectrum frame " + frame + " because client has acked " + this + "]");
      } 
      SpectrumRecord spectrumRecord = new SpectrumRecord(this, clientWantsSpectrumNybbles);
      spectrumRecord.handleSpectrum(frame, data, length);
      try {
	if (! queue.offer(spectrumRecord, QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
	  System.out.println("[Dropped spectrum frame " + frame + " due to full client queue for " + this + "]");
	}
      } catch (InterruptedException ie) { }
    }
  }

  public Object getWriteLock() {
    return writeLock;
  }

  public void handleStage(int frame, double data[], int length) {
  }

  public void handleStage(int frame, Complex data[], int length) {
  }

  public void handlePhase(int frame, double phi, boolean dcd) {
    phaseRecord.handlePhase(frame, phi, dcd);
  }

  public void handleText(int frame, String text) {
    TextRecord textRecord = new TextRecord(this);
    textRecord.handleText(frame, text);
    try {
      if (! queue.offer(textRecord, QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
	System.out.println("[Dropped text due to full client queue for " + this + "]");
      }
    } catch (InterruptedException ie) { }
  }

  public void setSampleRate(int r) {
    sampleRate=r;
    SpectrumRecord spectrumRecord = new SpectrumRecord(this, clientWantsSpectrumNybbles);
    spectrumRecord.setSampleRate(r);
    try {
      if (! queue.offer(spectrumRecord, QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
      	System.out.println("[Dropped text sampleRate due to full client queue for " + this + "]");
      }
    } catch (InterruptedException ie) { }
  }

  public void setFrequency(double f) {
    setFrequency(f, false); 
  }

  public void setFrequency(double f, boolean userClick) {
    SpectrumRecord spectrumRecord = new SpectrumRecord(this, clientWantsSpectrumNybbles);
    spectrumRecord.setFrequency(f);
    // We'd really like not to lose this one; probably we should consider closing the client
    // when the queue gets full.
    try {
      if (! queue.offer(spectrumRecord, QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
	System.out.println("[Dropped setFrequency due to full client queue for " + this + "]");
      }
    } catch (InterruptedException ie) { }
    if (demodulator != null)
      demodulator.setFrequency(f);
  }


  void writeCommand(char s) {
    if (bufferedOutputStream == null) {
      return;
    }
    try {
      bufferedOutputStream.write((byte)s);
    } catch (IOException e) {
      e.printStackTrace();
      closeDown();
    }
  }


  void writeClient(byte s) {
    if (bufferedOutputStream == null) {
      return;
    }
    try {
      bufferedOutputStream.write(s);
    } catch (IOException e) {
      e.printStackTrace();
      closeDown();
    }
  }

  void writeShort(short s) {
    if (bufferedOutputStream == null) {
      return;
    }
    try {
      bufferedOutputStream.write((byte)(s >> 8));
      bufferedOutputStream.write((byte)(s &0xff));
    } catch (IOException e) {
      e.printStackTrace();
      closeDown();
    }
  }

  void writeClient(byte[] byteData) {
    if (bufferedOutputStream == null) {
      return;
    }
    int len = byteData.length;
    try {
      bufferedOutputStream.write((byte)(len >> 8));
      bufferedOutputStream.write((byte)(len & 0xff));
      bufferedOutputStream.write(byteData);
    } catch (IOException e) {
      e.printStackTrace();
      closeDown();
    }
  }

  void writeShortString(String s) {
    if (bufferedOutputStream == null) {
      return;
    }
    try {
      byte[] b = s.getBytes("iso-8859-1");
      if (b.length>255) throw new RuntimeException("short string too long: " + s);
      bufferedOutputStream.write((char)b.length);
      bufferedOutputStream.write(b);
    } catch (IOException e) {
      e.printStackTrace();
      closeDown();
    }
  }

  public void showNextStage() {
  }


  void flushClient() {
    if (bufferedOutputStream != null) {
      try {
	bufferedOutputStream.flush();
      } catch (IOException ioe) {
	throw new RuntimeException(ioe);
      }
    }
  }

  void writeMode(String mode) {
    synchronized (writeLock) {
      writeCommand('M');
      writeShortString(mode);
    }
  }

  void writeBandwidth(double bandwidth) {
    int b = (int)(bandwidth*1000);
    int b1 = b / 65536;
    int b2 = (b-b1*65536)/256;
    int b3 = (b-b1*65536-b2*256);
    synchronized (writeLock) {
      writeCommand('B');
      writeClient((byte)b1); // byte1*65536 / 1000
      writeClient((byte)b2); // byte2*256 / 1000
      writeClient((byte)b3); // byte3 / 1000
    }
  }

  void writeClicks(int clicks) {
    synchronized (writeLock) {
      writeCommand('F');
      writeShort((short)clicks);
    }
  }


  class Responder extends Thread {
    Responder() {
      super();
    }

    public void run() {
      try {
	while(true) {
	  try {
	    Runnable item = null;
	    do {
	      item = queue.take();
	      item.run();
	      // We don't queue phaseRecords but let them accumulate inside the record itself as
	      // dropping phase records is of little consequence.
	      // If the phaseRecord has enough data now, it will send itself.  If not, it will
	      // buffer more.
	      phaseRecord.run();
	    } while (item != null);
	  } catch(InterruptedException exp) { }
	}
      } finally {
	closeDown();
      }
    }
  }

  class Requester extends Thread {
    Requester() {
      super();
    }
    public void run() {
      try {
	while(!done) {
	  int c=-1;
	  try {
	    c = inputStream.read();
	  } catch (SocketException e) {
	    done = true;
	  }
	  switch(c) {
	  case 'F': {
	    int hi = inputStream.read();
	    if (hi < 0) throw new RuntimeException("read hi " + hi);
	    int lo = inputStream.read();
	    if (lo < 0) throw new RuntimeException("read lo " + lo);
	    int clicks = hi * 256 + lo;
	    double dfrequency = (clicks * sampleRate) / SPECTRUM_WIDTH;
	    System.out.println("read setFrequency " + dfrequency);
	    setFrequency(dfrequency);
	    synchronized (writeLock) {
	      writeClicks(clicks);
	      writeBandwidth(31.25);
	    }
	    break;
	  } 
	  case 's': 
	  case 'S': {
	    lastSpectrumFrameAcked = lastSpectrumFrame;
	    // TODO: Honor this and turn it into a keepalive
	    int hi = inputStream.read();
	    if (hi < 0) throw new RuntimeException("read hi " + hi);
	    int lo = inputStream.read();
	    if (lo < 0) throw new RuntimeException("read lo " + lo);
	    int spectrumWidth = hi * 256 + lo;
	    clientWantsSpectrumNybbles=(c=='s');
	    System.out.println("spectrumWidth width " + spectrumWidth + " requested at frame " + lastSpectrumFrame +" but we always do " + SPECTRUM_WIDTH+(clientWantsSpectrumNybbles ? " 4 bits" : " 8 bits"));
	    break;
	  }
	  case 'P': {
	    int n = inputStream.read();
	    System.out.println("Client wants " + n+ " phases buffered, but we always do " + PhaseRecord.MAX_PHASES);
	    break;
	  }
	  default:
	    System.err.print(c);
	    break;
	  case -1:
	    done=true;
	    break;
	  }
	}
      } catch (IOException ioe) {
	throw new RuntimeException(ioe);
      } finally {
	closeDown();
      }
    }
  }

  public String toString() {
    return super.toString() + "/socket=" + connection;
  }

}

