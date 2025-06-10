package application;

import java.util.*;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.stream.*;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.StringConverter;

public class InteractiveLineChart extends LineChart <Number, Number> {

	private double xm, xLo, xHi, ym, yLo, yHi;
	private boolean isPanning;
	private static final double ZOOM_FACTOR = 1.1;
	private static final double LEGEND_LINE_LENGTH = 10.0;
	private Map <Node, Boolean> legendEntryMap = new HashMap<>();
	
	private final CustomNumberAxis xAxis, yAxis;
	
	private final CheckMenuItem menuGrid = new CheckMenuItem("Show Grid");
	private final CheckMenuItem menuLegend = new CheckMenuItem("Show Legend");
	private final CheckMenuItem menuSymbols = new CheckMenuItem("Show Symbols");
	private final CheckMenuItem menuAxesAuto = new CheckMenuItem("Axes Autorange");
	
	//the one and only public constructor
	public InteractiveLineChart() {
		this(new CustomNumberAxis(), new CustomNumberAxis());
	}
	
	//builder class to add data conveniently
	public SeriesBuilder seriesBuilder() {
		return new SeriesBuilder();
	}
	
	private InteractiveLineChart(CustomNumberAxis xAxis, CustomNumberAxis yAxis) {
		super(xAxis, yAxis);
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		setAnimated(false);
		setAlternativeRowFillVisible(false);
		setAlternativeColumnFillVisible(false);
		setAxisSortingPolicy(SortingPolicy.NONE);
		setHorizontalZeroLineVisible(false);
		setVerticalZeroLineVisible(false);
		
		legendSideProperty().addListener(_ -> updateLegend());
		
		//put a rectangle on top of the data area because gridlines and series will catch mouse input otherwise
		Region region = (Region) lookup(".chart-plot-background");
		Rectangle plotArea = new Rectangle();
		plotArea.setFill(Color.TRANSPARENT);
		plotArea.xProperty().bind(region.layoutXProperty().add(region.getParent().layoutXProperty()));
		plotArea.yProperty().bind(region.layoutYProperty().add(region.getParent().layoutYProperty()));
		plotArea.widthProperty().bind(region.widthProperty());
		plotArea.heightProperty().bind(region.heightProperty());
		getChildren().add(plotArea);
		
		//handle mouse input
		Stream.of(plotArea, xAxis, yAxis).forEach(node -> node.setOnMousePressed(mouseEvent -> {
			if (mouseEvent.getButton() == MouseButton.PRIMARY) {
				isPanning = true;
				xm = mouseEvent.getX();
				xLo = xAxis.getLowerBound();
				xHi = xAxis.getUpperBound();
				ym = mouseEvent.getY();
				yLo = yAxis.getLowerBound();
				yHi = yAxis.getUpperBound();
			}
		}));
		plotArea.setOnMouseDragged(mouseEvent -> {
			if (isPanning) {
				xAxis.pan(mouseEvent.getX(), xm, xLo, xHi);
				yAxis.pan(mouseEvent.getY(), ym, yLo, yHi);
			}
		});
		xAxis.setOnMouseDragged(mouseEvent -> {
			if (isPanning) {
				xAxis.pan(mouseEvent.getX(), xm, xLo, xHi);
			}
		});
		yAxis.setOnMouseDragged(mouseEvent -> {
			if (isPanning) {
				yAxis.pan(mouseEvent.getY(), ym, yLo, yHi);
			}
		});
		setOnMouseReleased(_ -> {
			isPanning = false;
		});
		
		plotArea.setOnScroll(scrollEvent -> {
			double f = scrollEvent.getDeltaY() < 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
			xAxis.zoom(scrollEvent.getX(), f);
			yAxis.zoom(scrollEvent.getY(), f);
		});
		xAxis.setOnScroll(scrollEvent -> {
			if (scrollEvent.getX() > 0 && scrollEvent.getX() < xAxis.getWidth()) {
				double f = scrollEvent.getDeltaY() < 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
				xAxis.zoom(scrollEvent.getX(), f);
			}
		});
		yAxis.setOnScroll(scrollEvent -> {
			if (scrollEvent.getY() > 0 && scrollEvent.getY() < yAxis.getHeight()) {
				double f = scrollEvent.getDeltaY() < 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
				yAxis.zoom(scrollEvent.getY(), f);
			}
		});
		
		//react to window resizing, zoom axes to counteract resized viewport and have data stay in the center
		xAxis.widthProperty().addListener((_, oldVal, newVal) -> {
			if (xAxis.isAutoRanging() == false)	{
				xAxis.zoom(newVal.doubleValue() / 2.0, newVal.doubleValue() / oldVal.doubleValue());
			}
		});
		yAxis.heightProperty().addListener((_, oldVal, newVal) -> {
			if (yAxis.isAutoRanging() == false)	{
				yAxis.zoom(newVal.doubleValue() / 2.0, newVal.doubleValue() / oldVal.doubleValue());
			}
		});
		
		//context menu build up
		MenuItem menuEqual = new MenuItem("Set Axes Equal");
		menuEqual.setOnAction(_ -> setAxesEqual());
		
		ToggleGroup legendGroup = new ToggleGroup();
		Menu menuSide = new Menu("Legend Side");
		RadioMenuItem legendTop = new RadioMenuItem("Top");
		RadioMenuItem legendBottom = new RadioMenuItem("Bottom");
		RadioMenuItem legendLeft = new RadioMenuItem("Left");
		RadioMenuItem legendRight = new RadioMenuItem("Right");
		Stream.of(legendTop, legendBottom, legendLeft, legendRight).forEach(rmi -> {
			rmi.setToggleGroup(legendGroup);
			menuSide.getItems().add(rmi);
		});
		legendGroup.selectedToggleProperty().addListener((_, _, newval) -> {
			if (newval == legendTop) setLegendSide(Side.TOP);
			if (newval == legendBottom) setLegendSide(Side.BOTTOM);
			if (newval == legendLeft) setLegendSide(Side.LEFT);
			if (newval == legendRight) setLegendSide(Side.RIGHT);
		});
		legendSideProperty().addListener((_, _, newval) -> {
			if (newval == Side.TOP) legendTop.setSelected(true);
			if (newval == Side.BOTTOM) legendBottom.setSelected(true);
			if (newval == Side.LEFT) legendLeft.setSelected(true);
			if (newval == Side.RIGHT) legendRight.setSelected(true);
		});
		legendBottom.setSelected(true);
		
		ContextMenu cm = new ContextMenu(menuSide, menuEqual, new SeparatorMenuItem(), menuGrid, menuLegend, menuSymbols, menuAxesAuto);
		
		//context menu actions
		menuGrid.selectedProperty().bindBidirectional(horizontalGridLinesVisibleProperty());
		menuGrid.selectedProperty().bindBidirectional(verticalGridLinesVisibleProperty());
		menuLegend.selectedProperty().bindBidirectional(legendVisibleProperty());
		menuAxesAuto.selectedProperty().bindBidirectional(xAxis.autoRangingProperty());
		menuAxesAuto.selectedProperty().bindBidirectional(yAxis.autoRangingProperty());
		
		//context menu activate
		plotArea.setOnContextMenuRequested(ev -> cm.show(plotArea, ev.getScreenX(), ev.getScreenY()));
		setOnMousePressed(_ -> cm.hide());
	}
	
	public void setAxesEqual() {
		xAxis.setAutoRanging(false);
		yAxis.setAutoRanging(false);
		double ratio = Math.abs(xAxis.getScale()) / Math.abs(yAxis.getScale());
		if (ratio < 1) yAxis.zoom(yAxis.getHeight() / 2.0, 1 / ratio);
		if (ratio > 1) xAxis.zoom(xAxis.getWidth() / 2.0, ratio);
	}
	
	public void setLegendEntry(Node seriesNode, Boolean hasLegendEntry) {
		legendEntryMap.put(seriesNode, hasLegendEntry);
	}
	
	//clear chart and set default properties
	public void reset() {
		getData().clear();
		menuAxesAuto.setSelected(true);
	}

	
	@Override
	protected void updateLegend() {
		//System.out.println("updateLegend");
		Pane legend = getLegendSide().isHorizontal() ? new HBox(15) : new VBox(5);
		legend.getStyleClass().add("chart-legend");
		for (Series <Number, Number> series : getData()) {
			Shape seriesShape = (Shape) series.getNode();
			Boolean hasLegendEntry = legendEntryMap.getOrDefault(seriesShape, true);
			if (hasLegendEntry == null || hasLegendEntry == true) {
				Label item = new Label();
				item.textProperty().bind(series.nameProperty());
				Line line = new Line(0, 0, LEGEND_LINE_LENGTH, 0);
				line.strokeProperty().bind(seriesShape.strokeProperty());
				line.strokeWidthProperty().bind(seriesShape.strokeWidthProperty());
				item.setGraphic(line);
				item.getStyleClass().add("chart-legend-item");
				legend.getChildren().add(item);
			}
		}
		setLegend(legend.getChildren().size() > 0 ? legend : null);
	}

	
	//Builder class to add data series to the chart conveniently
	public class SeriesBuilder {
		
		private SeriesBuilder() {}
		
		/**
		 * set Values for x-axis
		 * @param series of individual double values or array of double
		 * @return Builder object
		 */
		public SeriesBuilder setX(double... x) {
			return setValues(x, 0);
		}
		
		/**
		 * set Values for y-axis
		 * @param series of individual double values or array of double
		 * @return Builder object
		 */
		public SeriesBuilder setY(double... y) {
			return setValues(y, 1);
		}
		
		/**
		 * set Values for x-axis
		 * @param collection of Double values, must not be null, must not contain null values
		 * @return Builder object
		 */
		public SeriesBuilder setX(Collection <? extends Number> x) {
			return setValues(x, 0);
		}

		/**
		 * set Values for y-axis
		 * @param collection of Double values, must not be null, must not contain null values
		 * @return Builder object
		 */
		public SeriesBuilder setY(Collection <? extends Number> y) {
			return setValues(y, 1);
		}

		/**
		 * provide a collection of arbitrary objects and an extractor function to get values for x-axis
		 * @param <E>
		 * @param items
		 * @param mapper
		 * @return Builder object
		 */
		public <E> SeriesBuilder setX(Collection <E> items, ToDoubleFunction <E> mapper) {
			return setValues(items, mapper, 0);
		}
		
		/**
		 * provide a collection of arbitrary objects and an extractor function to get values for y-axis
		 * @param <E>
		 * @param items
		 * @param mapper
		 * @return Builder object
		 */
		public <E> SeriesBuilder setY(Collection <E> items, ToDoubleFunction <E> mapper) {
			return setValues(items, mapper, 1);
		}
		
		/**
		 * provide an interval and a count to generate values for x-axis,
		 * provide a function to generate values for y-axis from each x value
		 * @param x0 start of interval for x-axis
		 * @param x1 end of interval for x-axis
		 * @param countX number of values on x
		 * @param functionXtoY generator to calculate y for x
		 * @return
		 */
		public SeriesBuilder setDiscreteFunction(double x0, double x1, int countX, DoubleUnaryOperator functionXtoY) {
			double[] x = new double[countX];
			double[] y = new double[countX];
			for (int i = 0; i < countX; i++) {
				x[i] = x0 + (x1 - x0) * i / (countX - 1);
				y[i] = functionXtoY.applyAsDouble(x[i]);
			}
			return setX(x).setY(y);
		}

		
		private double[] dataX, dataY;
		private XYChart.Series<Number, Number> series = new Series<>();
		private String name;
		private Color color = Color.BLACK;
		private Double lineWidth;
		private DataSymbol symbol;
		private boolean isFilled = false;
		private Boolean hasLegendEntry = true;
		
		/**
		 * add data to the chart
		 * @return generated Series
		 */
		public Series <Number, Number> plot() {
			if (countX() == 0) {
				dataX = IntStream.range(0, countY()).mapToDouble(i -> i).toArray();
			}
			if (countX() != countY()) {
				throw new RuntimeException("number of data elements must be equal, x=" + countX() + ", y=" + countY());
			}
			
			StringBuilder cssSymbol = new StringBuilder(""), cssLine = new StringBuilder("");
			if (symbol != null) {
				cssSymbol.append(symbol.path);
			}
			if (color != null) {
				cssLine.append("-fx-stroke: " + hexColor(color) + "; ");
				cssSymbol.append("-fx-background-color: " + hexColor(color));
				if (symbol == null && isFilled == false) cssSymbol.append(", white");
				if (symbol != null && symbol.isFillable && isFilled == false) cssSymbol.append(", white");
				cssSymbol.append(" ;");
			}
			if (lineWidth != null) {
				cssLine.append("-fx-stroke-width: " + lineWidth + "; ");
			}
			if (name == null) {
				name = "data " + getData().size();
			}
			
			ObservableList <Data <Number, Number>> dataList = FXCollections.observableArrayList();
			for (int i = 0; i < countX(); i++) dataList.add(new Data <Number, Number> (dataX[i], dataY[i]));
			series.setData(dataList);
			getData().add(series);
			series.setName(name);
			series.getNode().setStyle(cssLine.toString());

			//set properties AFTER series has been added to chart
			series.getData().forEach(d -> symbolSettings(d, cssSymbol.toString()));
			series.getData().addListener((ListChangeListener<Data<Number, Number>>) change -> {
				while (change.next()) {
					for (Data<Number, Number> d : change.getAddedSubList()) symbolSettings(d, cssSymbol.toString());
				}
			});
			setLegendEntry(series.getNode(), hasLegendEntry);
			return series;
		}
		
		public SeriesBuilder setName(String name) {
			this.name = name;
			return this;
		}
		
		public SeriesBuilder setColor(Color color) {
			this.color = color;
			return this;
		}
		
		public SeriesBuilder setLineWidth(double width) {
			this.lineWidth = width;
			return this;
		}
		
		public SeriesBuilder setSymbol(DataSymbol symbol) {
			this.symbol = symbol;
			return this;
		}
		
		public SeriesBuilder setSymbolFilled(boolean isFilled) {
			this.isFilled = isFilled;
			return this;
		}
		
		public SeriesBuilder setEnableLegendEntry(boolean hasLegendEntry) {
			this.hasLegendEntry = hasLegendEntry;
			return this;
		}
		
		//private members ------------------------------
		
		private void symbolSettings(Data <Number, Number> d, String cssSymbol) {
			Node node = d.getNode();
			node.setStyle(cssSymbol);
			node.visibleProperty().bind(menuSymbols.selectedProperty());
			//Tooltip tt = new Tooltip(String.format("x=%1.4f\ny=%1.4f", d.getXValue(), d.getYValue()));
			//tt.setShowDelay(Duration.millis(200));
			//Tooltip.install(node, tt);
		}
		
		private <E> SeriesBuilder setValues(Collection <E> elements, ToDoubleFunction <E> mapper, int axisIdx) {
			return setValues(elements.stream().mapToDouble(mapper).toArray(), axisIdx);
		}
		
		private SeriesBuilder setValues(Collection <? extends Number> val, int axisIdx) {
			return setValues(val.stream().mapToDouble(d -> d.doubleValue()).toArray(), axisIdx);
		}
		
		private SeriesBuilder setValues(double[] values, int axisIdx) {
			if (axisIdx == 0) {
				dataX = values;
				
			} else if (axisIdx == 1) {
				dataY = values;
				
			} else {
				throw new RuntimeException("internal error");
			}
			return this;
		}
		
		private int countX() {
			return countArray(dataX);
		}
		
		private int countY() {
			return countArray(dataY);
		}
		
		private int countArray(double[] array) {
			return array == null ? 0 : array.length;
		}
		
		private String hexColor(Color color) {
			return "#" + hex(color.getRed()) + hex(color.getGreen()) + hex(color.getBlue()) + hex(color.getOpacity());
		}
		
		private String hex(double colorValue) {
			return String.format("%02x", (int) (colorValue * 255));
		}
	}

	
	public enum DataSymbol {
		DIAMOND	("-fx-background-radius: 0; -fx-background-insets: 0, 2.5; "
				+ "-fx-padding: 6px 5px 6px 5px; -fx-shape: \"M 5,0 L 10,9 L 5,18 L 0,9 Z\"; ", true),
		CIRCLE ("-fx-background-insets: 0, 2; -fx-background-radius: 5px; -fx-padding: 5px; ", true),
		
		SQUARE ("-fx-background-radius: 0; -fx-padding: 4.5px; ", true),
		
		TRIANGLE ("-fx-background-radius: 0; -fx-background-insets: 0 0 1 0, 2.7 2.2 3 2.2; -fx-shape: \"M5,0 L10,8 L0,8 Z\"; ", true),
		
		CROSS ("-fx-background-radius: 0; -fx-background-insets: 0; "
				+ "-fx-shape: \"M2,0 L5,4 L8,0 L10,0 L10,2 L6,5 L10,8 L10,10 L8,10 L5,6 L2, 10 L0,10 L0,8 L4,5 L0,2 L0,0 Z\"; ", false),
		PLUS ("-fx-background-radius: 0; -fx-background-insets: 0; "
				+ "-fx-shape: \"M0,0 h 4 v -4 h 2 v 4 h 4 v 2 h -4 v 4 h -2 v -4 h -4 v -2 Z\"; ", false),
		NONE ("-fx-padding: 0px; ", false),
		;

		private String path;
		private boolean isFillable;
		
		private DataSymbol(String path, boolean isFillable) {
			this.path = path;
			this.isFillable = isFillable;
		}
	}
}


class CustomNumberAxis extends ValueAxis <Number> {

	private static final double MARGIN = 0.01;		//distance between plot area and edge of graph
	private static final double TICK_GAP = 10.0;	//minimal gap between tick labels
	
	private int tickMagnitude = 0;
	private double majorTickDelta = 50.0;
	private List <Number> majorTickValues = new ArrayList<>();
	private double tickLabelSize = 25.0;
	private double tickLabelWidth = 0.0;
	private double[] tickRange = new double[3];
	
	//default formatter for tick labels for this axis
	private final StringConverter<Number> tickLabelFormatter = new StringConverter<>() {

		@Override
		public String toString(Number number) {
			String str;
			double value = number.doubleValue();
			if (tickMagnitude > 4) str = String.format("%1.0f E%d", value / Math.pow(10, tickMagnitude), tickMagnitude);
			else if (tickMagnitude >= 0) str = String.format("%,1.0f", value);
			else str = String.format("%,1." + (-tickMagnitude) + "f", value);
			return str;
		}

		@Override
		public Number fromString(String string) {
			return null;
		}
		
	};
	
	{
		setTickLabelFormatter(tickLabelFormatter);
	}
	
	
	void zoom(double center, double f) {
		setAutoRanging(false);
		double mid = getValueForDisplay(center).doubleValue();
		double lo = getLowerBound();
		double hi = getUpperBound();
		setLowerBound(mid - (mid - lo) * f);
		setUpperBound(mid + (hi - mid) * f);
		double s = calculateNewScale(axisLength(), getLowerBound(), getUpperBound());
		setScale(s);
	}
	
	void pan(double mousePos, double m, double lo, double hi) {
		setAutoRanging(false);
		double delta = (m - mousePos) / getScale();
		setLowerBound(lo + delta);
		setUpperBound(hi + delta);
	}
	
	private double axisLength() {
		return isYAxis() ? getHeight() : getWidth();
	}
	
	private boolean isYAxis() {
		return getSide().isVertical();
	}
	
	
	@Override
	protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
		double margin = (maxValue - minValue) * MARGIN;
		double lo = minValue - margin, hi = maxValue + margin;
		double scale = calculateNewScale(length, lo, hi);
		return new double[] {lo, hi, scale, length};
	}
	
	@Override
	protected void setRange(Object rangeObj, boolean animate) {
		double[] range = (double[]) rangeObj;
		setLowerBound(range[0]);
		currentLowerBound.set(range[0]);
		setUpperBound(range[1]);
		setScale(range[2]);
	}

	@Override
	protected Object getRange() {
		double[] range = new double[] { getLowerBound(), getUpperBound(), getScale() };
		return range;
	}
	
	@Override
	protected List <Number> calculateTickValues(double length, Object rangeObj) {
		double[] range = (double[]) rangeObj;
		if (Arrays.equals(range, tickRange) == false) {							// recalculate ticks only when range has changed
			//System.out.println("calculateTickValues " + (isYAxis() ? "Y " : "X ") + Arrays.toString(range));
			tickRange = range;
			double lo = range[0], hi = range[1], scale = Math.abs(range[2]);
			double deltaStart = tickLabelSize / scale;
			TickDelta tickDelta = new TickDelta(0);								// find tick interval based on previous labels
			while (tickDelta.totalValue() > deltaStart) tickDelta = tickDelta.decrease();
			while (tickDelta.totalValue() <= deltaStart) tickDelta = tickDelta.increase();
			
			while (true) {
				tickMagnitude = tickDelta.magnitude();
				majorTickDelta = tickDelta.totalValue();
				majorTickValues.clear();
				double delta = majorTickDelta;
	
				double labelSize = 5.0;
				double labelWidth = 0.0;
				double majorTick = Math.floor(lo / delta) * delta; 				// first major tick mark likely outside visible range
				while (majorTick < hi && majorTickValues.size() < 1000) {		// put together list of ticks, safeguard list size
					majorTickValues.add(majorTick);
					majorTick += delta;
					String str = tickLabelFormatter.toString(majorTick);		// find max label size to fit
					Dimension2D dim = measureTickMarkLabelSize(str, getTickLabelRotation());
					double d;
					d = isYAxis() ? dim.getHeight() : dim.getWidth();
					if (d > labelSize) labelSize = d;
					d = dim.getWidth();
					if (d > labelWidth) labelWidth = d;
				}
				
				double deltaPixel = delta * scale;
				if (deltaPixel < labelSize + TICK_GAP) {
					tickDelta = tickDelta.increase();
					
				} else {
					tickLabelSize = labelSize;
					tickLabelWidth = labelWidth;
					break;
				}
			}
			//System.out.println(majorTickValues);
		}
		
		return majorTickValues;
	}
	
	@Override
	protected List <Number> calculateMinorTickMarks() {
		int tickCount = getMinorTickCount();
		double deltaTick = majorTickDelta / tickCount;
		return majorTickValues.stream().mapToDouble(n -> n.doubleValue())
				.flatMap(d -> DoubleStream.iterate(d + deltaTick, dd -> dd + deltaTick).limit(tickCount - 1))
				.mapToObj(d -> d).collect(Collectors.toCollection(ArrayList::new));
	}

	@Override
	protected String getTickMarkLabel(Number value) {
		return tickLabelFormatter.toString(value);
	}
	
	@Override
	protected double computePrefWidth(double height) {
		double width = 100.0;
		if (isYAxis()) {
			calculateTickValues(height, autoRange(height));
			double tickMarkLength = isTickMarkVisible() && getTickLength() > 0 ? getTickLength() : 0;
			width = tickLabelWidth + getTickLabelGap() + tickMarkLength;
		}
		return width;
	}
	
	
	private record TickDelta(int index) {
		
		private static final int DELTA_LIST[] = { 1, 2, 5 };	//possible steps between major ticks
		
		TickDelta increase() { return new TickDelta(index + 1); }
		
		TickDelta decrease() { return new TickDelta(index - 1); } 
		
		int magnitude() { 
			return index / DELTA_LIST.length; 
		}
		
		int baseValue() {
			int idx = index % DELTA_LIST.length;
			if (index < 0) idx += DELTA_LIST.length - 1;
			return DELTA_LIST[idx]; 
		}
		
		double totalValue() { 
			return baseValue() * Math.pow(10.0, magnitude()); 
		}
	}
}