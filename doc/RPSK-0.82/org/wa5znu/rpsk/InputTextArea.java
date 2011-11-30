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
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.text.BreakIterator;


public class InputTextArea extends TextArea {
  public InputTextArea(int rows, int cols) {
    super("", rows, cols, TextArea.SCROLLBARS_BOTH);
    setBackground(java.awt.Color.white);
    setEditable(false);
  }

  int lastSent = 0;

  public synchronized void clear() {
    lastSent = getCaretPosition();
    setText("");
    lastSent = 0;
  }

  /**
   * Don't let the user modify text that's already been sent.
   * @param      str the non-<code>null</code> text to insert
   * @param      pos the position at which to insert
   */
  public void insert(String str, int pos) {
    System.err.println("insert " + str+" " + pos);
    if (pos >= lastSent) {
      super.insert(str, pos);
    } else {
      System.err.println("beep");
    }
  }

  /**
   * Don't let the user modify text that's already been sent.     
     *
     * @param     str      the non-<code>null</code> text to use as
     *                     the replacement
     * @param     start    the start position
     * @param     end      the end position
     * @see       java.awt.TextArea#insert
     */
    public void replaceRange(String str, int start, int end) {
      if (end >= lastSent) {
	super.replaceRange(str, start, end);
      } else {
	System.err.println("beep");
      }
    }


  /**
   * Don't let the user position back to text that's already been sent.
   * This restriction is not optimal since it keeps you from selecting old text,
   * but until we can change the display of the text to show it has been sent,
   * it seems valuable.
   *
   * @param        position the position of the text insertion caret
   * @exception    IllegalArgumentException if the value supplied
   *                   for <code>position</code> is less than zero
   */
    public synchronized void setCaretPosition(int position) {
      System.err.println("setCaretPosition " + position);
      if (position >= lastSent) {
	super.setCaretPosition(position);
      } else {
	System.err.println("beep");
      }
    }
}
