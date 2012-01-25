/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TreeAdapter;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * File Viewer example
 * <p>
 * original source code (2012-01-21):
 * http://www.java2s.com/Code/Java/SWT-JFace-Eclipse/FileViewerexampleinSWT.htm
 */
public class FolderViewer {

//	private final static String		DRIVE_A						= "a:" + File.separator;
//	private final static String		DRIVE_B						= "b:" + File.separator;

	/* UI elements */
	private Display					display;

	private Shell					shell;

	private ToolBar					toolBar;

	private Label					numObjectsLabel;

	private Label					diskSpaceLabel;

	private File					currentDirectory			= null;

	private boolean					initial						= true;

	/* Drag and drop optimizations */
	private boolean					isDragging					= false;							// if this app is dragging

	private boolean					isDropping					= false;							// if this app is dropping

	private File[]					processedDropFiles			= null;							// so Drag only deletes what it
	// needs to

	private File[]					deferredRefreshFiles		= null;							// to defer notifyRefreshFiles
	// while we do DND

	private boolean					deferredRefreshRequested	= false;							// to defer
	// notifyRefreshFiles
	// while we do DND

	private ProgressDialog			progressDialog				= null;							// progress dialog for
	// locally-initiated
	// operations

	/* Combo view */
	private static final String		COMBODATA_ROOTS				= "Combo.roots";

	// File[]: Array of files whose paths are currently displayed in the combo
	private static final String		COMBODATA_LASTTEXT			= "Combo.lastText";

	// String: Previous selection text string

	private Combo					combo;

	/* Tree view */
//	private IconCache				iconCache					= new IconCache();

	private static final String		TREEITEMDATA_FILE			= "TreeItem.file";

	// File: File associated with tree item
	private static final String		TREEITEMDATA_IMAGEEXPANDED	= "TreeItem.imageExpanded";

	// Image: shown when item is expanded
	private static final String		TREEITEMDATA_IMAGECOLLAPSED	= "TreeItem.imageCollapsed";

	// Image: shown when item is collapsed
	private static final String		TREEITEMDATA_STUB			= "TreeItem.stub";

	// Object: if not present or null then the item has not been populated

	private Tree					tree;

	private Label					treeScopeLabel;

	/* Table view */
	private static final DateFormat	dateFormat					= DateFormat.getDateTimeInstance(
																		DateFormat.MEDIUM,
																		DateFormat.MEDIUM);

	private static final String		TABLEITEMDATA_FILE			= "TableItem.file";

	// File: File associated with table row
	private static final String		TABLEDATA_DIR				= "Table.dir";

	// File: Currently visible directory
	private static final int[]		tableWidths					= new int[] { 150, 60, 75, 150 };

	private final String[]			tableTitles					= new String[] {
			FolderViewer.getResourceString("table.Name.title"),
			FolderViewer.getResourceString("table.Size.title"),
			FolderViewer.getResourceString("table.Type.title"),
			FolderViewer.getResourceString("table.Modified.title") };

	private Table					table;

	private Label					tableContentsOfLabel;

	/* Table update worker */
	// Control data
	private final Object			workerLock					= new Object();

	// Lock for all worker control data and state
	private volatile Thread			workerThread				= null;

	// The worker's thread
	private volatile boolean		workerStopped				= false;

	// True if the worker must exit on completion of the current cycle
	private volatile boolean		workerCancelled				= false;

	// True if the worker must cancel its operations prematurely perhaps due to
	// a state update

	// Worker state information -- this is what gets synchronized by an update
	private volatile File			workerStateDir				= null;

	// State information to use for the next cycle
	private volatile File			workerNextDir				= null;

	/* Simulate only flag */
	// when true, disables actual filesystem manipulations and outputs results
	// to standard out
	private boolean					simulateOnly				= true;

	/**
	 * Manages the worker's thread
	 */
	private final Runnable			workerRunnable;
	{
		workerRunnable = new Runnable() {
			public void run() {

				while (!workerStopped) {
					synchronized (workerLock) {
						workerCancelled = false;
						workerStateDir = workerNextDir;
					}
					workerExecute();
					synchronized (workerLock) {
						try {
							if ((!workerCancelled) && (workerStateDir == workerNextDir)) {
								workerLock.wait();
							}
						} catch (final InterruptedException e) {}
					}
				}
				workerThread = null;

				// wake up UI thread in case it is in a modal loop awaiting thread
				// termination (see workerStop())
				display.wake();
			}
		};
	}

	/**
	 * Instances of this class manage a progress dialog for file operations.
	 */
	class ProgressDialog {

		public final static int	COPY				= 0;
		public final static int	DELETE				= 1;
		public final static int	MOVE				= 2;

		Shell					shell;

		Label					messageLabel, detailLabel;

		ProgressBar				progressBar;

		Button					cancelButton;

		boolean					isCancelled			= false;

		final String			operationKeyName[]	= { "Copy", "Delete", "Move" };

		/**
		 * Creates a progress dialog but does not open it immediately.
		 * 
		 * @param parent
		 *            the parent Shell
		 * @param style
		 *            one of COPY, MOVE
		 */
		public ProgressDialog(final Shell parent, final int style) {
			shell = new Shell(parent, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL);
			final GridLayout gridLayout = new GridLayout();
			shell.setLayout(gridLayout);
			shell.setText(getResourceString("progressDialog." + operationKeyName[style] + ".title"));
			shell.addShellListener(new ShellAdapter() {
				@Override
				public void shellClosed(final ShellEvent e) {
					isCancelled = true;
				}
			});

			messageLabel = new Label(shell, SWT.HORIZONTAL);
			messageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
			messageLabel.setText(getResourceString("progressDialog." + operationKeyName[style] + ".description"));

			progressBar = new ProgressBar(shell, SWT.HORIZONTAL | SWT.WRAP);
			progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
			progressBar.setMinimum(0);
			progressBar.setMaximum(0);

			detailLabel = new Label(shell, SWT.HORIZONTAL);
			final GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			gridData.widthHint = 400;
			detailLabel.setLayoutData(gridData);

			cancelButton = new Button(shell, SWT.PUSH);
			cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_FILL));
			cancelButton.setText(getResourceString("progressDialog.cancelButton.text"));
			cancelButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					isCancelled = true;
					cancelButton.setEnabled(false);
				}
			});
		}

		/**
		 * Adds to the progress of completion of the total work units.
		 * 
		 * @param work
		 *            the number of work units completed to add
		 */
		public void addProgress(final int work) {
			setProgress(progressBar.getSelection() + work);
		}

		/**
		 * Adds to the total number of work units to be performed.
		 * 
		 * @param work
		 *            the number of work units to add
		 */
		public void addWorkUnits(final int work) {
			setTotalWorkUnits(progressBar.getMaximum() + work);
		}

		/**
		 * Closes the dialog and disposes its resources.
		 */
		public void close() {
			shell.close();
			shell.dispose();
			shell = null;
			messageLabel = null;
			detailLabel = null;
			progressBar = null;
			cancelButton = null;
		}

		/**
		 * Returns true if the Cancel button was been clicked.
		 * 
		 * @return true if the Cancel button was clicked.
		 */
		public boolean isCancelled() {
			return isCancelled;
		}

		/**
		 * Opens the dialog.
		 */
		public void open() {
			shell.pack();
			final Shell parentShell = (Shell) shell.getParent();
			final Rectangle rect = parentShell.getBounds();
			final Rectangle bounds = shell.getBounds();
			bounds.x = rect.x + (rect.width - bounds.width) / 2;
			bounds.y = rect.y + (rect.height - bounds.height) / 2;
			shell.setBounds(bounds);
			shell.open();
		}

		/**
		 * Sets the detail text to show the filename along with a string representing the operation
		 * being performed on that file.
		 * 
		 * @param file
		 *            the file to be detailed
		 * @param operation
		 *            one of COPY, DELETE
		 */
		public void setDetailFile(final File file, final int operation) {
			detailLabel.setText(getResourceString(
					"progressDialog." + operationKeyName[operation] + ".operation",
					new Object[] { file }));
		}

		/**
		 * Sets the progress of completion of the total work units.
		 * 
		 * @param work
		 *            the total number of work units completed
		 */
		public void setProgress(final int work) {
			progressBar.setSelection(work);
			while (display.readAndDispatch()) {} // enable event processing
		}

		/**
		 * Sets the total number of work units to be performed.
		 * 
		 * @param work
		 *            the total number of work units
		 */
		public void setTotalWorkUnits(final int work) {
			progressBar.setMaximum(work);
		}
	}

	private static int compareFiles(final File a, final File b) {
		// boolean aIsDir = a.isDirectory();
		// boolean bIsDir = b.isDirectory();
		// if (aIsDir && ! bIsDir) return -1;
		// if (bIsDir && ! aIsDir) return 1;

		// sort case-sensitive files in a case-insensitive manner
		int compare = a.getName().compareToIgnoreCase(b.getName());
		if (compare == 0) {
			compare = a.getName().compareTo(b.getName());
		}
		return compare;
	}

	/**
	 * Gets a directory listing
	 * 
	 * @param file
	 *            the directory to be listed
	 * @return an array of files this directory contains, may be empty but not null
	 */
	static File[] getDirectoryList(final File file) {
		final File[] list = file.listFiles();
		if (list == null) {
			return new File[0];
		}
		sortFiles(list);
		return list;
	}

	/**
	 * Returns a string from the resource bundle. We don't want to crash because of a missing
	 * String. Returns the key if not found.
	 */
	static String getResourceString(final String key) {
		return key;
	}

	/**
	 * Returns a string from the resource bundle and binds it with the given arguments. If the key
	 * is not found, return the key.
	 */
	static String getResourceString(final String key, final Object[] args) {
		try {
			return MessageFormat.format(getResourceString(key), args);
		} catch (final MissingResourceException e) {
			return key;
		} catch (final NullPointerException e) {
			return "!" + key + "!";
		}
	}

	/**
	 * Runs main program.
	 */
	public static void main(final String[] args) {
		final Display display = new Display();
		final FolderViewer application = new FolderViewer();
		final Shell shell = application.open(display);
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		application.close();
		display.dispose();
	}

	private static void sortBlock(final File[] files, final int start, final int end, final File[] mergeTemp) {
		final int length = end - start + 1;
		if (length < 8) {
			for (int i = end; i > start; --i) {
				for (int j = end; j > start; --j) {
					if (compareFiles(files[j - 1], files[j]) > 0) {
						final File temp = files[j];
						files[j] = files[j - 1];
						files[j - 1] = temp;
					}
				}
			}
			return;
		}
		final int mid = (start + end) / 2;
		sortBlock(files, start, mid, mergeTemp);
		sortBlock(files, mid + 1, end, mergeTemp);
		int x = start;
		int y = mid + 1;
		for (int i = 0; i < length; ++i) {
			if ((x > mid) || ((y <= end) && compareFiles(files[x], files[y]) > 0)) {
				mergeTemp[i] = files[y++];
			} else {
				mergeTemp[i] = files[x++];
			}
		}
		for (int i = 0; i < length; ++i) {
			files[i + start] = mergeTemp[i];
		}
	}

	/**
	 * Sorts files lexicographically by name.
	 * 
	 * @param files
	 *            the array of Files to be sorted
	 */
	static void sortFiles(final File[] files) {
		/* Very lazy merge sort algorithm */
		sortBlock(files, 0, files.length - 1, new File[files.length]);
	}

	/**
	 * Foreign method: removes all children of a TreeItem.
	 * 
	 * @param treeItem
	 *            the TreeItem
	 */
	private static void treeItemRemoveAll(final TreeItem treeItem) {
		final TreeItem[] children = treeItem.getItems();
		for (int i = 0; i < children.length; ++i) {
			children[i].dispose();
		}
	}

	/**
	 * Closes the main program.
	 */
	void close() {
		workerStop();
//		iconCache.freeResources();
	}

	/**
	 * Copies a file or entire directory structure.
	 * 
	 * @param oldFile
	 *            the location of the old file or directory
	 * @param newFile
	 *            the location of the new file or directory
	 * @return true iff the operation succeeds without errors
	 */
	boolean copyFileStructure(final File oldFile, final File newFile) {
		if (oldFile == null || newFile == null) {
			return false;
		}

		// ensure that newFile is not a child of oldFile or a dupe
		File searchFile = newFile;
		do {
			if (oldFile.equals(searchFile)) {
				return false;
			}
			searchFile = searchFile.getParentFile();
		}
		while (searchFile != null);

		if (oldFile.isDirectory()) {
			/*
			 * Copy a directory
			 */
			if (progressDialog != null) {
				progressDialog.setDetailFile(oldFile, ProgressDialog.COPY);
			}
			if (simulateOnly) {
				// System.out.println(getResourceString("simulate.DirectoriesCreated.text",
				// new Object[] { newFile.getPath() }));
			} else {
				if (!newFile.mkdirs()) {
					return false;
				}
			}
			final File[] subFiles = oldFile.listFiles();
			if (subFiles != null) {
				if (progressDialog != null) {
					progressDialog.addWorkUnits(subFiles.length);
				}
				for (final File subFile : subFiles) {
					final File oldSubFile = subFile;
					final File newSubFile = new File(newFile, oldSubFile.getName());
					if (!copyFileStructure(oldSubFile, newSubFile)) {
						return false;
					}
					if (progressDialog != null) {
						progressDialog.addProgress(1);
						if (progressDialog.isCancelled()) {
							return false;
						}
					}
				}
			}
		} else {
			/*
			 * Copy a file
			 */
			if (simulateOnly) {
				// System.out.println(getResourceString("simulate.CopyFromTo.text",
				// new Object[] { oldFile.getPath(), newFile.getPath() }));
			} else {
				FileReader in = null;
				FileWriter out = null;
				try {
					in = new FileReader(oldFile);
					out = new FileWriter(newFile);

					int count;
					while ((count = in.read()) != -1) {
						out.write(count);
					}
				} catch (final FileNotFoundException e) {
					return false;
				} catch (final IOException e) {
					return false;
				} finally {
					try {
						if (in != null) {
							in.close();
						}
						if (out != null) {
							out.close();
						}
					} catch (final IOException e) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Creates the combo box view.
	 * 
	 * @param parent
	 *            the parent control
	 */
	private void createComboView(final Composite parent, final Object layoutData) {
		combo = new Combo(parent, SWT.NONE);
		combo.setLayoutData(layoutData);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				final String lastText = (String) combo.getData(COMBODATA_LASTTEXT);
				final String text = combo.getText();
				if (text == null) {
					return;
				}
				if (lastText != null && lastText.equals(text)) {
					return;
				}
				combo.setData(COMBODATA_LASTTEXT, text);
				notifySelectedDirectory(new File(text));
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final File[] roots = (File[]) combo.getData(COMBODATA_ROOTS);
				if (roots == null) {
					return;
				}
				final int selection = combo.getSelectionIndex();
				if (selection >= 0 && selection < roots.length) {
					notifySelectedDirectory(roots[selection]);
				}
			}
		});
	}

	/**
	 * Creates the File Menu.
	 * 
	 * @param parent
	 *            the parent menu
	 */
	private void createFileMenu(final Menu parent) {
		final Menu menu = new Menu(parent);
		final MenuItem header = new MenuItem(parent, SWT.CASCADE);
		header.setText(getResourceString("menu.File.text"));
		header.setMenu(menu);

		final MenuItem simulateItem = new MenuItem(menu, SWT.CHECK);
		simulateItem.setText(getResourceString("menu.File.SimulateOnly.text"));
		simulateItem.setSelection(simulateOnly);
		simulateItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				simulateOnly = simulateItem.getSelection();
			}
		});

		final MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(getResourceString("menu.File.Close.text"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				shell.close();
			}
		});
	}

	/**
	 * Creates the Help Menu.
	 * 
	 * @param parent
	 *            the parent menu
	 */
	private void createHelpMenu(final Menu parent) {
		final Menu menu = new Menu(parent);
		final MenuItem header = new MenuItem(parent, SWT.CASCADE);
		header.setText(getResourceString("menu.Help.text"));
		header.setMenu(menu);

		final MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(getResourceString("menu.Help.About.text"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
				box.setText(getResourceString("dialog.About.title"));
				box.setMessage(getResourceString(
						"dialog.About.description",
						new Object[] { System.getProperty("os.name") }));
				box.open();
			}
		});
	}

	/**
	 * Construct the UI
	 * 
	 * @param container
	 *            the ShellContainer managing the Shell we are rendering inside
	 */
	private void createShellContents() {
		shell.setText(getResourceString("Title", new Object[] { "" }));
//		shell.setImage(iconCache.stockImages[iconCache.shellIcon]);
		final Menu bar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(bar);
		createFileMenu(bar);
		createHelpMenu(bar);

		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginHeight = gridLayout.marginWidth = 0;
		shell.setLayout(gridLayout);

		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.widthHint = 185;
		createComboView(shell, gridData);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		createToolBar(shell, gridData);

		final SashForm sashForm = new SashForm(shell, SWT.NONE);
		sashForm.setOrientation(SWT.HORIZONTAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		gridData.horizontalSpan = 3;
		sashForm.setLayoutData(gridData);
		createTreeView(sashForm);
		createTableView(sashForm);
		sashForm.setWeights(new int[] { 2, 5 });

		numObjectsLabel = new Label(shell, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		gridData.widthHint = 185;
		numObjectsLabel.setLayoutData(gridData);

		diskSpaceLabel = new Label(shell, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		diskSpaceLabel.setLayoutData(gridData);
	}

	/**
	 * Creates the file details table.
	 * 
	 * @param parent
	 *            the parent control
	 */
	private void createTableView(final Composite parent) {

		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginHeight = gridLayout.marginWidth = 2;
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);
		tableContentsOfLabel = new Label(composite, SWT.BORDER);
		tableContentsOfLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

		table = new Table(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

		for (int i = 0; i < tableTitles.length; ++i) {
			final TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(tableTitles[i]);
			column.setWidth(tableWidths[i]);
		}
		table.setHeaderVisible(true);
		table.addSelectionListener(new SelectionAdapter() {
			private File[] getSelectedFiles() {
				final TableItem[] items = table.getSelection();
				final File[] files = new File[items.length];

				for (int i = 0; i < items.length; ++i) {
					files[i] = (File) items[i].getData(TABLEITEMDATA_FILE);
				}
				return files;
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent event) {
				doDefaultFileAction(getSelectedFiles());
			}

			@Override
			public void widgetSelected(final SelectionEvent event) {
				notifySelectedFiles(getSelectedFiles());
			}
		});
	}

	/**
	 * Creates the toolbar
	 * 
	 * @param shell
	 *            the shell on which to attach the toolbar
	 * @param layoutData
	 *            the layout data
	 */
	private void createToolBar(final Shell shell, final Object layoutData) {
		toolBar = new ToolBar(shell, SWT.NULL);
		toolBar.setLayoutData(layoutData);
		ToolItem item = new ToolItem(toolBar, SWT.SEPARATOR);
		item = new ToolItem(toolBar, SWT.PUSH);
//		item.setImage(iconCache.stockImages[iconCache.cmdParent]);
		item.setToolTipText(getResourceString("tool.Parent.tiptext"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				doParent();
			}
		});
		item = new ToolItem(toolBar, SWT.PUSH);
//		item.setImage(iconCache.stockImages[iconCache.cmdRefresh]);
		item.setToolTipText(getResourceString("tool.Refresh.tiptext"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				doRefresh();
			}
		});
		final SelectionAdapter unimplementedListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
				box.setText(getResourceString("dialog.NotImplemented.title"));
				box.setMessage(getResourceString("dialog.ActionNotImplemented.description"));
				box.open();
			}
		};

		item = new ToolItem(toolBar, SWT.SEPARATOR);
		item = new ToolItem(toolBar, SWT.PUSH);
//		item.setImage(iconCache.stockImages[iconCache.cmdCut]);
		item.setToolTipText(getResourceString("tool.Cut.tiptext"));
		item.addSelectionListener(unimplementedListener);
		item = new ToolItem(toolBar, SWT.PUSH);
//		item.setImage(iconCache.stockImages[iconCache.cmdCopy]);
		item.setToolTipText(getResourceString("tool.Copy.tiptext"));
		item.addSelectionListener(unimplementedListener);
		item = new ToolItem(toolBar, SWT.PUSH);
//		item.setImage(iconCache.stockImages[iconCache.cmdPaste]);
		item.setToolTipText(getResourceString("tool.Paste.tiptext"));
		item.addSelectionListener(unimplementedListener);

		item = new ToolItem(toolBar, SWT.SEPARATOR);
		item = new ToolItem(toolBar, SWT.PUSH);
//		item.setImage(iconCache.stockImages[iconCache.cmdDelete]);
		item.setToolTipText(getResourceString("tool.Delete.tiptext"));
		item.addSelectionListener(unimplementedListener);
		item = new ToolItem(toolBar, SWT.PUSH);
//		item.setImage(iconCache.stockImages[iconCache.cmdRename]);
		item.setToolTipText(getResourceString("tool.Rename.tiptext"));
		item.addSelectionListener(unimplementedListener);

		item = new ToolItem(toolBar, SWT.SEPARATOR);
		item = new ToolItem(toolBar, SWT.PUSH);
//		item.setImage(iconCache.stockImages[iconCache.cmdSearch]);
		item.setToolTipText(getResourceString("tool.Search.tiptext"));
		item.addSelectionListener(unimplementedListener);
		item = new ToolItem(toolBar, SWT.PUSH);
//		item.setImage(iconCache.stockImages[iconCache.cmdPrint]);
		item.setToolTipText(getResourceString("tool.Print.tiptext"));
		item.addSelectionListener(unimplementedListener);
	}

	/**
	 * Creates the file tree view.
	 * 
	 * @param parent
	 *            the parent control
	 */
	private void createTreeView(final Composite parent) {

		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginHeight = gridLayout.marginWidth = 2;
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);

		treeScopeLabel = new Label(composite, SWT.BORDER);
		treeScopeLabel.setText(FolderViewer.getResourceString("details.AllFolders.text"));
		treeScopeLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

		tree = new Tree(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE);
		tree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

		tree.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(final SelectionEvent event) {
				final TreeItem[] selection = tree.getSelection();
				if (selection != null && selection.length != 0) {
					final TreeItem item = selection[0];
					item.setExpanded(true);
					treeExpandItem(item);
				}
			}

			public void widgetSelected(final SelectionEvent event) {
				final TreeItem[] selection = tree.getSelection();
				if (selection != null && selection.length != 0) {
					final TreeItem item = selection[0];
					final File file = (File) item.getData(TREEITEMDATA_FILE);

					notifySelectedDirectory(file);
				}
			}
		});
		tree.addTreeListener(new TreeAdapter() {
			@Override
			public void treeCollapsed(final TreeEvent event) {
				final TreeItem item = (TreeItem) event.item;
				final Image image = (Image) item.getData(TREEITEMDATA_IMAGECOLLAPSED);
				if (image != null) {
					item.setImage(image);
				}
			}

			@Override
			public void treeExpanded(final TreeEvent event) {
				final TreeItem item = (TreeItem) event.item;
				final Image image = (Image) item.getData(TREEITEMDATA_IMAGEEXPANDED);
				if (image != null) {
					item.setImage(image);
				}
				treeExpandItem(item);
			}
		});
	}

	/**
	 * Deletes a file or entire directory structure.
	 * 
	 * @param oldFile
	 *            the location of the old file or directory
	 * @return true iff the operation succeeds without errors
	 */
	boolean deleteFileStructure(final File oldFile) {
		if (oldFile == null) {
			return false;
		}
		if (oldFile.isDirectory()) {
			/*
			 * Delete a directory
			 */
			if (progressDialog != null) {
				progressDialog.setDetailFile(oldFile, ProgressDialog.DELETE);
			}
			final File[] subFiles = oldFile.listFiles();
			if (subFiles != null) {
				if (progressDialog != null) {
					progressDialog.addWorkUnits(subFiles.length);
				}
				for (final File subFile : subFiles) {
					final File oldSubFile = subFile;
					if (!deleteFileStructure(oldSubFile)) {
						return false;
					}
					if (progressDialog != null) {
						progressDialog.addProgress(1);
						if (progressDialog.isCancelled()) {
							return false;
						}
					}
				}
			}
		}
		if (simulateOnly) {
			// System.out.println(getResourceString("simulate.Delete.text",
			// new Object[] { oldFile.getPath(), oldFile.getPath() }));
			return true;
		} else {
			return oldFile.delete();
		}
	}

	/**
	 * Performs the default action on a set of files.
	 * 
	 * @param files
	 *            the array of files to process
	 */
	void doDefaultFileAction(final File[] files) {
		// only uses the 1st file (for now)
		if (files.length == 0) {
			return;
		}
		final File file = files[0];

		if (file.isDirectory()) {
			notifySelectedDirectory(file);
		} else {
			final String fileName = file.getAbsolutePath();
			if (!Program.launch(fileName)) {
				final MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
				dialog.setMessage(getResourceString("error.FailedLaunch.message", new Object[] { fileName }));
				dialog.setText(shell.getText());
				dialog.open();
			}
		}
	}

	/**
	 * Navigates to the parent directory
	 */
	void doParent() {
		if (currentDirectory == null) {
			return;
		}
		final File parentDirectory = currentDirectory.getParentFile();
		notifySelectedDirectory(parentDirectory);
	}

	/**
	 * Performs a refresh
	 */
	void doRefresh() {
		notifyRefreshFiles(null);
	}

	/**
	 * Gets filesystem root entries
	 * 
	 * @return an array of Files corresponding to the root directories on the platform, may be empty
	 *         but not null
	 */
	File[] getRoots() {
		return File.listRoots();
	}

	/**
	 * Handles deferred Refresh notifications (due to Drag & Drop)
	 */
	void handleDeferredRefresh() {
		if (isDragging || isDropping || !deferredRefreshRequested) {
			return;
		}
		if (progressDialog != null) {
			progressDialog.close();
			progressDialog = null;
		}

		deferredRefreshRequested = false;
		final File[] files = deferredRefreshFiles;
		deferredRefreshFiles = null;

//		shell.setCursor(iconCache.stockCursors[iconCache.cursorWait]);

		/*
		 * Table view: Refreshes information about any files in the list and their children.
		 */
		boolean refreshTable = false;
		if (files != null) {
			for (int i = 0; i < files.length; ++i) {
				final File file = files[i];
				if (file.equals(currentDirectory)) {
					refreshTable = true;
					break;
				}
				final File parentFile = file.getParentFile();
				if ((parentFile != null) && (parentFile.equals(currentDirectory))) {
					refreshTable = true;
					break;
				}
			}
		} else {
			refreshTable = true;
		}
		if (refreshTable) {
			workerUpdate(currentDirectory, true);
		}

		/*
		 * Combo view: Refreshes the list of roots
		 */
		final File[] roots = getRoots();

		if (files == null) {
			boolean refreshCombo = false;
			final File[] comboRoots = (File[]) combo.getData(COMBODATA_ROOTS);

			if ((comboRoots != null) && (comboRoots.length == roots.length)) {
				for (int i = 0; i < roots.length; ++i) {
					if (!roots[i].equals(comboRoots[i])) {
						refreshCombo = true;
						break;
					}
				}
			} else {
				refreshCombo = true;
			}

			if (refreshCombo) {
				combo.removeAll();
				combo.setData(COMBODATA_ROOTS, roots);
				for (int i = 0; i < roots.length; ++i) {
					final File file = roots[i];
					combo.add(file.getPath());
				}
			}
		}

		/*
		 * Tree view: Refreshes information about any files in the list and their children.
		 */
		treeRefresh(roots);

		// Remind everyone where we are in the filesystem
		final File dir = currentDirectory;
		currentDirectory = null;
		notifySelectedDirectory(dir);

//		shell.setCursor(iconCache.stockCursors[iconCache.cursorDefault]);
	}

	/**
	 * Notifies the application components that files must be refreshed
	 * 
	 * @param files
	 *            the files that need refreshing, empty array is a no-op, null refreshes all
	 */
	void notifyRefreshFiles(final File[] files) {
		if (files != null && files.length == 0) {
			return;
		}

		if ((deferredRefreshRequested) && (deferredRefreshFiles != null) && (files != null)) {
			// merge requests
			final File[] newRequest = new File[deferredRefreshFiles.length + files.length];
			System.arraycopy(deferredRefreshFiles, 0, newRequest, 0, deferredRefreshFiles.length);
			System.arraycopy(files, 0, newRequest, deferredRefreshFiles.length, files.length);
			deferredRefreshFiles = newRequest;
		} else {
			deferredRefreshFiles = files;
			deferredRefreshRequested = true;
		}
		handleDeferredRefresh();
	}

	/**
	 * Notifies the application components that a new current directory has been selected
	 * 
	 * @param dir
	 *            the directory that was selected, null is ignored
	 */
	void notifySelectedDirectory(File dir) {
		if (dir == null) {
			return;
		}
		if (currentDirectory != null && dir.equals(currentDirectory)) {
			return;
		}
		currentDirectory = dir;
		notifySelectedFiles(null);

		/*
		 * Shell: Sets the title to indicate the selected directory
		 */
		shell.setText(getResourceString("Title", new Object[] { currentDirectory.getPath() }));

		/*
		 * Table view: Displays the contents of the selected directory.
		 */
		workerUpdate(dir, false);

		/*
		 * Combo view: Sets the combo box to point to the selected directory.
		 */
		final File[] comboRoots = (File[]) combo.getData(COMBODATA_ROOTS);
		int comboEntry = -1;
		if (comboRoots != null) {
			for (int i = 0; i < comboRoots.length; ++i) {
				if (dir.equals(comboRoots[i])) {
					comboEntry = i;
					break;
				}
			}
		}
		if (comboEntry == -1) {
			combo.setText(dir.getPath());
		} else {
			combo.select(comboEntry);
		}

		/*
		 * Tree view: If not already expanded, recursively expands the parents of the specified
		 * directory until it is visible.
		 */
		final Vector /* of File */path = new Vector();
		// Build a stack of paths from the root of the tree
		while (dir != null) {
			path.add(dir);
			dir = dir.getParentFile();
		}
		// Recursively expand the tree to get to the specified directory
		TreeItem[] items = tree.getItems();
		TreeItem lastItem = null;
		for (int i = path.size() - 1; i >= 0; --i) {
			final File pathElement = (File) path.elementAt(i);

			// Search for a particular File in the array of tree items
			// No guarantee that the items are sorted in any recognizable
			// fashion, so we'll
			// just sequential scan. There shouldn't be more than a few thousand
			// entries.
			TreeItem item = null;
			for (int k = 0; k < items.length; ++k) {
				item = items[k];
				if (item.isDisposed()) {
					continue;
				}
				final File itemFile = (File) item.getData(TREEITEMDATA_FILE);
				if (itemFile != null && itemFile.equals(pathElement)) {
					break;
				}
			}
			if (item == null) {
				break;
			}
			lastItem = item;
			if (i != 0 && !item.getExpanded()) {
				treeExpandItem(item);
				item.setExpanded(true);
			}
			items = item.getItems();
		}
		tree.setSelection((lastItem != null) ? new TreeItem[] { lastItem } : new TreeItem[0]);
	}

	/**
	 * Notifies the application components that files have been selected
	 * 
	 * @param files
	 *            the files that were selected, null or empty array indicates no active selection
	 */
	void notifySelectedFiles(final File[] files) {
		/*
		 * Details: Update the details that are visible on screen.
		 */
		if ((files != null) && (files.length != 0)) {
			numObjectsLabel.setText(getResourceString("details.NumberOfSelectedFiles.text", new Object[] { new Integer(
					files.length) }));
			long fileSize = 0L;
			for (int i = 0; i < files.length; ++i) {
				fileSize += files[i].length();
			}
			diskSpaceLabel.setText(getResourceString("details.FileSize.text", new Object[] { new Long(fileSize) }));
		} else {
			// No files selected
			diskSpaceLabel.setText("");
			if (currentDirectory != null) {
				final int numObjects = getDirectoryList(currentDirectory).length;
				numObjectsLabel.setText(getResourceString(
						"details.DirNumberOfObjects.text",
						new Object[] { new Integer(numObjects) }));
			} else {
				numObjectsLabel.setText("");
			}
		}
	}

	/**
	 * Opens the main program.
	 */
	public Shell open(final Display display) {
		// Create the window
		this.display = display;
//		iconCache.initResources(display);
		shell = new Shell();
		createShellContents();
		notifyRefreshFiles(null);
		shell.open();
		return shell;
	}

	/**
	 * Handles expand events on a tree item.
	 * 
	 * @param item
	 *            the TreeItem to fill in
	 */
	private void treeExpandItem(final TreeItem item) {
//		shell.setCursor(iconCache.stockCursors[iconCache.cursorWait]);
		final Object stub = item.getData(TREEITEMDATA_STUB);
		if (stub == null) {
			treeRefreshItem(item, true);
		}
//		shell.setCursor(iconCache.stockCursors[iconCache.cursorDefault]);
	}

	/**
	 * Initializes a folder item.
	 * 
	 * @param item
	 *            the TreeItem to initialize
	 * @param folder
	 *            the File associated with this TreeItem
	 */
	private void treeInitFolder(final TreeItem item, final File folder) {
		item.setText(folder.getName());
//		item.setImage(iconCache.stockImages[iconCache.iconClosedFolder]);
		item.setData(TREEITEMDATA_FILE, folder);
//		item.setData(TREEITEMDATA_IMAGEEXPANDED, iconCache.stockImages[iconCache.iconOpenFolder]);
//		item.setData(TREEITEMDATA_IMAGECOLLAPSED, iconCache.stockImages[iconCache.iconClosedFolder]);
	}

	/**
	 * Initializes a volume item.
	 * 
	 * @param item
	 *            the TreeItem to initialize
	 * @param volume
	 *            the File associated with this TreeItem
	 */
	private void treeInitVolume(final TreeItem item, final File volume) {
		item.setText(volume.getPath());
//		item.setImage(iconCache.stockImages[iconCache.iconClosedDrive]);
		item.setData(TREEITEMDATA_FILE, volume);
//		item.setData(TREEITEMDATA_IMAGEEXPANDED, iconCache.stockImages[iconCache.iconOpenDrive]);
//		item.setData(TREEITEMDATA_IMAGECOLLAPSED, iconCache.stockImages[iconCache.iconClosedDrive]);
	}

	/*
	 * This worker updates the table with file information in the background. <p> Implementation
	 * notes: <ul> <li> It is designed such that it can be interrupted cleanly. <li> It uses
	 * asyncExec() in some places to ensure that SWT Widgets are manipulated in the right thread.
	 * Exclusive use of syncExec() would be inappropriate as it would require a pair of context
	 * switches between each table update operation. </ul> </p>
	 */

	/**
	 * Traverse the entire tree and update only what has changed.
	 * 
	 * @param roots
	 *            the root directory listing
	 */
	private void treeRefresh(final File[] masterFiles) {
		final TreeItem[] items = tree.getItems();
		int masterIndex = 0;
		int itemIndex = 0;
		for (int i = 0; i < items.length; ++i) {
			final TreeItem item = items[i];
			final File itemFile = (File) item.getData(TREEITEMDATA_FILE);
			if ((itemFile == null) || (masterIndex == masterFiles.length)) {
				// remove bad item or placeholder
				item.dispose();
				continue;
			}
			final File masterFile = masterFiles[masterIndex];
			final int compare = compareFiles(masterFile, itemFile);
			if (compare == 0) {
				// same file, update it
				treeRefreshItem(item, false);
				++itemIndex;
				++masterIndex;
			} else if (compare < 0) {
				// should appear before file, insert it
				final TreeItem newItem = new TreeItem(tree, SWT.NULL, itemIndex);
				treeInitVolume(newItem, masterFile);
				new TreeItem(newItem, SWT.NULL); // placeholder child item to get "expand" button
				++itemIndex;
				++masterIndex;
				--i;
			} else {
				// should appear after file, delete stale item
				item.dispose();
			}
		}
		for (; masterIndex < masterFiles.length; ++masterIndex) {
			final File masterFile = masterFiles[masterIndex];
			final TreeItem newItem = new TreeItem(tree, SWT.NULL);
			treeInitVolume(newItem, masterFile);
			new TreeItem(newItem, SWT.NULL); // placeholder child item to get
			// "expand" button
		}
	}

	/**
	 * Traverse an item in the tree and update only what has changed.
	 * 
	 * @param dirItem
	 *            the tree item of the directory
	 * @param forcePopulate
	 *            true iff we should populate non-expanded items as well
	 */
	private void treeRefreshItem(final TreeItem dirItem, final boolean forcePopulate) {
		final File dir = (File) dirItem.getData(TREEITEMDATA_FILE);

		if (!forcePopulate && !dirItem.getExpanded()) {
			// Refresh non-expanded item
			if (dirItem.getData(TREEITEMDATA_STUB) != null) {
				treeItemRemoveAll(dirItem);
				new TreeItem(dirItem, SWT.NULL); // placeholder child item to
				// get "expand" button
				dirItem.setData(TREEITEMDATA_STUB, null);
			}
			return;
		}
		// Refresh expanded item
		dirItem.setData(TREEITEMDATA_STUB, this); // clear stub flag

		/* Get directory listing */
		final File[] subFiles = (dir != null) ? FolderViewer.getDirectoryList(dir) : null;
		if (subFiles == null || subFiles.length == 0) {
			/* Error or no contents */
			treeItemRemoveAll(dirItem);
			dirItem.setExpanded(false);
			return;
		}

		/* Refresh sub-items */
		final TreeItem[] items = dirItem.getItems();
		final File[] masterFiles = subFiles;
		int masterIndex = 0;
		int itemIndex = 0;
		File masterFile = null;
		for (int i = 0; i < items.length; ++i) {
			while ((masterFile == null) && (masterIndex < masterFiles.length)) {
				masterFile = masterFiles[masterIndex++];
				if (!masterFile.isDirectory()) {
					masterFile = null;
				}
			}

			final TreeItem item = items[i];
			final File itemFile = (File) item.getData(TREEITEMDATA_FILE);
			if ((itemFile == null) || (masterFile == null)) {
				// remove bad item or placeholder
				item.dispose();
				continue;
			}
			final int compare = compareFiles(masterFile, itemFile);
			if (compare == 0) {
				// same file, update it
				treeRefreshItem(item, false);
				masterFile = null;
				++itemIndex;
			} else if (compare < 0) {
				// should appear before file, insert it
				final TreeItem newItem = new TreeItem(dirItem, SWT.NULL, itemIndex);
				treeInitFolder(newItem, masterFile);
				new TreeItem(newItem, SWT.NULL); // add a placeholder child
				// item so we get the
				// "expand" button
				masterFile = null;
				++itemIndex;
				--i;
			} else {
				// should appear after file, delete stale item
				item.dispose();
			}
		}
		while ((masterFile != null) || (masterIndex < masterFiles.length)) {
			if (masterFile != null) {
				final TreeItem newItem = new TreeItem(dirItem, SWT.NULL);
				treeInitFolder(newItem, masterFile);
				new TreeItem(newItem, SWT.NULL); // add a placeholder child
				// item so we get the
				// "expand" button
				if (masterIndex == masterFiles.length) {
					break;
				}
			}
			masterFile = masterFiles[masterIndex++];
			if (!masterFile.isDirectory()) {
				masterFile = null;
			}
		}
	}

	/**
	 * Adds a file's detail information to the directory list
	 */
	private void workerAddFileDetails(final File file) {
		final String nameString = file.getName();
		final String dateString = dateFormat.format(new Date(file.lastModified()));
		final String sizeString;
		final String typeString;
//		final Image iconImage;

		if (file.isDirectory()) {
			typeString = getResourceString("filetype.Folder");
			sizeString = "";
//			iconImage = iconCache.stockImages[iconCache.iconClosedFolder];
		} else {
			sizeString = getResourceString("filesize.KB", new Object[] { new Long((file.length() + 512) / 1024) });

			final int dot = nameString.lastIndexOf('.');
			if (dot != -1) {
				final String extension = nameString.substring(dot);
				final Program program = Program.findProgram(extension);
				if (program != null) {
					typeString = program.getName();
//					iconImage = iconCache.getIconFromProgram(program);
				} else {
					typeString = getResourceString("filetype.Unknown", new Object[] { extension.toUpperCase() });
//					iconImage = iconCache.stockImages[iconCache.iconFile];
				}
			} else {
				typeString = getResourceString("filetype.None");
//				iconImage = iconCache.stockImages[iconCache.iconFile];
			}
		}
		final String[] strings = new String[] { nameString, sizeString, typeString, dateString };

		display.syncExec(new Runnable() {
			public void run() {
				// guard against the shell being closed before this runs
				if (shell.isDisposed()) {
					return;
				}
				final TableItem tableItem = new TableItem(table, 0);
				tableItem.setText(strings);
//				tableItem.setImage(iconImage);
				tableItem.setData(TABLEITEMDATA_FILE, file);
			}
		});
	}

	/**
	 * Updates the table's contents
	 */
	private void workerExecute() {
		File[] dirList;
		// Clear existing information
		display.syncExec(new Runnable() {
			public void run() {
				tableContentsOfLabel.setText(FolderViewer.getResourceString(
						"details.ContentsOf.text",
						new Object[] { workerStateDir.getPath() }));
				table.removeAll();
				table.setData(TABLEDATA_DIR, workerStateDir);
			}
		});
		dirList = getDirectoryList(workerStateDir);

		for (int i = 0; (!workerCancelled) && (i < dirList.length); i++) {
			workerAddFileDetails(dirList[i]);
		}

	}

	/**
	 * Stops the worker and waits for it to terminate.
	 */
	void workerStop() {
		if (workerThread == null) {
			return;
		}
		synchronized (workerLock) {
			workerCancelled = true;
			workerStopped = true;
			workerLock.notifyAll();
		}
		while (workerThread != null) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Notifies the worker that it should update itself with new data. Cancels any previous
	 * operation and begins a new one.
	 * 
	 * @param dir
	 *            the new base directory for the table, null is ignored
	 * @param force
	 *            if true causes a refresh even if the data is the same
	 */
	void workerUpdate(final File dir, final boolean force) {
		if (dir == null) {
			return;
		}
		if ((!force) && (workerNextDir != null) && (workerNextDir.equals(dir))) {
			return;
		}

		synchronized (workerLock) {
			workerNextDir = dir;
			workerStopped = false;
			workerCancelled = true;
			workerLock.notifyAll();
		}
		if (workerThread == null) {
			workerThread = new Thread(workerRunnable);
			workerThread.start();
		}
	}
}