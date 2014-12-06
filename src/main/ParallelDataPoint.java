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

import mpi.MPI;
import mpi.MPIException;

public class ParallelDataPoint {


	public static Double threshold = 0.00000001;
	
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
		
		try {
			MPI.Init(args);
		} catch (MPIException e){
			System.out.println("Could not initialize MPI.");
			errorExit();
		}
		
		long startTime = System.currentTimeMillis();
		
		DataPoint[] results = null;
		
		try {
			results = parallelDataPointKMeans(numClusters, data);
		} catch (MPIException e){
			System.out.println("Error running parallel K-Means!");
			e.printStackTrace();
			errorExit();
		}
		long endTime = System.currentTimeMillis();
		long elapsedTime = endTime - startTime;
		if (MPI.COMM_WORLD.Rank() == 0){
			System.out.println("Running Time: " + elapsedTime);
			writeOutput(results, outputFile);
		}
		try {
			MPI.Finalize();
		} catch (MPIException e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}
	
	private static DataPoint[] parallelDataPointKMeans(int numClusters, List<DataPoint> data) throws MPIException{
		
		int slaveSize = MPI.COMM_WORLD.Size();
		int myRank = MPI.COMM_WORLD.Rank();
		int dataSize = data.size();
		
		DataPoint[] centroids = selectKRandomCentroids(numClusters, data);
/*		int l = 0;
		for (int i = 5; i < 5 + numClusters; i++){
			centroids[l] = data.get(i);
			l++;
		}*/
		int iterations = 0;
		
		while(true){
			
			if (myRank == 0){
				for (int i = 1; i < slaveSize; i++){
					MPI.COMM_WORLD.Send(centroids, 0, numClusters, MPI.OBJECT, i, 0);
				}
				DataPoint[][] temp_centroids = new DataPoint[slaveSize-1][numClusters];
				int[][] points_added = new int[slaveSize - 1][numClusters];
				for (int i = 1; i < slaveSize; i++){
					DataPoint[] temp_c = new DataPoint[numClusters];
					int[] temp_num = new int[numClusters];
					MPI.COMM_WORLD.Recv(temp_c, 0, numClusters, MPI.OBJECT, i, 0);
					temp_centroids[i-1] = Arrays.copyOf(temp_c, numClusters);
					MPI.COMM_WORLD.Recv(temp_num, 0, numClusters, MPI.INT, i, 1);
					points_added[i-1] = Arrays.copyOf(temp_num, numClusters);
				}
				DataPoint[] new_centroids = new DataPoint[numClusters];
				for (int j = 0; j < numClusters; j++){
					Double x = 0d;
					int x_num = 0;
					Double y = 0d;
					int y_num = 0;
					for (int i = 0; i < slaveSize -1; i++){
						x += (temp_centroids[i][j].getX() * points_added[i][j]);
						x_num += points_added[i][j];
						y += (temp_centroids[i][j].getY() * points_added[i][j]);
						y_num += points_added[i][j];
					}
					new_centroids[j] = new DataPoint(x/x_num, y/y_num);
				}
				if (checkCentroidVariations(centroids, new_centroids, numClusters)){
					System.out.println("Number of Iterations = " + (iterations + 1));
					boolean[] run = new boolean[1];
					run[0] = false;
					for (int i = 1; i < slaveSize; i++){
						MPI.COMM_WORLD.Send(run, 0, 1, MPI.BOOLEAN, i, 2);
					}
					return new_centroids;
				}
				boolean[] run = new boolean[1];
				run[0] = true;
				for (int i = 1; i < slaveSize; i++){
					MPI.COMM_WORLD.Send(run, 0, 1, MPI.BOOLEAN, i, 2);
				}
				centroids = Arrays.copyOf(new_centroids, new_centroids.length);
				iterations += 1;
				
				
			}
			else {
				MPI.COMM_WORLD.Recv(centroids, 0, numClusters, MPI.OBJECT, 0, 0);
				int[] range = getRange(dataSize, slaveSize - 1, myRank);
				List<List<DataPoint>> clusters = new ArrayList<List<DataPoint>>();
				for (int i = 0; i < numClusters; i++){
					clusters.add(new ArrayList<DataPoint>());
				}
				for (int i = range[0]; i < range[1]; i++){
					int closest = DataPoint.getClosestPoint(data.get(i), centroids);
					clusters.get(closest).add(data.get(i));
				}
				DataPoint[] new_centroids = new DataPoint[numClusters];
				int[] num_added = new int[numClusters];
				int index = 0;
				for (List<DataPoint> cluster : clusters){
					if (cluster.size() != 0){
						new_centroids[index] = DataPoint.getMeanOfCluster(cluster);
					}
					else {
						new_centroids[index] = new DataPoint(0.0, 0.0);
					}
					num_added[index] = cluster.size();
					index += 1;
				}
				MPI.COMM_WORLD.Send(new_centroids, 0, numClusters, MPI.OBJECT, 0, 0);
				MPI.COMM_WORLD.Send(num_added, 0, numClusters, MPI.INT, 0, 1);
				boolean[] run = new boolean[1];
				MPI.COMM_WORLD.Recv(run, 0, 1, MPI.BOOLEAN, 0, 2);
				if (!run[0]){
					System.out.println("Slave " + myRank +" done!");
					return null;
				}
			}
			
		}
	}
	
	private static int[] getRange(int numPoints, int numSlaves, int slaveRank) {
		int between = (int) Math.ceil((double) numPoints / (double) numSlaves);
		if (slaveRank != numSlaves) {
			int[] range = { between * (slaveRank - 1), between * slaveRank };
			return range;
		} else {
			int[] range = { between * (slaveRank - 1), numPoints };
			return range;
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
