/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.INatTablePropertiesProvider;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tour.printing.ActionPrint;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.ITourProviderByID;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionDuplicateTour;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetPerson;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;
import net.tourbook.ui.views.geoCompare.GeoPartComparerItem;
import net.tourbook.ui.views.rawData.ActionMergeTour;
import net.tourbook.ui.views.rawData.Action_Reimport_SubMenu;
import net.tourbook.ui.views.rawData.SubMenu_AdjustTourValues;
import net.tourbook.ui.views.tourBook.natTable.DataProvider_ColumnHeader;
import net.tourbook.ui.views.tourBook.natTable.NatTable_DataLoader;
import net.tourbook.ui.views.tourBook.natTable.NatTable_Header_Tooltip;
import net.tourbook.ui.views.tourBook.natTable.TourRowDataProvider;

import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.hover.HoverLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnOverrideLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.ImagePainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.CellPainterDecorator;
import org.eclipse.nebula.widgets.nattable.reorder.ColumnReorderLayer;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectRowsCommand;
import org.eclipse.nebula.widgets.nattable.selection.config.DefaultRowSelectionLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.style.theme.ModernNatTableThemeConfiguration;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.util.CellEdgeEnum;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourBookView extends ViewPart implements ITourProvider2, ITourViewer3, ITourProviderByID, ITreeViewer, INatTablePropertiesProvider {

// SET_FORMATTING_OFF

   private static final String           COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP           = net.tourbook.ui.Messages.ColumnFactory_TimeZoneDifference_Tooltip;

// SET_FORMATTING_ON
   //
   static public final String             ID                                              = "net.tourbook.views.tourListView";          //$NON-NLS-1$
   //
   private final static IPreferenceStore  _prefStore                                      = TourbookPlugin.getPrefStore();
   private final static IPreferenceStore  _prefStoreCommon                                = CommonActivator.getPrefStore();
   //
   private static final IDialogSettings   _state                                          = TourbookPlugin.getState(ID);
   private static final IDialogSettings   _state_NatTable                                 = TourbookPlugin.getState(ID + "_NAT_TABLE"); //$NON-NLS-1$
   private static final IDialogSettings   _state_Tree                                     = TourbookPlugin.getState(ID + "_TREE");      //$NON-NLS-1$
   //
   private static final String            STATE_CSV_EXPORT_PATH                           = "STATE_CSV_EXPORT_PATH";                    //$NON-NLS-1$
   //
   private static final String            STATE_IS_LINK_WITH_OTHER_VIEWS                  = "STATE_IS_LINK_WITH_OTHER_VIEWS";           //$NON-NLS-1$
   private static final String            STATE_IS_SELECT_YEAR_MONTH_TOURS                = "STATE_IS_SELECT_YEAR_MONTH_TOURS";         //$NON-NLS-1$
   static final String                    STATE_IS_SHOW_SUMMARY_ROW                       = "STATE_IS_SHOW_SUMMARY_ROW";                //$NON-NLS-1$
   static final String                    STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS         = "STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS";  //$NON-NLS-1$
   private static final String            STATE_SELECTED_MONTH                            = "STATE_SELECTED_MONTH";                     //$NON-NLS-1$
   private static final String            STATE_SELECTED_TOURS                            = "STATE_SELECTED_TOURS";                     //$NON-NLS-1$
   private static final String            STATE_SELECTED_YEAR                             = "STATE_SELECTED_YEAR";                      //$NON-NLS-1$
   private static final String            STATE_VIEW_LAYOUT                               = "STATE_VIEW_LAYOUT";                        //$NON-NLS-1$
   //
   private static final String            STATE_SORT_COLUMN_DIRECTION                     = "STATE_SORT_COLUMN_DIRECTION";              //$NON-NLS-1$
   private static final String            STATE_SORT_COLUMN_ID                            = "STATE_SORT_COLUMN_ID";                     //$NON-NLS-1$
   //
   static final boolean                   STATE_IS_SHOW_SUMMARY_ROW_DEFAULT               = true;
   static final boolean                   STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS_DEFAULT = true;
   //
   private static final String            CSV_EXPORT_DEFAULT_FILE_NAME                    = "TourBook_";                                //$NON-NLS-1$
   //
   /**
    * The header column id needs a different id than the body column otherwise drag&drop or column
    * selection shows the 1st row image :-(
    */
   private static final String            HEADER_COLUMN_ID_POSTFIX                        = "_HEADER";
   //
   private static TourBookViewLayout      _viewLayout;
   //
   private ColumnFactory                  _columnFactory;
   private ColumnManager                  _columnManager_NatTable;
   private ColumnManager                  _columnManager_Tree;
   //
   private OpenDialogManager              _openDlgMgr                                     = new OpenDialogManager();
   //
   private PostSelectionProvider          _postSelectionProvider;
   //
   private ISelectionListener             _postSelectionListener;
   private IPartListener2                 _partListener;
   private ITourEventListener             _tourPropertyListener;
   private IPropertyChangeListener        _prefChangeListener;
   private IPropertyChangeListener        _prefChangeListenerCommon;
   //
   private NatTable                       _tourViewer_NatTable;
   private TreeViewer                     _tourViewer_Tree;
   private ItemComparator_Table           _tourViewer_Table_Comparator                    = new ItemComparator_Table();
   //
   private TVITourBookRoot                _rootItem_Tree;
   private long                           _hoveredTourId;
   //
   private DataLayer                      _natTable_ColumnHeader_DataLayer;
   private ColumnHeaderLayer              _natTable_ColumnHeader_Layer;
   private ColumnHideShowLayer            _natTable_Body_ColumnHideShowLayer;
   private ColumnReorderLayer             _natTable_Body_ColumnReorderLayer;
   private DataLayer                      _natTable_Body_DataLayer;
   private HoverLayer                     _natTable_Body_HoverLayer;
   private SelectionLayer                 _natTable_Body_SelectionLayer;
   private ViewportLayer                  _natTable_Body_ViewportLayer;
   private NatTable_DataLoader            _natTable_DataLoader;
   //
   private NatTableContentTooltip         _natTable_Tooltip;
   //
   private int                            _selectedYear                                   = -1;
   private int                            _selectedYearSub                                = -1;
   private final ArrayList<Long>          _selectedTourIds                                = new ArrayList<>();
   //
   private boolean                        _isCollapseOthers;
   private boolean                        _isInFireSelection;
   private boolean                        _isInReload;
   private boolean                        _isInStartup;
   private boolean                        _isLayoutNatTable;
   //
   private final TourDoubleClickState     _tourDoubleClickState                           = new TourDoubleClickState();
//   private TableViewerTourInfoToolTip     _tourInfoToolTip_NatTable;
   private TreeViewerTourInfoToolTip      _tourInfoToolTip_Tree;
//
   private TagMenuManager                 _tagMenuManager;
   private MenuManager                    _viewerMenuManager_NatTable;
   private MenuManager                    _viewerMenuManager_Table;
   private MenuManager                    _viewerMenuManager_Tree;
   private IContextMenuProvider           _viewerContextMenuProvider_NatTable             = new ContextMenuProvider_NatTable();
   private IContextMenuProvider           _viewerContextMenuProvider_Tree                 = new ContextMenuProvider_Tree();
   //
   private SubMenu_AdjustTourValues       _subMenu_AdjustTourValues;
   private Action_Reimport_SubMenu        _subMenu_Reimport;
   //
   private ActionCollapseAll              _actionCollapseAll;
   private ActionCollapseOthers           _actionCollapseOthers;
   private ActionDuplicateTour            _actionDuplicateTour;
   private ActionEditQuick                _actionEditQuick;
   private ActionExpandSelection          _actionExpandSelection;
   private ActionExport                   _actionExportTour;
   private ActionExportViewCSV            _actionExportViewCSV;
   private ActionDeleteTourMenu           _actionDeleteTour;
   private ActionEditTour                 _actionEditTour;
   private ActionJoinTours                _actionJoinTours;
   private ActionLinkWithOtherViews       _actionLinkWithOtherViews;
   private ActionMergeTour                _actionMergeTour;
   private ActionModifyColumns            _actionModifyColumns;
   private ActionOpenTour                 _actionOpenTour;
   private ActionOpenMarkerDialog         _actionOpenMarkerDialog;
   private ActionOpenAdjustAltitudeDialog _actionOpenAdjustAltitudeDialog;
   private ActionPrint                    _actionPrintTour;
   private ActionRefreshView              _actionRefreshView;
   private ActionSelectAllTours           _actionSelectAllTours;
   private ActionSetTourTypeMenu          _actionSetTourType;
   private ActionSetPerson                _actionSetOtherPerson;
   private ActionToggleViewLayout         _actionToggleViewLayout;
   private ActionTourBookOptions          _actionTourBookOptions;
   //
   private PixelConverter                 _pc;
   //
   /*
    * UI controls
    */
   private PageBook                    _pageBook;
   //
   private Composite                   _parent;
   private Composite                   _viewerContainer_NatTable;
   private Composite                   _viewerContainer_Tree;
   //
   private Menu                        _contextMenu_NatTable;
   private Menu                        _contextMenu_Tree;

   private ActionShowOnlySelectedTours _actionShowOnlySelectedTours;

   private class ActionLinkWithOtherViews extends ActionToolbarSlideout {

      public ActionLinkWithOtherViews() {

         super(TourbookPlugin.getImageDescriptor(Messages.Image__SyncViews), null);

         isToggleAction = true;
         notSelectedTooltip = Messages.Calendar_View_Action_LinkWithOtherViews;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new SlideoutLinkWithOtherViews(_parent, toolbar, TourBookView.this);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {
         super.onSelect();
      }
   }

   private class ActionTourBookOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutTourBookOptions(_parent, toolbar, TourBookView.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ContentProvider_Tree implements ITreeContentProvider {

      @Override
      public Object[] getChildren(final Object parentElement) {
         return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _rootItem_Tree.getFetchedChildrenAsArray();
      }

      @Override
      public Object getParent(final Object element) {
         return ((TreeViewerItem) element).getParentItem();
      }

      @Override
      public boolean hasChildren(final Object element) {
         return ((TreeViewerItem) element).hasChildren();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   private class ContextMenuProvider_NatTable implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_contextMenu_NatTable != null) {
            _contextMenu_NatTable.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _contextMenu_NatTable;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _contextMenu_NatTable = createUI_52_ViewerContextMenu_NatTable();

         return _contextMenu_NatTable;
      }

   }

   private class ContextMenuProvider_Tree implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_contextMenu_Tree != null) {
            _contextMenu_Tree.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _contextMenu_Tree;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _contextMenu_Tree = createUI_62_ViewerContextMenu_Tree();

         return _contextMenu_Tree;
      }

   }

   public class ItemComparator_Table /* extends ViewerComparator */ {

      public static final int  ASCENDING  = 0;
      private static final int DESCENDING = 1;

      private String           __sortColumnId;
      private int              __sortDirection;

//      @Override
//      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
//
//         final TVITourBookTour tourItem1 = ((TVITourBookTour) obj1);
//         final TVITourBookTour tourItem2 = ((TVITourBookTour) obj2);
//
//         int result = 0;
//
//         switch (__sortColumnId) {
//
//         case TableColumnFactory.TIME_DATE_ID:
//
//            // 1st Column: Date/time
//
//            result = tourItem1.colDateTime_MS > tourItem2.colDateTime_MS ? 1 : -1;
//            break;
//
//         /*
//          * BODY
//          */
//
//         case TableColumnFactory.BODY_AVG_PULSE_ID:
//            break;
//
//         case TableColumnFactory.BODY_CALORIES_ID:
//            break;
//
//         case TableColumnFactory.BODY_PULSE_MAX_ID:
//            break;
//
//         case TableColumnFactory.BODY_PERSON_ID:
//            break;
//
//         case TableColumnFactory.BODY_RESTPULSE_ID:
//            break;
//
//         case TableColumnFactory.BODY_WEIGHT_ID:
//            break;
//
//         /*
//          * DATA
//          */
//
//         case TableColumnFactory.DATA_DP_TOLERANCE_ID:
//            break;
//
//         case TableColumnFactory.DATA_IMPORT_FILE_NAME_ID:
//            break;
//
//         case TableColumnFactory.DATA_IMPORT_FILE_PATH_ID:
//            break;
//
//         case TableColumnFactory.DATA_NUM_TIME_SLICES_ID:
//            break;
//
//         case TableColumnFactory.DATA_TIME_INTERVAL_ID:
//            break;
//
//         /*
//          * DEVICE
//          */
//         case TableColumnFactory.DEVICE_DISTANCE_ID:
//            break;
//
//         case TableColumnFactory.DEVICE_NAME_ID:
//            break;
//
//         /*
//          * ELEVATION
//          */
//
//         case TableColumnFactory.ALTITUDE_AVG_CHANGE_ID:
//            break;
//
//         case TableColumnFactory.ALTITUDE_MAX_ID:
//            break;
//
//         case TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_DOWN_ID:
//            break;
//
//         case TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_UP_ID:
//            break;
//
//         /*
//          * MOTION
//          */
//
//         case TableColumnFactory.MOTION_AVG_PACE_ID:
//            break;
//
//         case TableColumnFactory.MOTION_AVG_SPEED_ID:
//            break;
//
//         case TableColumnFactory.MOTION_DISTANCE_ID:
//            break;
//
//         case TableColumnFactory.MOTION_MAX_SPEED_ID:
//            break;
//
//         /*
//          * POWER
//          */
//
//         case TableColumnFactory.POWER_AVG_ID:
//            break;
//
//         case TableColumnFactory.POWER_MAX_ID:
//            break;
//
//         case TableColumnFactory.POWER_NORMALIZED_ID:
//            break;
//
//         case TableColumnFactory.POWER_TOTAL_WORK_ID:
//            break;
//
//         /*
//          * POWERTRAIN
//          */
//
//         case TableColumnFactory.POWERTRAIN_AVG_CADENCE_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_CADENCE_MULTIPLIER_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_GEAR_FRONT_SHIFT_COUNT_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_GEAR_REAR_SHIFT_COUNT_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER_ID:
//            break;
//
//         /*
//          * RUNNING DYNAMICS
//          */
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_MAX_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MAX_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STEP_LENGTH_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STEP_LENGTH_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STEP_LENGTH_MAX_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MAX_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_RATIO_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MAX_ID:
//            break;
//
//         /*
//          * SURFING
//          */
//
//         case TableColumnFactory.SURFING_MIN_DISTANCE_ID:
//            break;
//
//         case TableColumnFactory.SURFING_MIN_SPEED_START_STOP_ID:
//            break;
//
//         case TableColumnFactory.SURFING_MIN_SPEED_SURFING_ID:
//            break;
//
//         case TableColumnFactory.SURFING_MIN_TIME_DURATION_ID:
//            break;
//
//         case TableColumnFactory.SURFING_NUMBER_OF_EVENTS_ID:
//            break;
//
//         /*
//          * TIME
//          */
//
//         case TableColumnFactory.TIME_DRIVING_TIME_ID:
//            break;
//
//         case TableColumnFactory.TIME_PAUSED_TIME_ID:
//            break;
//
//         case TableColumnFactory.TIME_PAUSED_TIME_RELATIVE_ID:
//            break;
//
//         case TableColumnFactory.TIME_RECORDING_TIME_ID:
//            break;
//
//         case TableColumnFactory.TIME_TIME_ZONE_ID:
//            break;
//
//         case TableColumnFactory.TIME_TIME_ZONE_DIFFERENCE_ID:
//            break;
//
//         case TableColumnFactory.TIME_TOUR_START_TIME_ID:
//            break;
//
//         case TableColumnFactory.TIME_WEEK_DAY_ID:
//            break;
//
//         case TableColumnFactory.TIME_WEEK_NO_ID:
//            break;
//
//         case TableColumnFactory.TIME_WEEKYEAR_ID:
//            break;
//
//         /*
//          * TOUR
//          */
//
//         case TableColumnFactory.TOUR_LOCATION_START_ID:
//            break;
//
//         case TableColumnFactory.TOUR_LOCATION_END_ID:
//            break;
//
//         case TableColumnFactory.TOUR_NUM_MARKERS_ID:
//            break;
//
//         case TableColumnFactory.TOUR_NUM_PHOTOS_ID:
//            break;
//
//         case TableColumnFactory.TOUR_TAGS_ID:
//            break;
//
//         case TableColumnFactory.TOUR_TITLE_ID:
//            break;
//
//         case TableColumnFactory.TOUR_TYPE_ID:
//            break;
//
//         case TableColumnFactory.TOUR_TYPE_TEXT_ID:
//            break;
//
//         /*
//          * TRAINING
//          */
//
//         case TableColumnFactory.TRAINING_EFFECT_AEROB_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_EFFECT_ANAEROB_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_FTP_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_INTENSITY_FACTOR_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_POWER_TO_WEIGHT_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_STRESS_SCORE_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_PERFORMANCE_LEVEL_ID:
//            break;
//
//         /*
//          * WEATHER
//          */
//
//         case TableColumnFactory.WEATHER_CLOUDS_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_TEMPERATURE_AVG_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_TEMPERATURE_MIN_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_TEMPERATURE_MAX_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_WIND_DIR_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_WIND_SPEED_ID:
//            break;
//
//         case TableColumnFactory.DATA_SEQUENCE_ID:
//         default:
//
//            result = tourItem1.col_Sequence > tourItem2.col_Sequence ? 1 : -1;
//
//            break;
//         }

//         if (__sortColumnId.equals(_columnId_1stColumn_Date)) {
//
//         } else if (__sortColumnId.equals(_columnId_Title)) {
//
//            // title
//
//            result = tourItem1.getTourTitle().compareTo(tourItem2.getTourTitle());
//
//         } else if (__sortColumnId.equals(_columnId_ImportFileName)) {
//
//            // file name
//
//            final String importFilePath1 = tourItem1.getImportFilePath();
//            final String importFilePath2 = tourItem2.getImportFilePath();
//
//            if (importFilePath1 != null && importFilePath2 != null) {
//
//               result = importFilePath1.compareTo(importFilePath2);
//            }
//
//         } else if (__sortColumnId.equals(_columnId_DeviceName)) {
//
//            // device name
//
//            result = tourItem1.getDeviceName().compareTo(tourItem2.getDeviceName());
//
//         } else if (__sortColumnId.equals(_columnId_TimeZone)) {
//
//            // time zone
//
//            final String timeZoneId1 = tourItem1.getTimeZoneId();
//            final String timeZoneId2 = tourItem2.getTimeZoneId();
//
//            if (timeZoneId1 != null && timeZoneId2 != null) {
//
//               final int zoneCompareResult = timeZoneId1.compareTo(timeZoneId2);
//
//               result = zoneCompareResult;
//
//            } else if (timeZoneId1 != null) {
//
//               result = 1;
//
//            } else if (timeZoneId2 != null) {
//
//               result = -1;
//            }
//         }
//
// do a 2nd sorting by date/time when not yet sorted
//         if (result == 0) {
//            result = tourItem1.colDateTime_MS > tourItem2.colDateTime_MS ? 1 : -1;
//         }
//
//         // if descending order, flip the direction
//         if (__sortDirection == DESCENDING) {
//            result = -result;
//         }
//
//         return result;
//      }

      /**
       * Does the sort. If it's a different column from the previous sort, do an ascending sort. If
       * it's the same column as the last sort, toggle the sort direction.
       *
       * @param widget
       *           Column widget
       */
//      private void setSortColumn(final Widget widget) {
//
//         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
//         final String columnId = columnDefinition.getColumnId();
//
//         if (columnId.equals(__sortColumnId)) {
//
//            // Same column as last sort; toggle the direction
//
//            __sortDirection = 1 - __sortDirection;
//
//         } else {
//
//            // New column; do an ascent sorting
//
//            __sortColumnId = columnId;
//            __sortDirection = ASCENDING;
//         }
//
//         updateUI_ShowSortDirection(__sortColumnId, __sortDirection);
//      }
   }

   private static class ItemComparer_Tree implements IElementComparer {

      @Override
      public boolean equals(final Object a, final Object b) {

         if (a == b) {
            return true;
         }

         if (a instanceof TVITourBookYear && b instanceof TVITourBookYear) {

            final TVITourBookYear item1 = (TVITourBookYear) a;
            final TVITourBookYear item2 = (TVITourBookYear) b;
            return item1.tourYear == item2.tourYear;
         }

         if (a instanceof TVITourBookYearCategorized && b instanceof TVITourBookYearCategorized) {

            final TVITourBookYearCategorized item1 = (TVITourBookYearCategorized) a;
            final TVITourBookYearCategorized item2 = (TVITourBookYearCategorized) b;
            return item1.tourYear == item2.tourYear && item1.tourYearSub == item2.tourYearSub;
         }

         if (a instanceof TVITourBookTour && b instanceof TVITourBookTour) {

            final TVITourBookTour item1 = (TVITourBookTour) a;
            final TVITourBookTour item2 = (TVITourBookTour) b;
            return item1.tourId == item2.tourId;
         }

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }
   }

//   /**
//    * [1] IConfiguration for registering a UI binding to open a menu
//    */
//   private class NatTable_Config_DebugMenu extends AbstractUiBindingConfiguration {
//
//      private final Menu debugMenu;
//
//      public NatTable_Config_DebugMenu(final NatTable natTable) {
//
//         // [2] create the menu using the PopupMenuBuilder
//         this.debugMenu = new PopupMenuBuilder(natTable)
//               .withInspectLabelsMenuItem()
//               .build();
//      }
//
//      @Override
//      public void configureUiBindings(final UiBindingRegistry uiBindingRegistry) {
//
//         // [3] bind the PopupMenuAction to a right click
//         // using GridRegion.COLUMN_HEADER instead of null would
//         // for example open the menu only on performing a right
//         // click on the column header instead of any region
//
//         uiBindingRegistry.registerMouseDownBinding(
//
//               new MouseEventMatcher(
//                     SWT.NONE,
//                     null,
//                     MouseEventMatcher.RIGHT_BUTTON),
//
//               new PopupMenuAction(this.debugMenu)
//
//         );
//      }
//
//   }

   private final class NatTable_ConfigField_TourType extends AbstractRegistryConfiguration {

      private IRowDataProvider<TVITourBookTour> _dataProvider;

      private NatTable_ConfigField_TourType(final IRowDataProvider<TVITourBookTour> body_DataProvider) {

         _dataProvider = body_DataProvider;
      }

      @Override
      public void configureRegistry(final IConfigRegistry configRegistry) {

         final ImagePainter decoratorCellPainter = new ImagePainter() {

            @Override
            protected Image getImage(final ILayerCell cell, final IConfigRegistry configRegistry) {

               // get the row object

               final int rowIndex = cell.getRowIndex();

               final TVITourBookTour tviTour = _dataProvider.getRowObject(rowIndex);

               if (tviTour == null) {
                  return null;
               }

               final long tourTypeId = tviTour.getTourTypeId();
               final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourTypeId);

               return tourTypeImage;
            }
         };

         // apply painter to the body cells and not to the header cells
         configRegistry.registerConfigAttribute(

               CellConfigAttributes.CELL_PAINTER,
               new CellPainterDecorator(null, CellEdgeEnum.LEFT, decoratorCellPainter),
               DisplayMode.NORMAL,
               TableColumnFactory.TOUR_TYPE_ID);
      }
   }

   private final class NatTable_ConfigField_Weather extends AbstractRegistryConfiguration {

      private IRowDataProvider<TVITourBookTour> _dataProvider;

      private NatTable_ConfigField_Weather(final IRowDataProvider<TVITourBookTour> body_DataProvider) {

         _dataProvider = body_DataProvider;
      }

      @Override
      public void configureRegistry(final IConfigRegistry configRegistry) {

         final ImagePainter decoratorCellPainter = new ImagePainter() {

            @Override
            protected Image getImage(final ILayerCell cell, final IConfigRegistry configRegistry) {

               // get the row object

               final int rowIndex = cell.getRowIndex();

               final TVITourBookTour tviTour = _dataProvider.getRowObject(rowIndex);

               if (tviTour == null) {
                  return null;
               }

               final String windClouds = tviTour.colClouds;

               if (windClouds == null) {
                  return null;
               } else {
                  final Image cellImage = net.tourbook.common.UI.IMAGE_REGISTRY.get(windClouds);
                  if (cellImage == null) {
                     return null;
                  } else {
                     return cellImage;
                  }
               }
            }
         };

         configRegistry.registerConfigAttribute(
               CellConfigAttributes.CELL_PAINTER,
               new CellPainterDecorator(null, CellEdgeEnum.LEFT, decoratorCellPainter),
               DisplayMode.NORMAL,
               TableColumnFactory.WEATHER_CLOUDS_ID);
      }
   }

   private class NatTable_Configuration_CellStyle extends AbstractRegistryConfiguration {

      private ArrayList<ColumnDefinition> _allSortedColumns;

      public NatTable_Configuration_CellStyle(final ArrayList<ColumnDefinition> allSortedColumns) {

         _allSortedColumns = allSortedColumns;
      }

      @Override
      public void configureRegistry(final IConfigRegistry configRegistry) {

         // loop: all displayed columns
         for (final ColumnDefinition colDef : _allSortedColumns) {

            if (!colDef.isColumnDisplayed()) {
               // visible columns are displayed first
               break;
            }

            final String columnId = colDef.getColumnId();

            switch (columnId) {

            case TableColumnFactory.TOUR_TYPE_ID:
            case TableColumnFactory.WEATHER_CLOUDS_ID:

               // images are displayed for these column
               break;

            default:

               Style style;

               final HorizontalAlignmentEnum columnAlignment = natTableConvert_ColumnAlignment(colDef.getColumnStyle());

               // body style
               style = new Style();
               style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, columnAlignment);

               configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                     style,
                     DisplayMode.NORMAL,
                     columnId);

               // header style
               style = style.clone();
               configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                     style,
                     DisplayMode.NORMAL,
                     columnId + HEADER_COLUMN_ID_POSTFIX);
               break;
            }
         }
      }

      /**
       * Convert col def style into nat table style
       *
       * @param columnStyle
       * @return
       */
      private HorizontalAlignmentEnum natTableConvert_ColumnAlignment(final int columnStyle) {

         switch (columnStyle) {

         case SWT.LEFT:
            return HorizontalAlignmentEnum.LEFT;

         case SWT.RIGHT:
            return HorizontalAlignmentEnum.RIGHT;

         default:
            return HorizontalAlignmentEnum.CENTER;
         }
      }
   }

   private final class NatTable_Configuration_Hover extends AbstractRegistryConfiguration {

      @Override
      public void configureRegistry(final IConfigRegistry configRegistry) {

         Style style;

         style = new Style();
         style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_YELLOW);

         configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, style, DisplayMode.HOVER);

         style = new Style();
         style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_RED);

         configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, style, DisplayMode.SELECT_HOVER);
      }
   }

   private class NatTable_Configuration_Theme extends ModernNatTableThemeConfiguration {

      public NatTable_Configuration_Theme() {

         super();

         /*
          * Overwrite default modern theme
          */

         // hide grid lines
         this.renderBodyGridLines = false;

         // show selection header with default colors
         this.cHeaderSelectionBgColor = cHeaderBgColor;
         this.cHeaderSelectionFgColor = cHeaderFgColor;

//         public static final Color COLOR_LIST_SELECTION = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
//         public static final Color COLOR_LIST_SELECTION_TEXT = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);

         // default selection style
//         this.defaultSelectionBgColor = GUIHelper.COLOR_LIST_SELECTION;
//         this.defaultSelectionFgColor = GUIHelper.COLOR_LIST_SELECTION_TEXT;
         this.defaultSelectionBgColor = GUIHelper.COLOR_BLACK;
         this.defaultSelectionFgColor = GUIHelper.COLOR_YELLOW;
      }
   }

//   private final class NatTable_DataLayer_Body extends DataLayer {
//
//      private NatTable_DataLayer_Body(final IDataProvider dataProvider) {
//         super(dataProvider);
//      }
//
//      private SizeConfig getColumnWidthConfig() {
//         return columnWidthConfig;
//      }
//   }

   void actionExportViewCSV() {

      // get selected items
      final ITreeSelection selection = (ITreeSelection) _tourViewer_Tree.getSelection();

      if (selection.size() == 0) {
         return;
      }

      final String defaultExportFilePath = _state.get(STATE_CSV_EXPORT_PATH);

      final String defaultExportFileName = CSV_EXPORT_DEFAULT_FILE_NAME
            + TimeTools.now().format(TimeTools.Formatter_FileName)
            + UI.SYMBOL_DOT
            + Util.CSV_FILE_EXTENSION;

      /*
       * get export filename
       */
      final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
      dialog.setText(Messages.dialog_export_file_dialog_text);

      dialog.setFilterPath(defaultExportFilePath);
      dialog.setFilterExtensions(new String[] { Util.CSV_FILE_EXTENSION });
      dialog.setFileName(defaultExportFileName);

      final String selectedFilePath = dialog.open();
      if (selectedFilePath == null) {
         return;
      }

      final File exportFilePath = new Path(selectedFilePath).toFile();

      // keep export path
      _state.put(STATE_CSV_EXPORT_PATH, exportFilePath.getPath());

      if (exportFilePath.exists()) {
         if (net.tourbook.ui.UI.confirmOverwrite(exportFilePath) == false) {
            // don't overwrite file, nothing more to do
            return;
         }
      }

      new CSVExport(selection, selectedFilePath, this);

//      // DEBUGGING: USING DEFAULT PATH
//      final IPath path = new Path(defaultExportFilePath).removeLastSegments(1).append(defaultExportFileName);
//
//      new CSVExport(selection, path.toOSString());
   }

   void actionSelectYearMonthTours() {

      if (_actionSelectAllTours.isChecked()) {

         // reselect selection
         _tourViewer_Tree.setSelection(_tourViewer_Tree.getSelection());
      }
   }

   void actionShowOnlySelectedTours() {
      // TODO Auto-generated method stub

   }

   /**
    * Toggle view layout, when the Ctrl-key is pressed, then the toggle action is reversed.
    * <p>
    * <code>
    * Forward:    month    -> week        -> natTable  -> month...<br>
    * Reverse:    month    -> natTable    -> week      -> month...
    * </code>
    *
    * @param event
    */
   void actionToggleViewLayout(final Event event) {

      final boolean isForwards = UI.isCtrlKey(event) == false;

      if (_viewLayout == TourBookViewLayout.CATEGORY_MONTH) {

         if (isForwards) {

            // month -> week

            toggleLayout_Category_Week();

         } else {

            // month -> natTable

            toggleLayout_NatTable();
         }

      } else if (_viewLayout == TourBookViewLayout.CATEGORY_WEEK) {

         if (isForwards) {

            // week -> natTable

            toggleLayout_NatTable();

         } else {

            // week -> month

            toggleLayout_Category_Month();
         }

      } else if (_viewLayout == TourBookViewLayout.NAT_TABLE) {

         if (isForwards) {

            // natTable -> month

            toggleLayout_Category_Month();

         } else {

            // natTable -> week

            toggleLayout_Category_Week();
         }
      }

      enableActions();

      reopenFirstSelectedTour();
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {
         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourBookView.this) {

               // ensure the tour tooltip is hidden, it occured that even closing this view did not close the tooltip
               if (_tourInfoToolTip_Tree != null) {
                  _tourInfoToolTip_Tree.hideToolTip();
               }
            }
         }

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               @SuppressWarnings("unchecked")
               final ArrayList<Long> backupTourIds = (ArrayList<Long>) _selectedTourIds.clone();

               reloadViewer();

               // reloadViewer() is reselecting by row position and not by tour id
               if (_isLayoutNatTable) {

                  _tourViewer_NatTable.getDisplay().timerExec(300, () -> {
                     reselectNatTableViewer(backupTourIds);
                  });
               }

            } else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

               _tourViewer_NatTable.refresh();

               // update tourbook viewer
               _tourViewer_Tree.refresh();

               // redraw must be done to see modified tour type image colors
               _tourViewer_Tree.getTree().redraw();

            } else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

               _columnFactory.updateToolTipState();

            } else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

               _columnManager_NatTable.saveState(_state_NatTable,
                     _natTable_Body_DataLayer,
                     _natTable_Body_ColumnReorderLayer,
                     _natTable_Body_ColumnHideShowLayer);
               _columnManager_NatTable.clearColumns();

               _columnManager_Tree.saveState(_state_Tree);
               _columnManager_Tree.clearColumns();

               _columnFactory.defineAllColumns();

               recreateViewer_NatTable();
               _tourViewer_Tree = (TreeViewer) recreateViewer_Tree();

            } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

               _tourViewer_Tree.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

               _tourViewer_Tree.refresh();

               /*
                * the tree must be redrawn because the styled text does not show with the new color
                */
               _tourViewer_Tree.getTree().redraw();
            }
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);

      /*
       * Common preferences
       */
      _prefChangeListenerCommon = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.TIME_ZONE_LOCAL_ID)) {

               recreateViewer_NatTable();
               _tourViewer_Tree = (TreeViewer) recreateViewer_Tree();
            }
         }
      };

      // register the listener
      _prefStoreCommon.addPropertyChangeListener(_prefChangeListenerCommon);
   }

   private void addSelectionListener() {

      // this view part is a selection listener
      _postSelectionListener = new ISelectionListener() {

         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            // prevent to listen to a selection which is originated by this year chart
            if (part == TourBookView.this) {
               return;
            }

            onSelectionChanged(selection);
         }
      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourPropertyListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == TourBookView.this) {
               return;
            }

            if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {

               /*
                * it is possible when a tour type was modified, the tour can be hidden or visible in
                * the viewer because of the tour type filter
                */
               reloadViewer();

            } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED
                  || eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

               reloadViewer();
            }
         }
      };
      TourManager.getInstance().addTourEventListener(_tourPropertyListener);
   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {

      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

      _subMenu_AdjustTourValues = new SubMenu_AdjustTourValues(this, this);
      _subMenu_Reimport = new Action_Reimport_SubMenu(this);

      _actionCollapseAll = new ActionCollapseAll(this);
      _actionCollapseOthers = new ActionCollapseOthers(this);
      _actionDuplicateTour = new ActionDuplicateTour(this);
      _actionDeleteTour = new ActionDeleteTourMenu(this);
      _actionEditQuick = new ActionEditQuick(this);
      _actionEditTour = new ActionEditTour(this);
      _actionExpandSelection = new ActionExpandSelection(this);
      _actionExportTour = new ActionExport(this);
      _actionExportViewCSV = new ActionExportViewCSV(this);
      _actionJoinTours = new ActionJoinTours(this);
      _actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
      _actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
      _actionMergeTour = new ActionMergeTour(this);
      _actionModifyColumns = new ActionModifyColumns(this);
      _actionOpenTour = new ActionOpenTour(this);
      _actionPrintTour = new ActionPrint(this);
      _actionRefreshView = new ActionRefreshView(this);
      _actionSetOtherPerson = new ActionSetPerson(this);
      _actionSetTourType = new ActionSetTourTypeMenu(this);
      _actionSelectAllTours = new ActionSelectAllTours(this);
      _actionShowOnlySelectedTours = new ActionShowOnlySelectedTours(this);
      _actionToggleViewLayout = new ActionToggleViewLayout(this);
      _actionTourBookOptions = new ActionTourBookOptions();

      _actionLinkWithOtherViews = new ActionLinkWithOtherViews();

      fillActionBars();
   }

   private void createMenuManager() {

      _tagMenuManager = new TagMenuManager(this, true);

      _viewerMenuManager_NatTable = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager_NatTable.setRemoveAllWhenShown(true);
      _viewerMenuManager_NatTable.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {

//            _tourInfoToolTip_NatTable.hideToolTip();

            fillContextMenu(manager, false);
         }
      });

      _viewerMenuManager_Table = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager_Table.setRemoveAllWhenShown(true);
      _viewerMenuManager_Table.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {

//            _tourInfoToolTip_Table.hideToolTip();

            fillContextMenu(manager, false);
         }
      });

      _viewerMenuManager_Tree = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager_Tree.setRemoveAllWhenShown(true);
      _viewerMenuManager_Tree.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {

            _tourInfoToolTip_Tree.hideToolTip();

            fillContextMenu(manager, true);
         }
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      initUI(parent);
      restoreState_BeforeUI();

      createMenuManager();

      // define all columns for the viewer
      _columnManager_NatTable = new ColumnManager(this, _state_NatTable);
      _columnManager_NatTable.setIsCategoryAvailable(true);

      _columnManager_Tree = new ColumnManager(this, _state_Tree);
      _columnManager_Tree.setIsCategoryAvailable(true);

      _columnFactory = new ColumnFactory(_columnManager_NatTable, _columnManager_Tree, _pc);
      _columnFactory.defineAllColumns();

      createUI(parent);
      createActions();

      addSelectionListener();
      addPartListener();
      addPrefListener();
      addTourEventListener();

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      restoreState();

      enableActions();

      // update the viewer

      // delay loading, that the app filters are initialized
      Display.getCurrent().asyncExec(() -> {

         if (_tourViewer_Tree.getTree().isDisposed()) {
            return;
         }

         _isInStartup = true;

         setupTourViewerContent();

         reselectTourViewer();

         restoreState_AfterUI();
      });
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _viewerContainer_NatTable = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer_NatTable);
      {
         createUI_20_TourViewer_NatTable(_viewerContainer_NatTable);
      }

      _viewerContainer_Tree = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer_Tree);
      {
         createUI_30_TourViewer_Tree(_viewerContainer_Tree);
      }
   }

   private void createUI_20_TourViewer_NatTable(final Composite parent) {

      // this MUST be done after the nattable is created
      _columnManager_NatTable.setupNatTable(this);

      // data provider
      _natTable_DataLoader = new NatTable_DataLoader(this, _columnManager_NatTable);
      final ArrayList<ColumnDefinition> allSortedColumns = _natTable_DataLoader.allSortedColumns;

      final String sortColumnId = _tourViewer_Table_Comparator.__sortColumnId;
      final int sortDirection = _tourViewer_Table_Comparator.__sortDirection;

      _natTable_DataLoader.setSortColumn(sortColumnId, sortDirection);

      // body layer
      final IRowDataProvider<TVITourBookTour> body_DataProvider = new TourRowDataProvider(_natTable_DataLoader);
      _natTable_Body_DataLayer = new DataLayer(body_DataProvider);

      // hover layer
      _natTable_Body_HoverLayer = new HoverLayer(_natTable_Body_DataLayer);

      // column drag&drop layer
      _natTable_Body_ColumnReorderLayer = new ColumnReorderLayer(_natTable_Body_HoverLayer);

      // show/hide columns
      _natTable_Body_ColumnHideShowLayer = new ColumnHideShowLayer(_natTable_Body_ColumnReorderLayer);

      // selection layer
      _natTable_Body_SelectionLayer = new SelectionLayer(_natTable_Body_ColumnHideShowLayer, false);

      // register the DefaultRowSelectionLayerConfiguration that contains the
      // default styling and functionality bindings (search, tick update)
      // and different configurations for a move command handler that always
      // moves by a row and row only selection bindings
      _natTable_Body_SelectionLayer.addConfiguration(new DefaultRowSelectionLayerConfiguration());

      // use a RowSelectionModel that will perform row selections and is able to identify a row via unique ID
      final IRowIdAccessor<TVITourBookTour> rowIdAccessor = new IRowIdAccessor<TVITourBookTour>() {

         @Override
         public Serializable getRowId(final TVITourBookTour rowObject) {
            return rowObject.tourId;
         }

      };
      _natTable_Body_SelectionLayer.setSelectionModel(new RowSelectionModel<>(
            _natTable_Body_SelectionLayer,
            body_DataProvider,
            rowIdAccessor));

      // body viewport
      _natTable_Body_ViewportLayer = new ViewportLayer(_natTable_Body_SelectionLayer);
      _natTable_Body_ViewportLayer.addConfiguration(new NatTable_ConfigField_TourType(body_DataProvider));
      _natTable_Body_ViewportLayer.addConfiguration(new NatTable_ConfigField_Weather(body_DataProvider));

      /*
       * Create: Column header layer
       */
      final IDataProvider columnHeader_DataProvider = new DataProvider_ColumnHeader(_natTable_DataLoader, _columnManager_NatTable);
      _natTable_ColumnHeader_DataLayer = new DataLayer(columnHeader_DataProvider);
      _natTable_ColumnHeader_Layer = new ColumnHeaderLayer(
            _natTable_ColumnHeader_DataLayer,
            _natTable_Body_ViewportLayer,
            _natTable_Body_SelectionLayer);

      /*
       * Create: Row header layer
       */
      final DefaultRowHeaderDataProvider rowHeader_DataProvider = new DefaultRowHeaderDataProvider(body_DataProvider);
      final DefaultRowHeaderDataLayer rowHeader_DataLayer = new DefaultRowHeaderDataLayer(rowHeader_DataProvider);
      final ILayer rowHeader_Layer = new RowHeaderLayer(rowHeader_DataLayer, _natTable_Body_ViewportLayer, _natTable_Body_SelectionLayer);

      /*
       * Create: Corner layer
       */
      final DefaultCornerDataProvider corner_DataProvider = new DefaultCornerDataProvider(columnHeader_DataProvider, rowHeader_DataProvider);
      final DataLayer corner_DataLayer = new DataLayer(corner_DataProvider);
      final ILayer corner_Layer = new CornerLayer(corner_DataLayer, rowHeader_Layer, _natTable_ColumnHeader_Layer);

      /*
       * Create: Grid layer composed with the prior created layer stacks
       */
      final GridLayer gridLayer = new GridLayer(_natTable_Body_ViewportLayer, _natTable_ColumnHeader_Layer, rowHeader_Layer, corner_Layer);

      /*
       * Setup other data
       */
      natTable_SetColumnWidths(allSortedColumns, _natTable_Body_DataLayer);
      natTable_RegisterColumnLabels(allSortedColumns, _natTable_Body_DataLayer, _natTable_ColumnHeader_DataLayer);

      /*
       * Create: Table
       */
      // turn the auto configuration off as we want to add our hover styling configuration
      _tourViewer_NatTable = new NatTable(parent, gridLayer, false);

      _columnManager_NatTable.setupNatTable_PostCreate();

      // add mouse double click listener
      final IMouseAction mouseDoubleClickAction = new IMouseAction() {

         @Override
         public void run(final NatTable natTable, final MouseEvent event) {
            TourManager.getInstance().tourDoubleClickAction(TourBookView.this, _tourDoubleClickState);
         }
      };
      _tourViewer_NatTable.getUiBindingRegistry().registerDoubleClickBinding(MouseEventMatcher.bodyLeftClick(SWT.NONE), mouseDoubleClickAction);

      // setup selection listener for the nattable
      final ISelectionProvider selectionProvider = new RowSelectionProvider<>(
            _natTable_Body_SelectionLayer,
            body_DataProvider,
            false); // Provides rows where any cell in the row is selected

      selectionProvider.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelect_NatTableItem(event);
         }
      });

      /*
       * Do NatTable configuration
       */

      // as the autoconfiguration of the NatTable is turned off, we have to add the DefaultNatTableStyleConfiguration manually
      _tourViewer_NatTable.addConfiguration(new DefaultNatTableStyleConfiguration());

      _tourViewer_NatTable.addConfiguration(new NatTable_Configuration_CellStyle(_natTable_DataLoader.allSortedColumns));

      // add the style configuration for hover
      _tourViewer_NatTable.addConfiguration(new NatTable_Configuration_Hover());

      // [4] add the menu configuration to a NatTable instance
//      _tourViewer_NatTable.addConfiguration(new NatTable_Config_DebugMenu(_tourViewer_NatTable));

      _tourViewer_NatTable.configure();

      // overwrite theme with MT's own theme based on the modern theme
      _tourViewer_NatTable.setTheme(new NatTable_Configuration_Theme());

      GridDataFactory.fillDefaults().grab(true, true).applyTo(_tourViewer_NatTable);

      // set header tooltip
      _natTable_Tooltip = new NatTable_Header_Tooltip(_tourViewer_NatTable, this);
      _natTable_Tooltip.setPopupDelay(0);

      createUI_50_ContextMenu_NatTable();
   }

   private void createUI_30_TourViewer_Tree(final Composite parent) {

      // must be called before the columns are created
      updateUI_TourViewerColumns_Tree();

      // tour tree
      final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

      tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      tree.setHeaderVisible(true);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _tourViewer_Tree = new TreeViewer(tree);
      _columnManager_Tree.createColumns(_tourViewer_Tree);

      _tourViewer_Tree.setComparer(new ItemComparer_Tree());
      _tourViewer_Tree.setUseHashlookup(true);

      _tourViewer_Tree.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelect_TreeItem(event);
         }
      });

      _tourViewer_Tree.addDoubleClickListener(new IDoubleClickListener() {

         @Override
         public void doubleClick(final DoubleClickEvent event) {

            final Object selection = ((IStructuredSelection) _tourViewer_Tree.getSelection()).getFirstElement();

            if (selection instanceof TVITourBookTour) {

               TourManager.getInstance().tourDoubleClickAction(TourBookView.this, _tourDoubleClickState);

            } else if (selection != null) {

               // expand/collapse current item

               final TreeViewerItem tourItem = (TreeViewerItem) selection;

               if (_tourViewer_Tree.getExpandedState(tourItem)) {
                  _tourViewer_Tree.collapseToLevel(tourItem, 1);
               } else {
                  _tourViewer_Tree.expandToLevel(tourItem, 1);
               }
            }
         }
      });

      /*
       * The context menu must be created after the viewer is created which is also done after the
       * measurement system has changed
       */
      createUI_60_ContextMenu_Tree();

      // set tour info tooltip provider
      _tourInfoToolTip_Tree = new TreeViewerTourInfoToolTip(_tourViewer_Tree);
   }

   /**
    * Setup context menu for the nattable
    */
   private void createUI_50_ContextMenu_NatTable() {

      _contextMenu_NatTable = createUI_52_ViewerContextMenu_NatTable();

      _columnManager_NatTable.createHeaderContextMenu(
            _tourViewer_NatTable,
            _viewerContextMenuProvider_NatTable,
            _natTable_ColumnHeader_Layer);
   }

   /**
    * Creates context menu for the viewer
    *
    * @return Returns the {@link Menu} widget
    */
   private Menu createUI_52_ViewerContextMenu_NatTable() {

      final Menu contextMenu = _viewerMenuManager_NatTable.createContextMenu(_tourViewer_NatTable);

      contextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
            _tagMenuManager.onShowMenu(menuEvent, _tourViewer_NatTable, Display.getCurrent().getCursorLocation(), null);
         }
      });

      return contextMenu;
   }

   /**
    * Setup context menu for the viewer
    */
   private void createUI_60_ContextMenu_Tree() {

      _contextMenu_Tree = createUI_62_ViewerContextMenu_Tree();

      final Tree tree = (Tree) _tourViewer_Tree.getControl();

      _columnManager_Tree.createHeaderContextMenu(tree, _viewerContextMenuProvider_Tree);
   }

   /**
    * Creates context menu for the viewer
    *
    * @return Returns the {@link Menu} widget
    */
   private Menu createUI_62_ViewerContextMenu_Tree() {

      final Tree tree = (Tree) _tourViewer_Tree.getControl();

      final Menu contextMenu = _viewerMenuManager_Tree.createContextMenu(tree);

      contextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
            _tagMenuManager.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip_Tree);
         }
      });

      return contextMenu;
   }

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);
      TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStoreCommon.removePropertyChangeListener(_prefChangeListenerCommon);

      if (_natTable_DataLoader != null) {
         _natTable_DataLoader.resetTourItems();
         _natTable_DataLoader = null;
      }
      if (_rootItem_Tree != null) {
         _rootItem_Tree.clearChildren();
         _rootItem_Tree = null;
      }

      super.dispose();
   }

   private void enableActions() {

      int numTourItems = 0;
      int numSelectedItems = 0;

      boolean firstElementHasChildren = false;

      TVITourBookItem firstElement = null;
      TVITourBookTour firstTour = null;

      _hoveredTourId = -1;

      if (_isLayoutNatTable) {

         final RowSelectionModel<TVITourBookTour> rowSelectionModel = getNatTable_SelectionModel();

         boolean isHoveredInSelection = false;
         int hoveredRow = -1;

         final Point hoveredPosition = _natTable_Body_HoverLayer.getCurrentHoveredCellPosition();
         if (hoveredPosition != null) {

            hoveredRow = hoveredPosition.y;

            final Set<Range> allSelectedRowPositions = rowSelectionModel.getSelectedRowPositions();
            for (final Range range : allSelectedRowPositions) {
               if (range.contains(hoveredRow)) {
                  isHoveredInSelection = true;
                  break;
               }
            }
         }

         if (isHoveredInSelection) {

            // mouse is hovering the selected tours

            numTourItems = numSelectedItems = rowSelectionModel.getSelectedRowCount();

            final List<TVITourBookTour> selection = rowSelectionModel.getSelectedRowObjects();

            if (selection.isEmpty() == false) {
               firstTour = selection.get(0);
            }

         } else {

            // mouse is not hovering a tour selection

            final TVITourBookTour fetchedTour = _natTable_DataLoader.getFetchedTour(hoveredRow);
            if (fetchedTour != null) {

               numTourItems = numSelectedItems = 1;
               firstTour = fetchedTour;

               _hoveredTourId = fetchedTour.tourId;
            }
         }

      } else {

         final ITreeSelection selection = (ITreeSelection) _tourViewer_Tree.getSelection();

         /*
          * count number of selected items
          */

         for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

            final Object treeItem = iter.next();
            if (treeItem instanceof TVITourBookTour) {
               if (numTourItems == 0) {
                  firstTour = (TVITourBookTour) treeItem;
               }
               numTourItems++;
            }
         }

         firstElement = (TVITourBookItem) selection.getFirstElement();
         firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();
         numSelectedItems = selection.size();
      }

      final boolean isTourSelected = numTourItems > 0;
      final boolean isOneTour = numTourItems == 1;
      final boolean isAllToursSelected = _actionSelectAllTours.isChecked();
      boolean isDeviceTour = false;
      boolean canMergeTours = false;

      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

      TourData firstSavedTour = null;

      if (isOneTour) {
         firstSavedTour = TourManager.getInstance().getTourData(firstTour.getTourId());
      }

      if (firstSavedTour != null) {

         isDeviceTour = firstSavedTour.isManualTour() == false;
         canMergeTours = isOneTour && isDeviceTour && firstSavedTour.getMergeSourceTourId() != null;
      }

      final boolean useWeatherRetrieval = _prefStore.getBoolean(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL) &&
            !_prefStore.getString(ITourbookPreferences.WEATHER_API_KEY).equals(UI.EMPTY_STRING);

      final boolean isTableLayout = _isLayoutNatTable;
      final boolean isTreeLayout = !isTableLayout;

      // set double click infos
      _tourDoubleClickState.canEditTour = isOneTour;
      _tourDoubleClickState.canOpenTour = isOneTour;
      _tourDoubleClickState.canQuickEditTour = isOneTour;
      _tourDoubleClickState.canEditMarker = isOneTour;
      _tourDoubleClickState.canAdjustAltitude = isOneTour;

      /*
       * enable actions
       */
      _subMenu_AdjustTourValues.setEnabled(isTourSelected || isAllToursSelected);
      _subMenu_AdjustTourValues.getActionRetrieveWeatherData().setEnabled(useWeatherRetrieval);

      _subMenu_Reimport.setEnabled(isTourSelected);

      _actionDeleteTour.setEnabled(isTourSelected);
      _actionDuplicateTour.setEnabled(isOneTour && !isDeviceTour);
      _actionEditQuick.setEnabled(isOneTour);
      _actionEditTour.setEnabled(isOneTour);
      _actionExportTour.setEnabled(isTourSelected);
      _actionExportViewCSV.setEnabled(numSelectedItems > 0);
      _actionJoinTours.setEnabled(numTourItems > 1);
      _actionMergeTour.setEnabled(canMergeTours);
      _actionOpenAdjustAltitudeDialog.setEnabled(isOneTour && isDeviceTour);
      _actionOpenMarkerDialog.setEnabled(isOneTour && isDeviceTour);
      _actionOpenTour.setEnabled(isOneTour);
      _actionPrintTour.setEnabled(isTourSelected);
      _actionSetOtherPerson.setEnabled(isTourSelected);
      _actionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);
      _actionShowOnlySelectedTours.setEnabled(isTableLayout);

      _actionCollapseOthers.setEnabled(numSelectedItems == 1 && firstElementHasChildren);
      _actionExpandSelection.setEnabled(
            firstElement == null
                  ? false
                  : numSelectedItems == 1
                        ? firstElementHasChildren
                        : true);

      _actionSelectAllTours.setEnabled(isTreeLayout);
      _actionToggleViewLayout.setEnabled(true);

      _tagMenuManager.enableTagActions(isTourSelected, isOneTour, firstTour == null ? null : firstTour.getTagIds());

      TourTypeMenuManager.enableRecentTourTypeActions(
            isTourSelected,
            isOneTour
                  ? firstTour.getTourTypeId()
                  : TourDatabase.ENTITY_IS_NOT_SAVED);
   }

   private void fillActionBars() {

      /*
       * fill view menu
       */
      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

      menuMgr.add(_actionRefreshView);
      menuMgr.add(new Separator());
      menuMgr.add(_actionModifyColumns);

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionSelectAllTours);
      tbm.add(_actionToggleViewLayout);
      tbm.add(_actionShowOnlySelectedTours);

      tbm.add(new Separator());
      tbm.add(_actionExpandSelection);
      tbm.add(_actionCollapseAll);
      tbm.add(_actionLinkWithOtherViews);
      tbm.add(_actionTourBookOptions);

      // update that actions are fully created otherwise action enable will fail
      tbm.update(true);
   }

   /**
    * @param menuMgr
    * @param isTree
    *           When <code>true</code> then tree actions are also displayed.
    */
   private void fillContextMenu(final IMenuManager menuMgr, final boolean isTree) {

      menuMgr.add(_actionEditQuick);
      menuMgr.add(_actionEditTour);
      menuMgr.add(_actionOpenMarkerDialog);
      menuMgr.add(_actionOpenAdjustAltitudeDialog);
      menuMgr.add(_actionOpenTour);
      menuMgr.add(_actionDuplicateTour);
      menuMgr.add(_actionMergeTour);
      menuMgr.add(_actionJoinTours);

      _tagMenuManager.fillTagMenu(menuMgr, true);

      // tour type actions
      menuMgr.add(new Separator());
      menuMgr.add(_actionSetTourType);
      TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

      // add tree only items
      if (isTree) {

         menuMgr.add(new Separator());
         menuMgr.add(_actionCollapseOthers);
         menuMgr.add(_actionExpandSelection);
         menuMgr.add(_actionCollapseAll);
      }

      menuMgr.add(new Separator());
      menuMgr.add(_actionExportTour);
      menuMgr.add(_actionExportViewCSV);
      menuMgr.add(_actionPrintTour);

      menuMgr.add(new Separator());
      menuMgr.add(_subMenu_AdjustTourValues);
      menuMgr.add(_subMenu_Reimport);
      menuMgr.add(_actionSetOtherPerson);
      menuMgr.add(_actionDeleteTour);

      enableActions();
   }

   /**
    * Returns the {@link ColumnManager} of the currently selected nattable/table/tree
    */
   @Override
   public ColumnManager getColumnManager() {

      if (_isLayoutNatTable) {

         return _columnManager_NatTable;

      } else {

         return _columnManager_Tree;
      }
   }

   @Override
   public NatTable getNatTable() {
      return _tourViewer_NatTable;
   }

   @Override
   public ColumnHideShowLayer getNatTable_Body_ColumnHideShowLayer() {
      return _natTable_Body_ColumnHideShowLayer;
   }

   @Override
   public ColumnReorderLayer getNatTable_Body_ColumnReorderLayer() {
      return _natTable_Body_ColumnReorderLayer;
   }

   @Override
   public DataLayer getNatTable_Body_DataLayer() {
      return _natTable_Body_DataLayer;
   }

   /**
    * @param event
    * @return Returns the {@link ColumnDefinition} of the currently selected row or
    *         <code>null</code> when nothing is selected.
    */
   public ColumnDefinition getNatTable_SelectedColumnDefinition(final Event event) {

      final NatTable natTable = _tourViewer_NatTable;

      final int colPos = natTable.getColumnPositionByX(event.x);
      final int rowPos = natTable.getRowPositionByY(event.y);

      final ILayerCell cell = natTable.getCellByPosition(colPos, rowPos);
      if (cell != null) {

         final int colIndexByPos = natTable.getColumnIndexByPosition(colPos);
         if (colIndexByPos == -1) {

            // a column is not hit
            return null;
         }

         return _columnManager_NatTable.getActiveProfile().getVisibleColumnDefinitions().get(colIndexByPos);
      }

      return null;
   }

   @SuppressWarnings("unchecked")
   private RowSelectionModel<TVITourBookTour> getNatTable_SelectionModel() {

      return (RowSelectionModel<TVITourBookTour>) _natTable_Body_SelectionLayer.getSelectionModel();
   }

   @Override
   public PostSelectionProvider getPostSelectionProvider() {
      return _postSelectionProvider;
   }

   @Override
   public Set<Long> getSelectedTourIDs() {

      final Set<Long> tourIds = new HashSet<>();

      IStructuredSelection selectedTours;

      if (_isLayoutNatTable) {

         final RowSelectionModel<TVITourBookTour> rowSelectionModel = getNatTable_SelectionModel();

         final List<TVITourBookTour> selectedTVITours = rowSelectionModel.getSelectedRowObjects();

         for (final TVITourBookTour tviTourBookTour : selectedTVITours) {
            tourIds.add(tviTourBookTour.tourId);
         }

         if (tourIds.size() == 0 && _hoveredTourId != -1) {

            // when nothing is selected but mouse is hovering a tour, return this tour id

            tourIds.add(_hoveredTourId);
         }

         return tourIds;

      } else {

         selectedTours = _tourViewer_Tree.getStructuredSelection();
      }

      for (final Iterator<?> tourIterator = selectedTours.iterator(); tourIterator.hasNext();) {

         final Object viewItem = tourIterator.next();

         if (viewItem instanceof TVITourBookYear) {

            // one year is selected

            if (_actionSelectAllTours.isChecked()) {

               // loop: all months
               for (final TreeViewerItem viewerItem : ((TVITourBookYear) viewItem).getFetchedChildren()) {
                  if (viewerItem instanceof TVITourBookYearCategorized) {
                     getYearSubTourIDs((TVITourBookYearCategorized) viewerItem, tourIds);
                  }
               }
            }

         } else if (viewItem instanceof TVITourBookYearCategorized) {

            // one month/week is selected

            if (_actionSelectAllTours.isChecked()) {
               getYearSubTourIDs((TVITourBookYearCategorized) viewItem, tourIds);
            }

         } else if (viewItem instanceof TVITourBookTour) {

            // one tour is selected

            tourIds.add(((TVITourBookTour) viewItem).getTourId());
         }
      }

      return tourIds;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      // get selected tour id's
      final Set<Long> tourIds = getSelectedTourIDs();

      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      BusyIndicator.showWhile(Display.getCurrent(), () -> {
         TourManager.loadTourData(new ArrayList<>(tourIds), selectedTourData, false);
      });

      return selectedTourData;
   }

   IDialogSettings getState() {
      return _state;
   }

   /**
    * @return the {@link #_tourViewer_NatTable}
    */
   public NatTable getTourViewer_NatTable() {
      return _tourViewer_NatTable;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tourViewer_Tree;
   }

   @Override
   public ColumnViewer getViewer() {

      if (_isLayoutNatTable) {

         return null;

      } else {

         return _tourViewer_Tree;
      }
   }

   /**
    * @return Returns the layout of the view
    */
   public TourBookViewLayout getViewLayout() {
      return _viewLayout;
   }

   /**
    * @param yearSubItem
    * @param tourIds
    * @return Return all tours for one yearSubItem
    */
   private void getYearSubTourIDs(final TVITourBookYearCategorized yearSubItem, final Set<Long> tourIds) {

      // get all tours for the month item
      for (final TreeViewerItem viewerItem : yearSubItem.getFetchedChildren()) {
         if (viewerItem instanceof TVITourBookTour) {

            final TVITourBookTour tourItem = (TVITourBookTour) viewerItem;
            tourIds.add(tourItem.getTourId());
         }
      }
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   boolean isShowSummaryRow() {

      return Util.getStateBoolean(_state, TourBookView.STATE_IS_SHOW_SUMMARY_ROW, TourBookView.STATE_IS_SHOW_SUMMARY_ROW_DEFAULT);
   }

   /**
    * Register column labels for the body and header -> this is necessary to apply styling, images,
    * ...
    *
    * @param allSortedColumns
    * @param body_DataLayer
    * @param columnHeader_DataLayer
    */
   private void natTable_RegisterColumnLabels(final ArrayList<ColumnDefinition> allSortedColumns,
                                              final DataLayer body_DataLayer,
                                              final DataLayer columnHeader_DataLayer) {

      final ColumnOverrideLabelAccumulator body_ColumnLabelAccumulator = new ColumnOverrideLabelAccumulator(body_DataLayer);
      final ColumnOverrideLabelAccumulator columnHeader_ColumnLabelAccumulator = new ColumnOverrideLabelAccumulator(columnHeader_DataLayer);

      body_DataLayer.setConfigLabelAccumulator(body_ColumnLabelAccumulator);
      columnHeader_DataLayer.setConfigLabelAccumulator(columnHeader_ColumnLabelAccumulator);

      for (int colIndex = 0; colIndex < allSortedColumns.size(); colIndex++) {

         final ColumnDefinition colDef = allSortedColumns.get(colIndex);

         if (!colDef.isColumnDisplayed()) {
            // ignore hidden colums
            return;
         }

         final String columnId = colDef.getColumnId();

         columnHeader_ColumnLabelAccumulator.registerColumnOverrides(colIndex, columnId + HEADER_COLUMN_ID_POSTFIX);

         body_ColumnLabelAccumulator.registerColumnOverrides(colIndex, columnId);
      }
   }

   /**
    * Select tours (rows) in the NatTable by it's row positions, the selection is delayed that tours
    * are loaded ahead.
    *
    * @param allRowPositions
    */
   private void natTable_SelectTours(final int[] allRowPositions) {

      if (allRowPositions == null || allRowPositions.length == 0 || allRowPositions[0] == -1) {
         return;
      }

      Display.getDefault().asyncExec(() -> {

         /*
          * timerExec() MUST be run in the UI thread, without asyncExec() it is NOT in the UI thread
          */

         Display.getDefault().timerExec(500, () -> {

            /*
             * It took me hours to solve this issue, first deselect the old selection otherwise is
             * was PRESERVED :-(((
             */
            _natTable_Body_SelectionLayer.clear(false);

            final SelectRowsCommand command = new SelectRowsCommand(
                  _natTable_Body_SelectionLayer,
                  0,
                  allRowPositions,
                  false,
                  true,
//                allRowPositions[allRowPositions.length - 1] //
                  allRowPositions[0] //
//                  -1 // do not scroll into view
            );

            _natTable_Body_SelectionLayer.doCommand(command);
         });
      });
   }

   /**
    * @param allSortedColumns
    * @param body_DataLayer
    */
   private void natTable_SetColumnWidths(final ArrayList<ColumnDefinition> allSortedColumns, final DataLayer body_DataLayer) {

      // set column widths
      for (int colIndex = 0; colIndex < allSortedColumns.size(); colIndex++) {

         final ColumnDefinition colDef = allSortedColumns.get(colIndex);

         if (!colDef.isColumnDisplayed()) {
            // visible columns are displayed first
            continue;
//            break;
         }

         body_DataLayer.setColumnWidthByPosition(colIndex, colDef.getColumnWidth(), false);
      }
   }

   private void onSelect_CreateTourSelection(final HashSet<Long> tourIds) {

      ISelection selection;
      if (tourIds.size() == 0) {

         // fire selection that nothing is selected

         selection = new SelectionTourIds(new ArrayList<Long>());

      } else {

         // keep selected tour id's
         _selectedTourIds.clear();
         _selectedTourIds.addAll(tourIds);

         selection = tourIds.size() == 1 //
               ? new SelectionTourId(_selectedTourIds.get(0))
               : new SelectionTourIds(_selectedTourIds);

      }

      _isInFireSelection = true;
      {
         // _postSelectionProvider should be removed when all parts are listening to the TourManager event
         if (_isInStartup) {

            _isInStartup = false;

            // this view can be inactive -> selection is not fired with the SelectionProvider interface

            TourManager.fireEventWithCustomData(TourEventId.TOUR_SELECTION, selection, this);

         } else {

            _postSelectionProvider.setSelection(selection);
         }
      }
      _isInFireSelection = false;

      enableActions();
   }

   private void onSelect_NatTableItem(final SelectionChangedEvent event) {

      if (_isInReload) {
         return;
      }

      final HashSet<Long> tourIds = new HashSet<>();
      final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

      for (final Object selectedItem : selection) {
         tourIds.add(((TVITourBookTour) selectedItem).tourId);
      }

      onSelect_CreateTourSelection(tourIds);
   }

   private void onSelect_TreeItem(final SelectionChangedEvent event) {

      if (_isInReload) {
         return;
      }

      final boolean isSelectAllChildren = _actionSelectAllTours.isChecked();

      final HashSet<Long> tourIds = new HashSet<>();

      boolean isFirstYear = true;
      boolean isFirstYearSub = true;
      boolean isFirstTour = true;

      final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());

      // loop: all selected items
      for (final Iterator<?> itemIterator = selectedTours.iterator(); itemIterator.hasNext();) {

         final Object treeItem = itemIterator.next();

         if (isSelectAllChildren) {

            // get ALL tours from all selected tree items (year/month/tour)

            if (treeItem instanceof TVITourBookYear) {

               // year is selected

               final TVITourBookYear yearItem = ((TVITourBookYear) treeItem);
               if (isFirstYear) {
                  // keep selected year
                  isFirstYear = false;
                  _selectedYear = yearItem.tourYear;
               }

               // get all tours for the selected year
               for (final TreeViewerItem viewerItem : yearItem.getFetchedChildren()) {
                  if (viewerItem instanceof TVITourBookYearCategorized) {
                     getYearSubTourIDs((TVITourBookYearCategorized) viewerItem, tourIds);
                  }
               }

            } else if (treeItem instanceof TVITourBookYearCategorized) {

               // month/week/day is selected

               final TVITourBookYearCategorized yearSubItem = (TVITourBookYearCategorized) treeItem;
               if (isFirstYearSub) {
                  // keep selected year/month/week/day
                  isFirstYearSub = false;
                  _selectedYear = yearSubItem.tourYear;
                  _selectedYearSub = yearSubItem.tourYearSub;
               }

               // get all tours for the selected month
               getYearSubTourIDs(yearSubItem, tourIds);

            } else if (treeItem instanceof TVITourBookTour) {

               // tour is selected

               final TVITourBookTour tourItem = (TVITourBookTour) treeItem;
               if (isFirstTour) {
                  // keep selected tour
                  isFirstTour = false;
                  _selectedYear = tourItem.tourYear;
                  _selectedYearSub = tourItem.tourYearSub;
               }

               tourIds.add(tourItem.getTourId());
            }

         } else {

            // get only selected tours

            if (treeItem instanceof TVITourBookTour) {

               final TVITourBookTour tourItem = (TVITourBookTour) treeItem;

               if (isFirstTour) {
                  // keep selected tour
                  isFirstTour = false;
                  _selectedYear = tourItem.tourYear;
                  _selectedYearSub = tourItem.tourYearSub;
               }

               tourIds.add(tourItem.getTourId());
            }
         }
      }

      onSelect_CreateTourSelection(tourIds);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (_isInFireSelection) {
         return;
      }

      // show and select the selected tour
      if (selection instanceof SelectionTourId) {

         final long newTourId = ((SelectionTourId) selection).getTourId();

         selectTour(newTourId);

      } else if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();

         if (firstElement instanceof GeoPartComparerItem) {

            // show selected compared tour

            final GeoPartComparerItem comparerItem = (GeoPartComparerItem) firstElement;

            selectTour(comparerItem.tourId);
         }

      } else if (selection instanceof SelectionDeletedTours) {

         reloadViewer();
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      if (_isLayoutNatTable) {

         return recreateViewer_NatTable();

      } else {

         return recreateViewer_Tree();
      }
   }

   private ColumnViewer recreateViewer_NatTable() {

      final RowSelectionModel<TVITourBookTour> selectionModel = getNatTable_SelectionModel();
      final int[] allRowPositions = selectionModel.getFullySelectedRowPositions(0);

      // maybe prevent memory leaks
      _natTable_DataLoader.resetTourItems();

      _viewerContainer_NatTable.setRedraw(false);
      {
         _tourViewer_NatTable.dispose();

         createUI_20_TourViewer_NatTable(_viewerContainer_NatTable);

         _viewerContainer_NatTable.layout(true, true);

         setupTourViewerContent();

      }
      _viewerContainer_NatTable.setRedraw(true);

      natTable_SelectTours(allRowPositions);

      return null;
   }

   private ColumnViewer recreateViewer_Tree() {

      _viewerContainer_Tree.setRedraw(false);
      {
         final Object[] expandedElements = _tourViewer_Tree.getExpandedElements();
         final ISelection selection = _tourViewer_Tree.getSelection();

         _tourViewer_Tree.getTree().dispose();

         createUI_30_TourViewer_Tree(_viewerContainer_Tree);
         _viewerContainer_Tree.layout();

         setupTourViewerContent();

         _tourViewer_Tree.setExpandedElements(expandedElements);
         _tourViewer_Tree.setSelection(selection);
      }
      _viewerContainer_Tree.setRedraw(true);

      return _tourViewer_Tree;
   }

   @Override
   public void reloadViewer() {

      if (_isInReload) {
         return;
      }

      _natTable_DataLoader.resetTourItems();

      if (_isLayoutNatTable) {

         int[] allRowPositions;

         _tourViewer_NatTable.setRedraw(false);
         _isInReload = true;
         {
            final RowSelectionModel<TVITourBookTour> selectionModel = getNatTable_SelectionModel();
            allRowPositions = selectionModel.getFullySelectedRowPositions(0);

            setupTourViewerContent();
         }
         _isInReload = false;
         _tourViewer_NatTable.setRedraw(true);

         natTable_SelectTours(allRowPositions);

      } else {

         final Tree tree = _tourViewer_Tree.getTree();
         tree.setRedraw(false);
         _isInReload = true;
         {
            final Object[] expandedElements = _tourViewer_Tree.getExpandedElements();
            final ISelection selection = _tourViewer_Tree.getSelection();

            setupTourViewerContent();

            _tourViewer_Tree.setExpandedElements(expandedElements);
            _tourViewer_Tree.setSelection(selection, true);
         }
         _isInReload = false;
         tree.setRedraw(true);
      }
   }

   void reopenFirstSelectedTour() {

      if (_isLayoutNatTable) {

         setupTourViewerContent();

      } else {

         _selectedYear = -1;
         _selectedYearSub = -1;
         TVITourBookTour selectedTourItem = null;

         final ISelection oldSelection = _tourViewer_Tree.getSelection();
         if (oldSelection != null) {

            final Object selection = ((IStructuredSelection) oldSelection).getFirstElement();
            if (selection instanceof TVITourBookTour) {

               selectedTourItem = (TVITourBookTour) selection;

               _selectedYear = selectedTourItem.tourYear;

               if (_viewLayout == TourBookViewLayout.CATEGORY_WEEK) {
                  _selectedYearSub = selectedTourItem.colWeekNo;
               } else {
                  _selectedYearSub = selectedTourItem.tourMonth;
               }
            }
         }

         reloadViewer();
         reselectTourViewer();

         final IStructuredSelection newSelection = (IStructuredSelection) _tourViewer_Tree.getSelection();
         if (newSelection != null) {

            final Object selection = newSelection.getFirstElement();
            if (selection instanceof TVITourBookTour) {

               selectedTourItem = (TVITourBookTour) selection;

               _tourViewer_Tree.collapseAll();
               _tourViewer_Tree.expandToLevel(selectedTourItem, 0);
               _tourViewer_Tree.setSelection(new StructuredSelection(selectedTourItem), false);
            }
         }
      }
   }

   /**
    * Reselect tours in {@link NatTable} with the provided tour id's
    *
    * @param selectedTourIds
    */
   private void reselectNatTableViewer(final ArrayList<Long> selectedTourIds) {

      final CompletableFuture<int[]> allRowPositions = _natTable_DataLoader.getRowIndexFromTourId(selectedTourIds);

      allRowPositions.thenRun(() -> {

         try {

            natTable_SelectTours(allRowPositions.get());

         } catch (InterruptedException | ExecutionException e) {

            // ignore, should not happen (hopefully :-)
            e.printStackTrace();
         }
      });
   }

   /**
    * Reselect tours from {@link #_selectedTourIds}
    */
   private void reselectTourViewer() {

      if (_isLayoutNatTable) {

         reselectNatTableViewer(_selectedTourIds);

      } else {

         // find the old selected year/[month/week] in the new tour items
         TreeViewerItem reselectYearItem = null;
         TreeViewerItem reselectYearSubItem = null;
         final ArrayList<TreeViewerItem> reselectTourItems = new ArrayList<>();

         /*
          * get the year/month/tour item in the data model
          */
         final ArrayList<TreeViewerItem> rootItems = _rootItem_Tree.getChildren();

         for (final TreeViewerItem rootItem : rootItems) {

            if (rootItem instanceof TVITourBookYear) {

               final TVITourBookYear tourBookYear = ((TVITourBookYear) rootItem);
               if (tourBookYear.tourYear == _selectedYear) {

                  reselectYearItem = rootItem;

                  final Object[] yearSubItems = tourBookYear.getFetchedChildrenAsArray();
                  for (final Object yearSub : yearSubItems) {

                     final TVITourBookYearCategorized tourBookYearSub = ((TVITourBookYearCategorized) yearSub);
                     if (tourBookYearSub.tourYearSub == _selectedYearSub) {

                        reselectYearSubItem = tourBookYearSub;

                        final Object[] tourItems = tourBookYearSub.getFetchedChildrenAsArray();
                        for (final Object tourItem : tourItems) {

                           final TVITourBookTour tourBookTour = ((TVITourBookTour) tourItem);
                           final long treeTourId = tourBookTour.tourId;

                           for (final Long tourId : _selectedTourIds) {
                              if (treeTourId == tourId) {
                                 reselectTourItems.add(tourBookTour);
                                 break;
                              }
                           }
                        }
                        break;
                     }
                  }
                  break;
               }
            }
         }

         // select year/month/tour in the viewer
         if (reselectTourItems.size() > 0) {

            _tourViewer_Tree.setSelection(new StructuredSelection(reselectTourItems) {}, false);

         } else if (reselectYearSubItem != null) {

            _tourViewer_Tree.setSelection(new StructuredSelection(reselectYearSubItem) {}, false);

         } else if (reselectYearItem != null) {

            _tourViewer_Tree.setSelection(new StructuredSelection(reselectYearItem) {}, false);

         } else if (rootItems.size() > 0)

         {

            // the old year was not found, select the newest year

//         final TreeViewerItem yearItem = rootItems.get(rootItems.size() - 1);

//         _tourViewer.setSelection(new StructuredSelection(yearItem) {}, true);
         }

         // move the horizontal scrollbar to the left border
         final ScrollBar horizontalBar = _tourViewer_Tree.getTree().getHorizontalBar();
         if (horizontalBar != null) {
            horizontalBar.setSelection(0);
         }
      }
   }

   private void restoreState() {

      // set tour viewer reselection data
      try {
         _selectedYear = _state.getInt(STATE_SELECTED_YEAR);
      } catch (final NumberFormatException e) {
         _selectedYear = -1;
      }

      try {
         _selectedYearSub = _state.getInt(STATE_SELECTED_MONTH);
      } catch (final NumberFormatException e) {
         _selectedYearSub = -1;
      }

      final String[] selectedTourIds = _state.getArray(STATE_SELECTED_TOURS);
      _selectedTourIds.clear();

      if (selectedTourIds != null) {
         for (final String tourId : selectedTourIds) {
            try {
               _selectedTourIds.add(Long.valueOf(tourId));
            } catch (final NumberFormatException e) {
               // ignore
            }
         }
      }

      _actionSelectAllTours.setChecked(_state.getBoolean(STATE_IS_SELECT_YEAR_MONTH_TOURS));
      _isCollapseOthers = Util.getStateBoolean(_state,
            STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS,
            STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS_DEFAULT);

      /*
       * View layout
       */
      _viewLayout = (TourBookViewLayout) Util.getStateEnum(_state, STATE_VIEW_LAYOUT, TourBookViewLayout.CATEGORY_MONTH);

      String viewLayoutImage = null;

      if (_viewLayout == TourBookViewLayout.CATEGORY_MONTH) {

         viewLayoutImage = Messages.Image__TourBook_Month;

         _isLayoutNatTable = false;

      } else if (_viewLayout == TourBookViewLayout.CATEGORY_WEEK) {

         viewLayoutImage = Messages.Image__TourBook_Week;

         _isLayoutNatTable = false;

      } else if (_viewLayout == TourBookViewLayout.NAT_TABLE) {

         viewLayoutImage = Messages.Image__TourBook_NatTable;

         _isLayoutNatTable = true;
      }

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(viewLayoutImage));

      /*
       * View options
       */
      _columnFactory.setIsShowSummaryRow(isShowSummaryRow());

      _columnFactory.updateToolTipState();
   }

   private void restoreState_AfterUI() {

      /*
       * This must be selected lately otherwise the selection state is set but is not visible
       * (button is not pressed). Could not figure out why this occures after debugging this issue
       */
      _actionLinkWithOtherViews.setSelection(_state.getBoolean(STATE_IS_LINK_WITH_OTHER_VIEWS));
   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, TableColumnFactory.TIME_DATE_ID);
      final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, ItemComparator_Table.DESCENDING);

      // update comparator
      _tourViewer_Table_Comparator.__sortColumnId = sortColumnId;
      _tourViewer_Table_Comparator.__sortDirection = sortDirection;
   }

   @PersistState
   private void saveState() {

      // save selection in the tour viewer
      _state.put(STATE_SELECTED_YEAR, _selectedYear);
      _state.put(STATE_SELECTED_MONTH, _selectedYearSub);

      // convert tour id's into string
      final ArrayList<String> selectedTourIds = new ArrayList<>();
      for (final Long tourId : _selectedTourIds) {
         selectedTourIds.add(tourId.toString());
      }
      _state.put(STATE_SELECTED_TOURS, selectedTourIds.toArray(new String[selectedTourIds.size()]));

      // action: select tours for year/yearSub
      _state.put(STATE_IS_SELECT_YEAR_MONTH_TOURS, _actionSelectAllTours.isChecked());

      _state.put(STATE_IS_LINK_WITH_OTHER_VIEWS, _actionLinkWithOtherViews.getSelection());
      _state.put(STATE_VIEW_LAYOUT, _viewLayout.name());

      // viewer columns
      _state.put(STATE_SORT_COLUMN_ID, _tourViewer_Table_Comparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _tourViewer_Table_Comparator.__sortDirection);

      _columnManager_Tree.saveState(_state_Tree);

      _columnManager_NatTable.saveState(
            _state_NatTable,
            _natTable_Body_DataLayer,
            _natTable_Body_ColumnReorderLayer,
            _natTable_Body_ColumnHideShowLayer);

   }

   private void selectTour(final long tourId) {

      if (_isLayoutNatTable) {

         // for performance reasons a tour cannot be selected by it's ID only by table index
         // TODO: get table index for a tour from db

         return;
      }

      // check if enabled
      if (_actionLinkWithOtherViews.getSelection() == false) {

         // linking is disabled

         return;
      }

      // check with old id
      final long oldTourId = _selectedTourIds != null && _selectedTourIds.size() == 1
            ? _selectedTourIds.get(0)
            : -1;

      if (tourId == oldTourId) {

         // tour id is the same

         return;
      }

      // link with other views

      final TourData tourData = TourManager.getTour(tourId);

      if (tourData == null) {
         return;
      }

      _selectedTourIds.clear();
      _selectedTourIds.add(tourId);

      if (_viewLayout == TourBookViewLayout.CATEGORY_WEEK) {

         _selectedYear = tourData.getStartWeekYear();
         _selectedYearSub = tourData.getStartWeek();

      } else {

         final ZonedDateTime tourStartTime = tourData.getTourStartTime();

         _selectedYear = tourStartTime.getYear();
         _selectedYearSub = tourStartTime.getMonthValue();
      }

      // run async otherwise an internal NPE occures
      _parent.getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {

            final Tree tree = _tourViewer_Tree.getTree();
            tree.setRedraw(false);
            _isInReload = true;
            {
               if (_isCollapseOthers) {

                  try {

                     _tourViewer_Tree.collapseAll();

                  } catch (final Exception e) {

                     /**
                      * <code>

                        Caused by: java.lang.NullPointerException
                        at org.eclipse.jface.viewers.AbstractTreeViewer.getSelection(AbstractTreeViewer.java:2956)
                        at org.eclipse.jface.viewers.StructuredViewer.handleSelect(StructuredViewer.java:1211)
                        at org.eclipse.jface.viewers.StructuredViewer$4.widgetSelected(StructuredViewer.java:1241)
                        at org.eclipse.jface.util.OpenStrategy.fireSelectionEvent(OpenStrategy.java:239)
                        at org.eclipse.jface.util.OpenStrategy.access$4(OpenStrategy.java:233)
                        at org.eclipse.jface.util.OpenStrategy$1.handleEvent(OpenStrategy.java:403)
                        at org.eclipse.swt.widgets.EventTable.sendEvent(EventTable.java:84)
                        at org.eclipse.swt.widgets.Widget.sendEvent(Widget.java:1053)
                        at org.eclipse.swt.widgets.Widget.sendEvent(Widget.java:1077)
                        at org.eclipse.swt.widgets.Widget.sendSelectionEvent(Widget.java:1094)
                        at org.eclipse.swt.widgets.TreeItem.setExpanded(TreeItem.java:1385)
                        at org.eclipse.jface.viewers.TreeViewer.setExpanded(TreeViewer.java:332)
                        at org.eclipse.jface.viewers.AbstractTreeViewer.internalCollapseToLevel(AbstractTreeViewer.java:1571)
                        at org.eclipse.jface.viewers.AbstractTreeViewer.internalCollapseToLevel(AbstractTreeViewer.java:1586)
                        at org.eclipse.jface.viewers.AbstractTreeViewer.collapseToLevel(AbstractTreeViewer.java:751)
                        at org.eclipse.jface.viewers.AbstractTreeViewer.collapseAll(AbstractTreeViewer.java:733)

                        at net.tourbook.ui.views.tourBook.TourBookView$70.run(TourBookView.java:3406)

                        at org.eclipse.swt.widgets.RunnableLock.run(RunnableLock.java:35)
                        at org.eclipse.swt.widgets.Synchronizer.runAsyncMessages(Synchronizer.java:135)
                        ... 22 more

                      * </code>
                      */

                     // this occures sometimes but it seems that it's an eclipse internal problem
                     StatusUtil.log("This is a known issue when a treeviewer do a collapseAll()", e); //$NON-NLS-1$
                  }
               }

               reselectTourViewer();
            }
            _isInReload = false;
            tree.setRedraw(true);
         }
      });
   }

   public void setActiveYear(final int activeYear) {
      _selectedYear = activeYear;
   }

   @Override
   public void setFocus() {

      if (_isLayoutNatTable) {

// this do not work, the workaround is to select a row:
//
//         _tourViewer_NatTable.doCommand(new SelectRowsCommand(_natTable_Grid_BodyLayer, 0, 80, false, false));
//
//         _tourViewer_NatTable.getDisplay().asyncExec(() -> {
//
//            if (!_tourViewer_NatTable.isDisposed()) {
//               _tourViewer_NatTable.setFocus();
//            }
//         });

         _tourViewer_NatTable.setFocus();

      } else {

         final Tree tree = _tourViewer_Tree.getTree();

         if (tree.isDisposed()) {
            return;
         }

         tree.setFocus();
      }
   }

   void setLinkAndCollapse(final boolean isCollapseOthers) {

      _isCollapseOthers = isCollapseOthers;
   }

   private void setupTourViewerContent() {

      if (_isLayoutNatTable) {

         _tourViewer_NatTable.refresh();

         _pageBook.showPage(_viewerContainer_NatTable);

      } else {

         if (_rootItem_Tree != null) {
            _rootItem_Tree.clearChildren();
         }

         _rootItem_Tree = new TVITourBookRoot(this);

         _tourViewer_Tree.getTree().setRedraw(false);
         {
            _tourViewer_Tree.setContentProvider(new ContentProvider_Tree());
            _tourViewer_Tree.setInput(_rootItem_Tree);

            _pageBook.showPage(_viewerContainer_Tree);
         }
         _tourViewer_Tree.getTree().setRedraw(true);
      }

   }

   private void toggleLayout_Category_Month() {

      _viewLayout = TourBookViewLayout.CATEGORY_MONTH;

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_Month));

      _isLayoutNatTable = false;
   }

   private void toggleLayout_Category_Week() {

      _viewLayout = TourBookViewLayout.CATEGORY_WEEK;

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_Week));

      _isLayoutNatTable = false;
   }

   private void toggleLayout_NatTable() {

      _viewLayout = TourBookViewLayout.NAT_TABLE;

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_NatTable));

      _isLayoutNatTable = true;
   }

   @Override
   public void toursAreModified(final ArrayList<TourData> modifiedTours) {

      // do a reselection of the selected tours to fire the multi tour data selection

      actionSelectYearMonthTours();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {

   }

   void updateTourBookOptions() {

      _columnFactory.setIsShowSummaryRow(isShowSummaryRow());

      reloadViewer();
   }

   private void updateUI_TourViewerColumns_Tree() {

      // set tooltip text

      final String timeZone = _prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID);
      final String timeZoneTooltip = NLS.bind(COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP, timeZone);

      _columnFactory.getColDef_TimeZoneOffset_Tree().setColumnHeaderToolTipText(timeZoneTooltip);
   }

}
