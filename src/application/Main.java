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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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
	private int currentImage = DEFAULT_IMAGE;
	private int currentSize = DEFAULT_RESOLUTION;
	private ResizeMethod currentResizeMethod = ResizeMethod.NEAREST_NEIGHBOUR;
	
	enum ResizeMethod {
		NEAREST_NEIGHBOUR,
		BILINEAR_INTERPOLATION
	}
	
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
		Image top_image = getSlice();
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
				currentResizeMethod = ResizeMethod.NEAREST_NEIGHBOUR;
				System.out.println("Resize method changed to NEAREST_NEIGHBOUR");
			} else if (rb2.isSelected()) {
				currentResizeMethod = ResizeMethod.BILINEAR_INTERPOLATION;
				System.out.println("Resize method changed to BILINEAR INTERPOLATION");
			}
			
			imageView.setImage(null); // clear the old image
			Image newImage = getSlice();
			imageView.setImage(newImage); // Update the GUI so the new image is displayed
		});
		
		sizeSlider.valueProperty().addListener((ob, oldVal, newVal) -> {
			System.out.println("New size value: " + newVal.intValue());
			
			//set new size value
			currentSize = newVal.intValue();
			
			imageView.setImage(null); // clear the old image
			Image newImage = getSlice();
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
		
		createThumbWindow(0,0);
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
	
	public Image getSlice() {
		WritableImage image = new WritableImage(currentSize, currentSize);
		
		PixelWriter image_writer = image.getPixelWriter();
		
		if(currentResizeMethod == ResizeMethod.NEAREST_NEIGHBOUR) {
			// perform resize using nearest neighbor
			for (int y = 0; y < currentSize; y++) {
				for (int x = 0; x < currentSize; x++) {
					// calculate relative position in original image
					int relativeX = (int)(x*(DEFAULT_RESOLUTION/(double)currentSize));
					int relativeY = (int)(y*(DEFAULT_RESOLUTION/(double)currentSize));
					
					float val = grey[currentImage][relativeY][relativeX];
					Color color = Color.color(val, val, val);

					// Apply the new colour
					image_writer.setColor(x, y, color);
				}
			}
		} else if(currentResizeMethod == ResizeMethod.BILINEAR_INTERPOLATION) {
			System.out.println("Bilinear Resizing should be implemented");
		}
		
		
		
		return image;
	}
	
	public void createThumbWindow(double atX, double atY) {
		// create image containing all thumbs
		WritableImage thumbImage = new WritableImage(500, 500);
		ImageView thumbView = new ImageView(thumbImage);
		PixelWriter imageWriter = thumbImage.getPixelWriter();
		
		// make the image initially white
		for (int x = 0; x < thumbImage.getHeight(); x++) {
			for (int y = 0; y < thumbImage.getWidth(); y++) {
				imageWriter.setColor(x, y, Color.WHITE);
			}
		}
		
		// TODO create final variables for rows and columns as they are fixed
		// it would be sth like: rowNumber = 10; imagesInCol = 12; thumbImageSize = 38; gap = 4;
		// loop through each image in particular row and column
		for(int row = 0; row < 10; row++) { 
			for(int col = 0; col < 12; col++) {
				// draw image resized using nearest neighour technique
				for(int x = 0; x < 38; x++) {
					for(int y = 0; y < 38; y++) {
						int pictureNo = row * 12 + col;

						if(pictureNo < PICTURE_NUMBER) { // in case last row is not perfect
							int relativeX = (int) (x*DEFAULT_RESOLUTION/(double)38);
							int relativeY = (int) (y*DEFAULT_RESOLUTION/(double)38);

							float val = grey[pictureNo][relativeX][relativeY];
							Color color = Color.color(val, val, val);
							
							imageWriter.setColor(y + col*(38+4), x + row*(38+4), color);
						}
					}
				}
			}
		}
		
		// create layout for the image
		StackPane thumbLayout = new StackPane();
		thumbLayout.getChildren().add(thumbView);
		
		// create new scene
		Scene thumbViewScene = new Scene(thumbLayout, thumbImage.getWidth(), thumbImage.getHeight());
		
		thumbView.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {			
			//find out which picture is underneath
			double hoverX = event.getX();
			double hoverY = event.getY();
			
			int selectedColumn = (int) (hoverX/(38+4));
			int selectedRow = (int) (hoverY/(38+4));
			
			//check if it is not in the gap between
			if (hoverX % (38+4) <= 38 && hoverY % (38+4) <= 38) {
				int selectedPicture = selectedRow * 12 + selectedColumn;
				
				//check if its not out of the thumbnails
				if (selectedPicture < PICTURE_NUMBER) {
					// set displayed picture to new one, refresh the view
					this.currentImage = selectedPicture;
					
					imageView.setImage(null); // clear the old image
					Image newImage = getSlice();
					imageView.setImage(newImage); // Update the GUI so the new image is displayed
				}
			}

			event.consume();
		});
		
		// create window
		Stage newWindow = new Stage();
		newWindow.setTitle("CThead Slices");
		newWindow.setScene(thumbViewScene);
		
		newWindow.setX(atX);
		newWindow.setY(atY);
		
		newWindow.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
