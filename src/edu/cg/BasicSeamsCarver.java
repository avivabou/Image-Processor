package edu.cg;

import java.awt.image.BufferedImage;


public class BasicSeamsCarver extends ImageProcessor {
	// An enum describing the carving scheme used by the seams carver.
	// VERTICAL_HORIZONTAL means vertical seams are removed first.
	// HORIZONTAL_VERTICAL means horizontal seams are removed first.
	// INTERMITTENT means seams are removed intermittently : vertical, horizontal, vertical, horizontal etc.
	public static enum CarvingScheme {
		VERTICAL_HORIZONTAL("Vertical seams first"),
		HORIZONTAL_VERTICAL("Horizontal seams first"),
		INTERMITTENT("Intermittent carving");

		public final String description;

		private CarvingScheme(String description) {
			this.description = description;
		}
	}

	// A simple coordinate class which assists the implementation.
	// ##### Different implementation than the given "Coordinates". #####
	private class AdvancedPixel {

		public final int Color;
		private final int greyedColor;
		public double Value;
		public double MinimumSum = 0;
		public boolean Available = true;
		public AdvancedPixel Top = null;
		public AdvancedPixel Left = null;
		public AdvancedPixel Right = null;
		public AdvancedPixel Bottom = null;
		public AdvancedPixel MinimumPicked = null;

		public AdvancedPixel(int Color) {
			this.Color = Color;
			greyedColor = (((Color & 0xff0000) >> 16) * rgbWeights.redWeight
					+ ((Color & 0xff00) >> 8) * rgbWeights.greenWeight
					+ (Color & 0xff) * rgbWeights.blueWeight)
					/ rgbWeights.weightsSum;
			Reset();
		}

		public void PickMinimum(AdvancedPixel pixel,double edgesPrice) {
			MinimumPicked = pixel;
			MinimumSum = pixel.MinimumSum + Value + edgesPrice;
		}

		public void MarkPickedPath(boolean Vertical) {
			Available = false;
			if (Vertical) {
				if (Left != null) Left.Right = Right;
				if (Right != null) Right.Left = Left;
				shiftLeft();
			} else {
				if (Top != null) Top.Bottom = Bottom;
				if (Bottom != null) Bottom.Top = Top;
				shiftUp();
			}
			if (MinimumPicked != null) MinimumPicked.MarkPickedPath(Vertical);
		}

		public void Reset() {
			if (Available) {
				int dx = getDiffrence(this,Left);
				int dy = getDiffrence(this,Top);
				Value = Math.sqrt ((Math.pow(dx, 2) + Math.pow(dy, 2)));
				MinimumSum = Value;
			} else {
				Value = Double.MAX_VALUE;
				MinimumSum = Value;
			}
		}

		private void shiftLeft() {
			if (Right == null) return;
			if (Top != null) {
				Top.Bottom = Right;
			}
			if (Bottom != null) {
				Bottom.Top = Right;
			}
			Right.shiftLeft();
			Right.Top = Top;
			Right.Bottom = Bottom;
		}

		private void shiftUp() {
			if (Bottom == null) return;
			if (Left != null) {
				Left.Right = Bottom;
			}
			if (Right != null) {
				Right.Left = Bottom;
			}
			Bottom.shiftUp();
			Bottom.Left = Left;
			Bottom.Right = Right;
		}
	}

	// TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework 
	// instructions PDF to make an educated decision.
	private final int numberOfVerticalSeamsToCarve, numberOfHorizontalSeamsToCarve;
	private final int width, height;
	private AdvancedPixel[][] imageSeamsMap;

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
							int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.
		numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		width = workingImage.getWidth();
		height = workingImage.getHeight();
		imageSeamsMap = new AdvancedPixel[width][height];
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				imageSeamsMap[i][j] = new AdvancedPixel(workingImage.getRGB(i, j));
		setNeighbors(width, height);
	}


	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
		// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
		// Note you must consider the 'carvingScheme' parameter in your procedure.
		// Return the resulting image.
		switch(carvingScheme)
		{
			case HORIZONTAL_VERTICAL:
				return new BasicSeamsCarver(logger,resize(false,true),outWidth,outHeight,rgbWeights).resize(true,true);
			case VERTICAL_HORIZONTAL:
				return new BasicSeamsCarver(logger,resize(true,true),outWidth,outHeight,rgbWeights).resize(false,true);
			default:
				return intermittentShrink();
		}
	}

	public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
		// TODO :  Present either vertical or horizontal seams on the input image.
		// If showVerticalSeams = true, carve 'numberOfVerticalSeamsToCarve' vertical seams from the image.
		// Then, generate a new image from the input image in which you mark all of the vertical seams that
		// were chosen in the Seam Carving process.
		// This is done by painting each pixel in each seam with 'seamColorRGB' (overriding its' previous value).
		// Similarly, if showVerticalSeams = false, carve 'numberOfHorizontalSeamsToCarve' horizontal seams
		// from the image.
		// Then, generate a new image from the input image in which you mark all of the horizontal seams that
		// were chosen in the Seam Carving process.
		int i_limit = showVerticalSeams ? numberOfVerticalSeamsToCarve : numberOfHorizontalSeamsToCarve;
		for (int i = 0; i < i_limit; i++)
			findAndMarkSeam(showVerticalSeams);
		BufferedImage ans = new BufferedImage(width, height, workingImageType);
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				if (imageSeamsMap[x][y].Available)
					ans.setRGB(x, y, workingImage.getRGB(x, y));
				else
					ans.setRGB(x, y, seamColorRGB);
			}
		return ans;
	}

	protected BufferedImage resize(boolean Vertical, boolean Shrink) {
		int oWidth = Vertical?outWidth:width;
		int oHeight = Vertical?height:outHeight;
		int i_limit = Vertical?numberOfVerticalSeamsToCarve:numberOfHorizontalSeamsToCarve;
		for (int i = 0; i < i_limit; i++)
			findAndMarkSeam(Vertical);
		if (Shrink)
			return shrink(Vertical,oWidth,oHeight);
		else
			return enlarge(Vertical,oWidth,oHeight);
	}

	private void setNeighbors(int width, int height) {
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				if (x > 0) imageSeamsMap[x][y].Left = imageSeamsMap[x - 1][y];
				if (x < width - 1) imageSeamsMap[x][y].Right = imageSeamsMap[x + 1][y];
				if (y > 0) imageSeamsMap[x][y].Top = imageSeamsMap[x][y - 1];
				if (y < height - 1) imageSeamsMap[x][y].Bottom = imageSeamsMap[x][y + 1];
			}
	}

	private void resetValues() {
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				imageSeamsMap[x][y].Reset();
	}

	private void findAndMarkSeam(boolean Vertical) {
		resetValues();
		calcSeam(Vertical);
		/*if (Vertical) calcVerticalSeam();
		else calcHorizontalSeam();*/
		AdvancedPixel minimumPixel = null;
		AdvancedPixel current;
		double minimumValue = Double.MAX_VALUE;
		int i_limit = Vertical ? width : height;
		for (int i = 0; i < i_limit; i++) {
			current = Vertical ? imageSeamsMap[i][height - 1] : imageSeamsMap[width - 1][i];
			if (!current.Available) continue;
			if (current.MinimumSum < minimumValue) {
				minimumPixel = current;
				minimumValue = current.MinimumSum;
			}
		}
		minimumPixel.MarkPickedPath(Vertical);
	}

	private void calcSeam(boolean Vertical)
	{
		int i_limit,j_limit;
		if (Vertical)
		{
			i_limit = height;
			j_limit = width;
		}
		else
		{
			i_limit = width;
			j_limit = height;
		}

		for (int i = 1; i < i_limit; i++) {
			for (int j = 0; j < j_limit; j++) {
				AdvancedPixel cur = Vertical? imageSeamsMap[j][i] : imageSeamsMap[i][j];
				if (!cur.Available) continue;
				pickMinimum(cur,Vertical);
			}
		}
	}

	private void pickMinimum(AdvancedPixel Pixel, boolean Vertical)
	{
		AdvancedPixel picked,diag1,diag2;
		double minimumSum,edgesPrice,diag1EdgesPrice,diag2EdgesPrice;
		if (Vertical) {
			picked = Pixel.Top;
			diag1 = picked.Left;
			diag2 = picked.Right;
			edgesPrice = getDiffrence(Pixel.Left, Pixel.Right);
		}
		else
		{
			picked = Pixel.Left;
			diag1 = picked.Top;
			diag2 = picked.Bottom;
			edgesPrice = getDiffrence(Pixel.Top, Pixel.Bottom);
		}
		minimumSum = picked.MinimumSum;
		diag1EdgesPrice = getDiffrence(Pixel.Left, Pixel.Top);
		diag2EdgesPrice = Vertical? getDiffrence(Pixel.Right, Pixel.Top):getDiffrence(Pixel.Left, Pixel.Bottom);
		if ((diag1 != null) && (diag1.MinimumSum + diag1EdgesPrice < minimumSum + edgesPrice)) {
			picked = diag1;
			minimumSum = diag1.MinimumSum;
			edgesPrice = diag1EdgesPrice;
		}
		if ((diag2 != null) && (diag2.MinimumSum + diag2EdgesPrice < minimumSum + edgesPrice)) {
			picked = diag2;
			edgesPrice = diag2EdgesPrice;
		}
		Pixel.PickMinimum(picked, edgesPrice);
	}

	private int getDiffrence(AdvancedPixel P1, AdvancedPixel P2)
	{
		if (P1 == null || P2 == null) return 255;
			/*return P2.greyedColor;
		if (P2 == null)
			return P1.greyedColor;*/
		return Math.abs(P1.greyedColor - P2.greyedColor);
	}

	private BufferedImage shrink(boolean Vertical ,int oWidth, int oHeight)
	{
		BufferedImage ans = new BufferedImage(oWidth, oHeight, workingImageType);
		int j_limit = Vertical?height:width;
		for (int j = 0; j < j_limit; j++) {
			int k = 0;
			AdvancedPixel current = Vertical? imageSeamsMap[k][j]:imageSeamsMap[j][k];
			while (current != null) {
				if (current.Available) {
					if (Vertical) ans.setRGB(k, j, current.Color);
					else ans.setRGB(j,k,  current.Color);
					k++;
				}
				current = Vertical? current.Right : current.Bottom;
			}
		}
		return ans;
	}

	private BufferedImage oneStepShrink(boolean Vertical) {
		int oWidth = Vertical?width-1:width;
		int oHeight = Vertical?height:height-1;
		findAndMarkSeam(Vertical);
		return shrink(Vertical,oWidth,oHeight);
	}


	private BufferedImage intermittentShrink()
	{
		boolean veritcal = numberOfVerticalSeamsToCarve > numberOfHorizontalSeamsToCarve;
		int verticalCounter = 0;
		int horizontalCounter = 0;
		BufferedImage current = workingImage;
		while (verticalCounter < numberOfVerticalSeamsToCarve || horizontalCounter < numberOfHorizontalSeamsToCarve)
		{
			current = new BasicSeamsCarver(logger,current,outWidth,outHeight,rgbWeights).oneStepShrink(veritcal);
			if (veritcal) verticalCounter++;
			else horizontalCounter++;
			//Keep the proportion using the formula:
			// (VerCounter / VerToCrave) < (HorCounter / HorToCrave) = VerCounter * HorToCrave < HorCounter * VerToCrave
			// I found this more "fair" then checking if VerCounter < HorCounter
			// Since if there are more seams to crave in one of those direction, at the end we should complete the rest.
			veritcal = (verticalCounter * numberOfHorizontalSeamsToCarve) < (horizontalCounter * numberOfVerticalSeamsToCarve);
		}
		return current;
	}

	private BufferedImage enlarge(boolean Vertical ,int oWidth, int oHeight) {
		BufferedImage ans = new BufferedImage(oWidth, oHeight, workingImageType);
		int i_limit = Vertical ? width : height;
		int j_limit = Vertical ? height : width;
		for (int j = 0; j < j_limit; j++) {
			int k = 0;
			for (int i = 0; i < i_limit; i++) {
				AdvancedPixel current = Vertical ? imageSeamsMap[i][j] : imageSeamsMap[j][i];
				if (Vertical) ans.setRGB(i + k, j, current.Color);
				else ans.setRGB(j, i + k, current.Color);
				if (!current.Available) {
					k++;
					if (Vertical) ans.setRGB(i + k, j, current.Color);
					else ans.setRGB(j, i + k, current.Color);
				}
			}
		}
		return ans;
	}

}
