// Copyright (c) 2004 Leigh L. Klotz, Jr. <leigh@wa5nzu.org>        
// 
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//                                        
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//                                        
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package org.wa5znu.rpsk;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.io.EOFException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.io.IOException;

public class Connection {
  RPSK parent;
  InputStream inputStream = null;
  OutputStream outputStream = null;
  String callsign;
  String password;
  boolean wantsCompression = false;
  Socket socket;

  public Connection(RPSK parent, String host, int port, boolean wantsCompression) {
    this.parent = parent;
    this.wantsCompression=wantsCompression;

    try {
      socket = new Socket(host, port);
      inputStream = new BufferedInputStream(socket.getInputStream());
      outputStream = new BufferedOutputStream(socket.getOutputStream());
      //      socket.setTcpNodelay(true);
    } catch (Exception e) {
      die(e);
    }
  }

  void login(String callsign, String password) {
    this.password = password;
    this.callsign = callsign;
    requestSetPassword(callsign + "-" + password);
  }

  void logout() {
    this.password = null;
    this.callsign = null;
    requestSetPassword("");
  }


  // R<len>ButtonName
  public synchronized void requestPushButton(String buttonName) {
    try {
      int len = buttonName.length();
      if (len > 255) 
	return;
      outputStream.write('R');
      outputStream.write(len);
      for (int i = 0; i < len; i++) {
	outputStream.write(buttonName.charAt(i));
      }
      outputStream.flush();
      System.err.println("requestPushButton " + buttonName);
    } catch (Exception e) {
      System.err.println(e + ": Couldn't write requestPushButton " + buttonName);
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  // When we tell the server the frequency changed
  void requestSetFrequency(int offset) {
    try {
      outputStream.write('F');
      outputStream.write(offset / 256);
      outputStream.write(offset & 255);
      outputStream.flush();
    } catch (Exception e) {
      System.err.println(e + ": Couldn't write request frequency " + offset);
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    // crock
    parent.outputText.append("\n----------- " + 
			     (parent.carrierFrequencyKnown() ? (parent.formatMHz(parent.carrierFrequencyMHz(offset))+" Mhz / ") : "")+
			     parent.offsetAsHz(offset) + " Hz " + new Date() + " ----------\n");
  }


  // 0 - 1024
  void requestSetSpectrumWidth(int width) {
    try {
      outputStream.write(wantsCompression ? 's' : 'S');
      outputStream.write(width / 256);
      outputStream.write(width & 255);
      outputStream.flush();
      // System.err.println("requestSetSpectrumWidth " + width);
    } catch (Exception e) {
      System.err.println(e + ": Couldn't write request spectrum width " + width);
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  // M<len>ModeName
  public void requestSetMode(String modeName) {
    try {
      int len = modeName.length();
      if (len > 255) 
	return;
      outputStream.write('M');
      outputStream.write(len);
      for (int i = 0; i < len; i++) {
	outputStream.write(modeName.charAt(i));
      }
      outputStream.flush();
    } catch (Exception e) {
      System.err.println(e + ": Couldn't write request mode " + modeName);
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  // X<len>callsign-password
  void requestSetPassword(String password) {
    try {
      int len = password.length();
      if (len > 255) 
	return;
      outputStream.write('X');
      outputStream.write(len);
      // LEN can be 0 (for logout)
      for (int i = 0; i < len; i++) {
	outputStream.write(password.charAt(i));
      }
      outputStream.flush();
      System.err.println("requestSetPassword");
    } catch (Exception e) {
      System.err.println(e + ": Couldn't set Password");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  // 0 - 255
  void requestSetMiniscopeSkipAmount(int amount) {
    try {
      outputStream.write('P');
      outputStream.write(amount);
      outputStream.flush();
    } catch (Exception e) {
      System.err.println(e + ": Couldn't write miniscope skip amount " + amount);
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  void die(Exception e) {
    System.err.println(e);
    e.printStackTrace(System.err);
    System.exit(-1);
  }


  // What we do every now and then to let the server know we're happy.
  void sendAck() {
    requestSetSpectrumWidth(parent.SPECTRUM_WIDTH);
  }

}
