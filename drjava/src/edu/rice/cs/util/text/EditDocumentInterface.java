/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is part of DrJava.  Download the current version of this project from http://www.drjava.org/
 * or http://sourceforge.net/projects/drjava/
 *
 * DrJava Open Source License
 * 
 * Copyright (C) 2001-2005 JavaPLT group at Rice University (javaplt@rice.edu).  All rights reserved.
 *
 * Developed by:   Java Programming Languages Team, Rice University, http://www.cs.rice.edu/~javaplt/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 * documentation files (the "Software"), to deal with the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and 
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 *     - Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 *       following disclaimers.
 *     - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
 *       following disclaimers in the documentation and/or other materials provided with the distribution.
 *     - Neither the names of DrJava, the JavaPLT, Rice University, nor the names of its contributors may be used to 
 *       endorse or promote products derived from this Software without specific prior written permission.
 *     - Products derived from this software may not be called "DrJava" nor use the term "DrJava" as part of their 
 *       names without prior written permission from the JavaPLT group.  For permission, write to javaplt@rice.edu.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * WITH THE SOFTWARE.
 * 
 *END_COPYRIGHT_BLOCK*/

package edu.rice.cs.util.text;

import java.awt.print.Pageable;
import java.awt.print.PrinterException;
import java.io.Serializable;

/** Provides a toolkit-independent way to interact with a console document.  Allows components to use 
 *  documents that can be supplied by Swing, SWT (Eclipse), or other toolkits.  The document also has the 
 *  ability to restrict edits based on a conditional object, unless a separate method is called
 *  to force the edit.
 *
 * @version $Id$
 */
public interface EditDocumentInterface extends ReadersWritersLocking, Serializable {

  /** Gets the object which can determine whether an insert or remove edit should be applied, based on the inputs.
   *  @return an Object to determine legality of inputs
   */
  public DocumentEditCondition getEditCondition();

  /** Provides an object which can determine whether an insert or remove edit should be applied, based on the inputs.
   *  @param condition Object to determine legality of inputs
   */
  public void setEditCondition(DocumentEditCondition condition);

  /** Inserts a string into the document at the given offset and the given named style, if the edit condition allows it.
   *  @param offs Offset into the document
   *  @param str String to be inserted
   *  @param style Name of the style to use.  Must have been added using addStyle.
   *  @throws EditDocumentException if the offset is illegal
   */
  public void insertText(int offs, String str, String style);

  /** Inserts a string into the document at the given offset and named style, regardless of the edit condition.
   *  @param offs Offset into the document
   *  @param str String to be inserted
   *  @param style Name of the style to use.  Must have been
   *  added using addStyle.
   *  @throws EditDocumentException if the offset is illegal
   */
  public void forceInsertText(int offs, String str, String style);

  /** Removes a portion of the document, if the edit condition allows it.
   *  @param offs Offset to start deleting from
   *  @param len Number of characters to remove
   *  @throws EditDocumentException if the offset or length are illegal
   */
  public void removeText(int offs, int len);

  /** Removes a portion of the document, regardless of the edit condition.
   *  @param offs Offset to start deleting from
   *  @param len Number of characters to remove
   *  @throws EditDocumentException if the offset or length are illegal
   */
  public void forceRemoveText(int offs, int len);

  /** Returns the length of the document. */
  public int getLength();

  /** Returns a portion of the document.  Differs from getText in AbstractDocumentInterface by throwing
   *  EditDocumentException instead of BadLocationException.  (Why bother? It avoids referencing a Swing class.)
   *  @param offs First offset of the desired text
   *  @param len Number of characters to return
   *  @throws EditDocumentException if the offset or length are illegal
   */
  public String getDocText(int offs, int len);
  
  /** Appends a string to this in the given named style, if the edit condition allows it.
   *  @param str String to be inserted
   *  @param style Name of the style to use.  Must have been added using addStyle.
   *  @throws EditDocumentException if the offset is illegal
   */
  public void append(String str, String style);
  
  /** Gets the String identifying the default style for this document if one exists; null otherwise. */
  public String getDefaultStyle();
  
  /** Returns the Pageable object for printing.
   *  @return A Pageable representing this document.
   */
  public Pageable getPageable() throws IllegalStateException;
  
  /** Prints the given console document */
  public void print() throws PrinterException;
}
