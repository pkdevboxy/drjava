/*BEGIN_COPYRIGHT_BLOCK
 *
 * Copyright (c) 2001-2008, JavaPLT group at Rice University (drjava@rice.edu)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of DrJava, the JavaPLT group, Rice University, nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software is Open Source Initiative approved Open Source Software.
 * Open Source Initative Approved is a trademark of the Open Source Initiative.
 * 
 * This file is part of DrJava.  Download the current version of this project
 * from http://www.drjava.org/ or http://sourceforge.net/projects/drjava/
 * 
 * END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.config;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/** Class representing values that can be inserted as variables in external processes.
 *
 *  @version $Id$
 */
public abstract class DrJavaProperty implements Comparable<DrJavaProperty> {
  /** Whether the invalidation listening mechanism has been deactivated due to an error. */
  public volatile boolean DEACTIVATED_DUE_TO_ERROR = false;
  
  /** Name of the property. Must be unique. */
  protected String _name;
  /** Value of the property. */
  protected String _value = "--uninitialized--";
  /** Is the value current? */
  protected boolean _isCurrent = false;
  /** Set of other properties that are listening to this property, i.e.
    * when this property is invalidated, the other properties are too. */
  protected Set<DrJavaProperty> _listening = new HashSet<DrJavaProperty>();
  
  /** Create a property. */
  public DrJavaProperty(String name) {
    if (name==null) { throw new IllegalArgumentException("DrJavaProperty name is null"); }
    _name = name;
  }

  /** Create a property. */
  public DrJavaProperty(String name, String value) {
    this(name);
    if (value==null) { throw new IllegalArgumentException("DrJavaProperty value is null"); }
    _value = value;
    _isCurrent = true;
  }
  
  /** Return the name of the property. */
  public String getName() { return _name; }
  
  /** Return the value of the property. If it is not current, update first. */
  public String getCurrent() {
    if (!_isCurrent) {
      update();
      if (_value==null) { throw new IllegalArgumentException("DrJavaProperty value is null"); }
      _isCurrent = true;
    }
    return _value;
  }
  
  /** Update the property so the value is current. */
  public abstract void update();
  
  /** Return the value, which might be stale. */
  public String toString() {
    return _value;
  }
  
  /** Return true if the value is current. */
  public boolean isCurrent() { return _isCurrent; }
    
  /** Mark the value as stale and invalidate other properties that are listening. */
  public void invalidate() {
    _invalidate();
    invalidateOthers(new HashSet<DrJavaProperty>());
  }
  
  /** Just invalidate. */
  protected void _invalidate() { _isCurrent = false; }
  
  public DrJavaProperty listenToInvalidatesOf(DrJavaProperty other) {
    if (other==this) {
      DEACTIVATED_DUE_TO_ERROR = true;
      RuntimeException e = new IllegalArgumentException("Property cannot listen for invalidation of itself. "+
                                                        "Variables for external processes will not function correctly anymore. "+
                                                        "This is a SERIOUS programming error. Please notify the DrJava team.");
      edu.rice.cs.drjava.ui.DrJavaErrorHandler.record(e);
      throw e;
    }
    other._listening.add(this);
    return this;
  }
  
  /** Compare two properties. */
  public int compareTo(DrJavaProperty o) {
    return _name.compareTo(o._name);
  }
  
  /** @return true if the specified property is equal to this one. */
  public boolean equals(Object other) {
    if (other == null || other.getClass() != this.getClass()) return false;
    DrJavaProperty o = (DrJavaProperty)other;
    return _name.equals(o._name) && (_isCurrent == o._isCurrent) && _value.equals(o._value);
  }
  
  /** @return the hash code. */
  public int hashCode() {
    int result;
    result = _name.hashCode();
    result = 31 * result + (_value.hashCode());
    result = 31 * result + (_isCurrent?1:0);
    return result;
  }
  
  /** Invalidate those properties that are listening to this property.
    * @param alreadyVisited set of properties already visited, to avoid cycles. */
  protected void invalidateOthers(Set<DrJavaProperty> alreadyVisited) {
    if (DEACTIVATED_DUE_TO_ERROR) { return; }          
    if (alreadyVisited.contains(this)) {
      Iterator<DrJavaProperty> it = alreadyVisited.iterator();
      StringBuilder sb = new StringBuilder("Invalidating ");
      sb.append(getName());
      sb.append(" after already having invalidated ");
      boolean first = true;
      while(it.hasNext()) {
        if (first) { first = false; } 
        else { sb.append(", "); }
        sb.append(it.next().getName());
      }
      sb.append(". Variables for external processes will not function correctly anymore. "+
                "This is a SERIOUS programming error. Please notify the DrJava team.");
      DEACTIVATED_DUE_TO_ERROR = true;
      RuntimeException e = new InfiniteLoopException(sb.toString());
      edu.rice.cs.drjava.ui.DrJavaErrorHandler.record(e);
      throw e;
    }
    alreadyVisited.add(this);
    Iterator<DrJavaProperty> it = _listening.iterator();
    while(it.hasNext()) {
      DrJavaProperty prop = it.next();
      prop._invalidate();
      prop.invalidateOthers(alreadyVisited);
    }
  }
  
  /** Exception thrown if an infinite loop of invalidation listening is detected. */
  public static class InfiniteLoopException extends RuntimeException {
    public InfiniteLoopException(String s) { super(s); } 
  }
} 