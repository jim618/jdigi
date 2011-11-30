package org.wa5znu.rpsk;

import java.awt.TextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.TextListener;
import java.awt.event.TextEvent;
import java.io.IOException;

// TODO: race condition before setClient
// Should use an interface for TX anyway
public class InputTextLine extends TextField implements ActionListener, TextListener {
  TX tx;
  boolean hasNewText = false;

  public InputTextLine(int cols) {
    super(cols);
    addActionListener(this);
    addTextListener(this);
  }

  void setClient(TX tx) {
    this.tx = tx;
  }


  /**
   * getText is expensive.
   */
  private synchronized String getNewText() {
    if (hasNewText) {
      hasNewText = false;
      String text = getText();
      if (text != null && text.length() > 0) {
	setText("");
	return text;
      } else {
	return null;
      }
    }
    return null;
  }

  /**
   * Notice when there has been typing.
   */
  public synchronized void textValueChanged(TextEvent e) {
    hasNewText = true;
    if (tx != null) {
      String text = getNewText();
      if (text != null) 
	tx.addText(text);
    }
  }

  /** Handle the text field Return. */
  public synchronized void actionPerformed(ActionEvent e) {
    String text = getNewText();
    if (tx != null) {
      if (text == null) text = "";
      tx.addTextNewline(text);
    }
  }
}



