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
import java.text.BreakIterator;


public class OutputTextArea extends TextArea {
  BreakIterator boundary = BreakIterator.getWordInstance();
  int col = 0;
  final static int MARGIN = 8;

  public OutputTextArea(int rows, int cols) {
    super("", rows, cols, TextArea.SCROLLBARS_BOTH);
    setBackground(java.awt.Color.white);
    setEditable(false);
  }

  
  // You must call init
  public void init() {
    setVisible(true);
  }
    
  void rubout() {
    try {
      setEditable(true);
      replaceRange("", getCaretPosition()-1, getCaretPosition());
      setEditable(false);
    } catch (IllegalArgumentException iex) {
      System.err.println("[Unable to rubout]");
    }
  }


  // Single character at a time append.
  // Unfortunately this never does the wrapping.
  public void appendWrapped(char c) {
    // System.err.print(c);
    switch(c) {
    case '\n':
    case '\r':
      newline();
      break;
    case '':
      if (--col < 0) {
	col = 0;	// uh oh
      }
      rubout();
      break;
    case ' ':
      if (col > getColumns()-MARGIN) {
	newline();
      } else {
	append(" ");
	c++;
      }
      break;
    default:
      append(""+c);
      col++;
    }
  }

  // unfortunately this never gets called becauase we add one at a time.
  public void appendWrapped(String s) {
    System.err.println("appendWrapped " +s);
    boundary.setText(s);
    int start = boundary.first();
    for (int end = boundary.next();
	 end != BreakIterator.DONE;
	 start = end, end = boundary.next()) {
      int len = end-start;
      if (col+len >= getColumns()) 
	newline();
      col += len;
      append(s.substring(start,end));
    }
  }

  void newline() {
    col = 0;
    append("\n");
  }
}
