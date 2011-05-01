/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;

@Entity
public class TourPersonHRZone implements Comparable<TourPersonHRZone> {

	/**
	 * Unique id for the {@link TourPersonHRZone} entity
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long		hrZoneId		= TourDatabase.ENTITY_IS_NOT_SAVED;

	@ManyToOne(optional = false)
	private TourPerson	tourPerson;

	/**
	 * unique id for manually created markers because the {@link #hrZoneId} is 0 when the marker is
	 * not persisted
	 */
	@Transient
	private long		_createId		= 0;

	/**
	 * manually created marker or imported marker create a unique id to identify them, saved marker
	 * are compared with the marker id
	 */
	private static int	_createCounter	= 0;

	public TourPersonHRZone() {}

	public TourPersonHRZone(final TourPerson tourPerson) {

		this.tourPerson = tourPerson;

		_createId = ++_createCounter;
	}

	@Override
	public int compareTo(final TourPersonHRZone o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TourPersonHRZone)) {
			return false;
		}

		final TourPersonHRZone other = (TourPersonHRZone) obj;

		if (_createId == 0) {

			// hr zone is from the database
			if (hrZoneId != other.hrZoneId) {
				return false;
			}
		} else {

			// hr zone was create
			if (_createId != other._createId) {
				return false;
			}
		}

		return true;
	}

	public TourPerson getTourPerson() {
		return tourPerson;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
		result = prime * result + (int) (hrZoneId ^ (hrZoneId >>> 32));
		return result;
	}

	public void setTourPerson(final TourPerson tourPerson) {
		this.tourPerson = tourPerson;
	}

}
