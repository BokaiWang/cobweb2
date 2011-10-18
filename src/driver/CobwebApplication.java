/*
 * $$$$$: Comments by Liang $$$$$$: Codes modified and/or added by Liang
 */

// / test
package driver;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import cobweb.LocalUIInterface.TickEventListener;
import cobweb.UIInterface;
import cobweb.UIInterface.MouseMode;
import cobweb.UIInterface.UIClient;
import cobweb.ViewerClosedCallback;
import cobweb.ViewerPlugin;
import driver.config.GUI;

/**
 * This class consists of methods to allow the user to use the Cobweb simulation
 * tool.  It implements all necessary methods defined by the UIClient class, and
 * makes use of the JFrame class.
 * 
 * @author Liang
 *
 */
public class CobwebApplication extends JFrame implements UIClient {

	private static final String WINDOW_TITLE = "COBWEB 2";

	private static final String MODIFY_THIS_FILE = "Modify Simulation File";

	private static final String MODIFY_CURRENT_DATA = "Modify Simulation";

	private static final long serialVersionUID = 2112476687880153089L;

	// $$$$$$ A file copy method. Feb 11
	/**
	 * Copies the contents from the source file to the destination file.  If 
	 * the destination file does not exist, it will be created.
	 * 
	 * @param src The file name of the source file to be copied from
	 * @param dest The file name of the destination file to be copied to 
	 * @throws IOException 
	 */
	public static void copyFile(String src, String dest) throws IOException {
		File sourceFile = new File(src);
		File destFile = new File(dest);

		if (!destFile.exists())
			destFile.createNewFile();

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null)
				source.close();
			if (destination != null)
				destination.close();
		}
	}

	// $$$$$$ Add a greeting string for the textWindow. Mar 25
	public static final String GREETINGS = "Welcome to COBWEB 2";

	private String midFile; // $$$$$$ added for supporting "Modify Current Data", to temporary save the name when adding
	// a file. Feb 14

	private String currentFile; // $$$$$$ added for saving current used file name. Mar 14

	private DisplayPanel displayPanel; // $$$$$$ added to avoid duplicately information lines shown in textWindow. Apr
	// 1

	private PauseButton pauseButton;

	private StepButton stepButton;

	public JTextField tickField;

	private JMenuItem stoneMenu;

	private JMenuItem observeMenu;

	private JMenu foodMenu;

	private JMenu agentMenu;
	private boolean invokedByModify; // $$$$$$ The value is determined by whether a "Test Data" window is invoked by one
	// of "Modify This File"
	// and "Modify Current Data" or by one of "Open", "Create New Data" and "Retrieve Default Data". Mar 14

	// $$$$$$ Reserved file names. Feb 8
	public static final String INITIAL_OR_NEW_INPUT_FILE_NAME = "initial_or_new_input_(reserved)";
	public static final String DEFAULT_DATA_FILE_NAME = "default_data_(reserved)";

	public static final String CURRENT_DATA_FILE_NAME = "current_data_(reserved)";
	// $$$$$$ Frequently-used file suffixes. Feb 11
	public static final String CONFIG_FILE_EXTENSION = ".xml";

	public static final String TEMPORARY_FILE_EXTENSION = ".cwtemp";

	private int modifyingDefaultDataReminder = 0; // $$$$$$ added for openCurrentFile() method. Mar 25

	private Logger myLogger = Logger.getLogger("COBWEB2");

	private JPanel mainPanel;
	private JLabel tickDisplay;

	private JPanel controls;


	// constructor
	public CobwebApplication() {
		super(WINDOW_TITLE);

		/*** $$$$$$ For cancelling the output info text window, remove some codes in the field to the below block. Apr 22 */
		myLogger.info(GREETINGS);
		myLogger.info("JVM Memory: " + Runtime.getRuntime().maxMemory() / 1024 + "KB");

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				CobwebApplication.this.quitApplication();
			}
		});

		setLayout(new BorderLayout());
		setSize(580, 650);

		// Create the various widgits to make the application go.

		JMenuBar myMenuBar = makeMenuBar();

		setJMenuBar(myMenuBar);

		setLocationRelativeTo(null);

		setVisible(true);
	}

	/**
	 * Creates the about dialog box, which contains information pertaining 
	 * to the Cobweb version being used, and the date it was last modified.
	 */
	public void aboutDialog() {
		final javax.swing.JDialog whatDialog = new javax.swing.JDialog(GUI.frame, // $$$$$$ change from Dialog mult. Feb
				// 18
				"About Cobweb", true); // $$$$$$ change from "this" to "GUI.frame" specifically for MS Windows. Feb 22
		whatDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // $$$$$$ added on Feb 18

		JPanel info = new JPanel();
		info.setAlignmentX(CENTER_ALIGNMENT);
		info.add(new JLabel("<html><center>COBWEB2 2003/2011<br/>version: <br/>" 
				+  Versionator.getVersion().replace(" ", "<br/>") 
				+ "</center></html>"));

		JPanel term = new JPanel();
		JButton close = new JButton("Close");
		term.add(close);

		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				whatDialog.setVisible(false);
			}
		});

		whatDialog.setLayout(new BorderLayout());
		whatDialog.add(info, BorderLayout.CENTER);
		whatDialog.add(term, BorderLayout.SOUTH);
		whatDialog.setSize(300, 150);
		//whatDialog.pack();
		whatDialog.setVisible(true);
	}

	// $$$$$$ Implement "create New Data". Mar 14
	/**
	 * Opens an initial simulation settings file using the simulation settings 
	 * window.  The user can modify the simulation settings and save the 
	 * settings to a new file.  The method is invoked when the user selects 
	 * "Create New Data" located under "File" in the main tool bar.
	 * 
	 * @see CobwebApplication#onMenuCreateNew()
	 */
	public void createNewData() {
		// $$$$$ a file named as the below name will be automatically created or modified when everytime running the
		// $$$$$ following code. Please refer to GUI.GUI.close.addActionListener, "/* write UI info to xml file */". Jan
		// 24
		String newInput = INITIAL_OR_NEW_INPUT_FILE_NAME + CONFIG_FILE_EXTENSION; // $$$$$$ added for implementing
		// "Modify Current Data". Feb 12
		GUI.createAndShowGUI(this, newInput, false); // $$$$$$ change the name from original "input.xml". Jan 31
		if (uiPipe == null) {
			setCurrentFile(newInput);
		} // $$$$$$ added on Mar 14
		// $$$$$$ Added to check if the new data file is hidden. Feb 22
		File inf = new File(newInput);
		if (inf.isHidden() != false || ((inf.exists() != false) && (inf.canWrite() == false))) {
			JOptionPane
			.showMessageDialog(
					GUI.frame, // $$$$$$ change from "this" to "GUI.frame" specifically for MS Windows. Feb 22
					"Caution:  The new data file \""
					+ newInput
					+ "\" is NOT allowed to be modified.\n"
					+ "\n                  Any modification of this data file will be neither implemented nor saved.");
		}
		/*
		 * // resets the counter to 0. There is no other work needing to be // done in the UI because // the UI
		 * comprises of the interface. The simulation itself will // be reloaded. // $$$$$$ Modified on Feb 28 if
		 * (uiPipe != null) { uiPipe.reset(); refreshAll(uiPipe); }
		 */
	}

	/**
	 * Creates a dialog box with contact information about a specified person
	 * in the credits menu.
	 * 
	 * @param parentDialog The credits dialog box that invoked the creation of this dialog box
	 * @param S The contact information that will be shown in the dialog box.
	 * @param length The length of the dialog box in pixels
	 * @param width The width of the dialog box in pixels
	 * @see CobwebApplication#creditsDialog()
	 */
	private void creditDialog(JDialog parentDialog, String[] S, int length, int width) { // $$$$$$ modified on Feb 22

		final javax.swing.JDialog creditDialog = new javax.swing.JDialog(parentDialog, // $$$$$$ change from "this" to
				// parentDialog. Feb 22
				"Click on Close to continue", true);

		JPanel credit = new JPanel();
		for (int i = 0; i < S.length; ++i) {
			credit.add(new JLabel(S[i]), "Center");
		}

		JPanel term = new JPanel();
		/* new */
		// $$$$$$ Silence the unused "Open" button. Feb 22
		// Button choosefile = new Button("Open");
		// term.add(choosefile);
		JButton close = new JButton("Close");
		term.add(close);
		/* new */
		// choosefile.addActionListener(new event.ActionListener() {
		// public void actionPerformed(event.ActionEvent evt) { /* openFileDialog() ; */
		// }
		// });
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				creditDialog.setVisible(false);
			}
		});

		creditDialog.add(credit, "Center");
		creditDialog.add(term, "South");

		creditDialog.setSize(length, width);
		creditDialog.setVisible(true);

	}

	/**
	 * The credits dialog box that is created when the user selects "Credits" 
	 * located under "Help" in the main tool bar.  It contains a list of 
	 * buttons for important people that can be contacted for more information 
	 * about Cobweb.  The information can be accessed by clicking on the buttons.
	 */
	public void creditsDialog() {
		final javax.swing.JDialog theDialog = new javax.swing.JDialog(GUI.frame, "Credits", // $$$$$$ change from Dialog
				// mult. Feb 18
				true); // $$$$$$ change from "this" to "GUI.frame" specifically for MS Windows. Feb 22
		theDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // $$$$$$ added on Feb 18

		JPanel credit = new JPanel();
		JButton brad = new JButton("Brad Bass, PhD");
		JButton jeff = new JButton("Jeff Hill");
		JButton jin = new JButton("Jin Soo Kang");
		credit.add(new JLabel("Coordinator"));
		credit.add(brad);
		credit.add(new JLabel("_______________"));
		credit.add(new JLabel("Programmers"));
		credit.add(jeff);
		credit.add(jin);

		JPanel term = new JPanel();
		JButton close = new JButton("Close");
		term.add(close);

		brad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String[] S = { "Brad Bass, PhD", "Adaptations and Impacts Research Group",
						"Environment Canada at Univ of Toronto", "Inst. for Environmental Studies",
						"33 Willcocks Street", "Toronto, Ont M5S 3E8 CANADA",
						"TEL: (416) 978-6285  FAX: (416) 978-3884", "brad.bass@ec.gc.ca" };
				creditDialog(theDialog, S, 300, 300);
			}
		});

		jeff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String[] S = { "Main Structural Programming By", "", "Jeff Hill", "oni1@home.com" };

				CobwebApplication.this.creditDialog(theDialog, S, 250, 150); // $$$$$$ change from "this" to
				// parentDialog. Feb 22
			}
		});

		jin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String[] S = { "Update & Additional Programming By", "", "Jin Soo Kang",
						"Undergraduate, Computer Science", "University of Toronto", "jin.kang@utoronto.ca",
				"[2000 - 2001]" };

				CobwebApplication.this.creditDialog(theDialog, S, 300, 250); // $$$$$$ change from "this" to
				// parentDialog. Feb 22
			}
		});

		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				theDialog.setVisible(false);
			}
		});

		theDialog.add(credit, "Center");
		theDialog.add(term, "South");
		theDialog.setSize(150, 265);
		theDialog.setVisible(true);

	}

	/**
	 * Returns the name of the file being used by the current simulation.
	 * 
	 * @return The name of the current file being used for the current simulation.
	 */
	// $$$$$$ Implemented on Mar 14
	public String getCurrentFile() {
		return currentFile;
	}

	/**
	 * Returns the user interface pipe being used.
	 * 
	 * @return User Interface Pipe
	 */
	// $$$$$$ get UI. Mar 14
	public UIInterface getUI() {
		return uiPipe;
	}

	// $$$$$$ Implemented on Mar 14
	public boolean isInvokedByModify() {
		return invokedByModify;
	}

	public boolean isReadyToRefresh() {
		return displayPanel != null && displayPanel.isReadyToRefresh();
	}

	/**
	 * Provides the user interface pipe with the location of the log file to 
	 * write to.
	 * 
	 * @param filePath The file path and file name.
	 */
	public void logFile(String filePath) {
		if (uiPipe != null) {
			try {
				uiPipe.log(filePath);
			} catch (Exception ex) {
				throw new CobwebUserException("Cannot save log file!", ex);
			}
		}
	}

	/**
	 * Allows the user to select the log file to write to.
	 */
	public void logFileDialog() {
		FileDialog theDialog = new FileDialog(this, // $$$$$$ modified from "this". Feb 29
				"Choose a file to save log to", FileDialog.SAVE);
		theDialog.setVisible(true);
		if (theDialog.getFile() != null) {
			logFile(theDialog.getDirectory() + theDialog.getFile());
		}
	}

	/**
	 * Creates the main menu bar, which contains all options to allow the user 
	 * to modify the simulation, save the simulation, etc.
	 * 
	 * @return The menu bar object.
	 */
	private JMenuBar makeMenuBar() {
		// Build the menu items
		JMenuItem openMenu = new JMenuItem("Open");
		openMenu.setActionCommand("Open");
		openMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuOpen();
			}
		});

		// $$$$$$ Add "Set Default Data" menu. Feb 21
		JMenuItem setMenu = new JMenuItem("Set Default Data");
		setMenu.setActionCommand("Set Default Data");
		setMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuSetDefault();
			}
		});


		// $$$$$$ Add "Save Sample Population" menu.
		JMenuItem saveSamplePopMenu = new JMenuItem("Save Sample Population");
		saveSamplePopMenu.setActionCommand("Save Sample Population");
		saveSamplePopMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuSaveSample();
			}
		});

		JMenuItem insertSamplePopMenu = new JMenuItem("Insert Sample Population");
		insertSamplePopMenu.setActionCommand("Insert Sample Population");
		insertSamplePopMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuInsertSample();
			}
		});

		// $$$$$$ Add "Retrieve Default Data" menu. Feb 4
		JMenuItem defaultMenu = new JMenuItem("Retrieve Default Data");
		defaultMenu.setActionCommand("Retrieve Default Data");
		defaultMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuRetrieveDefault();
			}
		});
		// $$$$$$ Add "Modify Current Data" menu. Feb 12
		JMenuItem currentDataMenu = new JMenuItem(MODIFY_CURRENT_DATA);
		currentDataMenu.setActionCommand(MODIFY_CURRENT_DATA);
		currentDataMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuModifyCurrent();
			}
		});

		JMenuItem NewDataFileMenu = new JMenuItem("Create New Data");
		NewDataFileMenu.setActionCommand("Create New Data");
		NewDataFileMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuCreateNew();
			}
		});
		JMenuItem modifyMenu = new JMenuItem(MODIFY_THIS_FILE);
		modifyMenu.setActionCommand(MODIFY_THIS_FILE);
		modifyMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuModifyThis();
			}
		});
		JMenuItem saveMenu = new JMenuItem("Save");
		saveMenu.setActionCommand("Save");
		saveMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuSave();
			}
		});
		JMenuItem logMenu = new JMenuItem("Log");
		logMenu.setActionCommand("Log");
		logMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuLog();
			}
		});
		JMenuItem quitMenu = new JMenuItem("Quit");
		quitMenu.setActionCommand("Quit");
		quitMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuQuit();
			}
		});
		JMenuItem reportMenu = new JMenuItem("Report");
		reportMenu.setActionCommand("Report");
		reportMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuReport();
			}
		});

		JMenuItem aboutMenu = new JMenuItem("About");
		aboutMenu.setActionCommand("About");
		aboutMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuAbout();
			}
		});
		JMenuItem creditsMenu = new JMenuItem("Credits");
		creditsMenu.setActionCommand("Credits");
		creditsMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuCredits();
			}
		});

		observeMenu = new JMenuItem("Observation Mode");
		observeMenu.setActionCommand("Observation Mode");
		observeMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuObserve();
			}
		});

		stoneMenu = new JMenuItem("Select Stones");
		stoneMenu.setActionCommand("Select Stones");
		stoneMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuStones();
			}
		});

		foodMenu = new JMenu("Select Food");
		agentMenu = new JMenu("Select Agents");

		JMenuItem removeStones = new JMenuItem("Remove All Stones");
		removeStones.setActionCommand("Remove All Stones");
		removeStones.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuRemoveStones();
			}
		});

		JMenuItem removeFood = new JMenuItem("Remove All Food");
		removeFood.setActionCommand("Remove All Food");
		removeFood.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuRemoveFood();
			}
		});

		JMenuItem removeAgents = new JMenuItem("Remove All Agents");
		removeAgents.setActionCommand("Remove All Agents");
		removeAgents.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuRemoveAgents();
			}
		});
		// $$$$$$ Added on Feb 29
		JMenuItem removeWaste = new JMenuItem("Remove All Waste");
		removeWaste.setActionCommand("Remove All Waste");
		removeWaste.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuRemoveWaste();
			}
		});

		JMenuItem removeAll = new JMenuItem("Remove All");
		removeAll.setActionCommand("Remove All");
		removeAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMenuRemoveAll();
			}
		});

		// Assemble the items into menus
		JMenu EditMenu = new JMenu("Edit");
		EditMenu.add(observeMenu);
		EditMenu.add(stoneMenu);
		EditMenu.add(agentMenu);
		EditMenu.add(foodMenu);
		EditMenu.add(new JSeparator());
		EditMenu.add(removeStones);
		EditMenu.add(removeFood);
		EditMenu.add(removeAgents);
		EditMenu.add(removeWaste); // $$$$$$ added on Feb 29
		EditMenu.add(removeAll);

		JMenu fileMenu = new JMenu("File");
		fileMenu.add(openMenu);
		fileMenu.add(NewDataFileMenu);
		fileMenu.add(modifyMenu);

		// $$$$$$ Add "Retrieve Default Data" menu. Feb 4
		fileMenu.add(defaultMenu);
		// $$$$$$ Add "Modify Current Data" menu. Feb 12
		fileMenu.add(currentDataMenu);
		// $$$$$$ Add "Set Default Data" menu. Feb 21
		fileMenu.add(new JSeparator());
		fileMenu.add(setMenu);

		fileMenu.add(new JSeparator());
		fileMenu.add(saveSamplePopMenu);
		fileMenu.add(insertSamplePopMenu);

		fileMenu.add(new JSeparator());
		fileMenu.add(saveMenu);
		fileMenu.add(reportMenu);
		fileMenu.add(logMenu);
		fileMenu.add(new JSeparator());
		fileMenu.add(quitMenu);

		JMenu helpMenu = new JMenu("Help");

		helpMenu.add(aboutMenu);
		// helpMenu.add(new JSeparator()); // $$$$$$ silenced on Mar 28
		helpMenu.add(creditsMenu);

		viewMenu = new JMenu("View");

		// Assemble the menus into a menu bar
		JMenuBar myMenuBar = new JMenuBar();
		myMenuBar.add(fileMenu);
		myMenuBar.add(EditMenu);
		myMenuBar.add(viewMenu);
		myMenuBar.add(helpMenu);
		return myMenuBar;
	}

	/**
	 * Copies the current simulation data being used to a temporary file, which 
	 * can be modified and saved by the user.  
	 * 
	 * <p>Used when the user selects "File" -> "Modify Simulation"
	 * 
	 * @see CobwebApplication#onMenuModifyCurrent()
	 */
	// $$$$$$ Added for "Modify Current Data" menu. This method modifies only the data, but NOT the input file. Feb 12
	private void openCurrentData() {
		String currentData = CURRENT_DATA_FILE_NAME + TEMPORARY_FILE_EXTENSION;
		File cf = new File(currentData);
		cf.deleteOnExit();
		// $$$$$$ Implement a medium file for modification. Feb 12
		midFile = getCurrentFile(); // $$$$$$ added on Mar 14
		if (midFile.equals(currentData) == false) { // $$$$$ if not accessed by choosing "Modify Current Data" menu
			try {
				copyFile(midFile, currentData);
			} catch (Exception ex) {
				throw new CobwebUserException("Cannot open config file", ex);
			}
		}
		GUI.createAndShowGUI(this, currentData, true);
	}

	/**
	 * Opens the simulation settings window with the current simulation file 
	 * data.  The user can modify and save the file here.  If the user tries 
	 * to overwrite data found in the default data file, a dialog box will be 
	 * created to tell the user the proper way to create new default data.
	 *   
	 * <p>Used when the user selects "File" -> "Modify Simulation File"
	 * 
	 * @see CobwebApplication#onMenuModifyThis()
	 */
	public void openCurrentFile() { // $$$$$ "Modify This File" method
		// $$$$$ a file named as the below name will be automatically created or modified when everytime running the
		// following code. Please refer to GUI.GUI.close.addActionListener, "/* write UI info to xml file */". Jan 24
		// $$$$$$ modify a file previously accessed by "Modify Current Data". Mar 18
		if (getCurrentFile().equals(CURRENT_DATA_FILE_NAME + TEMPORARY_FILE_EXTENSION)) {
			try {
				copyFile(getCurrentFile(), midFile);
			} catch (Exception ex) {
				// $$$$$$ added on Feb 21
				Logger.getLogger("COBWEB2").log(Level.WARNING, "Modify file failed", ex);
				JOptionPane.showMessageDialog(this, // $$$$$$ modified from "this". Feb 29
						"Modify this file failed: " + ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
			}
			setCurrentFile(midFile);
		}

		GUI.createAndShowGUI(this, getCurrentFile(), true); // $$$$$$ modified on Mar 14

		// $$$$$$ Added on Mar 25
		if (getCurrentFile().equals(DEFAULT_DATA_FILE_NAME + TEMPORARY_FILE_EXTENSION)) {
			if (modifyingDefaultDataReminder == 0) {
				// $$$$$ Ask if need to remind again. Mar 25
				Object[] options = { "Yes, please", "No, thanks" };
				int n = JOptionPane.showOptionDialog(GUI.frame,
						"Default data would not be affected by \"Modify This File\" menu.\n"
						+ "\nTo set up new default data, please use \"Set Default Data\" menu instead.\n"
						+ "\n\nWould you like to be reminded next time?", "Modifying Default Data Reminder",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, // do not use a custom Icon
						options, // the titles of buttons
						options[0]); // default button titl

				modifyingDefaultDataReminder = n;
			}
		}
	}

	public void openFile(SimulationConfig p) {
		if (uiPipe.isRunning())
			uiPipe.pause();
		uiPipe.load(p);
	}

	@Override
	public void fileOpened(SimulationConfig conf) {

		UIsettings();

		displayPanel.setUI(uiPipe);

		File f = new File(conf.getFilename());
		setTitle(WINDOW_TITLE + "  - " + f.getName());

		uiPipe.setRunnable(true);	
	}

	/**
	 *Opens an existing xml file, selected by the user through a dialog box, 
	 *which contains all the information for a simulation environment.
	 *
	 *<p> Used when the user selects "File" -> "Open"
	 *
	 *@see CobwebApplication#onMenuOpen()
	 */
	public void openFileDialog() {
		FileDialog theDialog = new FileDialog(GUI.frame, // $$$$$$ modified from "this". Feb 29
				"Open a State File", FileDialog.LOAD);
		theDialog.setVisible(true);
		String directory = theDialog.getDirectory();
		String file = theDialog.getFile();

		// $$$$$$ Modify the following block to check whether the file exists. Feb 21 $$$$$$ Remodified on Mar 14
		if (file != null && directory != null) {
			File of = new File(directory + file);
			if (of.exists() != false) {
				setCurrentFile(directory + file); // $$$$$ donot need this line if using the below block instead
				/*
				 * $$$$$ If wanting the "Test Data" window to show up, use the below block instead. Feb 28 // silence
				 * this block on Mar 31 Parser p = new Parser(getCurrentFile()); // $$$$$$ Changed on Feb 29 if (uiPipe
				 * != null) { uiPipe.killScheduler(); uiPipe = null; } //uiPipe.killScheduler(); //uiPipe = null; if
				 * (GUI.frame.isVisible() == true) GUI.frame.dispose(); // $$$$$ for allowing only one "Test Data"
				 * window to show up. Feb 28 //if (tickField != null && !tickField.getText().equals(""))
				 * {tickField.setText("");} // $$$$$$ reset tickField. Mar 14 CobwebApplication.this.openFile(p);
				 */

				// /* $$$$$$ If NOT wanting the "Test Data" window to show up, use the above block instead. Feb 28 //
				// implement this block on Mar 31
				if (GUI.frame != null && GUI.frame.isVisible() == true) {
					GUI.frame.dispose(); // $$$$$ for allowing only one "Test Data" window to show up. Feb 28
				}
				GUI.createAndShowGUI(this, getCurrentFile(), true);
				// CobwebApplication.this.setEnabled(false); // $$$$$$ to make sure the "Cobweb Application" frame
				// disables when ever the "Test Data" window showing
				// $$$$$$ Modified on Feb 28
				// if (uiPipe != null) {
				// uiPipe.reset();
				// refreshAll(uiPipe);
				// }
				// */
			} else {
				// if (uiPipe != null && GUI.frame.isVisible() == true) GUI.frame.dispose(); // $$$$$ for allowing only
				// one "Test Data" window to show up. Feb 28
				JOptionPane.showMessageDialog(
						this, // $$$$$ change from "GUI.frame". Mar 17
						"File \" " + directory + file + "\" could not be found!", "Warning",
						JOptionPane.WARNING_MESSAGE);
				if (uiPipe == null) {
					GUI.frame.toFront(); // $$$$$$ Mar 17
				}
			}
		}
	}

	/**
	 * Exits the CobwebApplication.
	 * 
	 * <p> Used when the user selects "File" -> "Quit"
	 * 
	 * @see CobwebApplication#onMenuQuit()
	 */
	public void quitApplication() {
		if (uiPipe != null) {
			uiPipe.killScheduler();
		}
		System.exit(0);
	}

	/**
	 * @see DisplayPanel#refresh(boolean)
	 */
	public void refresh(boolean wait) {
		if (displayPanel != null) {
			displayPanel.refresh(wait);
		}
	}

	/**
	 * Opens a dialog box for the user to select the file he/she would like 
	 * to report to.
	 * 
	 * <p> Used when the user selects "File" -> "Report"
	 * 
	 * @see CobwebApplication#onMenuReport()
	 */
	public void reportDialog() {
		FileDialog theDialog = new FileDialog(this, // $$$$$$ modified from "this". Feb 29
				"Choose a file to save report to", FileDialog.SAVE);
		theDialog.setVisible(true);
		if (theDialog.getFile() != null) {
			reportFile(theDialog.getDirectory() + theDialog.getFile());
		}
	}

	/**
	 * @param filePath file path and file name
	 * @see UIInterface#report(String)
	 */
	public void reportFile(String filePath) {
		if (uiPipe != null) {
			try {
				uiPipe.report(filePath);
			} catch (Exception ex) {
				throw new CobwebUserException("Cannot save report file", ex);
			}
		}
	}

	/**
	 * Loads the default files simulation settings for the current simulation.  
	 * Uses the default file if available.  If not, then it will create a temporary 
	 * default data file to use.
	 * 
	 * <p> Used when the user selects "File" -> "Retrieve Default Data"
	 */
	// $$$$$$ Added for the "Retrieve Default Data" menu. Feb 18
	private void retrieveDefaultData() {
		// $$$$$$ Two fashions for retrieving default data:
		// $$$$$$ The first fashion for retrieving default data -- using the file default_data_(reserved).xml if one is
		// provided. Feb 11
		String defaultData = DEFAULT_DATA_FILE_NAME + CONFIG_FILE_EXTENSION; // $$$$$$ Feb 21

		File df = new File(defaultData); // $$$$$$ default_data_(reserved).xml Feb 11
		boolean isTheFirstFashion = false;
		if (df.exists() != false) {
			if (df.canWrite() != false) { // $$$$$$ added on Feb 21
				df.setReadOnly();
			}
			isTheFirstFashion = true;
		}

		String tempDefaultData = DEFAULT_DATA_FILE_NAME + TEMPORARY_FILE_EXTENSION;
		File tdf = new File(tempDefaultData); // $$$$$$ temporary file default_data_(reserved).temp Feb 11
		tdf.deleteOnExit();

		if (isTheFirstFashion != false) { // $$$$$$ Use the first fashion. Feb 11
			// $$$$$$ Copy default_data_(reserved).xml to the temporary file. Feb 11
			try {
				copyFile(defaultData, tempDefaultData);
			} catch (Exception ex) {
				isTheFirstFashion = false;
			}
		}

		if (isTheFirstFashion == false) { // $$$$$$ Use the second (stable) fashion as backup. Feb 11
			if (tdf.exists() != false) { // $$$$$$ added on Feb 21
				tdf.delete(); // delete the potential default_data file created by last time pressing
				// "Retrieve Default Data" menu. Feb 8
			}
		}

		// $$$$$$ Modified on Mar 14
		GUI.createAndShowGUI(this, tempDefaultData, false);
		if (uiPipe == null) {
			setCurrentFile(tempDefaultData);
		} // $$$$$$ added on Mar 14
		// $$$$$$ Modified on Feb 28
		/*
		 * if (uiPipe != null) { uiPipe.reset(); refreshAll(uiPipe); }
		 */
	}

	/**
	 * Saves the current data being used to savingFile.
	 * 
	 * @param savingFile Contains the file path and name
	 * @see CobwebApplication#saveFileDialog()
	 */
	/*
	 * $$$$$$ Modify this method to save test parameters rather than to save the state of the simulation. see
	 * cobweb.LocalUIInterface#save Feb 12 public void saveFile(String filePath) { if (uiPipe != null) { try {
	 * uiPipe.save(filePath); } catch (Throwable e) { textArea.append("Save failed:" + e.getMessage()); } } }
	 */
	public void saveFile(String savingFile) {
		try {
			// $$$$$$ The following block added to handle a readonly file. Feb 22
			File sf = new File(savingFile);
			if ((sf.isHidden() != false) || ((sf.exists() != false) && (sf.canWrite() == false))) {
				JOptionPane.showMessageDialog(
						GUI.frame, // $$$$$$ change from "this" to "GUI.frame" specifically for MS Windows. Feb 22
						"Caution:  File \"" + savingFile + "\" is NOT allowed to be written to.", "Warning",
						JOptionPane.WARNING_MESSAGE);
			} else {
				// $$$$$ The following line used to be the original code. Feb 22
				copyFile(getCurrentFile(), savingFile); // $$$$$$ modified on Mar 14
			}
		} catch (Exception ex) {
			Logger.getLogger("COBWEB2").log(Level.WARNING, "Save failed", ex);
		}
	}

	/**
	 * Opens the dialog box to allow the user to select the file to save 
	 * the current data to.
	 */
	public void saveFileDialog() {
		FileDialog theDialog = new FileDialog(GUI.frame, // $$$$$$ modified from "this". Feb 29
				"Choose a file to save state to", FileDialog.SAVE);
		theDialog.setVisible(true);
		// String savingFileName = "";
		if (theDialog.getFile() != null) {
			// $$$$$$ Check if the saving filename is one of the names reserved by CobwebApplication. Feb 22
			// String savingFileName;
			// savingFileName = theDialog.getFile();

			/* Block silenced by Andy because he finds it annoying not being able to modify the default input. */

			/*
			 * if ( (savingFileName.contains(INITIAL_OR_NEW_INPUT_FILE_NAME) != false) ||
			 * (savingFileName.contains(CURRENT_DATA_FILE_NAME) != false) ||
			 * (savingFileName.contains(DEFAULT_DATA_FILE_NAME) != false)) { JOptionPane.showMessageDialog(GUI.frame,
			 * "Save State: The filename\"" + savingFileName + "\" is reserved by Cobweb Application.\n" +
			 * "                       Please choose another file to save.", "Warning", JOptionPane.WARNING_MESSAGE);
			 * saveFileDialog(); } else { // $$$$$ If filename not reserved. Feb 22
			 */
			saveFile(theDialog.getDirectory() + theDialog.getFile());
			// }
		}
	}

	/**
	 * Sets the current file as input.
	 * 
	 * @param input Name of the new current file.
	 */
	public void setCurrentFile(String input) {
		currentFile = input;
	}

	/**
	 * Allows the user to select a new file to use as the default data file.  
	 * The selected file is copied into the default data file if the default 
	 * data file is writable or doesn�t exist.
	 * 
	 * Used when the user selects "File" -> "Set Default Data"
	 * 
	 * @see CobwebApplication#onMenuSetDefault()
	 */
	// $$$$$$ Implement the "Set Default Data" menu, using the default_data_(reserved).xml file. Feb 21
	private void setDefaultData() {
		String defaultData = DEFAULT_DATA_FILE_NAME + CONFIG_FILE_EXTENSION;
		// $$$$$ prepare the file default_data_(reserved).xml to be writable
		File df = new File(defaultData);
		if (df.isHidden() != false) {
			JOptionPane.showMessageDialog(
					this, // $$$$$$ change from "this" to "GUI.frame" specifically for MS Windows. Feb 22. Change back
					// on Mar 17
					"Cannot set default data:  file \"" + defaultData + "\" is hidden.", "Warning",
					JOptionPane.WARNING_MESSAGE);
			if (uiPipe == null) {
				GUI.frame.toFront(); // $$$$$$ Mar 17
			}
			return;
		}

		if ((df.exists() == false) || (df.canWrite() == true)) {
			FileDialog setDialog = new FileDialog(GUI.frame, // $$$$$$ modified from "this". Feb 29
					"Set Default Data", FileDialog.LOAD);
			setDialog.setVisible(true);

			// $$$$$$ The following codes modified on Feb 22
			if (setDialog.getFile() != null) {
				String directory = setDialog.getDirectory();
				String file = setDialog.getFile();
				String chosenFile = directory + file;
				// $$$$$$ Modified on Mar 13
				File f = new File(chosenFile);
				if (f.exists() != false) {
					try {
						copyFile(chosenFile, defaultData);
						// df.setReadOnly(); // $$$$$$ disallow write again
					} catch (Exception ex) {
						Logger.getLogger("COBWEB2").log(Level.WARNING, "Unable to set default data", ex);
						JOptionPane.showMessageDialog(setDialog, "Fail to set default data!\n"
								+ "\nPossible cause(s): " + ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);

					}
				} else {
					if (uiPipe != null && GUI.frame != null && GUI.frame.isVisible() == true) {
						GUI.frame.dispose(); // $$$$$ for allowing only one "Test Data" window to show up. Feb 28
					}
					JOptionPane.showMessageDialog(this, "File \" " + chosenFile + "\" could not be found!", "Warning",
							JOptionPane.WARNING_MESSAGE);
					if (uiPipe == null) {
						GUI.frame.toFront(); // $$$$$$ Mar 17
					}
				}
			}

		} else { // $$$$$ write permission failed to set
			JOptionPane.showMessageDialog(
					this, // $$$$$$ change from "this" to "GUI.frame" specifically for MS Windows. Feb 22
					"Fail to set default data!\n"
					+ "\nPossible cause(s): Permission for the current folder may not be attained.", "Warning",
					JOptionPane.WARNING_MESSAGE);

			/*** $$$$$$ Cancel textWindow Apr 22 */
			Logger.getLogger("COBWEB2").log(Level.WARNING, "Unable to set default data");

			if (uiPipe == null) {
				GUI.frame.toFront(); // $$$$$$ Mar 17
				// df.delete();// $$$$$ do not need to keep the file default_data_(reserved).xml any more
			}
		}
		// $$$$$$ Disallow write again to make sure the default data file would not be modified by outer calling. Feb 22
		if (df.canWrite() != false) {
			df.setReadOnly();
		}
	}

	public void setInvokedByModify(boolean b) {
		invokedByModify = b;
	}

	public void UIsettings() {

		createDefaultUI();

		makeAgentFoodSelectMenu();

		makeViewMenu();

		tickField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				tickField.repaint();
			}

			@Override
			public void focusLost(FocusEvent e) {
				tickField.repaint();
			}
		});

		uiPipe.setTimeStopField(tickField);

		uiPipe.AddTickEventListener(new TickEventListener() {
			public void TickPerformed(long currentTick) {

				tickDisplay.setText("Tick: " + NumberFormat.getIntegerInstance().format(currentTick));
			}
		});

		uiPipe.setPauseButton(pauseButton); // $$$$$$ Mar 20

		validate();
		uiPipe.start();
	} // end of UISettings

	private void makeViewMenu() {
		viewMenu.removeAll();
		for (final ViewerPlugin viewer : uiPipe.getViewers()) {
			final JCheckBoxMenuItem box = new JCheckBoxMenuItem(viewer.getName(), false);

			box.addItemListener(new ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						viewer.on();
					} else if (e.getStateChange() == ItemEvent.DESELECTED) {
						viewer.off();
					}
				}
			});
			ViewerClosedCallback onClosed = new ViewerClosedCallback() {
				@Override
				public void viewerClosed() {
					box.setSelected(false);
				}
			};
			viewer.setClosedCallback(onClosed);
			viewMenu.add(box);
		}
	}

	protected void createDefaultUI() {
		if (mainPanel == null) {
			mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());
			add(mainPanel);
		}

		uiPipe.setFrameSkip(0);
		if (displayPanel == null) {
			displayPanel = new DisplayPanel(uiPipe);
		} else {
			displayPanel.setUI(uiPipe);
		}

		mainPanel.add(displayPanel, BorderLayout.CENTER);
		if (controls == null) {
			controls = new JPanel();
			// controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
			mainPanel.add(controls, BorderLayout.NORTH);
		}
		if (tickDisplay == null) {
			tickDisplay = new JLabel();
			tickDisplay.setPreferredSize(new Dimension(90, 20));
			controls.add(tickDisplay);
		}
		if (tickField == null) {
			controls.add(new JLabel("Stop at"));
			tickField = new JTextField(8);
			tickField.setPreferredSize(new Dimension(40, 20));
			controls.add(tickField);
		}

		if (pauseButton == null) {
			pauseButton = new PauseButton(uiPipe);
			controls.add(pauseButton);
			stepButton = new StepButton(uiPipe);
			controls.add(stepButton);
			controls.add(new JLabel("   Adjust Speed:"));
			SpeedBar sb = new SpeedBar(uiPipe);
			controls.add(sb);
		} else {
			pauseButton.setUI(uiPipe);
		}
	}


	private void makeAgentFoodSelectMenu() {
		JMenuItem foodtype[] = new JMenuItem[uiPipe.countAgentTypes()];
		JMenuItem agentype[] = new JMenuItem[uiPipe.countAgentTypes()];
		foodMenu.removeAll();
		agentMenu.removeAll();
		for (int i = 0; i < uiPipe.countAgentTypes(); i++) {
			foodtype[i] = new JMenuItem("Food Type " + (i + 1));
			foodtype[i].setActionCommand("Food Type " + (i + 1));
			foodtype[i].addActionListener(new FoodMouseActionListener(i));
			foodMenu.add(foodtype[i]);

			agentype[i] = new JMenuItem("Agent Type " + (i + 1));
			agentype[i].setActionCommand("Agent Type " + (i + 1));
			agentype[i].addActionListener(new AgentMouseActionListener(i));
			agentMenu.add(agentype[i]);
		}
	}


	private class FoodMouseActionListener implements ActionListener {

		private final int type;

		public FoodMouseActionListener(int type) {
			this.type = type;

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			displayPanel.setMouseMode(MouseMode.AddFood, type);
		}
	}

	private class AgentMouseActionListener implements ActionListener {

		private final int type;

		public AgentMouseActionListener(int type) {
			this.type = type;

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			displayPanel.setMouseMode(MouseMode.AddAgent, type);
		}

	}

	private UIInterface uiPipe;

	private JMenu viewMenu;

	/**
	 * Connects a user interface (simulation) to the graphical interface of the 
	 * Cobweb application (uiPipe).
	 * 
	 * @param simulation The user interface used for the simulation.
	 */
	@Override
	public void setSimulation(UIInterface simulation) {
		uiPipe = simulation;
	}

	/**
	 * @see CobwebApplication#openFileDialog()
	 */
	private void onMenuOpen() {
		pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		setInvokedByModify(false); // $$$$$$ added on Mar 14 // need to implement only if using the old "Open"
		// behaviour in Version 2006
		CobwebApplication.this.openFileDialog();
		// $$$$$$ Add "Set Default Data" menu. Feb 21
	}

	/**
	 * @see CobwebApplication#setDefaultData()
	 */
	private void onMenuSetDefault() {
		pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		CobwebApplication.this.setDefaultData();
		// $$$$$$ Add "Retrieve Default Data" menu. Feb 4
	}

	/**
	 * If a "Test Data" window is open (visible), dispose it (when hitting a 
	 * menu). Feb 29
	 */
	private void disposeGUIframe() {
		if (uiPipe != null && GUI.frame != null && GUI.frame.isVisible()) {
			GUI.frame.dispose();
		}
	}

	/**
	 * @see CobwebApplication#aboutDialog()
	 */
	private void onMenuAbout() {
		// pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		CobwebApplication.this.aboutDialog();
	}

	/**
	 * @see CobwebApplication#createNewData()
	 */
	private void onMenuCreateNew() {
		pauseUI(); // $$$$$$ Feb 12
		if (GUI.frame != null && GUI.frame.isVisible()) {
			GUI.frame.dispose(); // $$$$$ for allowing only one "Test Data" window to show up
		}
		// CobwebApplication.this.setEnabled(false); // $$$$$$ another way, to make sure the
		// "Cobweb Application" frame disables when ever "Test Data" window showing
		setInvokedByModify(false); // $$$$$$ added on Mar 14
		CobwebApplication.this.createNewData(); // $$$$$$ implemented on Mar 14
	}

	/**
	 * @see CobwebApplication#creditsDialog()
	 */
	private void onMenuCredits() {
		// pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		CobwebApplication.this.creditsDialog();
	}

	/**
	 * @see UIInterface#insertPopulation(String, String)
	 */
	private void onMenuInsertSample() {
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		if (uiPipe != null) {

			String option = openInsertSamplePopOptionsDialog();

			if (option != null){
				//Select the XML file
				FileDialog theDialog = new FileDialog(GUI.frame, 
						"Choose a file to load", FileDialog.LOAD);
				theDialog.setVisible(true);
				if (theDialog.getFile() != null) {
					//Load the XML file
					uiPipe.insertPopulation(theDialog.getDirectory() + theDialog.getFile(), option);
				}
			}

		}
	}

	/**
	 * @see CobwebApplication#logFileDialog()
	 */
	private void onMenuLog() {
		pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29

		if (uiPipe == null) {
			JOptionPane.showMessageDialog(GUI.frame, 
			"To create a log file, please press \"OK\" to launch the Cobweb Application first.");
		} else {
			CobwebApplication.this.logFileDialog();
		}

	}

	/**
	 * @see CobwebApplication#openCurrentData()
	 */
	private void onMenuModifyCurrent() {
		pauseUI(); // $$$$$$ Feb 12
		if (GUI.frame != null && GUI.frame.isVisible()) {
			GUI.frame.dispose(); // $$$$$ for allowing only one "Test Data" window to show up
		}
		// CobwebApplication.this.setEnabled(false); // $$$$$$ another way, to make sure the
		// "Cobweb Application" frame disables when ever "Test Data" window showing
		setInvokedByModify(true); // $$$$$$ added on Mar 14
		CobwebApplication.this.openCurrentData();
		// $$$$$$ Modified on Mar 14
	}

	/**
	 * @see CobwebApplication#openCurrentFile()
	 */
	private void onMenuModifyThis() {
		pauseUI(); // $$$$$$ Feb 12
		if (GUI.frame != null && GUI.frame.isVisible()) {
			GUI.frame.dispose(); // $$$$$ for allowing only one "Test Data" window to show up
		}
		// CobwebApplication.this.setEnabled(false); // $$$$$$ another way, to make sure the
		// "Cobweb Application" frame disables when ever "Test Data" window showing
		setInvokedByModify(true); // $$$$$$ added on Mar 14
		CobwebApplication.this.openCurrentFile();
	}

	private void onMenuObserve() {
		// pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		displayPanel.setMouseMode(MouseMode.Observe);
	}

	/**
	 * @see CobwebApplication#quitApplication()
	 */
	private void onMenuQuit() {
		CobwebApplication.this.quitApplication();
		// $$$$$$ Implement "Show/Hide Info" menu. Mar 14
	}

	/**
	 * @see UIInterface#clearAgents()
	 */
	private void onMenuRemoveAgents() {
		// pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		/* remove all agents */
		// $$$$$$ modified on Feb 29
		if (uiPipe != null) {
			uiPipe.clearAgents();
		}
		// mode = -3;
		// uiPipe.removeComponents(mode);

		// $$$$$$ Added on Feb 29
	}

	/**
	 * @see UIInterface#clearAgents()
	 * @see UIInterface#clearFood()
	 * @see UIInterface#clearStones()
	 */
	private void onMenuRemoveAll() {
		// pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		/* remove all */
		// $$$$$$ modified on Feb 29
		if (uiPipe != null) {
			uiPipe.clearAgents();
			uiPipe.clearFood();
			uiPipe.clearStones();
		}
	}

	/**
	 * @see UIInterface#clearFood()
	 */
	private void onMenuRemoveFood() {
		// pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		/* remove all food */
		// $$$$$$ modified on Feb 29
		if (uiPipe != null) {
			uiPipe.clearFood();
		}
		// mode = -2;
		// uiPipe.removeComponents(mode);
	}

	/**
	 * @see UIInterface#clearStones()
	 */
	private void onMenuRemoveStones() {
		// pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		/* remove all stones */
		// $$$$$$ modified on Feb 29
		if (uiPipe != null) {
			uiPipe.clearStones();
		}
		// mode = -1;
		// uiPipe.removeComponents(mode);
	}

	/**
	 * @see UIInterface#clearWaste()
	 */
	private void onMenuRemoveWaste() {
		// pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		/* remove all agents */
		// $$$$$$ modified on Feb 29
		if (uiPipe != null) {
			uiPipe.clearWaste();
		}
		// mode = -4;
		// uiPipe.removeComponents(mode);
	}

	/**
	 * @see CobwebApplication#reportDialog()
	 */
	private void onMenuReport() {
		pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		// $$$$$$ Modified on Feb 29
		if (uiPipe == null) {
			JOptionPane.showMessageDialog(GUI.frame, // $$$$$$ change from "displayPanel" to "GUI.frame"
					// specifically for MS Windows. Feb 22
			"To create a report file, please press \"OK\" to launch the Cobweb Application first.");
		} else {
			CobwebApplication.this.reportDialog();
		}
		// CobwebApplication.this.reportDialog();
	}

	/**
	 * @see CobwebApplication#retrieveDefaultData()
	 */
	private void onMenuRetrieveDefault() {
		pauseUI(); // $$$$$$ Feb 12
		if (GUI.frame != null && GUI.frame.isVisible()) {
			GUI.frame.dispose(); // $$$$$ for allowing only one "Test Data" window to show up
		}
		// CobwebApplication.this.setEnabled(false); // $$$$$$ another way, to make sure the
		// "Cobweb Application" frame disables when ever "Test Data" window showing
		setInvokedByModify(false); // $$$$$$ added on Mar 14
		CobwebApplication.this.retrieveDefaultData();
		// $$$$$$ Added for "Modify Current Data" menu. Feb 12
	}

	/**
	 * @see CobwebApplication#saveFileDialog()
	 */
	private void onMenuSave() {
		pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		if (GUI.frame == null || !GUI.frame.isVisible()) {
			GUI.createAndShowGUI(CobwebApplication.this, CobwebApplication.this.getCurrentFile(), true);// $$$$$$ changed from "GUI.frame.setVisible(true);".
			// Mar 17
		}
		CobwebApplication.this.saveFileDialog();
		// $$$$$$ Modified for very first time running. Feb 28
		if (GUI.frame != null && uiPipe != null) {
			GUI.frame.dispose(); // $$$$$$ Feb 8 $$$$$$ change from "setVisible(false)". Mar 17
			// CobwebApplication.this.toFront(); // $$$$$$ added on Feb 22
		}
	}

	/**
	 * @see UIInterface#saveCurrentPopulation(String, String, int)
	 */
	private void onMenuSaveSample() {
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		if (uiPipe != null) {


			// open dialog to choose population size to be saved
			HashMap<String, Object> result = openSaveSamplePopOptionsDialog();
			if (result != null){
				String option = (String)result.get("option"); 
				int amount = (Integer)result.get("amount");

				if (option != null && amount != -1) {
					// Open file dialog box
					FileDialog theDialog = new FileDialog(GUI.frame, 
							"Choose a file to save state to", FileDialog.SAVE);
					theDialog.setVisible(true);
					if (theDialog.getFile() != null) {

						//Save population in the specified file. 
						uiPipe.saveCurrentPopulation(theDialog.getDirectory() + theDialog.getFile(), option, amount);
					}
				}
			}
		}
	}

	/**
	 * Sets the mouse mode to allow adding stones to grid.
	 * 
	 * @see DisplayPanel#setMouseMode(MouseMode)
	 */
	private void onMenuStones() {
		// pauseUI(); // $$$$$$ Feb 12
		disposeGUIframe(); // added to ensure no popup GUI frame when hitting a menu. Feb 29
		/* switch to stone selection mode */
		displayPanel.setMouseMode(MouseMode.AddStone);
	}

	/**
	 * Opens a dialog box to allow the user to select the option of replacing the 
	 * current population, or merge with the the current population.
	 * 
	 * <p> Used when the user selects "File" -> "Insert Sample Population"
	 * 
	 * @return The option selected by the user.
	 * @see CobwebApplication#onMenuInsertSample()
	 */
	private String openInsertSamplePopOptionsDialog() {
		JRadioButton b1 = new JRadioButton("Replace current population");
		JRadioButton b2 = new JRadioButton("Merge with current population");
		b1.setSelected(true);

		ButtonGroup group = new ButtonGroup();
		group.add(b1);
		group.add(b2);

		Object[] array = {
				new JLabel("Select an option:"),
				b1,
				b2
		};

		int res = JOptionPane.showConfirmDialog(null, array, "Select", 
				JOptionPane.OK_CANCEL_OPTION);


		if (res == -1 || res == 2)			
			return null;


		String result = null;

		if (b1.isSelected()) { 
			result = "replace"; 
		}
		else if ( b2.isSelected()) {
			result = "merge";
		}

		return result;
	}

	/**
	 * Creates a hash that contains the information of whether the user selected 
	 * to save as a population or an amount, and what percentage or amount.
	 * 
	 * <p>Used when the user selects "File" -> "Save Sample Population"
	 * 
	 * @return A hash of the options the user selected.
	 */
	private HashMap<String, Object> openSaveSamplePopOptionsDialog() {

		JRadioButton b1 = new JRadioButton("Save a percentage (%) between 1-100");


		int popNum = uiPipe.getCurrentPopulationNum();

		JRadioButton b2 = new JRadioButton("Save an amount (between 1-"+ popNum + ")");
		b1.setSelected(true);

		ButtonGroup group = new ButtonGroup();
		group.add(b1);
		group.add(b2);

		JTextField amount = new JTextField(30);

		Object[] array = {
				new JLabel("Select an option:"),
				b1,
				b2,
				new JLabel("Enter the number for the selected option:"),
				amount
		};

		int res = JOptionPane.showConfirmDialog(null, array, "Select", 
				JOptionPane.OK_CANCEL_OPTION);

		if (res == -1 || res == 2)			
			return null;

		int am = -1;

		HashMap<String, Object> result = new HashMap<String, Object>();

		try {
			am = Integer.parseInt(amount.getText());
			if (am < 1)
				throw new Exception();
		} catch (Exception e) {
			JOptionPane.showMessageDialog((Component)null, "Invalid input.");
			return null;

		}

		result.put("amount", am);

		if (b1.isSelected()) { 
			result.put("option", "percentage");
		}
		else if ( b2.isSelected()) {
			result.put("option", "amount");
		}

		return result;


	}

	/**
	 * @see UIInterface#pause()
	 */
	// $$$$$$ A facilitating method to ensure the UI to pause. Feb 12
	private void pauseUI() {
		if (uiPipe != null && uiPipe.isRunning()) { // $$$$$$ changed from
			// "if (uiPipe.isPaused() == false) {", for the very
			// first run. Feb 28
			uiPipe.pause();
			pauseButton.repaint();
		}
	}

} // CobwebApplication
