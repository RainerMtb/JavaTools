package application;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class RawViewer extends Application {
	
	//list of implemented pixel formats and converter functions
	private final Map<String, FormatConverter> formatMap = Map.of(
			"Y", new FormatConverterY(), 
			"YUV420", new FormatConverterYUV420(), 
			"YUV444", new FormatConverterYUV444(), 
			"RGB24", new FormatConverterRGB24(),
			"BGR24", new FormatConverterBGR24(),
			"NV12", new FormatConverterNV12(),
			"NV21", new FormatConverterNV21()
			);
	
	//list of predefined selectable values
	private final List<Integer> pixelList = Arrays.asList(200, 360, 480, 576, 720, 840, 1024, 1080, 1440, 1920, 2560);
	
	//collection of svg paths for icons
	private final String svgOpen = "M320 464c8.8 0 16-7.2 16-16l0-288-80 0c-17.7 0-32-14.3-32-32l0-80L64 48c-8.8 0-16 7.2-16 16l0 384c0 8.8 7.2 16 16 16l256 0" +
			"zM0 64C0 28.7 28.7 0 64 0L229.5 0c17 0 33.3 6.7 45.3 18.7l90.5 90.5c12 12 18.7 28.3 18.7 45.3L384 448c0 35.3-28.7 64-64 64L64 512c-35.3 0-64-28.7-64-64L0 64" +
			"zM80 288c0-17.7 14.3-32 32-32l96 0c17.7 0 32 14.3 32 32l0 16 44.9-29.9c2-1.3 4.4-2.1 6.8-2.1c6.8 0 12.3 5.5 12.3 12.3l0 103.4c0 6.8-5.5 12.3-12.3 12.3" +
			"c-2.4 0-4.8-.7-6.8-2.1L240 368l0 16c0 17.7-14.3 32-32 32l-96 0c-17.7 0-32-14.3-32-32l0-96z";
	
	private final String svgReload = "M379.37,335.72l5.8,5.8c9.07,9.07,23.66,9.07,32.73,0l87.29-87.29c6.61-6.61,8.66-16.57,5.05-25.23-3.61-8.66-12-14.32-21.41-14.32h-56.74" +
			"C431.33,95.95,334.9-.07,216.04,0,96.7,0,0,96.7,0,216.04s96.7,216.04,216.04,216.04c17.05,0,30.89-13.84,30.89-30.89s-13.84-30.89-30.89-30.89c-85.24," + 
			"0-154.32-69.08-154.32-154.32S130.8,61.65,216.04,61.65s153.57,68.33,154.32,152.96l-56.12.07c-9.41,0-17.8,5.66-21.41,14.32-3.61,8.66-1.57,18.62,5.05," +
			"25.23l81.49,81.49Z";
	
	private final String svgPlay = "M73 39c-14.8-9.1-33.4-9.4-48.5-.9S0 62.6 0 80L0 432c0 17.4 9.4 33.4 24.5 41.9s33.7 8.1 48.5-.9L361 297c14.3-8.7 23-24.2 23-41" +
			"s-8.7-32.2-23-41L73 39z";
	
	private final String svgPause = "M48 64C21.5 64 0 85.5 0 112L0 400c0 26.5 21.5 48 48 48l32 0c26.5 0 48-21.5 48-48l0-288c0-26.5-21.5-48-48-48L48 64zm192 0" +
			"c-26.5 0-48 21.5-48 48l0 288c0 26.5 21.5 48 48 48l32 0c26.5 0 48-21.5 48-48l0-288c0-26.5-21.5-48-48-48l-32 0z";

	//input arguments
	private static String[] args;

	//preferences store settings permanently
	private Preferences prefs = Preferences.userRoot().node("rainermtb/rawviewer");
	
	private File inputFile;
	private String inputDirectory;
	private RandomAccessFile input;
	private LoaderTask loaderTask = new LoaderTask();
	
	private Spinner<Integer> spinnerFrameIdx;
	private ComboBox<Integer> comboWidth;
	private ComboBox<Integer> comboHeight;
	private ComboBox<String> comboFormat;
	private ComboBox<Double> comboFps;
	private ImageView imageView;
	private SimpleIntegerProperty propFrameMax = new SimpleIntegerProperty(1);

	//main function
	public static void main(String[] args) {
		RawViewer.args = args;
		launch(args);
	}
	
	//start a new loader task
	private void startLoader(boolean play) {
		loaderTask.cancel(false);
		loaderTask = new LoaderTask(play);
		Thread th = new Thread(loaderTask);
		th.start();
	}
	
	//start a new loader task
	private void startLoader() {
		startLoader(false);
	}

	//set up the interface
	@Override
	public void start(Stage stage) throws Exception {
		//controls on top
		Button btnOpen = makeIconButton(svgOpen);
		Button btnReload = makeIconButton(svgReload);
		
		Label lblWidth = new Label("  Width:");
		comboWidth = new ComboBox<>(FXCollections.observableList(pixelList));
		comboWidth.setEditable(true);
		comboWidth.setPrefWidth(85);
		
		Label lblHeight = new Label("  Height:");
		comboHeight = new ComboBox<>(FXCollections.observableList(pixelList));
		comboHeight.setEditable(true);
		comboHeight.setPrefWidth(85);
		
		Label lblFormat = new Label("  Format:");
		comboFormat = new ComboBox<>(FXCollections.observableArrayList(formatMap.keySet()).sorted());
		
		Label lblFps = new Label("  FPS:");
		comboFps = new ComboBox<>(FXCollections.observableArrayList(10.0, 15.0, 25.0, 30.0, 48.0, 60.0));
		comboFps.setEditable(true);
		comboFps.setPrefWidth(70);
		comboFps.setConverter(new DoubleStringConverter()); //converter must always be provided extra

		HBox hboxTop = new HBox(6, 
				btnOpen, 
				btnReload, 
				lblWidth, 
				comboWidth, 
				lblHeight, 
				comboHeight, 
				lblFormat, 
				comboFormat,
				lblFps,
				comboFps
				);
		hboxTop.setAlignment(Pos.CENTER_LEFT);
		hboxTop.setPadding(new Insets(4));

		//image in center --------------------------
		imageView = new ImageView();
		imageView.setPreserveRatio(true);
		Pane pane = new ImageViewPane(imageView);
		pane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));

		//bottom controls --------------------------
		
		//play
		Button btnPlay = makeIconButton(svgPlay);
		
		//pause
		Button btnPause = makeIconButton(svgPause);
		
		//frame index spinner
		SpinnerValueFactory.IntegerSpinnerValueFactory svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1, 0, 1);
		svf.maxProperty().bind(propFrameMax);
		spinnerFrameIdx = new Spinner<>(svf);
		spinnerFrameIdx.setPrefWidth(85.0);
		spinnerFrameIdx.setEditable(true);
		IntegerStringConverter spinnerConverter = new IntegerStringConverter() {
			@Override public Integer fromString(String value) {
				Integer i = svf.getValue();
				try { 
					i = Integer.valueOf(value); 
				} catch (NumberFormatException e) { 
					spinnerFrameIdx.cancelEdit(); 
				}
				return i;
			}
		};
		svf.setConverter(spinnerConverter);
		
		//frame index slider
		Slider frameSlider = new Slider(0.0, 1.0, 0.0);
		frameSlider.setSnapToTicks(true);
		frameSlider.setMajorTickUnit(1.0f);
		frameSlider.setBlockIncrement(10.0f);
		frameSlider.setMinorTickCount(0);
		frameSlider.maxProperty().bind(propFrameMax);
		frameSlider.valueProperty().addListener((_, _, newval) -> {
			svf.setValue(newval.intValue());
		});
		svf.valueProperty().addListener((_, _, newval) -> {
			frameSlider.setValue(newval);
		});
		
		//labels
		Label lblFrame = new Label("Frame:");
		Label lblFrameMin = new Label("0");
		lblFrameMin.setPrefWidth(20);
		lblFrameMin.setAlignment(Pos.CENTER_RIGHT);
		Label lblFrameMax = new Label("9999");
		lblFrameMax.setPrefWidth(40);
		lblFrameMax.textProperty().bind(propFrameMax.asString());

		//hbox on bottom of window
		HBox hboxBottom = new HBox(6, 
				btnPlay, 
				btnPause, 
				new Separator(Orientation.VERTICAL), 
				lblFrame, 
				spinnerFrameIdx, 
				lblFrameMin, 
				frameSlider, 
				lblFrameMax
				);
		hboxBottom.setAlignment(Pos.CENTER_LEFT);
		hboxBottom.setPadding(new Insets(4));
		HBox.setHgrow(frameSlider, Priority.ALWAYS);

		//main vbox
		VBox mainPane = new VBox(hboxTop, pane, hboxBottom);
		VBox.setVgrow(pane, Priority.ALWAYS);

		//converter for editable combo box
		IntegerStringConverter widthHeightInputConverter = new IntegerStringConverter() {
			@Override public Integer fromString(String value) {
				Integer i = Integer.valueOf(value);
				if (i > 0 && i < 65536) return i;
				else throw new RuntimeException();
			}
		};
		
		//combo box for width and height in pixels
		comboWidth.setConverter(widthHeightInputConverter);
		comboHeight.setConverter(widthHeightInputConverter);
		
		//read preferences
		double minWidth = 650.0;
		double minHeight = 450.0;
		inputDirectory = prefs.get("folder", System.getProperty("user.home", "."));
		comboWidth.setValue(prefs.getInt("width", 1920));
		comboHeight.setValue(prefs.getInt("height", 1080));
		comboFormat.setValue(prefs.get("format", "Y"));
		comboFps.setValue(prefs.getDouble("fps", 25.0));
		stage.setX(prefs.getDouble("posx", 50));
		stage.setY(prefs.getDouble("posy", 50));
		stage.setWidth(prefs.getDouble("width", minWidth));
		stage.setHeight(prefs.getDouble("height", minHeight));
		
		//writing preferences back to storage on close
		stage.setOnCloseRequest(_ -> {
			prefs.put("folder", inputDirectory);
			prefs.putInt("width", comboWidth.getValue());
			prefs.putInt("height", comboHeight.getValue());
			prefs.put("format", comboFormat.getValue());
			prefs.putDouble("fps", comboFps.getValue());
			prefs.putDouble("posx", stage.getX());
			prefs.putDouble("posy", stage.getY());
			prefs.putDouble("width", stage.getWidth());
			prefs.putDouble("height", stage.getHeight());
			try { prefs.flush(); } catch (BackingStoreException e) {}
		});

		//build and show the scene
		Scene scene = new Scene(mainPane, minWidth, minHeight);
		stage.setScene(scene);
		stage.setMinWidth(minWidth);
		stage.setMinHeight(minHeight);
		stage.setTitle("RawViewer");
		stage.show();

		//define actions -------------------
		
		//select file
		btnOpen.setOnAction(_ -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Select Input File");
			fileChooser.setInitialDirectory(new File(inputDirectory));
			fileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*.*"));
			File selectedFile = fileChooser.showOpenDialog(stage.getOwner());
			if (selectedFile != null) {
				inputFile = selectedFile;
				try {
					input = new RandomAccessFile(inputFile, "r");
					inputDirectory = inputFile.getParentFile().getAbsolutePath();
					stage.setTitle("RawViewer - " + selectedFile);
					startLoader();
				
				} catch (IOException e) {
					new Alert(AlertType.ERROR, "Cannot open file: " + e.getMessage(), ButtonType.OK).showAndWait();
				}
			}
		});
		
		//button actions
		btnReload.setOnAction(_ -> startLoader());
		comboWidth.setOnAction(_ -> startLoader());
		comboHeight.setOnAction(_ -> startLoader());
		comboFormat.setOnAction(_ -> startLoader());
		comboFps.setOnAction(_ -> startLoader());
		spinnerFrameIdx.valueProperty().addListener(_ -> startLoader());
		
		//scrolling on the image
		pane.setOnScroll(e -> {
			int delta = e.getDeltaY() < 0 ? -1 : 1;
			svf.increment(delta);
		});
		
		//playing
		btnPlay.setOnAction(_ -> startLoader(true));
		btnPause.setOnAction(_ -> loaderTask.cancel(false));
		
		//load file when cmd argument is given
		if (args.length > 0) {
			inputFile = new File(args[0]);
			stage.setTitle("RawViewer - " + inputFile);
			startLoader();
		}
	}

	//create button with icon from svg path
	private Button makeIconButton(String svg) {
		SVGPath svgOpen = new SVGPath();
		svgOpen.setContent(svg);
		svgOpen.setScaleX(0.035);
		svgOpen.setScaleY(0.035);
		Button btn = new Button();
		btn.setGraphic(svgOpen);
		double siz = 26.0;
		btn.setMinHeight(siz);
		btn.setMinWidth(siz);
		btn.setMaxHeight(siz);
		btn.setMaxWidth(siz);
		btn.setPrefWidth(siz);
		btn.setPrefHeight(siz);
		return btn;
	}

	//nested class to async load data
	class LoaderTask extends Task<Void> {
		
		private FormatConverter converter;
		private RandomAccessFile input;
		private ImageView imageView;
		private Integer w, h;
		private long idx, idxMax, nanosPerFrame;
		private final byte[] buffer;
		private final byte[] dest;
		
		public final boolean isPlaying;
		
		LoaderTask(boolean play) {
			this.converter = formatMap.get(comboFormat.getValue());
			this.input = RawViewer.this.input;
			this.imageView = RawViewer.this.imageView;
			this.w = comboWidth.getValue();
			this.h = comboHeight.getValue();
			this.idx = spinnerFrameIdx.getValue().longValue();
			double fps = comboFps.getValue();
			this.nanosPerFrame = (long) (1e9 / fps);
			this.isPlaying = play;
			buffer = new byte[w * h * 4];
			dest = new byte[w * h * 4];
			
			//determine number of frames in input file
			long frameCount = 0;
			try { frameCount = input.length() / converter.frameSize(w, h); } catch (Exception e) {}
			
			//set max frame index into UI
			idxMax = frameCount - 1;
			long idxMaxDisplay = Math.max(idxMax, 1);
			propFrameMax.setValue(idxMaxDisplay);
		}
		
		LoaderTask() {
			this.isPlaying = false;
			this.buffer = null;
			this.dest = null;
		}
		
		@Override
		protected Void call() throws Exception {
			if (!isCancelled() && input != null && input.length() > 0 && imageView != null && w != null && w > 0 && h != null && h > 0 && idx >= 0 && idx <= idxMax) {
				//System.out.println("loading...");
				WritableImage image = new WritableImage(w, h);
				int siz = converter.frameSize(w, h);
				if (!isCancelled()) {
					input.seek(siz * idx);
					input.read(buffer, 0, siz);
					converter.convert(w, h, buffer, dest);
				}
				if (!isCancelled()) {
					image.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getByteBgraInstance(), dest, 0, w * 4);
				}
				if (!isCancelled()) {
					Platform.runLater(() -> imageView.setImage(image));
				}
				
				while (isPlaying && idx < idxMax && isCancelled() == false) {
					long nextTime = System.nanoTime() + nanosPerFrame; //wait for next frame to show
					idx++;
					input.seek(siz * idx);
					input.read(buffer, 0, siz);
					converter.convert(w, h, buffer, dest);
					image.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getByteBgraInstance(), dest, 0, w * 4);
					while (System.nanoTime() < nextTime) {}
					Platform.runLater(() -> imageView.setImage(image));
				}
			}
			return null;
		}
		
		@Override
		protected void failed() {
			Throwable e = getException();
			if (e != null) e.printStackTrace();
		}
	}
}


//subclass of Pane to always center an ImageView
class ImageViewPane extends Pane {
	
	private ImageView imageView;
	
	ImageViewPane(ImageView imageView) {
		super(imageView);
		this.imageView = imageView;
	}
	
	@Override
	protected void layoutChildren() {
		imageView.setFitWidth(getWidth());
		imageView.setFitHeight(getHeight());
		imageView.setLayoutX((getWidth() - imageView.prefWidth(getHeight())) / 2.0);
		imageView.setLayoutY((getHeight() - imageView.prefHeight(getWidth())) / 2.0);
		super.layoutChildren();
	}
}

//convert input data to bgra buffer 
interface FormatConverter {
	
	//read from input file frame number idx, convert to bgra and store into dest
	public void convert(Integer w, Integer h, byte[] buffer, byte[] dest) throws IOException;
	
	//compute the framesize in bytes
	public int frameSize(int w, int h);
	
	//input in range 0..255
	default void yuvToRgb(float y, float u, float v, byte[] dest, int pixelOffset) {
		double r = Math.rint(Math.clamp(y + (1.370705f * (v - 128.0f)), 0.0f, 255.0f));
		double g = Math.rint(Math.clamp(y - (0.337633f * (u - 128.0f)) - (0.698001f * (v - 128.0f)), 0.0f, 255.0f));
		double b = Math.rint(Math.clamp(y + (1.732446f * (u - 128.0f)), 0.0f, 255.0f));
		int offset = pixelOffset * 4;
		dest[offset + 0] = (byte) b;
		dest[offset + 1] = (byte) g;
		dest[offset + 2] = (byte) r;
		dest[offset + 3] = (byte) 255;
	}
	
	//input in range 0..255
	default void yuvToRgb(byte y, byte u, byte v, byte[] dest, int offset) {
		float fy = y & 255;
		float fu = u & 255;
		float fv = v & 255;
		yuvToRgb(fy, fu, fv, dest, offset);
	}
}

class FormatConverterY implements FormatConverter {

	@Override
	public void convert(Integer w, Integer h, byte[] buffer, byte[] dest) throws IOException {
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				int i = r * w + c;
				float y = buffer[i] & 255;
				yuvToRgb(y, 128.0f, 128.0f, dest, i);
			}
		}
	}

	@Override
	public int frameSize(int w, int h) {
		return w * h;
	}
	
}

class FormatConverterYUV420 implements FormatConverter {

	@Override
	public void convert(Integer w, Integer h, byte[] buffer, byte[] dest) throws IOException {
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				int rr = r / 2;
				int cc = c / 2;
				int offset = r * w + c;
				byte y = buffer[offset];
				int i = h * w + rr * w / 2 + cc;
				byte u = buffer[i];
				i += h * w / 4;
				byte v = buffer[i];
				yuvToRgb(y, u, v, dest, offset);
			}
		}
	}

	@Override
	public int frameSize(int w, int h) {
		return w * h * 3 / 2;
	}
	
}

class FormatConverterYUV444 implements FormatConverter {

	@Override
	public void convert(Integer w, Integer h, byte[] buffer, byte[] dest) throws IOException {
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				int offset = r * w + c;
				yuvToRgb(buffer[offset], buffer[offset + w * h], buffer[offset + 2 * w * h], dest, offset);
			}
		}
	}

	@Override
	public int frameSize(int w, int h) {
		return w * h * 3;
	}
	
}

class FormatConverterRGB24 implements FormatConverter {

	@Override
	public void convert(Integer w, Integer h, byte[] buffer, byte[] dest) throws IOException {
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				int i = r * w + c;
				dest[i * 4 + 0] = buffer[i * 3 + 2];
				dest[i * 4 + 1] = buffer[i * 3 + 1];
				dest[i * 4 + 2] = buffer[i * 3 + 0];
				dest[i * 4 + 3] = (byte) 255;
			}
		}
	}

	@Override
	public int frameSize(int w, int h) {
		return w * h * 3;
	}
	
}

class FormatConverterBGR24 implements FormatConverter {
	
	@Override
	public void convert(Integer w, Integer h, byte[] buffer, byte[] dest) throws IOException {
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				int i = r * w + c;
				dest[i * 4 + 0] = buffer[i * 3 + 0];
				dest[i * 4 + 1] = buffer[i * 3 + 1];
				dest[i * 4 + 2] = buffer[i * 3 + 2];
				dest[i * 4 + 3] = (byte) 255;
			}
		}
	}
	
	@Override
	public int frameSize(int w, int h) {
		return w * h * 3;
	}
	
}

class FormatConverterNV12 implements FormatConverter {

	@Override
	public void convert(Integer w, Integer h, byte[] buffer, byte[] dest) throws IOException {
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				int offset = r * w + c;
				int rr = r / 2;
				int cc = c / 2;
				int i = h * w + rr * w + cc * 2;
				yuvToRgb(buffer[offset], buffer[i], buffer[i + 1], dest, offset);
			}
		}
	}

	@Override
	public int frameSize(int w, int h) {
		return w * h * 3 / 2;
	}
	
}

class FormatConverterNV21 implements FormatConverter {
	
	@Override
	public void convert(Integer w, Integer h, byte[] buffer, byte[] dest) throws IOException {
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				int offset = r * w + c;
				int rr = r / 2;
				int cc = c / 2;
				int i = h * w + rr * w + cc * 2;
				yuvToRgb(buffer[offset], buffer[i + 1], buffer[i], dest, offset);
			}
		}
	}
	
	@Override
	public int frameSize(int w, int h) {
		return w * h * 3 / 2;
	}
	
}
