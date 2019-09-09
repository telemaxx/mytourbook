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
package net.tourbook.tour;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.TreeSet;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboInput;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DialogQuickEdit extends TitleAreaDialog {

   private static final String      GRAPH_LABEL_HEARTBEAT_UNIT     = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
   private static final String      VALUE_UNIT_K_CALORIES          = net.tourbook.ui.Messages.Value_Unit_KCalories;

   private final boolean            _isOSX                         = net.tourbook.common.UI.IS_OSX;
   private final boolean            _isLinux                       = net.tourbook.common.UI.IS_LINUX;

   private final TourData           _tourData;

   private final IDialogSettings    _state;
   private PixelConverter           _pc;

   /**
    * contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the different section to the same width
    */
   private final ArrayList<Control> _firstColumnControls           = new ArrayList<>();
   private final ArrayList<Control> _firstColumnContainerControls  = new ArrayList<>();
   private final ArrayList<Control> _secondColumnControls          = new ArrayList<>();

   private int                      _hintDefaultSpinnerWidth;

   private boolean                  _isUpdateUI                    = false;
   private boolean                  _isTemperatureManuallyModified = false;
   private boolean                  _isWindSpeedManuallyModified   = false;
   private int[]                    _unitValueWindSpeed;
   private float                    _unitValueDistance;
   private float                    _unitValueTemperature;

   /*
    * UI controls
    */
   private FormToolkit        _tk;
   private Form               _formContainer;

   private CLabel             _lblWeather_CloudIcon;

   private Combo              _comboTitle;
   private Combo              _comboWeather_Clouds;
   private Combo              _comboWeather_Wind_DirectionText;
   private Combo              _comboWeather_Wind_SpeedText;

   private Spinner            _spinBodyWeight;
   private Spinner            _spinFTP;
   private Spinner            _spinRestPuls;
   private Spinner            _spinCalories;
   private Spinner            _spinWeather_Temperature_Avg;
   private Spinner            _spinWeather_Wind_SpeedValue;
   private Spinner            _spinWeather_Wind_DirectionValue;

   private Text               _txtDescription;
   private Text               _txtWeather;

   private MouseWheelListener _mouseWheelListener;
   {
      _mouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            Util.adjustSpinnerValueOnMouseScroll(event);
         }
      };
   }

   public DialogQuickEdit(final Shell parentShell, final TourData tourData) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);

      setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit).createImage());

      _tourData = tourData;

      _state = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.dialog_quick_edit_dialog_title);

      shell.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            onDispose();
         }
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.dialog_quick_edit_dialog_area_title);

      final ZonedDateTime tourStart = _tourData.getTourStartTime();

      setMessage(
            tourStart.format(TimeTools.Formatter_Date_F)
                  + UI.SPACE2
                  + tourStart.format(TimeTools.Formatter_Time_S));
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      final String okText = net.tourbook.ui.UI.convertOKtoSaveUpdateButton(_tourData);

      getButton(IDialogConstants.OK_ID).setText(okText);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

      // create ui
      createUI(dlgAreaContainer);

      updateUIFromModel();
      enableControls();

      return dlgAreaContainer;
   }

   /**
    * @param parent
    * @param title
    * @param isGrabVertical
    * @return
    */
   private Composite createSection(final Composite parent, final String title, final boolean isGrabVertical) {

      final Section section = _tk.createSection(parent, //
      //Section.TWISTIE |
//            Section.SHORT_TITLE_BAR
            Section.TITLE_BAR
      // | Section.DESCRIPTION
      // | Section.EXPANDED
      );

      section.setText(title);
      GridDataFactory.fillDefaults().grab(true, isGrabVertical).applyTo(section);

      final Composite sectionContainer = _tk.createComposite(section);
      section.setClient(sectionContainer);

//      section.addExpansionListener(new ExpansionAdapter() {
//         @Override
//         public void expansionStateChanged(final ExpansionEvent e) {
//            form.reflow(false);
//         }
//      });

      return sectionContainer;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _hintDefaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 14 : 7);

      _unitValueDistance = net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      _unitValueTemperature = net.tourbook.ui.UI.UNIT_VALUE_TEMPERATURE;
      _unitValueWindSpeed = net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == 1
            ? IWeather.windSpeedKmh
            : IWeather.windSpeedMph;

      _tk = new FormToolkit(parent.getDisplay());

      _formContainer = _tk.createForm(parent);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_formContainer);
      _tk.decorateFormHeading(_formContainer);
      _tk.setBorderStyle(SWT.BORDER);

      final Composite tourContainer = _formContainer.getBody();
      GridLayoutFactory.swtDefaults().applyTo(tourContainer);
      {
         createUI_110_Title(tourContainer);
         createUI_SectionSeparator(tourContainer);

         createUI_130_Personal(tourContainer);
         createUI_SectionSeparator(tourContainer);

         createUI_140_Weather(tourContainer);
      }

      final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

      // compute width for all controls and equalize column width for the different sections
      tourContainer.layout(true, true);
      UI.setEqualizeColumWidths(_firstColumnControls);
      UI.setEqualizeColumWidths(_secondColumnControls);

      tourContainer.layout(true, true);
      UI.setEqualizeColumWidths(_firstColumnContainerControls);
   }

   private void createUI_110_Title(final Composite parent) {

      Label label;
      final int defaultTextWidth = _pc.convertWidthInCharsToPixels(40);

      final Composite section = createSection(parent, Messages.tour_editor_section_tour, true);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(section);
      {
         /*
          * title
          */

         label = _tk.createLabel(section, Messages.tour_editor_label_tour_title);
         _firstColumnControls.add(label);

         // combo: tour title with history
         _comboTitle = new Combo(section, SWT.BORDER | SWT.FLAT);
         _comboTitle.setText(UI.EMPTY_STRING);

         _tk.adapt(_comboTitle, true, false);

         GridDataFactory.fillDefaults()
               .grab(true, false)
               .hint(defaultTextWidth, SWT.DEFAULT)
               .applyTo(_comboTitle);

         // fill combobox
         final TreeSet<String> dbTitles = TourDatabase.getAllTourTitles();
         for (final String title : dbTitles) {
            _comboTitle.add(title);
         }

         new AutocompleteComboInput(_comboTitle);

         /*
          * description
          */
         label = _tk.createLabel(section, Messages.tour_editor_label_description);
         GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
         _firstColumnControls.add(label);

         _txtDescription = _tk.createText(
               section, //
               UI.EMPTY_STRING,
               SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL//
         );

         final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

         int descLines = store.getInt(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT);
         descLines = descLines == 0 ? 5 : descLines;

         GridDataFactory.fillDefaults()
               .grab(true, true)
               //
               // SWT.DEFAULT causes lot's of problems with the layout therefore the hint is set
               //
               .hint(defaultTextWidth, _pc.convertHeightInCharsToPixels(descLines))
               .applyTo(_txtDescription);
      }
   }

   private void createUI_130_Personal(final Composite parent) {

      final Composite section = createSection(parent, Messages.tour_editor_section_personal, false);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(20, 5)
            .applyTo(section);
      {
         createUI_132_Personal_Col1(section);
         createUI_134_Personal_Col2(section);
      }
   }

   /**
    * 1. column
    */
   private void createUI_132_Personal_Col1(final Composite section) {

      final Composite container = _tk.createComposite(section);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      _firstColumnContainerControls.add(container);
      {
         {
            /*
             * calories
             */

            // label
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_tour_calories);
            _firstColumnControls.add(label);

            // spinner
            _spinCalories = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_spinCalories);
            _spinCalories.setMinimum(0);
            _spinCalories.setMaximum(1_000_000_000);
            _spinCalories.setDigits(3);

            _spinCalories.addMouseWheelListener(_mouseWheelListener);

            // label: kcal
            _tk.createLabel(container, VALUE_UNIT_K_CALORIES);
         }
         {
            /*
             * rest pulse
             */

            // label: Rest pulse
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_rest_pulse);
            label.setToolTipText(Messages.tour_editor_label_rest_pulse_Tooltip);
            _firstColumnControls.add(label);

            // spinner
            _spinRestPuls = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinRestPuls);
            _spinRestPuls.setMinimum(0);
            _spinRestPuls.setMaximum(200);
            _spinRestPuls.setToolTipText(Messages.tour_editor_label_rest_pulse_Tooltip);

            _spinRestPuls.addMouseWheelListener(_mouseWheelListener);

            // label: bpm
            _tk.createLabel(container, GRAPH_LABEL_HEARTBEAT_UNIT);
         }
      }
   }

   /**
    * 2. column
    */
   private void createUI_134_Personal_Col2(final Composite section) {

      final Composite container = _tk.createComposite(section);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            {
               /*
                * Body weight
                */
               // label: Weight
               final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_BodyWeight);
               label.setToolTipText(Messages.Tour_Editor_Label_BodyWeight_Tooltip);
               _secondColumnControls.add(label);

               // spinner: weight
               _spinBodyWeight = new Spinner(container, SWT.BORDER);
               GridDataFactory.fillDefaults()
                     .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(_spinBodyWeight);
               _spinBodyWeight.setDigits(1);
               _spinBodyWeight.setMinimum(0);
               _spinBodyWeight.setMaximum(6614); // 300.0 kg, 661.4 lbs

               _spinBodyWeight.addMouseWheelListener(_mouseWheelListener);

               // label: unit
               _tk.createLabel(container, UI.UNIT_LABEL_WEIGHT);
            }

            {
               /*
                * FTP - Functional Threshold Power
                */
               // label: FTP
               final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_FTP);
               label.setToolTipText(Messages.Tour_Editor_Label_FTP_Tooltip);
               _secondColumnControls.add(label);

               // spinner: FTP
               _spinFTP = new Spinner(container, SWT.BORDER);
               GridDataFactory.fillDefaults()
                     .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(_spinFTP);
               _spinFTP.setMinimum(0);
               _spinFTP.setMaximum(10000);

               _spinFTP.addMouseWheelListener(_mouseWheelListener);

               // spacer
               _tk.createLabel(container, UI.EMPTY_STRING);
            }
         }
      }
   }

   private void createUI_140_Weather(final Composite parent) {

      final Composite section = createSection(parent, Messages.tour_editor_section_weather, false);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(20, 5)
            .applyTo(section);
      {
         createUI_141_Weather(section);
         createUI_142_Weather(section);
         createUI_144_Weather_Col1(section);
      }
   }

   private void createUI_141_Weather(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * weather description
          */
         final Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_Weather);
         GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
         _firstColumnControls.add(label);

         _txtWeather = _tk.createText(
               container, //
               UI.EMPTY_STRING,
               SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL//
         );

         GridDataFactory.fillDefaults()
               .grab(true, true)
               //
               // SWT.DEFAULT causes lot's of problems with the layout therefore the hint is set
               //
               .hint(_pc.convertWidthInCharsToPixels(80), _pc.convertHeightInCharsToPixels(2))
               .applyTo(_txtWeather);
      }
   }

   private void createUI_142_Weather(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
      {
         {
            /*
             * wind speed
             */

            // label
            Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_speed);
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _firstColumnControls.add(label);

            // spinner
            _spinWeather_Wind_SpeedValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinWeather_Wind_SpeedValue);
            _spinWeather_Wind_SpeedValue.setMinimum(0);
            _spinWeather_Wind_SpeedValue.setMaximum(120);
            _spinWeather_Wind_SpeedValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            _spinWeather_Wind_SpeedValue.addModifyListener(new ModifyListener() {
               @Override
               public void modifyText(final ModifyEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
                  onSelectWindSpeedValue();
               }
            });
            _spinWeather_Wind_SpeedValue.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
                  onSelectWindSpeedValue();
               }
            });
            _spinWeather_Wind_SpeedValue.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  Util.adjustSpinnerValueOnMouseScroll(event);
                  if (_isUpdateUI) {
                     return;
                  }
                  onSelectWindSpeedValue();
               }
            });

            // label: km/h, mi/h
            label = _tk.createLabel(container, UI.UNIT_LABEL_SPEED);

            // combo: wind speed with text
            _comboWeather_Wind_SpeedText = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .indent(10, 0)
                  .span(2, 1)
                  .applyTo(_comboWeather_Wind_SpeedText);
            _tk.adapt(_comboWeather_Wind_SpeedText, true, false);
            _comboWeather_Wind_SpeedText.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _comboWeather_Wind_SpeedText.setVisibleItemCount(20);
            _comboWeather_Wind_SpeedText.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {

                  if (_isUpdateUI) {
                     return;
                  }
                  onSelectWindSpeedText();
               }
            });

            // fill combobox
            for (final String speedText : IWeather.windSpeedText) {
               _comboWeather_Wind_SpeedText.add(speedText);
            }
         }
         {
            /*
             * wind direction
             */

            // label
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_direction);
            label.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);
            _firstColumnControls.add(label);

            // combo: wind direction text
            _comboWeather_Wind_DirectionText = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _tk.adapt(_comboWeather_Wind_DirectionText, true, false);
            _comboWeather_Wind_DirectionText.setToolTipText(Messages.tour_editor_label_WindDirectionNESW_Tooltip);
            _comboWeather_Wind_DirectionText.setVisibleItemCount(16);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .applyTo(_comboWeather_Wind_DirectionText);
            _comboWeather_Wind_DirectionText.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {

                  if (_isUpdateUI) {
                     return;
                  }
                  onSelectWindDirectionText();
               }
            });

            // fill combobox
            for (final String fComboCloudsUIValue : IWeather.windDirectionText) {
               _comboWeather_Wind_DirectionText.add(fComboCloudsUIValue);
            }

            // spacer
            new Label(container, SWT.NONE);

            // spinner: wind direction value
            _spinWeather_Wind_DirectionValue = new Spinner(container, SWT.BORDER);
            _spinWeather_Wind_DirectionValue.setMinimum(-1);
            _spinWeather_Wind_DirectionValue.setMaximum(3600);
            _spinWeather_Wind_DirectionValue.setDigits(1);
            _spinWeather_Wind_DirectionValue.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .indent(10, 0)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinWeather_Wind_DirectionValue);

            _spinWeather_Wind_DirectionValue.addModifyListener(new ModifyListener() {
               @Override
               public void modifyText(final ModifyEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
                  onSelectWindDirectionValue();
               }
            });
            _spinWeather_Wind_DirectionValue.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
                  onSelectWindDirectionValue();
               }
            });
            _spinWeather_Wind_DirectionValue.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  Util.adjustSpinnerValueOnMouseScroll(event);
                  if (_isUpdateUI) {
                     return;
                  }
                  onSelectWindDirectionValue();
               }
            });

            // label: direction unit = degree
            _tk.createLabel(container, Messages.Tour_Editor_Label_WindDirection_Unit);
         }
      }
   }

   /**
    * weather: 1. column
    */
   private void createUI_144_Weather_Col1(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      _firstColumnContainerControls.add(container);
      {
         /*
          * temperature
          */

         // label
         Label label = _tk.createLabel(container, Messages.Tour_Editor_Label_Temperature);
         label.setToolTipText(Messages.Tour_Editor_Label_Temperature_Tooltip);
         _firstColumnControls.add(label);

         // spinner
         _spinWeather_Temperature_Avg = new Spinner(container, SWT.BORDER);
         GridDataFactory.fillDefaults()
               .align(SWT.BEGINNING, SWT.CENTER)
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .applyTo(_spinWeather_Temperature_Avg);
         _spinWeather_Temperature_Avg.setToolTipText(Messages.Tour_Editor_Label_Temperature_Tooltip);

         // the min/max temperature has a large range because fahrenheit has bigger values than celcius
         _spinWeather_Temperature_Avg.setMinimum(-600);
         _spinWeather_Temperature_Avg.setMaximum(1500);

         _spinWeather_Temperature_Avg.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
               if (_isUpdateUI) {
                  return;
               }
               _isTemperatureManuallyModified = true;
            }
         });
         _spinWeather_Temperature_Avg.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               if (_isUpdateUI) {
                  return;
               }
               _isTemperatureManuallyModified = true;
            }
         });
         _spinWeather_Temperature_Avg.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(final MouseEvent event) {
               Util.adjustSpinnerValueOnMouseScroll(event);
               if (_isUpdateUI) {
                  return;
               }
               _isTemperatureManuallyModified = true;
            }
         });

         // label: celcius, fahrenheit
         label = _tk.createLabel(container, UI.UNIT_LABEL_TEMPERATURE);

         /*
          * clouds
          */
         final Composite cloudContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(cloudContainer);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(cloudContainer);
         {
            // label: clouds
            label = _tk.createLabel(cloudContainer, Messages.tour_editor_label_clouds);
            label.setToolTipText(Messages.tour_editor_label_clouds_Tooltip);

            // icon: clouds
            _lblWeather_CloudIcon = new CLabel(cloudContainer, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.FILL)
                  .grab(true, false)
                  .applyTo(_lblWeather_CloudIcon);
         }
         _firstColumnControls.add(cloudContainer);

         // combo: clouds
         _comboWeather_Clouds = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_comboWeather_Clouds);
         _tk.adapt(_comboWeather_Clouds, true, false);
         _comboWeather_Clouds.setToolTipText(Messages.tour_editor_label_clouds_Tooltip);
         _comboWeather_Clouds.setVisibleItemCount(10);
         _comboWeather_Clouds.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               displayCloudIcon();
            }
         });

         // fill combobox
         for (final String cloudText : IWeather.cloudText) {
            _comboWeather_Clouds.add(cloudText);
         }

         // force the icon to be displayed to ensure the width is correctly set when the size is computed
         _isUpdateUI = true;
         {
            _comboWeather_Clouds.select(0);
            displayCloudIcon();
         }
         _isUpdateUI = false;
      }
   }

   private void createUI_SectionSeparator(final Composite parent) {
      final Composite sep = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 5).applyTo(sep);
   }

   private void displayCloudIcon() {

      final int selectionIndex = _comboWeather_Clouds.getSelectionIndex();

      final String cloudKey = IWeather.cloudIcon[selectionIndex];
      final Image cloundIcon = net.tourbook.common.UI.IMAGE_REGISTRY.get(cloudKey);

      _lblWeather_CloudIcon.setImage(cloundIcon);
   }

   private void enableControls() {

      _spinWeather_Temperature_Avg.setEnabled(_tourData.temperatureSerie == null);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
//      return null;
   }

   private int getWindSpeedTextIndex(final int speed) {

      // set speed to max index value
      int speedValueIndex = _unitValueWindSpeed.length - 1;

      for (int speedIndex = 0; speedIndex < _unitValueWindSpeed.length; speedIndex++) {

         final int speedMaxValue = _unitValueWindSpeed[speedIndex];

         if (speed <= speedMaxValue) {
            speedValueIndex = speedIndex;
            break;
         }
      }

      return speedValueIndex;
   }

   @Override
   protected void okPressed() {

      updateModelFromUI();

      if (_tourData.isValidForSave() == false) {
         // data are not valid to be saved which is done in the action which opened this dialog
         return;
      }

      super.okPressed();
   }

   private void onDispose() {

      if (_tk != null) {
         _tk.dispose();
      }

      _firstColumnControls.clear();
      _secondColumnControls.clear();
      _firstColumnContainerControls.clear();
   }

   private void onSelectWindDirectionText() {

      // N=0=0  NE=1=45  E=2=90  SE=3=135  S=4=180  SW=5=225  W=6=270  NW=7=315
      final int selectedIndex = _comboWeather_Wind_DirectionText.getSelectionIndex();

      // get degree from selected direction

      final int degree = (int) (selectedIndex * 22.5f * 10f);

      _spinWeather_Wind_DirectionValue.setSelection(degree);
   }

   private void onSelectWindDirectionValue() {

      int degree = _spinWeather_Wind_DirectionValue.getSelection();

      // this tricky code is used to scroll before 0 which will overscroll and starts from the beginning
      if (degree == -1) {
         degree = 3599;
         _spinWeather_Wind_DirectionValue.setSelection(degree);
      }

      if (degree == 3600) {
         degree = 0;
         _spinWeather_Wind_DirectionValue.setSelection(degree);
      }

      _comboWeather_Wind_DirectionText.select(net.tourbook.common.UI.getCardinalDirectionTextIndex(degree));
   }

   private void onSelectWindSpeedText() {

      _isWindSpeedManuallyModified = true;

      final int selectedIndex = _comboWeather_Wind_SpeedText.getSelectionIndex();
      final int speed = _unitValueWindSpeed[selectedIndex];

      final boolean isBackup = _isUpdateUI;
      _isUpdateUI = true;
      {
         _spinWeather_Wind_SpeedValue.setSelection(speed);
      }
      _isUpdateUI = isBackup;
   }

   private void onSelectWindSpeedValue() {

      _isWindSpeedManuallyModified = true;

      final int windSpeed = _spinWeather_Wind_SpeedValue.getSelection();

      final boolean isBackup = _isUpdateUI;
      _isUpdateUI = true;
      {
         _comboWeather_Wind_SpeedText.select(getWindSpeedTextIndex(windSpeed));
      }
      _isUpdateUI = isBackup;
   }

   /**
    * update tourdata from the fields
    */
   private void updateModelFromUI() {

      _tourData.setTourTitle(_comboTitle.getText().trim());
      _tourData.setTourDescription(_txtDescription.getText().trim());

      final float bodyWeight = UI.convertBodyWeightToMetric(_spinBodyWeight.getSelection());
      _tourData.setBodyWeight(bodyWeight / 10.0f);
      _tourData.setPower_FTP(_spinFTP.getSelection());
      _tourData.setRestPulse(_spinRestPuls.getSelection());
      _tourData.setCalories(_spinCalories.getSelection());

      _tourData.setWeatherWindDir((int) (_spinWeather_Wind_DirectionValue.getSelection() / 10.0f));
      if (_isWindSpeedManuallyModified) {
         /*
          * update the speed only when it was modified because when the measurement is changed
          * when the tour is being modified then the computation of the speed value can cause
          * rounding errors
          */
         _tourData.setWeatherWindSpeed((int) (_spinWeather_Wind_SpeedValue.getSelection() * _unitValueDistance));
      }

      final int cloudIndex = _comboWeather_Clouds.getSelectionIndex();
      String cloudValue = IWeather.cloudIcon[cloudIndex];
      if (cloudValue.equals(net.tourbook.common.UI.IMAGE_EMPTY_16)) {
         // replace invalid cloud key
         cloudValue = UI.EMPTY_STRING;
      }
      _tourData.setWeatherClouds(cloudValue);
      _tourData.setWeather(_txtWeather.getText().trim());

      if (_isTemperatureManuallyModified) {
         float temperature = (float) _spinWeather_Temperature_Avg.getSelection() / 10;
         if (_unitValueTemperature != 1) {

            temperature = ((temperature - net.tourbook.ui.UI.UNIT_FAHRENHEIT_ADD)
                  / net.tourbook.ui.UI.UNIT_FAHRENHEIT_MULTI);
         }
         _tourData.setAvgTemperature(temperature);
      }

   }

   private void updateUIFromModel() {

      _isUpdateUI = true;
      {
         /*
          * tour/event
          */
         // set field content
         _comboTitle.setText(_tourData.getTourTitle());
         _txtDescription.setText(_tourData.getTourDescription());

         /*
          * personal details
          */
         final float bodyWeight = UI.convertBodyWeightFromMetric(_tourData.getBodyWeight());
         _spinBodyWeight.setSelection(Math.round(bodyWeight * 10));
         _spinFTP.setSelection(_tourData.getPower_FTP());
         _spinRestPuls.setSelection(_tourData.getRestPulse());
         _spinCalories.setSelection(_tourData.getCalories());

         /*
          * wind properties
          */
         _txtWeather.setText(_tourData.getWeather());

         // wind direction
         final int weatherWindDirDegree = _tourData.getWeatherWindDir() * 10;
         _spinWeather_Wind_DirectionValue.setSelection(weatherWindDirDegree);
         _comboWeather_Wind_DirectionText.select(net.tourbook.common.UI.getCardinalDirectionTextIndex(weatherWindDirDegree));

         // wind speed
         final int windSpeed = _tourData.getWeatherWindSpeed();
         final int speed = (int) (windSpeed / _unitValueDistance);
         _spinWeather_Wind_SpeedValue.setSelection(speed);
         _comboWeather_Wind_SpeedText.select(getWindSpeedTextIndex(speed));

         // weather clouds
         _comboWeather_Clouds.select(_tourData.getWeatherIndex());

         // icon must be displayed after the combobox entry is selected
         displayCloudIcon();

         /*
          * avg temperature
          */
         float avgTemperature = _tourData.getAvgTemperature();

         if (_unitValueTemperature != 1) {
            final float metricTemperature = avgTemperature;
            avgTemperature = metricTemperature
                  * net.tourbook.ui.UI.UNIT_FAHRENHEIT_MULTI
                  + net.tourbook.ui.UI.UNIT_FAHRENHEIT_ADD;
         }

         _spinWeather_Temperature_Avg.setDigits(1);
         _spinWeather_Temperature_Avg.setSelection(Math.round(avgTemperature * 10));
      }
      _isUpdateUI = false;
   }
}
