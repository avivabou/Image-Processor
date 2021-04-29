package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;

public class ImageProcessor extends FunctioalForEachLoops {
	
	//MARK: Fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;
	
	//MARK: Constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage,
			RGBWeights rgbWeights, int outWidth, int outHeight) {
		super(); //Initializing for each loops...
		
		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}
	
	public ImageProcessor(Logger logger,
			BufferedImage workingImage,
			RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights,
				workingImage.getWidth(), workingImage.getHeight());
	}
	
	//MARK: Change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Changing hue done!");
		
		return ans;
	}
	
	//MARK: Nearest neighbor - example
	public BufferedImage nearestNeighbor() {
		logger.log("applies nearest neighbor interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();
		
		pushForEachParameters();
		setForEachOutputParameters();
		
		forEach((y, x) -> {
			int imgX = (int)Math.round((x*inWidth) / ((float)outWidth));
			int imgY = (int)Math.round((y*inHeight) / ((float)outHeight));
			imgX = Math.min(imgX,  inWidth-1);
			imgY = Math.min(imgY, inHeight-1);
			ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
		});
		
		popForEachParameters();
		
		return ans;
	}
	
	//MARK: Unimplemented methods
	public BufferedImage greyscale() {
		//TODO: Implement this method, remove the exception.
		logger.log("applies grey scale interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();

		pushForEachParameters();
		setForEachOutputParameters();

		forEach((y, x) -> {
			Color original = new Color (workingImage.getRGB(x,y));
			int greyedValue = original.getRed()*rgbWeights.redWeight+original.getGreen()*rgbWeights.greenWeight+original.getBlue()*rgbWeights.blueWeight;
			greyedValue /= rgbWeights.weightsSum;
			Color greyed = new Color(greyedValue,greyedValue,greyedValue);
			ans.setRGB(x, y,greyed.getRGB());
		});

		popForEachParameters();

		return ans;
	}

	public BufferedImage gradientMagnitude() {
		//TODO: Implement this method, remove the exception.
        logger.log("applies gradient Magnitude interpolation.");
        BufferedImage ans = newEmptyOutputSizedImage();
		BufferedImage current = greyscale();
        pushForEachParameters();
        setForEachOutputParameters();

        forEach((y, x) -> {
            int dx = getDifference(current,x,y,x-1,y);
			int dy = getDifference(current,x,y,x,y-1);
			double grad = Math.sqrt((Math.pow(dx,2) + Math.pow(dy,2)));
            //int d = (int)(grad*360)/255;				// Scalling [0,360] to [0,255]
			int d = (int) Math.min(grad,255); 			// as been told on piazza (@14) by Yarin Shechter
            Color gradient = new Color(d,d,d);
            ans.setRGB(x, y,gradient.getRGB());
        });

        popForEachParameters();

        return ans;
	}

	private int getDifference (BufferedImage Image,int x1, int y1, int x2, int y2)
	{
		int pix1 = Image.getRGB(x1,y1)&0Xff;
		int pix2 = 0;
		if (x2 >= 0 && y2 >= 0 )
		{
			pix2 = Image.getRGB(x2,y2)&0xff;
		}
		return pix1 - pix2;
	}

	public BufferedImage bilinear() {
		//TODO: Implement this method, remove the exception.
		logger.log("applies bilinear interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();

		pushForEachParameters();
		setForEachOutputParameters();

		float x_prop = (float)outWidth / (float)inWidth ;			// t
		float y_prop = (float)outHeight / (float)inHeight;			// s
		forEach((y, x) -> {
			float x1 = Math.min(x/x_prop,inWidth-1);
			float y1 = Math.min(y/y_prop,inHeight-1);
			float x2 = Math.min((x + 1)/x_prop,inWidth-1);
			float y2 = Math.min( (y + 1)/y_prop,inHeight-1);
			//String msg = String.format("p1=["+x1+","+y1+"]  p2=["+x2+","+y2+"]  P=["+x+","+y+"] prop=["+x_prop+","+y_prop+"]");
			//System.out.println(msg);
			int c1 = workingImage.getRGB((int)Math.ceil(x1),(int)Math.ceil(y1));
			int c2 = workingImage.getRGB((int)Math.floor(x2),(int)Math.ceil(y1));
			int c3 = workingImage.getRGB((int)Math.ceil(x1),(int)Math.floor(y2));
			int c4 = workingImage.getRGB((int)Math.floor(x2),(int)Math.floor(y2));
			Color c12 = getNewPixelColor(new Color(c1),new Color(c2),x1,x2);
            Color c34 = getNewPixelColor(new Color(c3),new Color(c4),x1,x2);
			ans.setRGB(x,y,getNewPixelColor(c12,c34,y1,y2).getRGB());
		});

		popForEachParameters();

		return ans;
	}

	private Color getNewPixelColor(Color c1, Color c2, float p1, float p2)
	{
		if (p1 == p2)
		{
			return  c1;
		}
		double prop = (p2-Math.ceil(p1))/(p2-p1);
		int R = (int) (c1.getRed()*prop + (c2.getRed()*(1-prop)));
		int G = (int) (c1.getGreen()*prop + (c2.getGreen()*(1-prop)));
		int B = (int) (c1.getBlue()*prop + (c2.getBlue()*(1-prop)));
		return new Color(R,G,B);
	}
	
	//MARK: Utilities
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}
	
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}
	
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}
	
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		
		forEach((y, x) -> 
			output.setRGB(x, y, workingImage.getRGB(x, y))
		);
		
		return output;
	}
}
