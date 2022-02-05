package application;
	
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;


public class Main extends Application {
	private static final String FILENAME = "CThead";
	private static final int PICTURE_WIDTH = 256;
	private static final int PICTURE_HEIGHT = 256;
	private static final int PICTURE_NUMBER = 113;
	
	private static final int MIN_RESOLUTION = 32;
	private static final int MAX_RESOLUTION = 1024;
	private static final int DEFAULT_RESOLUTION = 256;
	
	private static final double MIN_GAMMA = .1;
	private static final double MAX_GAMMA = 4;
	private static final double DEFAULT_GAMMA = 1;
	
	//default state
	private static final int DEFAULT_IMAGE = 76;
	
	ImageView imageView;
	private short cthead[][][];
	private float grey[][][];
	
	//current state variables;
	private int currentlyDisplayedImage = DEFAULT_IMAGE;
	private int currentlyDisplayedSize = DEFAULT_RESOLUTION;
	
	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("CThead Viewer");
		
		try {
			readData(FILENAME);
		} catch (IOException e) {
			System.out.println("Could not find CThead file in the working directory.");
			System.out.println("Working Directory = " + System.getProperty("user.dir"));
			e.printStackTrace();
			System.exit(1);
		}
		
		// get image slice
		Image top_image = GetSlice();
		imageView = new ImageView(top_image);
		
		// create buttons
		final ToggleGroup group = new ToggleGroup();
		
		RadioButton rb1 = new RadioButton("Nearest neighbour");
		rb1.setToggleGroup(group);
		rb1.setSelected(true); // set as default
		
		RadioButton rb2 = new RadioButton("Bilinear");
		rb2.setToggleGroup(group);
		
		//create sliders
		Slider sizeSlider =
				new Slider(MIN_RESOLUTION, MAX_RESOLUTION, DEFAULT_RESOLUTION);
		
		Slider gammaSlider = new Slider(MIN_GAMMA, MAX_GAMMA, DEFAULT_GAMMA);
		
		group.selectedToggleProperty().addListener((ob, o, n) -> {
			if (rb1.isSelected()) {
				System.out.println("Radio button 1 clicked");
			} else if (rb2.isSelected()) {
				System.out.println("Radio button 2 clicked");
			}
		});
		
		sizeSlider.valueProperty().addListener((ob, oldVal, newVal) -> {
			System.out.println("New size value: " + newVal.intValue());
			
			imageView.setImage(null); // clear the old image
			Image newImage = getSlice(76, newVal.intValue()); // TODO change fixed 76 to given value
			imageView.setImage(newImage); // Update the GUI so the new image is displayed
		});
		
		gammaSlider.valueProperty().addListener((ob, oldVal, newVal) -> {
			System.out.println("New gamma value: " + newVal.doubleValue());
			
			
		});
		
		// build main GUI scene
		VBox root = new VBox();
		
		root.getChildren().addAll(rb1, rb2, gammaSlider, sizeSlider, imageView);
		
		Scene scene = new Scene(root, 1024, 768);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	private void readData(String filename) throws IOException {
		File file = new File(filename);
		
		DataInputStream in = new DataInputStream(
				new BufferedInputStream(new FileInputStream(file)));
		
		short min = Short.MAX_VALUE;
		short max = Short.MIN_VALUE;
		
		short read;
		int b1;
		int b2;

		// allocate memory for the dataset
		cthead = new short[PICTURE_NUMBER][PICTURE_WIDTH][PICTURE_HEIGHT];
		grey = new float[PICTURE_NUMBER][PICTURE_WIDTH][PICTURE_HEIGHT];
		
		// read the data
		for (int k = 0; k < 113; k++) {
			for (int j = 0; j < 256; j++) {
				for (int i = 0; i < 256; i++) {
					// swap bytes
					b1 = ((int) in.readByte()) & 0xff;
					b2 = ((int) in.readByte()) & 0xff;
					read = (short) ((b2 << 8) | b1);
					
					if (read < min) {
						min = read; // update the minimum
					}

					if (read > max) {
						max = read; // update the maximum
					}

					cthead[k][j][i] = read;
					
					// scale color to 0-1 values
					grey[k][j][i] = ((float) cthead[k][j][i] - (float) min) / ((float) max - (float) min);
				}
			}
		}
		
		// diagnostic - forCThead this should be -1117, 2248
		System.out.println(min + " " + max);
	}
	
	public Image getSlice(int imageNumber, int size) {
		WritableImage image = new WritableImage(size, size);
		
		PixelWriter image_writer = image.getPixelWriter();
		
		// perform resize using nearest neighbor
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				// calculate relative position in original image
				int relativeX = (int)(x*(DEFAULT_RESOLUTION/(double)size));
				int relativeY = (int)(y*(DEFAULT_RESOLUTION/(double)size));
				
				float val = grey[imageNumber][relativeY][relativeX];
				Color color = Color.color(val, val, val);

				// Apply the new colour
				image_writer.setColor(x, y, color);
			}
		}
		
		return image;
	}
	
	//TODO delete this one, just for sake of test
	public Image GetSlice() {
		WritableImage image = new WritableImage(256, 256);
		// Find the width and height of the image to be process
		int width = (int) image.getWidth();
		int height = (int) image.getHeight();
		float val;

		// Get an interface to write to that image memory
		PixelWriter image_writer = image.getPixelWriter();

		// Iterate over all pixels
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// For each pixel, get the colour from the cthead slice 76
				val = grey[76][y][x];
				Color color = Color.color(val, val, val);

				// Apply the new colour
				image_writer.setColor(x, y, color);
			}
		}
		return image;
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
