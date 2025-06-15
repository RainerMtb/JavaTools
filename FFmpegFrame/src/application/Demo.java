package application;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.io.File;

import javax.imageio.ImageIO;

public class Demo {

	public static void main(String[] args) throws Exception {
		//provide the following data via program arguments
		String ffmpegPath = args[0];
		String videoFile = args[1];
		String imagesDirectory = args[2];
		
		//read individual frames from video
		FFmpegFrame reader = FFmpegFrame.readerBuilder().setFFmpegPath(ffmpegPath).setInputFile(videoFile).build();
		for (int i = 0; i < 20; i++) {
			BufferedImage image = reader.readFrame();
			File file = new File(imagesDirectory + "test" + i + ".jpg");
			System.out.println("writing image " + file);
			ImageIO.write(image, "jpg", file);
		}
		reader.close();
		
		//write individual frames to video
		String outputPath = Path.of(imagesDirectory, "test.mp4").toString();
		System.out.println();
		System.out.println("writing video " + outputPath);
		FFmpegFrame writer = FFmpegFrame.writerBuilder().setFFmpegPath(ffmpegPath).setOutputFile(outputPath).build();
		for (int i = 0; i < 20; i++) {
			File file = new File(imagesDirectory + "test" + i + ".jpg");
			BufferedImage image = ImageIO.read(file);
			writer.writeFrame(image);
			System.out.println("sending image " + file);
		}
		String str = writer.close();
		
		//ffmpeg result message
		System.out.println("return value " + writer.exitValue());
		System.out.println(str);
	}
}
