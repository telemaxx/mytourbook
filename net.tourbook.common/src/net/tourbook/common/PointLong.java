/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.tourbook.common;

import java.io.Serializable;

/*
 * this is a copy from org.eclipse.swt.graphics.Point but with long values
 */

/**
 * Instances of this class represent places on the (x, y) coordinate plane.
 * <p>
 * The coordinate space for rectangles and points is considered to have increasing values downward
 * and to the right from its origin making this the normal, computer graphics oriented notion of (x,
 * y) coordinates rather than the strict mathematical one.
 * </p>
 * <p>
 * The hashCode() method in this class uses the values of the public fields to compute the hash
 * value. When storing instances of the class in hashed collections, do not modify these fields
 * after the object has been inserted.
 * </p>
 * <p>
 * Application code does <em>not</em> need to explicitly release the resources managed by each
 * instance when those instances are no longer required, and thus no <code>dispose()</code> method
 * is provided.
 * </p>
 * 
 * @see Rectangle
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 */

public final class PointLong implements Serializable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 3513009271453972410L;

	/**
	 * the x coordinate of the point
	 */
	public long					x;

	/**
	 * the y coordinate of the point
	 */
	public long					y;

	/**
	 * Constructs a new point with the given x and y coordinates.
	 * 
	 * @param x
	 *            the x coordinate of the new point
	 * @param y
	 *            the y coordinate of the new point
	 */
	public PointLong(final long x, final long y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Compares the argument to the receiver, and returns true if they represent the <em>same</em>
	 * object using a class specific comparison.
	 * 
	 * @param object
	 *            the object to compare with this object
	 * @return <code>true</code> if the object is the same as this object and <code>false</code>
	 *         otherwise
	 * @see #hashCode()
	 */
	@Override
	public boolean equals(final Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof PointLong)) {
			return false;
		}
		final PointLong p = (PointLong) object;
		return (p.x == this.x) && (p.y == this.y);
	}

	/**
	 * Returns an integer hash code for the receiver. Any two objects that return <code>true</code>
	 * when passed to <code>equals</code> must return the same value for this method.
	 * 
	 * @return the receiver's hash
	 * @see #equals(Object)
	 */
	@Override
	public int hashCode() {
		return (int) (x ^ y);
	}

	/**
	 * Returns a string containing a concise, human-readable description of the receiver.
	 * 
	 * @return a string representation of the point
	 */
	@Override
	public String toString() {
		return "Point {" + x + ", " + y + "}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
