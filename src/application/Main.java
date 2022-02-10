package application;
	
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class Main extends Application {
	private static final String FILENAME = "CThead";

	private static final int SCENE_WIDTH = 1024;
	private static final int SCENE_HEIGHT = 768;
	
	private static final int PICTURE_WIDTH = 256;
	private static final int PICTURE_HEIGHT = 256;
	private static final int PICTURE_NUMBER = 113;
	
	private static final int MIN_RESOLUTION = 32;
	private static final int MAX_RESOLUTION = 1024;
	private static final int DEFAULT_RESOLUTION = 256;
	
	private static final double MIN_GAMMA = .1;
	private static final double MAX_GAMMA = 4;
	private static final double DEFAULT_GAMMA = 1;
	
	private static final int DEFAULT_IMAGE = 76;
	
	private static final ResizeMethod DEFAULT_RESIZE_METHOD 
											= ResizeMethod.NEAREST_NEIGHBOUR;
	
	ImageView imageView; // ImageView of the displayed image
	private short cthead[][][];
	private float grey[][][];
	
	//current state variables;
	private int currentImage = DEFAULT_IMAGE;
	private int currentSize = DEFAULT_RESOLUTION;
	private double currentGamma = DEFAULT_GAMMA;
	private ResizeMethod currentResizeMethod = DEFAULT_RESIZE_METHOD;
	
	enum ResizeMethod {
		NEAREST_NEIGHBOUR,
		BILINEAR_INTERPOLATION
	}
	
	@Override
	public void start(Stage primaryStage) {		
		primaryStage.setTitle("CThead Viewer");
		
		try {
			this.readData(FILENAME);
		} catch (IOException e) {
			System.out.println("Could not find CThead file in the working directory.");
			System.out.println("Working Directory = " + System.getProperty("user.dir"));
			e.printStackTrace();
			System.exit(1);
		}
		
		// get image slice
		Image topImage = getSlice();
		imageView = new ImageView(topImage);
		
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
				this.currentResizeMethod = ResizeMethod.NEAREST_NEIGHBOUR;
				System.out.println("Resize method changed to NEAREST_NEIGHBOUR");
			} else if (rb2.isSelected()) {
				this.currentResizeMethod = ResizeMethod.BILINEAR_INTERPOLATION;
				System.out.println("Resize method changed to BILINEAR INTERPOLATION");
			}

			this.updateImage(imageView);
		});
		
		sizeSlider.valueProperty().addListener((ob, oldVal, newVal) -> {	
			//set new size value
			this.currentSize = newVal.intValue();
			
			this.updateImage(imageView);
		});
		
		gammaSlider.valueProperty().addListener((ob, oldVal, newVal) -> {
			//set new gamma value
			this.currentGamma = newVal.doubleValue();
			
			this.updateImage(imageView);
		});
		
		// build main GUI scene
		VBox root = new VBox();
		
		root.getChildren().addAll(rb1, rb2, gammaSlider, sizeSlider, imageView);
		
		Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
		primaryStage.setScene(scene);
		primaryStage.show();
		
		this.createThumbWindow(0,0);
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
		for (int k = 0; k < PICTURE_NUMBER; k++) {
			for (int j = 0; j < PICTURE_WIDTH; j++) {
				for (int i = 0; i < PICTURE_HEIGHT; i++) {
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
		
		PixelWriter imageWriter = image.getPixelWriter();
		
		double relativeDivisor = DEFAULT_RESOLUTION/(double)currentSize;
		
		if(currentResizeMethod == ResizeMethod.NEAREST_NEIGHBOUR) {
			// perform resize using nearest neighbor
			for (int y = 0; y < currentSize; y++) {
				for (int x = 0; x < currentSize; x++) {
					// calculate relative position in original image
					int relativeX = (int)Math.round(x*relativeDivisor);
					int relativeY = (int)Math.round(y*relativeDivisor);
					
					if (relativeX > DEFAULT_RESOLUTION - 1) {
						relativeX = DEFAULT_RESOLUTION - 1;
					}
					
					if (relativeY > DEFAULT_RESOLUTION - 1) {
						relativeY = DEFAULT_RESOLUTION - 1;
					}
					
					float val = grey[currentImage][relativeY][relativeX];
					Color color = Color.color(val, val, val);

					imageWriter.setColor(x, y, color);
				}
			}
		} else if(currentResizeMethod == ResizeMethod.BILINEAR_INTERPOLATION) {
			for (int y = 0; y < currentSize; y++) {
				for (int x = 0; x < currentSize; x++) {
					// calculate relative position in original image
					double relativeX = x*relativeDivisor;
					double relativeY = y*relativeDivisor;
					
					if (relativeX > DEFAULT_RESOLUTION - 1) {
						relativeX = DEFAULT_RESOLUTION - 1;
					}
					if (relativeY > DEFAULT_RESOLUTION - 1) {
						relativeY = DEFAULT_RESOLUTION - 1;
					}
					
					int x1 = (int) Math.floor(relativeX);
					int x2 = (int) Math.ceil(relativeX);
					int y1 = (int) Math.floor(relativeY);
					int y2 = (int) Math.ceil(relativeY);
					
					float aColorValue = grey[currentImage][y1][x1];
					float bColorValue = grey[currentImage][y2][x1];
					float cColorValue = grey[currentImage][y2][x2];
					float dColorValue = grey[currentImage][y1][x2];
					
					// if both relative coordinates are integers, just do nearest neighbor
					if(relativeX - (int)relativeX == 0 && relativeY - (int)relativeY == 0) {
						float val = grey[currentImage]
								[(int)Math.round(relativeY)][(int)Math.round(relativeX)];
						Color color = Color.color(val, val, val);
						
						// Apply the new color
						imageWriter.setColor(x, y, color);
					
					// if only relative X coordinate is an integer, do lerp function only on y coordinates
					} else if (relativeX - (int)relativeX == 0) {
						float gColorValue = lerp(aColorValue, bColorValue,y1,y2,relativeY);
						
						Color color = Color.color(gColorValue, gColorValue, gColorValue);
						imageWriter.setColor(x, y, color);
					
					// if only relative Y coordinate is an integer, do lerp function only on x coordinates
					} else if (relativeY - (int)relativeY == 0) {
						float gColorValue = lerp(aColorValue, dColorValue,x1,x2,relativeX);
						
						Color color = Color.color(gColorValue, gColorValue, gColorValue);
						imageWriter.setColor(x, y, color);
						
					// if both coordinates are not integers, do full bilinear interpolation
					} else {
						float fColorValue = lerp(bColorValue, cColorValue,x1,x2,relativeX);
						
						float eColorValue = lerp(aColorValue, dColorValue,x1,x2,relativeX);
						
						float gColorValue = lerp(eColorValue, fColorValue,y1,y2,relativeY);
						
						Color color = Color.color(gColorValue, gColorValue, gColorValue);
						
						imageWriter.setColor(x, y, color);
					}
						//
						// b - -f- c
						// |    g  |
						// |       |
						// |       |
						// a - -e- d
						//
						// where:
						// a(x1,y1)
						// b(x1,y2)
						// c(x2,y2)
						// d(x2,y1)
						//
						// e(relativeX,y1)
						// f(relativeX,y2)
						// g(relativeX,relativeY)
				}
			}
		}
		
		// create a look-up table with new Gamma values
		HashMap<Integer, Double> gammaValues = new HashMap<>(256);
		for(int i=0; i<256; i++) {
			gammaValues.put(i, Math.pow(i/255.0, (1.0/currentGamma)));
		}
		
		// apply gamma correction
		PixelReader imageReader = image.getPixelReader();
		for (int y = 0; y < currentSize; y++) {
			for (int x = 0; x < currentSize; x++) {
				double val = imageReader.getColor(y,x).getRed(); // we could take any channel as it is grey
				//double newVal = Math.pow(val, 1.0/currentGamma); - this does not use look-up table
				double newVal = gammaValues.get(Integer.valueOf((int)Math.round(val * 255.0))); // get value from look-up table
				
				Color color = Color.color(newVal, newVal, newVal);
				
				imageWriter.setColor(y, x, color);
			}
		}

		return image;
	}
	
	public float lerp(float v1, float v2, double p1, double p2, double p) {		
		return (float)(v1 + (v2 - v1)*((p-p1)/(p2-p1)));
	}
	
	private static final int THUMB_IMAGE_WIDTH = 500;
	private static final int THUMB_IMAGE_HEIGHT = 500;
	private static final int THUMB_ROW_NUMBER = 10;
	private static final int THUMB_COL_NUMBER = 12;
	private static final int THUMB_PICTURE_SIZE = 38;
	private static final int THUMB_GAP_SIZE = 4;
	
	public void createThumbWindow(double atX, double atY) {
		// create image containing all thumbs
		WritableImage thumbImage = 
				new WritableImage(THUMB_IMAGE_WIDTH, THUMB_IMAGE_HEIGHT);
		ImageView thumbView = new ImageView(thumbImage);
		PixelWriter imageWriter = thumbImage.getPixelWriter();
		
		// make the image initially white
		for (int x = 0; x < thumbImage.getHeight(); x++) {
			for (int y = 0; y < thumbImage.getWidth(); y++) {
				imageWriter.setColor(x, y, Color.WHITE);
			}
		}
		
		double relativeDivisor = DEFAULT_RESOLUTION/(double)THUMB_PICTURE_SIZE;
		// TODO create final variables for rows and columns as they are fixed
		// it would be sth like: rowNumber = 10; imagesInCol = 12; thumbImageSize = 38; gap = 4;
		// loop through each image in particular row and column
		for(int row = 0; row < THUMB_ROW_NUMBER; row++) { 
			for(int col = 0; col < THUMB_COL_NUMBER; col++) {
				// draw image resized using nearest neighor technique
				for(int x = 0; x < THUMB_PICTURE_SIZE; x++) {
					for(int y = 0; y < THUMB_PICTURE_SIZE; y++) {
						int pictureNo = row * THUMB_COL_NUMBER + col;

						if(pictureNo < PICTURE_NUMBER) { // in case last row is not perfect
							int relativeX = (int) (x*relativeDivisor);
							int relativeY = (int) (y*relativeDivisor);

							float val = grey[pictureNo][relativeX][relativeY];
							Color color = Color.color(val, val, val);
							
							imageWriter.setColor(
									y + col*(THUMB_PICTURE_SIZE + THUMB_GAP_SIZE),
									x + row*(THUMB_PICTURE_SIZE + THUMB_GAP_SIZE),
									color);
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
			
			int selectedColumn = (int) (hoverX/(THUMB_PICTURE_SIZE + THUMB_GAP_SIZE));
			int selectedRow = (int) (hoverY/(THUMB_PICTURE_SIZE + THUMB_GAP_SIZE));
			
			//check if it is not in the gap between
			if (hoverX % (THUMB_PICTURE_SIZE + THUMB_GAP_SIZE) <= THUMB_PICTURE_SIZE 
					&& hoverY % (THUMB_PICTURE_SIZE + THUMB_GAP_SIZE) <= THUMB_PICTURE_SIZE) {
				int selectedPicture = selectedRow * THUMB_COL_NUMBER + selectedColumn;
				
				//check if its not out of the thumbnails
				if (selectedPicture < PICTURE_NUMBER) {
					// set displayed picture to new one, refresh the view
					this.currentImage = selectedPicture;
					
					updateImage(imageView);
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
	
	public void updateImage(ImageView imageView) {
		imageView.setImage(null); // clear the old image
		Image newImage = getSlice();
		imageView.setImage(newImage); // Update the GUI so the new image is displayed
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
