/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.common.color;

public interface IGradientColorProvider extends IMapColorProvider {

	/**
	 * Configure color provider by setting min/max values, units ..., the common color provider do
	 * not contain data values only subclasses.
	 * 
	 * @param imageSize
	 *            Width or height of the image.
	 * @param imageSize
	 * @param minValue
	 * @param maxValue
	 * @param unitText
	 * @param unitFormat
	 * @param isConvertIntoAbsoluteValues
	 */
	abstract void configureColorProvider(	int imageSize,
											float minValue,
											float maxValue,
											String unitText,
											LegendUnitFormat unitFormat,
											boolean isConvertIntoAbsoluteValues);

	abstract MapColorProfile getColorProfile();

	/**
	 * @param graphValue
	 * @return Returns RGB value for a graph value.
	 */
	abstract int getColorValue(float graphValue);

	/**
	 * @return Returns configuration how a map legend image is painted.
	 */
	abstract MapUnits getMapUnits();

	/**
	 * Set the colors for the map.
	 * 
	 * @param newMapColor
	 */
	abstract void setColorProfile(MapColorProfile mapColorProfile);
}