package application;

import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import application.InteractiveLineChart.DataSymbol;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class Demo extends Application {

	@Override
	public void start(Stage stage) throws Exception {
		//demo ui
		Button[] buttons = new Button[6];
		for (int i = 0; i < 6; i++) {
			buttons[i] = new Button(Integer.toString(i + 1));
			buttons[i].setStyle("-fx-font-size: 18");
			buttons[i].setPrefWidth(30);
		}
		
		Label label = new Label("Click buttons to load example data\nright click on chart to see available settings" + 
				"\ndrag and scroll the chart area to move the axes range and to inspect data points\ndragging and scrolling on one axis affects only that axis");
		label.setTextAlignment(TextAlignment.CENTER);
		label.setStyle("-fx-font-size: 14");
		label.setPadding(new Insets(10));
		
		//add the chart
		InteractiveLineChart chart = new InteractiveLineChart();
		
		//build the scene
		HBox hbox = new HBox(10, buttons);
		hbox.setAlignment(Pos.CENTER);
		hbox.setPadding(new Insets(10));
		
		VBox vbox = new VBox(label, hbox, new Separator(), chart);
		vbox.setAlignment(Pos.CENTER);
		VBox.setVgrow(chart, Priority.ALWAYS);
		
		Scene scene = new Scene(vbox, 800, 600);
		stage.setScene(scene);
		stage.show();
		
		//button actions, loading demo data
		//DEMO 1 -----------------------------
		buttons[0].setOnAction(_ -> {
			chart.reset();
			
			chart.seriesBuilder()
				.setDiscreteFunction(0, 2*Math.PI, 100, x -> Math.sin(x))
				.setSymbol(DataSymbol.DIAMOND)
				.setColor(Color.ORANGE)
				.setSymbolFilled(true)
				.setName("sine")
				.plot();
			
			chart.seriesBuilder()
				.setX(0, 8)
				.setY(-1, 1)
				.setLineWidth(1.5)
				.setColor(Color.DEEPSKYBLUE)
				.setSymbol(DataSymbol.SQUARE)
				.setSymbolFilled(true)
				.setName("line")
				.plot();
			
			chart.seriesBuilder()
				.setDiscreteFunction(0, 2*Math.PI, 100, x -> Math.cos(x))
				.setColor(Color.DARKOLIVEGREEN)
				.setSymbolFilled(false)
				.setName("cosine")
				.plot();
		});

		//DEMO 2 -----------------------------
		buttons[1].setOnAction(_ -> {
			chart.reset();
			
			chart.seriesBuilder()
				.setY(new double[] {-1.5, 3, 0.75, 5.0, 6, 2.334})
				.setColor(Color.CORNFLOWERBLUE)
				.setSymbol(DataSymbol.CROSS)
				.plot();
			
			chart.seriesBuilder()
				.setY(new double[] {4, 2, 7, 5.0, 3, 4})
				.setColor(Color.BROWN)
				.setLineWidth(1)
				.setSymbol(DataSymbol.PLUS)
				.plot();
		});
		
		//DEMO 3 -----------------------------
		buttons[2].setOnAction(_ -> {
			chart.reset();
	
			double[] data = {-1857, 500_000, 240_000, 20_130_568};
			chart.seriesBuilder().setY(data).plot();
		});
		
		//DEMO 4 -----------------------------
		buttons[3].setOnAction(_ -> {
			chart.reset();
			
			double[] tt = new double[201];
			for (int i = 0; i <= 200; i++) tt[i] = 2 * Math.PI * i / 200;
			double[] x = DoubleStream.of(tt).map(t -> Math.cos(t)).toArray();
			double[] y = DoubleStream.of(tt).map(t -> Math.sin(t)).toArray();
			chart.seriesBuilder()
				.setX(x)
				.setY(y)
				.setColor(Color.BLUEVIOLET)
				.setSymbol(DataSymbol.TRIANGLE)
				.setLineWidth(1.5)
				.plot();
		});
		
		//DEMO 5 -----------------------------
		buttons[4].setOnAction(_ -> {
			chart.reset();
			
			double r = 500_000, dx = 133_500, dy = -80_437;
			double[] tt = new double[201];
			for (int i = 0; i <= 200; i++) tt[i] = 2 * Math.PI * i / 200;
			double[] x = DoubleStream.of(tt).map(t -> Math.cos(t) * r + dx).toArray();
			double[] y = DoubleStream.of(tt).map(t -> Math.sin(t) * r + dy).toArray();
			for (int i = 0; i < tt.length; i += 5) {
				chart.seriesBuilder()
					.setX(dx, x[i])
					.setY(dy, y[i])
					.setColor(Color.BLACK)
					.setLineWidth(1)
					.setSymbol(DataSymbol.NONE)
					.setEnableLegendEntry(false)
					.plot();
			}
			chart.seriesBuilder()
				.setX(x)
				.setY(y)
				.setColor(Color.RED)
				.setLineWidth(3.0)
				.plot();
		});
		
		//DEMO 6 -----------------------------
		buttons[5].setOnAction(_ -> {
			chart.reset();
			
			List<Double> list = IntStream.rangeClosed(0, 300).mapToDouble(i -> 2 * Math.PI * i / 300).boxed().toList();
			chart.seriesBuilder()
				.setX(list, t -> 16 * Math.pow(Math.sin(t), 3))
				.setY(list, t -> 13 * Math.cos(t) - 5 * Math.cos(2*t) - 2 * Math.cos(3*t) - Math.cos(4*t))
				.setColor(Color.GREEN)
				.setSymbol(DataSymbol.NONE)
				.setLineWidth(1.5)
				.setName("Heart")
				.plot();
		});
	}

	public static void main(String[] args) {
		launch(args);
	}
}
