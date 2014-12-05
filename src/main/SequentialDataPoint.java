package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SequentialDataPoint {
	
	
	private static Double threshold = 0.00000001;
	
	public static void errorExit(){
		System.out.println("Exiting...");
		System.exit(-1);
	}
	
	public static void main(String[] args){
		
		int numArgs = args.length;
		if (numArgs != 3){
			System.out.println("Usage: java SeqDataPoint <cluster numbers> <input dataset> <output file>");
			errorExit();
		}
		else {
			int numClusters = 1;
			try {
				numClusters = Integer.parseInt(args[0]);
			} catch (NumberFormatException e){
				System.out.println("Number of clusters provided is not an integer!");
				errorExit();
			}
			String inputFile = args[1];
			String outputFile = args[2];
			
			List<DataPoint> data = new ArrayList<DataPoint>();
			
			try {
				BufferedReader in = new BufferedReader(new FileReader(inputFile));
				String line; 
				while ((line = in.readLine()) != null){
					String[] coords = line.split(",");
					Double x = Double.parseDouble(coords[0]);
					Double y = Double.parseDouble(coords[1]);
					DataPoint point = new DataPoint(x, y);
					data.add(point);
				}
				in.close();
			} catch (FileNotFoundException e){
				System.out.println("No such file as "+ inputFile);
				errorExit();
				
			} catch (NumberFormatException e) {
				System.out.println("Coordinates of data points provided are not numbers!");
				errorExit();
				
			} catch (IOException e) {
				System.out.println("Error reading the file "+ inputFile);
				errorExit();
			}
			
			long startTime = System.currentTimeMillis();
			DataPoint[] results = sequentialDataPointKMeans(numClusters, data);
			long endTime = System.currentTimeMillis();
			long elapsedTime = endTime - startTime;
			System.out.println("Running Time: " + elapsedTime);
			writeOutput(results, outputFile);
			
		}
		
	}
	
	private static DataPoint[] sequentialDataPointKMeans(int numClusters, List<DataPoint> data){
		
		DataPoint[] centroids = selectKRandomCentroids(numClusters, data);
		List<List<DataPoint>> clusters = new ArrayList<List<DataPoint>>();
		for (int i = 0; i < numClusters; i++){
			List<DataPoint> cluster =  new ArrayList<DataPoint>();
			clusters.add(cluster);
		}
		int iterations = 0;
		while (true){
			DataPoint[] new_centroids = new DataPoint[numClusters];
			for (DataPoint p: data){
				int closest = DataPoint.getClosestPoint(p, centroids);
				clusters.get(closest).add(p);
			}
			int index = 0;
			for (List<DataPoint> cluster : clusters){
				new_centroids[index] = DataPoint.getMeanOfCluster(cluster);
				index += 1;
			}
			if (checkCentroidVariations(centroids, new_centroids, numClusters)){
				System.out.println("Number of Iterations = " + (iterations + 1));
				return new_centroids;
			}
			centroids = Arrays.copyOf(new_centroids, new_centroids.length);
			iterations += 1;
		}
		
	}
	
	private static DataPoint[] selectKRandomCentroids(int K, List<DataPoint> data){
		List<DataPoint> shuffled = new ArrayList<DataPoint>(data);
		Collections.shuffle(shuffled);
		return shuffled.subList(0, K).toArray(new DataPoint[K]);
	}
	
	private static boolean checkCentroidVariations(DataPoint[] old_centroids, DataPoint[] new_centroids, int K){
		
		int check_count = 0;
		for (int i = 0; i < K; i++){
			Double distance = DataPoint.getDistance(old_centroids[i], new_centroids[i]);
			if (distance <= threshold){
				check_count += 1;
			}
		}
		return (check_count == K);
		
	}
	
	private static void writeOutput(DataPoint[] centroids, String outputFilename){
		File outputFile = new File(outputFilename);
		PrintWriter out = null;
		try {
			out = new PrintWriter(outputFile);
			int len = centroids.length;
			for (int i = 0; i < len; i++){
				out.println(centroids[i].toString());
			}
			out.close();
		} catch (FileNotFoundException e){
			System.out.println("No such file as " + outputFilename);
			errorExit();
		}
	}
}
