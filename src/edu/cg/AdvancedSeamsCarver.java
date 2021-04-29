package edu.cg;

import java.awt.image.BufferedImage;

public class AdvancedSeamsCarver extends BasicSeamsCarver {
	// TODO :  Decide on the fields your AdvancedSeamsCarver should include.
	
	public AdvancedSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super(logger, workingImage, outWidth, outHeight, rgbWeights);
	}
	
	public BufferedImage resizeWithSeamCarving(CarvingScheme carveScheme) {
		if (Math.abs(this.outWidth - this.inWidth) > this.inWidth / 2 || Math.abs(this.outHeight - this.inHeight) > this.inHeight / 2) {
			throw new RuntimeException("Can not apply seam carving: too many seams.");
		}
		logger.log("Scaling image width to " + this.outWidth + " pixels, and height to " + this.outHeight + " pixels.");
		if (this.outWidth <= this.inWidth && this.outHeight <= this.inHeight) {
			return carveImage(carveScheme);
		}
		else if (carveScheme == CarvingScheme.INTERMITTENT){
			throw new IllegalArgumentException("Intermittent carving is not supported in upscaling.");
		}
		else {
			// TODO: Implement the additional seam carving functionalities that AdvancedSeamsCarver offers.
			boolean vertical = carveScheme == CarvingScheme.VERTICAL_HORIZONTAL;
			return new AdvancedSeamsCarver(logger,resize(vertical,false),outWidth,outHeight,rgbWeights).resize(!vertical,false);
		}
	}


}