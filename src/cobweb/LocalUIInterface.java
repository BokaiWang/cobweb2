package cobweb;

import ga.GeneticsMutator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JTextField;

import temperature.TemperatureMutator;
import temperature.TemperatureParams;
import cobweb.Environment.EnvironmentStats;
import cobweb.Environment.Location;
import cwcore.AgentSpawner;
import cwcore.ComplexAgent;
import cwcore.ComplexEnvironment;
import cwcore.LinearWeightsController;
import disease.DiseaseMutator;
import driver.LinearAIGraph;
import driver.SimulationConfig;

/**
 * This class provides the definitions for a user interface that is running 
 * on a local machine.  
 * 
 * @author ???
 *
 */
public class LocalUIInterface implements UIInterface, DrawingHandler, cobweb.TickScheduler.Client {

	private final class LinearAIViewer implements ViewerPlugin {

		private LinearAIGraph aiGraph;
		private ViewerClosedCallback onClosed;

		@Override
		public void on() {
			aiGraph = new LinearAIGraph();
			aiGraph.setVisible(true);
			aiGraph.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					onClosed.viewerClosed();
				}
			});
		}

		@Override
		public void off() {
			if (aiGraph == null)
				return;
			aiGraph.setVisible(false);
			aiGraph.setEnabled(false);
			aiGraph = null;
		}

		@Override
		public String getName() {
			return "AI Weight Stats";
		}

		@Override
		public void setClosedCallback(ViewerClosedCallback onClosed) {
			this.onClosed = onClosed;

		}
	}

	/**
	 * AgentDrawInfo stores the drawable state of a single agent. AgentDrawInfo
	 * exists to make the data passed to newAgent calls persist for subsequent
	 * draw calls. Note that this class is private to LocalUIInterface; no other
	 * class (other than LocalUIInterface.DrawInfo) knows of the existence of
	 * this class.
	 */
	private static class AgentDrawInfo {

		/** Solid color of the agent. */
		java.awt.Color agentColor;

		java.awt.Color type;

		java.awt.Color action;

		/** Position in tile coordinates */
		Point2D position;

		/**
		 * Facing vector; not normalised, but only the sign of each component is
		 * considered.
		 */
		Point2D facing;

		private int[] xPts = new int[3];

		private int[] yPts = new int[3];

		private AgentDrawInfo(Color bodyColor, Color dotColor, Color borderColor, Point2D position, Point2D direction) {
			agentColor = bodyColor;
			type = dotColor;
			action = borderColor;
			this.position = position;
			facing = direction;
		}

		void draw(Graphics g, int tileWidth, int tileHeight) {
			g.setColor(agentColor);
			int topLeftX = position.x * tileWidth;
			int topLeftY = position.y * tileHeight;

			if (facing.x != 0 || facing.y != 0) {
				int deltaX = tileWidth / 2;
				int deltaY = tileHeight / 2;
				int centerX = topLeftX + deltaX;
				int centerY = topLeftY + deltaY;
				if (facing.x != 0 && facing.y != 0) {
					// Diagonal; deal with this later
				} else if (facing.x != 0) {
					// Horizontal facing...
					xPts[0] = centerX + facing.x * deltaX;
					xPts[1] = centerX - facing.x * deltaX;
					xPts[2] = xPts[1];

					yPts[0] = centerY;
					yPts[1] = centerY + deltaY;
					yPts[2] = centerY - deltaY;
				} else {
					// Vertical facing...
					xPts[0] = centerX;
					xPts[1] = centerX + deltaX;
					xPts[2] = centerX - deltaX;

					yPts[0] = centerY + facing.y * deltaY;
					yPts[1] = centerY - facing.y * deltaY;
					yPts[2] = yPts[1];
				}
				g.fillPolygon(xPts, yPts, 3);
				g.setColor(type);
				g.fillOval(topLeftX + tileWidth / 3, topLeftY + tileHeight / 3, tileWidth / 3 + 1, tileHeight / 3 + 1);
				g.setColor(action);
				g.drawPolygon(xPts, yPts, 3);
			} else {
				g.fillOval(topLeftX, topLeftY, tileWidth, tileHeight);
			}
		}
	}

	/**
	 * DrawInfo stores the frame data for the draw method to display. DrawInfo's
	 * are built by calling newTileColors and newAgent in response to a
	 * getDrawInfo call on the Environment or on an Agent. Note that DrawInfo is
	 * a private class, and only LocalUIInterface knows of it's existence. For
	 * this reason, DrawInfo is local to LocalUIInterface, and LocalUIInterface
	 * reads DrawInfo members directly.
	 */
	private static class DrawInfo {

		/** Width of the frame info, in tiles */
		int width;

		/** Height of the frame info, in tiles */
		int height;

		/**
		 * width * height array of colors for the tiles; The color for a
		 * specific tile at (x,y) is tileColors[y * width * + x]
		 */
		java.awt.Color[] tileColors;

		/** Linked list of AgentDrawInfo for the display of agents. */
		List<AgentDrawInfo> agents;

		List<PathDrawInfo> paths;

		public List<DropDrawInfo> drops = new LinkedList<DropDrawInfo>();

		private static ColorLookup colorMap = TypeColorEnumeration.getInstance();

		/**
		 * Construct a DrawInfo width specific width, height and tile colors.
		 * The tiles array is not copied; the caller is assumed to "give" the
		 * array to the drawing info, and not keep any local references around.
		 */
		DrawInfo(int w, int h, java.awt.Color[] tiles) {
			width = w;
			height = h;
			tileColors = tiles;
			agents = new LinkedList<AgentDrawInfo>();
			paths = new LinkedList<PathDrawInfo>();
		}

		/** Draw the tiles and the agents. */
		void draw(java.awt.Graphics g, int tileWidth, int tileHeight) {
			// Tiles
			int tileIndex = 0;
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					g.setColor(tileColors[tileIndex++]);
					g.fillRect(x * tileWidth + 1, y * tileHeight + 1, tileWidth - 1, tileHeight - 1);
				}
			}

			int half =(int)( tileWidth / 2.0f + 0.5f); 

			for (DropDrawInfo drop : drops) {
				int x = drop.pos.x;
				int y = drop.pos.y;
				g.setColor(drop.col);
				g.fillRect(x * tileWidth + 0, y * tileHeight + 0, half, half);
				g.fillRect(x * tileWidth + half, y * tileHeight + half, half, half);
			}

			// Grid lines
			g.setColor(COLOR_GRIDLINES);
			int totalWidth = tileWidth * width;
			for (int y = 0; y <= height; y++) {
				g.drawLine(0, y * tileHeight, totalWidth, y * tileHeight);
			}
			int totalHeight = tileHeight * height;
			for (int x = 0; x <= width; x++) {
				g.drawLine(x * tileWidth, 0, x * tileWidth, totalHeight);
			}

			// Agents
			for (AgentDrawInfo a : agents) {
				a.draw(g, tileWidth, tileHeight);
			}

			// Paths
			for (PathDrawInfo path : paths) {
				path.draw(g, tileWidth, tileHeight);
			}

			int limit = Math.min(TemperatureParams.TEMPERATURE_BANDS, height);
			// Temperature band labels
			for (int y = 0; y < height; y++) {
				int band = y * limit / height;
				g.setColor(colorMap.getColor(band, 5));
				int offset = (limit / 2 - band) * 3 / -2;
				for (int i = 0; i <= band; i++) {
					int x = (i + 2) * -3 + offset;
					g.drawLine(x - 1, y * tileHeight, x, (y + 1) * tileHeight);
				}
			}
		}

	}

	private static class DropDrawInfo {
		public Point2D pos;
		public Color col;

		public DropDrawInfo(Point2D pos, Color col) {
			this.pos = pos;
			this.col = col;
		}

	}

	private static class PathDrawInfo {

		private final List<Location> path;

		public PathDrawInfo(List<Location> path) {
			this.path = new LinkedList<Location>(path);
		}

		public void draw(Graphics g, int tileWidth, int tileHeight) {
			Iterator<Location> itr = path.iterator();
			if (!itr.hasNext()) {
				return;
			}
			Location p1;
			Location p2 = itr.next();
			Color o = g.getColor();

			int alpha = 63;

			int increment = 192 / path.size();

			while (itr.hasNext()) {
				g.setColor(new Color(0, 0, 255, alpha));
				p1 = p2;
				p2 = itr.next();
				int x = Math.min(p1.v[0], p2.v[0]);
				int y = Math.min(p1.v[1], p2.v[1]);
				int w = Math.abs(p1.v[0] - p2.v[0]);
				int h = Math.abs(p1.v[1] - p2.v[1]);

				if (w > 1 || h > 1) {
					continue;
				}

				int stripwidth = (tileWidth - tileWidth * 4 / 5) + 1;
				int stripheight = (tileHeight - tileHeight * 4 / 5) + 1;

				x = x * tileWidth + tileWidth * 2 / 5;
				y = y * tileHeight + tileHeight * 2 / 5;
				w = w * tileWidth + stripwidth;
				h = h * tileHeight + stripheight;

				if (p1.v[0] - p2.v[0] > 0) {
					w -= stripwidth;
					x += stripwidth;
				} else if (p1.v[0] - p2.v[0] < 0) {
					w -= stripwidth;
				} else if (p1.v[1] - p2.v[1] > 0) {
					h -= stripheight;
					y += stripheight;
				} else if (p1.v[1] - p2.v[1] < 0) {
					h -= stripheight;
				}

				g.fillRect(x, y, w, h);
				alpha += increment;
			}
			g.setColor(o);
		}
	}

	public static interface TickEventListener {

		public void TickPerformed(long currentTick);
	}

	final String POP_NAME = "Sample";

	long myClock = -1;

	long delay = 1;

	private static final Color COLOR_GRIDLINES = Color.lightGray;

	private final Set<TickEventListener> tickListeners = new LinkedHashSet<TickEventListener>();

	/**
	 * This is the frame data under construction by setAgent calls. Constructed
	 * in a separate member from the finished frame data so a refresh can occur
	 * while a draw is happening.
	 */
	private volatile DrawInfo newDrawingInfo;

	/** This is the most recent completed drawing info */
	private volatile DrawInfo theDrawingInfo;

	///** List of sample populations */
	//private HashMap<String, List<AgentDrawInfo>> samplePop;

	private UIInterface.UIClient theClient;

	private Environment theEnvironment;

	private volatile Scheduler theScheduler;

	private JTextField stopTickField;

	public int count = 0;

	private JButton pauseButton;

	private boolean runnable = false;

	private TemperatureMutator tempMutator;

	private GeneticsMutator geneticMutator;

	private DiseaseMutator diseaseMutator;

	private Writer logWriter;

	Logger myLogger = Logger.getLogger("COBWEB2");

	private SimulationConfig simulationConfig;

	/**
	 * Construct a LocalUIInterface from a specified state file URL.
	 * 
	 * Any failure to load the state file, either from file/network input
	 * errors, a badly formed state file, or a class specified in the data file
	 * being unknown will result in a InstantiationError being thrown with a
	 * descriptive and appropriate message string.
	 * 
	 * @param client the UIClient to notify of new frame data.
	 */
	public LocalUIInterface(UIClient client) {
		theClient = client;
	}

	/**
	 * @see ComplexEnvironment#addAgent(int, int, int)
	 */
	public void addAgent(int x, int y, int type) {
		theEnvironment.addAgent(x, y, type);
	}

	/**
	 * @see ComplexEnvironment#addFood(int, int, int)
	 */
	public void addFoodSource(int x, int y, int type) {
		theEnvironment.addFoodSource(x, y, type);
	}

	/**
	 * @see ComplexEnvironment#addStone(int, int)
	 */
	public void addStone(int x, int y) {
		theEnvironment.addStone(x, y);
	}

	public void AddTickEventListener(TickEventListener listener) {
		tickListeners.add(listener);
		listener.TickPerformed(myClock);
	}

	public void clearAgents() {
		theEnvironment.clearAgents();
	}

	public void clearFoodSources() {
		theEnvironment.clearFoodSources();
	}

	public void clearStones() {
		theEnvironment.clearStones();
	}

	public void clearWaste() {
		theEnvironment.clearWaste();
	}

	/* return number of TYPES of agents in the environment */
	public int countAgentTypes() {
		return theEnvironment.getTypeCount();
	}

	/**
	 * Display the most recent frame data.
	 * 
	 * @param g Graphics to render to
	 * @param tileWidth width, in pixels, of a single tile
	 * @param tileHeight height, in pixels, of a single tile
	 */
	public void draw(java.awt.Graphics g, int tileWidth, int tileHeight) {
		if (theDrawingInfo != null) {
			theDrawingInfo.draw(g, tileWidth, tileHeight);
		}
		// Don't synchronise the whole method; drawing could take a while,
		// and the wait in refresh might want to acquire the monitor on a
		// timeout.
		// // ^ odd, works fine
		// doRefreshNotification();
	}

	@Override
	public int getCurrentPopulationNum() {
		return theEnvironment.getCurrentPopulation();
	}

	/**
	 * Returns the tick count for the most recent frame data.
	 */
	public long getCurrentTime() {
		return myClock;
	}

	/**
	 * Returns the height, in tiles, of the environment.
	 * 
	 * @return 0 if there is no valid frame data, otherwise returns the height
	 *         of the frame data.
	 */
	public int getHeight() {
		return theEnvironment.getSize(1);
	}

	public JButton getPauseButton() {
		return pauseButton;
	}

	public EnvironmentStats getStatistics() {
		return theEnvironment.getStatistics();
	}

	public long getStopTime() {
		int stopTime = 0;
		if (stopTickField != null && !("").equals(stopTickField.getText())) {
			try {
				String text = stopTickField.getText();
				stopTime = Integer.parseInt(text);
			} catch (NumberFormatException e) {
				// broken input, don't care
			}
		}
		return stopTime;
	}

	public long getTime() {
		return theScheduler.getTime();
	}

	/**
	 * @return TextField TickField
	 */
	public JTextField getTimeStopField() {
		return stopTickField; // return the TickField instance
	}

	/**
	 * Returns the width, in tiles, of the environment.
	 * 
	 * @return 0 if there is no valid frame data, otherwise returns the width of
	 *         the frame data.
	 */

	public int getWidth() {
		return theEnvironment.getSize(0);
	}

	/**
	 * Initialize the specified Environment class.  The environment is created using the 
	 * environmentName.load method.
	 * 
	 * @param environmentName Class name of the environment used in this simulation.
	 * @param p Simulation parameters that can be defined by the simulation data file (xml file).
	 * @see Environment#load(Scheduler, SimulationConfig)
	 */
	private void InitEnvironment(String environmentName, SimulationConfig p) {
		try {
			if (theEnvironment == null || !theEnvironment.getClass().equals(Class.forName(environmentName))) {
				Class<?> environmentClass = Class.forName(environmentName);
				// Use reflection to find a constructor taking a Scheduler
				// parameter
				// and a Reader
				Constructor<?> environmentCtor = environmentClass.getConstructor();
				if (environmentCtor == null)
					throw new InstantiationError("No valid constructor found on environment class.");

				theEnvironment = (Environment) environmentCtor.newInstance();
			}
			theEnvironment.load(theScheduler, p);
		} catch (Exception ex) {
			throw new RuntimeException("Can't InitEnvironment", ex);
		}
		Environment.setUIPipe(this);
		updateEnvironmentDrawInfo();
	}

	/**
	 * Initialize the specified Scheduler class with state data read from the 
	 * reader. The type of scheduler used can be determined by the "scheduler" field 
	 * in the simulation data file being used.  This is a private helper to the 
	 * LocalUIInterface constructor.
	 * 
	 * @param schedulerName Class name of the scheduler used in this simulation.
	 * @param p Simulation parameters that can be defined by the simulation data file (xml file).
	 */
	private void InitScheduler(String schedulerName, SimulationConfig p) {
		try {
			if (theScheduler != null) {
				theScheduler.killScheduler();
			}

			Class<?> schedulerClass = Class.forName(schedulerName);
			// Use reflection to find a constructor taking a UIInterface
			// parameter and a Reader
			Constructor<?> theCtor = schedulerClass.getConstructor(UIInterface.class, SimulationConfig.class);
			if (theCtor == null)
				throw new InstantiationError("Correct constructor not found in " + schedulerName);

			theScheduler = (Scheduler) theCtor.newInstance(this, p);
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot initialize scheduler", e);
		}
	}

	/**
	 * @see Environment#insertPopulation(String, String)
	 */
	@Override
	public boolean insertPopulation(String fileName, String option) throws FileNotFoundException {

		return theEnvironment.insertPopulation(fileName, option);
	}

	public boolean isRunnable() {
		return runnable;
	}

	/**
	 * Is the simulation paused? Calls isSchedulerPaused on the scheduler.
	 * 
	 * @return the paused state flag of the simulation
	 */
	public boolean isRunning() {
		return theScheduler != null && theScheduler.isRunning();
	}

	/**
	 * Kill the simulation. Calls killScheduler on the scheduler.
	 */
	public void killScheduler() {
		theScheduler.killScheduler();
		try {
			if (logWriter != null)
				logWriter.close();
		} catch (IOException ex) {
			throw new RuntimeException("Unable to close log file properly", ex);
		}
	}

	/**
	 * Initialize the specified environment class with state data read from the 
	 * reader. It first adds the various mutators used to modify actions in the 
	 * event that the agent spawns, steps, or contacts another agent.  It then 
	 * uses the simulation parameters to modify the properties of the mutators.  
	 * It then initializes the simulation environment (InitEnvironment) using 
	 * the simulation configuration object.  Finally, it will start the scheduler, 
	 * which will start the simulation.  This is a private helper to the 
	 * LocalUIInterface constructor.
	 */
	public void load(SimulationConfig p) {
		this.simulationConfig = p;
		InitScheduler(p.getEnvParams().schedulerName, p);
		AgentSpawner.SetType(p.getEnvParams().agentName);

		// TODO: this is a hack to make the applet work when we switch grids and
		// the static information is not cleared, ComplexAgent should really
		// have a way to track which mutators have been bound
		if (!p.getEnvParams().keepOldAgents) {
			ComplexAgent.clearMutators();
			geneticMutator = null;
			diseaseMutator = null;
			tempMutator = null;
		}

		if (geneticMutator == null) {
			geneticMutator = new GeneticsMutator();
			ComplexAgent.addMutator(geneticMutator);
			ComplexAgent.setSimularityCalc(geneticMutator);
		}
		if (diseaseMutator == null) {
			diseaseMutator = new DiseaseMutator();
			ComplexAgent.addMutator(diseaseMutator);
		}
		if (tempMutator == null) {
			tempMutator = new TemperatureMutator();
			ComplexAgent.addMutator(tempMutator);
		}

		geneticMutator.setParams(p.getGeneticParams(), p.getEnvParams().getAgentTypes());

		diseaseMutator.setParams(p.getDiseaseParams(), p.getEnvParams().getAgentTypes());

		tempMutator.setParams(p.getTempParams(), p.getEnvParams());

		InitEnvironment(p.getEnvParams().environmentName, p);

		theScheduler.addSchedulerClient(this);
		theScheduler.addSchedulerClient(geneticMutator.getTracker());
		theScheduler.addSchedulerClient(diseaseMutator);

		theScheduler.setSleep(delay);

		setupViewers();

		theScheduler.startScheduler();
		tickNotification(0);

		theClient.fileOpened(p);
	}

	private void setupViewers() {
		for (ViewerPlugin viewer : viewers) {
			viewer.off();
		}
		viewers.clear();
		if (simulationConfig.getEnvParams().controllerName.equals(LinearWeightsController.class.getName())) {
			viewers.add(new LinearAIViewer());
		}
	}

	/**
	 * Set the log file.
	 * 
	 * @see cobweb.UIInterface#log
	 */
	public void log(String filePath) throws java.io.IOException {

		if (logWriter != null)
			logWriter.close();

		// Open the Writer...
		logWriter = new BufferedWriter(new FileWriter(filePath), 8*1024);
		// Fire it off to the environment
		theEnvironment.log(logWriter);
	}

	public void newAgent(java.awt.Color agentColor, java.awt.Color typeColor, java.awt.Color strategyColor,
			Point2D position, Point2D facing) {
		newDrawingInfo.agents.add(new AgentDrawInfo(agentColor, typeColor, strategyColor, position, facing));
	}

	public void newPath(List<Location> path) {
		newDrawingInfo.paths.add(new PathDrawInfo(path));
	}

	/**
	 * Notify the UI of new tile colors.
	 */
	public void newTileColors(int width, int height, java.awt.Color[] tileColors) {
		newDrawingInfo = new DrawInfo(width, height, tileColors);

	}

	@Override
	public void newDrop(Point2D position, Color color) {
		newDrawingInfo.drops .add(new DropDrawInfo(position, color));
	}

	public void observe(int x, int y) {
		theEnvironment.observe(x, y);
	}

	/**
	 * Pause the simulation. Calls pauseScheduler on the scheduler.
	 */
	public void pause() {
		theScheduler.pauseScheduler();
		refresh(true);
		try {
			if (logWriter != null)
				logWriter.flush();
		} catch (IOException ex) {
			throw new RuntimeException("Unable to close log file properly", ex);
		}
	}

	/**
	 * Inform the UI that new frame data is available. Calls getDrawInfo on the
	 * environment, then informs the client that the frame data has been
	 * updated.
	 * 
	 * @param wait Wait for refresh?
	 */
	public void refresh(boolean wait) {
		if (wait && pauseButton != null)
			pauseButton.repaint();
		if (theClient == null || !theClient.isReadyToRefresh())
			return;

		updateEnvironmentDrawInfo();
		// Wait for refresh calls theClient.refresh... if it didn't we'd have a
		// race condition between
		// the UI thread that refreshes and this thread which waits for the
		// refresh.
		theClient.refresh(wait);
	}

	public void removeAgent(int x, int y) {
		theEnvironment.removeAgent(x, y);
	}

	public void removeFood(int x, int y) {
		theEnvironment.removeFoodSource(x, y);
	}

	public void removeStone(int x, int y) {
		theEnvironment.removeStone(x, y);
	}

	public void RemoveTickEventListener(TickEventListener listener) {
		tickListeners.remove(listener);
	}

	public void report(String filePath) throws java.io.IOException {
		// Open the Writer...
		java.io.FileWriter outStream = new java.io.FileWriter(filePath);

		boolean running = isRunning();

		// Pause, if needed
		if (running) {
			pause();
		}

		// Save the environment
		theEnvironment.report(outStream);

		// Close the stream
		outStream.close();

		// Resume, if needed
		if (running) {
			resume();
		}
	}

	public void reset() {
		theScheduler.resetTime();
	}

	/**
	 * Resume the simulation from a paused state. Calls resumeScheduler on the
	 * scheduler.
	 */
	public void resume() {
		theScheduler.resumeScheduler();
	}

	/**
	 * Save the state of the simulation.
	 * 
	 * @see cobweb.UIInterface#save
	 */
	// $$$$$ Method save used to be invoked by the now silenced method
	// CobwebApplication.saveFile
	public void save(String filePath) throws java.io.IOException {
		// Open the Writer...
		java.io.FileWriter outStream = new java.io.FileWriter(filePath);

		boolean running = isRunning();

		// Pause, if needed
		if (running) {
			pause();
		}

		// Save the scheduler: blocks for appropriate timing
		theScheduler.saveScheduler(outStream);

		// Save the environment
		theEnvironment.save(outStream);

		// Close the stream
		outStream.close();

		// Resume, if needed
		if (running) {
			resume();
		}
	}

	/**
	 * Saves the current list of agents in the environment
	 */
	public boolean saveCurrentPopulation(String popName, String option, int amount) {

		// save the list of agents
		return theEnvironment.savePopulation(popName, option, amount);
	}

	/**
	 * Sets the number of frames "dropped" between updates of the frame data.
	 * Calls setSchedulerFrameSkip on the scheduler.
	 */
	public void setFrameSkip(long frameSkip) {
		theScheduler.setSchedulerFrameSkip(frameSkip);
	}

	public void setPauseButton(JButton pb) {
		pauseButton = pb;
	}

	public void setRunnable(boolean ready) {
		this.runnable = ready;
	}

	public void setTimeStopField(JTextField tickField) {
		stopTickField = tickField;
	}

	public void slowDown(long time) {
		delay = time;
		theScheduler.setSleep(delay);
	}

	/**
	 * Start the simulation. Calls startScheduler on the scheduler.
	 */
	public void start() {
		// Nothing
	}

	/** 
	 * A check is made to see if the simulation has completed.  If so, the next simulation 
	 * will be loaded.
	 */
	public void tickNotification(long tickCount) {
		myClock = tickCount;

		for (TickEventListener listener : tickListeners) {
			listener.TickPerformed(tickCount);
		}
	}

	public void tickZero() {
		refresh(true);
	}

	public void unObserve() {
		theEnvironment.unObserve();
	}

	private void updateEnvironmentDrawInfo() {
		theEnvironment.getDrawInfo(this);
		theDrawingInfo = newDrawingInfo;
		newDrawingInfo = null;
	}

	/**
	 * Write an entry into the log
	 */
	public void writeLogEntry() {
		theEnvironment.writeLogEntry();
	}

	public void writeOutput(String s) {
		myLogger.info(s);
	}

	@Override
	public boolean hasAgent(int x, int y) {
		return theEnvironment.hasAgent(x, y);
	}

	@Override
	public Agent getAgent(int x, int y) {
		return theEnvironment.getAgent(x, y);
	}

	@Override
	public boolean hasFood(int x, int y) {
		return theEnvironment.hasFood(x, y);
	}

	@Override
	public int getFood(int x, int y) {
		return theEnvironment.getFood(x, y);
	}

	@Override
	public boolean hasStone(int x, int y) {
		return theEnvironment.hasStone(x, y);
	}


	private Set<ViewerPlugin> viewers = new HashSet<ViewerPlugin>();

	@Override
	public Collection<ViewerPlugin> getViewers() {
		return viewers;
	}
}
