package application;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class FFmpegFrame {

	//make sure all ffmpeg instances are closed
	private static List<Process> instances = new ArrayList<>();
	
	static {
		Runnable runner = () -> {
			for (Process p : instances) p.destroy();
			instances.clear();
		};
		Runtime.getRuntime().addShutdownHook(new Thread(runner));
	}
	
	private final Process process;
	private final InputStream is;
	private final OutputStream os;
	private Integer exitValue = null;
	
	//only constructor
	private FFmpegFrame(Process process) {
		this.process = process;
		this.is = process.getInputStream();
		this.os = process.getOutputStream();
		instances.add(process);
	}

	//build an instance to read BufferedImages from a video file
	public static FFmpegReaderBuilder readerBuilder() {
		return new FFmpegReaderBuilder();
	}
	
	//build an instance to create a video from BufferedImages
	public static FFmpegWriterBuilder writerBuilder() {
		return new FFmpegWriterBuilder();
	}
	
	//create an instance from custom ffmpeg command line
	public static FFmpegFrame customFFmpegArgs(List<String> ffmpegArgs) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(ffmpegArgs);
		Process p = pb.start();
		return new FFmpegFrame(p);
	}
	
	//read bytes directly
	public int readBytes(byte[] data) throws IOException {
		return is.read(data);
	}
	
	//write bytes directly
	public void writeBytes(byte[] data) throws IOException {
		os.write(data);
	}
	
	//read video frame into BufferedImage, will be null when no data was read
	public BufferedImage readFrame() throws IOException {
		return ImageIO.read(is);
	}
	
	//write BufferedImage to video file
	public void writeFrame(BufferedImage image) throws IOException {
		ImageIO.write(image, "bmp", os);
	}

	//get the output from ffmpeg that would normally appear on the command line
	public String getOutput() {
		return process.errorReader().lines().collect(Collectors.joining("\n"));
	}
	
	//close the ffmpeg process
	public String close() throws Exception {
		return close(30_000);
	}
	
	//close the ffmpeg process and at most wait the given number of milliseconds for ffmpeg to terminate
	public String close(int millis) throws Exception {
		String str = null;
		if (process != null && process.isAlive()) {
			os.close();
			is.close();
			str = getOutput();
			process.waitFor(millis, TimeUnit.MILLISECONDS);
			process.destroy();
			exitValue = process.exitValue();
		}
		return str;
	}
	
	//check if the ffmpeg process is still alive
	public boolean isAlive() {
		return process.isAlive();
	}
	
	//get the exit value from ffmpeg process, null when process has not yet terminated
	public Integer exitValue() {
		return exitValue;
	}
	
	public static class FFmpegReaderBuilder {
		
		private String ffmpegPath = "ffmpeg";
		private String inputFile = "";
		private File workingDir = null;
		
		private FFmpegReaderBuilder() {}
		
		public FFmpegReaderBuilder setFFmpegPath(String ffmpegPath) {
			this.ffmpegPath = ffmpegPath;
			return this;
		}
		
		public FFmpegReaderBuilder setInputFile(String inputFile) {
			this.inputFile = inputFile;
			return this;
		}
		
		public FFmpegReaderBuilder setWorkingDir(File workingDir) {
			this.workingDir = workingDir;
			return this;
		}
		
		public FFmpegFrame build() throws IOException {
			List<String> args = Arrays.asList(ffmpegPath.toString(), "-i", inputFile.toString(), "-vcodec", "bmp", "-f", "image2pipe", "-");
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.directory(workingDir);
			Process p = pb.start();
			return new FFmpegFrame(p);
		}
	}
	
	public static class FFmpegWriterBuilder {
		
		private String ffmpegPath = "ffmpeg";
		private String outputFile = "";
		private File workingDir = null;
		private String codec = "libx264";
		private String pixFmt = "yuv420p";
		private double fps = 25.0;
		
		private FFmpegWriterBuilder() {}
		
		public FFmpegWriterBuilder setFFmpegPath(String ffmpegPath) {
			this.ffmpegPath = ffmpegPath;
			return this;
		}
		
		public FFmpegWriterBuilder setOutputFile(String outputFile) {
			this.outputFile = outputFile;
			return this;
		}
		
		public FFmpegWriterBuilder setWorkingDir(File workingDir) {
			this.workingDir = workingDir;
			return this;
		}
		
		public FFmpegWriterBuilder setFps(double fps) {
			this.fps = fps;
			return this;
		}
		
		public FFmpegWriterBuilder setCodec(String codec) {
			this.codec = codec;
			return this;
		}
		
		public FFmpegWriterBuilder setPixFmt(String pixFmt) {
			this.pixFmt = pixFmt;
			return this;
		}
		
		public FFmpegFrame build() throws IOException {
			List<String> args = Arrays.asList(
					ffmpegPath.toString(), "-f", "image2pipe", "-framerate", String.valueOf(fps), 
					"-i", "-", "-pix_fmt", pixFmt, "-vcodec", codec, outputFile.toString(), "-y");
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.directory(workingDir);
			Process p = pb.start();
			return new FFmpegFrame(p);
		}
	}
}
