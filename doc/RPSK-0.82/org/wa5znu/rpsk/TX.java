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

public class TX implements Runnable {
  Thread thread;
  
  InputTextArea inputTextArea;
  InputTextLine inputTextLine;
  Connection connection;
  StringBuffer textBuffer = new StringBuffer();

  public TX(Connection connection, InputTextArea inputTextArea, InputTextLine inputTextLine) {
    this.connection = connection;
    this.inputTextArea = inputTextArea;
    this.inputTextLine = inputTextLine;
    inputTextLine.setClient(this);
    thread = new Thread(this);
    thread.start();
  }


  synchronized void addText(String text) {
    textBuffer.append(text);
    notifyAll();
  }

  synchronized void addTextNewline(String text) {
    textBuffer.append(text);
    textBuffer.append('\n');
    notifyAll();
  }

  public void run() {
    boolean going = true;

    try {
      while (going) {
	processTX();
      }
    } catch (Exception e) {
      connection.die(e);
    }
    System.err.println("got eof");
  }

  synchronized void processTX() throws IOException {
    try {
      wait();
    } catch (InterruptedException ignore) { }
    String text = textBuffer.toString();
    if (text != null) {
      textBuffer.setLength(0);
      sendText(text);
    }
  }

  void sendText(String text) throws IOException {
    int selStart = inputTextArea.getSelectionStart();
    int selEnd = inputTextArea.getSelectionEnd();
    inputTextArea.replaceRange(text, selStart, selEnd);
    int len = text.length();
    int i = 0;
    int jlen = len;
    while (jlen > 255) {
      System.err.println("jlen = " + jlen);
      int start = i;
      int end = start + 255;
      connection.outputStream.write('T');
      connection.outputStream.write(255);
      for (; i < end; i++) {
	connection.outputStream.write(text.charAt(i));
      }
      jlen -= 255;
      connection.outputStream.flush();
    }
    if (jlen > 0) {
      connection.outputStream.write('T');
      connection.outputStream.write(len);
      for (; i < jlen; i++) {
	connection.outputStream.write(text.charAt(i));
      }
      connection.outputStream.flush();
    } else {
      new Throwable("jlen = 0").printStackTrace();
    }
  }
}
