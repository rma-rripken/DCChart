package com.rma.dcchart;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.util.Log;
import org.jfree.util.PrintStreamLogTarget;

/**
 * An example of a time series chart. For the most part, default settings are used, except that the renderer is modified
 * to show filled shapes (as well as lines) at each data point.
 *
 */
public class SlowdownChart extends ApplicationFrame
{
	public final static Logger logger = Logger.getLogger(SlowdownChart.class.getName());
	protected String computeName = null;
	protected Date startTime = null;

	/**
	 * A demonstration application showing how to create a simple time series chart. This example uses monthly data.
	 *
	 * @param title the frame title.
	 */
	public SlowdownChart(final String title)
	{
		super(title);
	}

	public void fill(XYDataset dataset)
	{

		final JFreeChart chart = createChart(dataset);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 570));
		chartPanel.setMouseZoomable(true, false);
		setContentPane(chartPanel);

	}

	/**
	 * Creates a chart.
	 *
	 * @param dataset a dataset.
	 *
	 * @return A chart.
	 */
	private JFreeChart createChart(final XYDataset dataset)
	{
		String name = computeName;
		if (this.computeName == null || computeName.isEmpty()) {
			name = "WAT compute";
		}

		final JFreeChart chart = ChartFactory.createTimeSeriesChart(
			name,
			"Date", "Events",
			dataset,
			true,
			true,
			false);

		chart.setBackgroundPaint(Color.white);

//        final StandardLegend sl = (StandardLegend) chart.getLegend();
//        sl.setDisplaySeriesShapes(true);

		final XYPlot plot = chart.getXYPlot();

	



		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
//        plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
//		plot.setDomainCrosshairVisible(true);
//		plot.setRangeCrosshairVisible(true);

		final XYItemRenderer renderer = plot.getRenderer();
		if (renderer instanceof StandardXYItemRenderer) {
			final StandardXYItemRenderer rr = (StandardXYItemRenderer) renderer;
			//rr.setPlotShapes(true);
			rr.setShapesFilled(true);
			rr.setItemLabelsVisible(true);
		}



		//////////////
		final StandardXYItemRenderer renderer2 = new StandardXYItemRenderer();
		renderer2.setSeriesPaint(0, Color.black);
		// renderer2.setPlotShapes(true);
		// renderer.setToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
		plot.setRenderer(1, renderer2);
		/////////

		final DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("MMM-dd-yyyy HH:mm"));

		return chart;

	}

//	// create rate line...
//	private XYDataset createDataset2(Map<String, TimeSeries> map)
//	{
//
//		final TimeSeriesCollection dataset = new TimeSeriesCollection();
//		Date lastDate = null;
//		for (Entry<String, TimeSeries> entry : map.entrySet()) {
//			String name = entry.getKey();
//			TimeSeries timeSeries = entry.getValue();
//
//			dataset.addSeries(getDeltaSeries(timeSeries));
//			//dataset.addSeries(getHrsTo500Series(timeSeries));
//			Date estCompletion = getEstCompletion(timeSeries);
//			if(lastDate == null || estCompletion.getTime() > lastDate.getTime()){
//				lastDate = estCompletion;
//			}
//
//
//		}
//
//		logger.info("Slowest node est to complete " + lastDate);
//
//		return dataset;
//
//	}

	public void addTsEntry(Map<String, TimeSeries> map, String name, String dateStr)
	{
		try {

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			Date date = format.parse(dateStr);

			TimeSeries ts = map.get(name);
			if (ts == null) {
				ts = new TimeSeries(name, Millisecond.class);
				if (this.startTime != null) {
					ts.addOrUpdate(new Millisecond(startTime), 0);
				}
				map.put(name, ts);
			}



			ts.addOrUpdate(new Millisecond(date), ts.getItemCount() + 1);
		} catch (ParseException ex) {
			Logger.getLogger(SlowdownChart.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Starting point for the demonstration application.
	 *
	 * @param args ignored.
	 */
	public static void main(final String[] args)
	{
		final String filepath;
		if (args != null && args.length > 0) {
			filepath = args[0];
		} else {
			filepath = "J:\\src\\wat\\dev\\install\\HEC-WAT\\wat - error.log";
		}

		logger.fine("starting with:" + filepath);

		Log.getInstance().addTarget(new PrintStreamLogTarget());
		final SlowdownChart demo = new SlowdownChart("Time Series Demo 1");
		demo.setVisible(true);

		SwingWorker worker = new SwingWorker<Map<String, TimeSeries>, Void>()
		{
			@Override
			protected Map<String, TimeSeries> doInBackground() throws Exception
			{
				Map<String, TimeSeries> map = demo.getMapFromFile(filepath);
				return map;
			}

			@Override
			protected void done()
			{
				try {
					super.done();
					Map<String, TimeSeries> map = get();
					XYDataset dataset = demo.createDatasetFromMap(map);
//					XYDataset dataset2 = demo.createDataset2(map);

					demo.fill(dataset);
					demo.pack();
					RefineryUtilities.centerFrameOnScreen(demo);
					demo.setVisible(true);
				} catch (InterruptedException ex) {
					Logger.getLogger(SlowdownChart.class.getName()).log(Level.SEVERE, null, ex);
				} catch (ExecutionException ex) {
					Logger.getLogger(SlowdownChart.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		};

		worker.execute();


	}

	public void populateTSMap(File f,
		Map<String, TimeSeries> tsMap)
	{
		Reader input = null;

		try {
			input = new FileReader(f);
		} catch (FileNotFoundException ex) {
			Logger.getLogger(SlowdownChart.class.getName()).log(Level.SEVERE, null, ex);
		}


		if (input != null) {


			String timepart = "^(\\d+\\-\\d+\\-\\d+\\s+\\d+\\:\\d+\\:\\d+\\.\\d+)\\s+";
			

			String regex = timepart + "(.*)\\(OutputTracker.java:298\\) Realization:(\\d+),(.*)$";

			Pattern eventPattern = Pattern.compile(regex);
		
			
			// should I be looking for lines like:
//			computeRealization:Lifecycle compute took 39.7 seconds

			BufferedReader b = new BufferedReader(input);
			try {

				String line = b.readLine();

				while (line != null) {

					Matcher matcher = eventPattern.matcher(line);

					if (matcher.matches()) {
						//	logger.info("had match");
						MatchResult match = matcher.toMatchResult();
//				 for (int i = 0; i < match.groupCount(); i++) {
//						 System.out.println(i + ":" + match.group(i));
//					 }

						String seriesName = match.group(3);
						String dateStr = match.group(1);
						addTsEntry(tsMap, seriesName, dateStr);
					} 
					
					line = b.readLine();
				}

			} catch (IOException ex) {
				Logger.getLogger(SlowdownChart.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				if (b != null) {
					try {
						b.close();
					} catch (IOException ex) {
						Logger.getLogger(SlowdownChart.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		}
	}

	public Map<String, TimeSeries> parseFileIntoTSMap(File f)
	{
		Map<String, TimeSeries> tsMap = new TreeMap<String, TimeSeries>();
		populateTSMap(f, tsMap);
		return tsMap;
	}

	public XYDataset createDatasetFromMap(Map<String, TimeSeries> tsMap)
	{
		final TimeSeriesCollection dataset = new TimeSeriesCollection();
		for (Entry<String, TimeSeries> entry : tsMap.entrySet()) {
			String string = entry.getKey();
			TimeSeries timeSeries = entry.getValue();
			dataset.addSeries(timeSeries);
		}

		dataset.setDomainIsPointsInTime(true);


		return dataset;
	}

	public Map<String, TimeSeries> getMapFromFile(String filepath)
	{
		File f = new File(filepath);
		Map<String, TimeSeries> tsMap = parseFileIntoTSMap(f);
		return tsMap;
	}

	private TimeSeries getDeltaSeries(TimeSeries timeSeries)
	{

		TimeSeries newTS = null;
		if (timeSeries != null) {
			String key = (String) timeSeries.getKey();
			newTS = new TimeSeries(key, Millisecond.class);

			List items = timeSeries.getItems();
			int itemCount = timeSeries.getItemCount();
			for (int i = 1; i < itemCount; i++) {
				RegularTimePeriod timePeriod = timeSeries.getTimePeriod(i);
				double diff =
					(timePeriod.getLastMillisecond() - timeSeries.getTimePeriod(i - 1).getFirstMillisecond()) / (60 *
					1000.0);
				newTS.add(timePeriod, diff);
			}
		}

		return newTS;
	}

	private TimeSeries getHrsTo500Series(TimeSeries timeSeries)
	{

		TimeSeries newTS = null;
		if (timeSeries != null) {
			String key = (String) timeSeries.getKey();
			newTS = new TimeSeries(key, Millisecond.class);

			List items = timeSeries.getItems();
			int itemCount = timeSeries.getItemCount();
			for (int i = 51; i < itemCount; i++) {
				RegularTimePeriod timePeriod = timeSeries.getTimePeriod(i);
				long elapsed = timePeriod.getLastMillisecond() - this.startTime.getTime();
				double millis_per_hr = 1000.0 * 60 * 60;

				double hrs_elapsed = elapsed / millis_per_hr;

				double hrs_per_event = hrs_elapsed / i;

				double hrs_to_500 = 500 * hrs_per_event;

				double remaining = hrs_to_500 - hrs_elapsed;

				newTS.add(timePeriod, remaining);

			}
		}

		return newTS;
	}

	private Date getEstCompletion(TimeSeries timeSeries)
	{
		Date retval = null;
		if (timeSeries != null) {
			String key = (String) timeSeries.getKey();
			int itemCount = timeSeries.getItemCount();
			int eventnum = itemCount - 1;
			RegularTimePeriod timePeriod = timeSeries.getTimePeriod(eventnum);
			retval = getEstCompletion(startTime.getTime(), timePeriod.getFirstMillisecond(), itemCount-1, 500);
			logger.info(key + " estimated to complete " + retval);
		}
		return retval;
	}

	private Date getEstCompletion(long start, long now, int eventnum, int total){
		long elapsed = now - start;

			double m_per_event = elapsed / eventnum;

			int remaining = total - eventnum;

			double m_remaining = remaining * m_per_event;

			Date estCompletion = new Date((long) m_remaining + now);
			return estCompletion;
	}

}
