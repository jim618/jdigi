/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.network;

import org.wa5znu.znuradio.receiver.TextHandler;

public class TextRecord implements TextHandler, Runnable {
  ClientConnection client;
  String data;

  public TextRecord(ClientConnection client) {
    super();
    this.client=client;
  }

  public synchronized void handleText(int frame, String text) {
    if (containsRubout(text)) {
      text = processInternalRubouts(text);
    }
    this.data=text;
    int len = text.length();
    if (len>255) throw new RuntimeException("too much text");
  }

  public synchronized void run() {
    // Look out for double lock here.
    synchronized(client.getWriteLock()) {
      client.writeCommand('C');
      client.writeShortString(data);
    }
  }

  private boolean containsRubout(String text) {
    return ((text.indexOf((char)8) != -1) || (text.indexOf((char)127) != -1));
  }

  String processInternalRubouts(String text) {
    int len = text.length();
    int j = 0;
    char result[] = new char[len];
    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);
      if (j > 0 && (c == 8 || c == 127)) {
	j--;
      } else {
	result[j++] = c;
      }
    }
    return new String(result, 0, j);
  }

}
