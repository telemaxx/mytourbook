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
package net.tourbook.common.formatter;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;

public class ValueFormatter_Time_SSS implements IValueFormatter {

   @Override
   public String printDouble(final double value) {
      return Messages.App_Error_NotSupportedValueFormatter;
   }

   @Override
   public String printLong(final long value) {
      return printLong(value, true, true);
   }

   @Override
   public String printLong(final long value, final boolean isHide0Value, final boolean isShowBiggerThan0) {

      if (value == 0 && isHide0Value) {
         return UI.EMPTY_STRING;
      }

      return Long.toString(value);
   }

   @Override
   public String toString() {
      return "ValueFormatter_Time_SSS [" // //$NON-NLS-1$
            + "printLong()" //$NON-NLS-1$
            + "]"; //$NON-NLS-1$
   }

}
