/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.action;

import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ActionToolbarSlideoutAdv;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.photo.Slideout_Map2_PhotoFilter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.ToolItem;

public class ActionMap2_PhotoFilter extends ActionToolbarSlideoutAdv {

   /*
    * UI resources
    */
// SET_FORMATTING_OFF

   private static Image _imageEnabled              = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER);
   private static Image _imageEnabled_NoPhotos     = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER_NO_PHOTOS);
   private static Image _imageEnabled_WithPhotos   = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER_WITH_PHOTOS);
   private static Image _imageDisabled             = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER_DISABLED);

// SET_FORMATTING_ON

   private IDialogSettings           _state;

   private Map2View                  _map2View;
   private Slideout_Map2_PhotoFilter _slideoutPhotoFilter;

   public ActionMap2_PhotoFilter(final Map2View map2View, final IDialogSettings state) {

      super(_imageEnabled, _imageDisabled);

      _map2View = map2View;
      _state = state;

      isToggleAction = true;
      notSelectedTooltip = Messages.Map_Action_PhotoFilter2_Tooltip;
   }

   @Override
   protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

      _slideoutPhotoFilter = new Slideout_Map2_PhotoFilter(toolItem, _map2View, _state);
      _slideoutPhotoFilter.setSlideoutLocation(SlideoutLocation.BELOW_CENTER);

      return _slideoutPhotoFilter;
   }

   public Slideout_Map2_PhotoFilter getPhotoFilterSlideout() {
      return _slideoutPhotoFilter;
   }

   @Override
   protected void onBeforeOpenSlideout() {

      _map2View.closeOpenedDialogs(this);
   }

   @Override
   protected void onSelect(final SelectionEvent selectionEvent) {

      // show/hide slideout
      super.onSelect(selectionEvent);

      _map2View.photoFilter_UpdateFromAction(getSelection());
   }

   public void updateUI() {

      final int numAllPhotos = _map2View.getPhotos().size();
      final int numFilteredPhotos = _map2View.getFilteredPhotos().size();

      getActionToolItem().setImage(

            numAllPhotos == 0
                  ? _imageEnabled
                  : numFilteredPhotos == 0
                        ? _imageEnabled_NoPhotos
                        : _imageEnabled_WithPhotos);
   }

   @Override
   protected void updateUI_ToolItem_Image() {

      updateUI();
   }

}
