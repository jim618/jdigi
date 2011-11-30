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

public class RX implements Runnable {
  Thread thread;
  Connection connection;
  RPSK parent;

  public RX(Connection connection, RPSK parent) {
    this.connection = connection;
    this.parent = parent;
    thread = new Thread(this);
    thread.start();
  }

  public void run() {
    boolean going = true;

    try {
      while (going) {
	if (! processRX()) break;
      }
    } catch (Exception e) {
      connection.die(e);
    }
    System.err.println("got eof");
  }

  private boolean processRX() throws IOException {
    int s;
    InputStream stream = connection.inputStream;
    synchronized(stream) {
      s = stream.read();
    }
    if (s < 0) return false;
    switch(s) {
    case 'M':
      if (! handleMode()) return false;
      break;
    case 'B':
      if (! parent.waterfall.handleBandwidth()) return false;
      break;
    case 'S':
      if (! parent.waterfall.handleSpectrum()) return false;
      break;
     case 's':
       if (! parent.waterfall.handleSpectrumCompressed()) return false;
       break;
    case 'C':
      if (! handleCharacters(false)) return false;
      break;
    case 'T':
      if (! handleCharacters(true)) return false;
    case 'P':
      if (! parent.phaseClock.handlePhaseHighlight(connection)) return false;
      break;
    case 'p':
      if (! parent.phaseClock.handlePhase(connection)) return false;
      break;
    case 'F':
      if (! handleFrequency()) return false;
      break;
    case 'R':
      if (! handleRadioButton()) return false;
    case '\n':
    case '\r':
    default:
      System.err.println("Ignoring stray " + s);
      break;
      //	default:
      //	  System.err.println("Could not handle " + ((char)s)+ "("+s+")");
      // return;
    }
    return true;
  }



  private boolean handleCharacters(boolean tx) throws IOException {
    if (tx) parent.outputText.appendWrapped("\n-----\n");
    InputStream inputStream = connection.inputStream;
    int len = inputStream.read();
    if (len < 0) return false;
    int togo = len;

    while (togo > 0) {
      int c = inputStream.read();
      if (c < 0) {
	return false;
      }
      parent.outputText.appendWrapped((char) c);
      togo -= 1;
    }
    return true;
  }


  /**
   * M followed by byte count follwed by mode name
   * BPSK
   * QPSK
   * MFSK16
   * MFSK8
   * PSK63
   * RTTY
   */
  private boolean handleMode() throws IOException {
    StringBuffer modeBuffer = new StringBuffer();
    InputStream inputStream = connection.inputStream;
    synchronized(inputStream) {
      int len = inputStream.read();
      if (len < 0) return false;
      int togo = len;

      while (togo > 0) {
	int c = inputStream.read();
	if (c < 0) {
	  return false;
	}
	modeBuffer.append((char) c);
	togo -= 1;
      }
    }
    String mode = modeBuffer.toString();
    parent.modeChoiceBox.select(mode);
    return true;
  }

  /**
   * R followed by byte count follwed by button
   */
  private boolean handleRadioButton() throws IOException {
    StringBuffer buttonBuffer = new StringBuffer();
    InputStream inputStream = connection.inputStream;
    synchronized(inputStream) {
      int len = inputStream.read();
      if (len < 0) return false;
      int togo = len;

      while (togo > 0) {
	int c = inputStream.read();
	if (c < 0) {
	  return false;
	}
	buttonBuffer.append((char) c);
	togo -= 1;
      }
      String button = buttonBuffer.toString();
      System.err.println("Server says " + button + " pushed");
      // FIXME
      // modeChoiceBox.select(mode);
      return true;
    }
  }


  // When the server tells us the frequency changed
  private boolean handleFrequency() throws IOException {
    InputStream inputStream = connection.inputStream;
    synchronized(inputStream) {
      int hi = inputStream.read();
      if (hi < 0) return false;
      int lo = inputStream.read();
      if (lo < 0) return false;
      parent.frequency = hi * 256 + lo;
    }
    parent.waterfall.setFrequency(parent.frequency);
    parent.freqDisplay.setText(parent.offsetAsHz()+ " Hz");
    return true;
  }

}
