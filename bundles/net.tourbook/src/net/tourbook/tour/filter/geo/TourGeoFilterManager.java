/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter.geo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import net.tourbook.application.ActionTourGeoFilter;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.filter.SQLFilterData;

public class TourGeoFilterManager {

   private static final Bundle           _bundle                  = TourbookPlugin.getDefault().getBundle();

   private static final IPath            _stateLocation           = Platform.getStateLocation(_bundle);
   private final static IPreferenceStore _prefStore               = TourbookPlugin.getPrefStore();

   private static final String           TOUR_FILTER_FILE_NAME    = "tour-geo-filter.xml";                  //$NON-NLS-1$
   private static final int              TOUR_FILTER_VERSION      = 1;

   private static final String           TAG_ROOT                 = "TourGeoFilterItems";                   //$NON-NLS-1$

   private static final String           ATTR_TOUR_FILTER_VERSION = "tourFilterVersion";                    //$NON-NLS-1$

   private static ActionTourGeoFilter    _actionTourFilter;

   private static boolean                _isTourGeoFilterEnabled;

   private static int[]                  _fireEventCounter        = new int[1];

   private static double                 _geo_Selected_Lat_1;
   private static double                 _geo_Selected_Lon_1;
   private static double                 _geo_Selected_Lat_2;
   private static double                 _geo_Selected_Lon_2;

   /**
    * Fire event that the tour filter has changed.
    */
   static void fireTourFilterModifyEvent() {

      _fireEventCounter[0]++;

      Display.getDefault().asyncExec(new Runnable() {

         final int __runnableCounter = _fireEventCounter[0];

         @Override
         public void run() {

            // skip all events which has not yet been executed
            if (__runnableCounter != _fireEventCounter[0]) {

               // a new event occured
               return;
            }

            _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
         }
      });

   }

   /**
    * @return Returns sql data for the selected tour filter profile or <code>null</code> when not
    *         available.
    */
   public static SQLFilterData getSQL() {

      if (_isTourGeoFilterEnabled == false /* || _selectedProfile == null */) {

         // tour filter is not enabled or not selected

         return null;
      }

      final StringBuilder sqlWhere = new StringBuilder();
      final ArrayList<Object> sqlParameters = new ArrayList<>();

      //         int latPart = (int) (latitude * 100);
      //         int lonPart = (int) (longitude * 100);
      //
      //         lat      ( -90 ... + 90) * 100 =  -9_000 +  9_000 = 18_000
      //         lon      (-180 ... +180) * 100 = -18_000 + 18_000 = 36_000
      //
      //         max      (9_000 + 9_000) * 100_000 = 18_000 * 100_000  = 1_800_000_000
      //
      //                                    Integer.MAX_VALUE = 2_147_483_647

//      final double latitude = partLatitude[serieIndex];
//      final double longitude = partLongitude[serieIndex];
//
//      final int latPart = (int) (latitude * 100);
//      final int lonPart = (int) (longitude * 100);
//
//      final int latLonPart = (latPart + 9_000) * 100_000 + (lonPart + 18_000);

      final double gridRounding = 0.000_000_1;
      final double gridSize = 0.01 + gridRounding;

      final double normalizedLat1 = _geo_Selected_Lat_1 + TourData.NORMALIZED_LATITUDE_OFFSET;
      final double normalizedLat2 = _geo_Selected_Lat_2 + TourData.NORMALIZED_LATITUDE_OFFSET;

      final double normalizedLon1 = _geo_Selected_Lon_1 + TourData.NORMALIZED_LONGITUDE_OFFSET;
      final double normalizedLon2 = _geo_Selected_Lon_2 + TourData.NORMALIZED_LONGITUDE_OFFSET;

//      System.out.println();

//      final TIntArrayList allGeoParts = new TIntArrayList();

      for (double normalizedLon = normalizedLon1; normalizedLon < normalizedLon2; normalizedLon += gridSize) {

         for (double normalizedLat = normalizedLat2; normalizedLat < normalizedLat1; normalizedLat += gridSize) {

            final double latitude = normalizedLat - TourData.NORMALIZED_LATITUDE_OFFSET;
            final double longitude = normalizedLon - TourData.NORMALIZED_LONGITUDE_OFFSET;

            final double latitude100 = latitude * 100;
            final double longitude100 = longitude * 100;

            final int latPart = (int) Math.round(latitude100);
            final int lonPart = (int) Math.round(longitude100);

            final int latLonPart = (latPart + 9_000) * 100_000 + (lonPart + 18_000);

//            allGeoParts.add(latLonPart);

            sqlParameters.add(latLonPart);

//            System.out.println(String.format("lon(x) %d  lat(y) %d  %s",
//
//                  lonPart,
//                  latPart,
//
//                  Integer.toString(latLonPart)
//
//            ));
// TODO remove SYSTEM.OUT.PRINTLN

         }
      }

//      final int numGeoParts = allGeoParts.size();
      final int numGeoParts = sqlParameters.size();

      /*
       * Create sql parameters
       */
      final StringBuilder sqlInParameters = new StringBuilder();
      for (int partIndex = 0; partIndex < numGeoParts; partIndex++) {
         if (partIndex == 0) {
            sqlInParameters.append(" ?"); //$NON-NLS-1$
         } else {
            sqlInParameters.append(", ?"); //$NON-NLS-1$
         }
      }

      final char NL = UI.NEW_LINE;

      final String selectGeoPart = "SELECT" + NL //                       //$NON-NLS-1$

            + " DISTINCT TourId " + NL //                           //$NON-NLS-1$

            + (" FROM " + TourDatabase.TABLE_TOUR_GEO_PARTS + NL) //      //$NON-NLS-1$
            + (" WHERE GeoPart IN (" + sqlInParameters + ")") + NL //      //$NON-NLS-1$ //$NON-NLS-2$
      ;

      sqlWhere.append(" AND TourId IN (" + selectGeoPart + ") ");

      final SQLFilterData tourFilterSQLData = new SQLFilterData(sqlWhere.toString(), sqlParameters);

      return tourFilterSQLData;
   }

   private static File getXmlFile() {

      final File layerFile = _stateLocation.append(TOUR_FILTER_FILE_NAME).toFile();

      return layerFile;
   }

   /**
    * Read filter profile xml file.
    *
    * @return
    */
   private static void readFilterProfile() {

      final File xmlFile = getXmlFile();

      if (xmlFile.exists()) {

         try (InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), UI.UTF_8)) {

            final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);
            for (final IMemento mementoChild : xmlRoot.getChildren()) {

//               final XMLMemento xmlProfile = (XMLMemento) mementoChild;
//               if (TAG_PROFILE.equals(xmlProfile.getType())) {
//
//                  final TourFilterProfile tourFilterProfile = new TourFilterProfile();
//
//                  tourFilterProfile.name = Util.getXmlString(xmlProfile, ATTR_NAME, UI.EMPTY_STRING);
//
//                  _filterProfiles.add(tourFilterProfile);
//
//                  // set selected profile
//                  if (Util.getXmlBoolean(xmlProfile, ATTR_IS_SELECTED, false)) {
//                     _selectedProfile = tourFilterProfile;
//                  }
//
//                  // loop: all properties
//                  for (final IMemento mementoProperty : xmlProfile.getChildren(TAG_PROPERTY)) {
//
//                     final XMLMemento xmlProperty = (XMLMemento) mementoProperty;
//
//                     final TourFilterFieldId fieldId = (TourFilterFieldId) Util.getXmlEnum(//
//                           xmlProperty,
//                           ATTR_FIELD_ID,
//                           TourFilterFieldId.TIME_TOUR_DATE);
//
//                     final TourFilterFieldOperator fieldOperator = (TourFilterFieldOperator) Util.getXmlEnum(//
//                           xmlProperty,
//                           ATTR_FIELD_OPERATOR,
//                           TourFilterFieldOperator.EQUALS);
//
//                     final TourFilterFieldConfig fieldConfig = getFieldConfig(fieldId);
//
//                     final TourFilterProperty filterProperty = new TourFilterProperty();
//
//                     filterProperty.fieldConfig = fieldConfig;
//                     filterProperty.fieldOperator = fieldOperator;
//                     filterProperty.isEnabled = Util.getXmlBoolean(xmlProperty, ATTR_IS_ENABLED, true);
//
//                     readFilterProfile_10_PropertyDetail(xmlProperty, filterProperty);
//
//                     tourFilterProfile.filterProperties.add(filterProperty);
//                  }
//               }
            }

         } catch (final Exception e) {
            StatusUtil.log(e);
         }
      }
   }

   public static void restoreState() {

      _isTourGeoFilterEnabled = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_GEO_FILTER_IS_SELECTED);
      _actionTourFilter.setSelection(_isTourGeoFilterEnabled);

//      readFilterProfile();
   }

   public static void saveState() {

      _prefStore.setValue(ITourbookPreferences.APP_TOUR_GEO_FILTER_IS_SELECTED, _actionTourFilter.getSelection());
//
//      final XMLMemento xmlRoot = writeFilterProfile();
//      final File xmlFile = getXmlFile();
//
//      Util.writeXml(xmlRoot, xmlFile);
   }

   public static void setFilter(final double geoLat1, final double geoLon1, final double geoLat2, final double geoLon2) {

      _geo_Selected_Lat_1 = geoLat1;
      _geo_Selected_Lon_1 = geoLon1;

      _geo_Selected_Lat_2 = geoLat2;
      _geo_Selected_Lon_2 = geoLon2;

//      System.out.println((UI.timeStampNano() + " [" + TourGeoFilterManager.class.getSimpleName() + "] () ")
//
//            + String.format("Lat %2.2f  %2.2f   Lon %3.2f  %3.2f",
//
//                  geoLat1,
//                  geoLat2,
//
//                  geoLon1,
//                  geoLon2));
//TODO remove SYSTEM.OUT.PRINTLN

      _actionTourFilter.setEnabled(true);

      fireTourFilterModifyEvent();
   }

   /**
    * Sets the state if the tour filter is active or not.
    *
    * @param isEnabled
    */
   public static void setFilterEnabled(final boolean isEnabled) {

      _isTourGeoFilterEnabled = isEnabled;

      fireTourFilterModifyEvent();
   }

   public static void setTourGeoFilterAction(final ActionTourGeoFilter _actionTourGeoFilter) {
      _actionTourFilter = _actionTourGeoFilter;
   }

   /**
    * @return
    */
   private static XMLMemento writeFilterProfile() {

      XMLMemento xmlRoot = null;

      try {

         xmlRoot = writeFilterProfile_10_Root();

         // loop: profiles
//         for (final TourFilterProfile tourFilterProfile : _filterProfiles) {
//
//            final IMemento xmlProfile = xmlRoot.createChild(TAG_PROFILE);
//
//            xmlProfile.putString(ATTR_NAME, tourFilterProfile.name);
//
//            // set flag for active profile
//            if (tourFilterProfile == _selectedProfile) {
//               xmlProfile.putBoolean(ATTR_IS_SELECTED, true);
//            }
//
//            // loop: properties
//            for (final TourFilterProperty filterProperty : tourFilterProfile.filterProperties) {
//
//               final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
//               final TourFilterFieldOperator fieldOperator = filterProperty.fieldOperator;
//
//               final IMemento xmlProperty = xmlProfile.createChild(TAG_PROPERTY);
//
//               Util.setXmlEnum(xmlProperty, ATTR_FIELD_ID, fieldConfig.fieldId);
//               Util.setXmlEnum(xmlProperty, ATTR_FIELD_OPERATOR, fieldOperator);
//               xmlProperty.putBoolean(ATTR_IS_ENABLED, filterProperty.isEnabled);
//
//               writeFilterProfile_20_PropertyDetail(xmlProperty, filterProperty);
//            }
//         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return xmlRoot;
   }

   private static XMLMemento writeFilterProfile_10_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plugin version
      final Version version = _bundle.getVersion();
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
      xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

      // layer structure version
      xmlRoot.putInteger(ATTR_TOUR_FILTER_VERSION, TOUR_FILTER_VERSION);

      return xmlRoot;
   }

}
