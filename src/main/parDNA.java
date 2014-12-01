package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Random;

import mpi.MPI;
import mpi.MPIException;
public class parDNA {
	DNA []ans;
	int K;
	private static int threshold = 5;
	public parDNA(int k) {
		K=k;
		ans= new DNA[k];
	}
	public void cluster(DNAs data) throws MPIException{
		int[] sum_diff = new int[1];
		sum_diff[0] = Integer.MAX_VALUE;
		int N = data.size();
		int Len = data.length;

		char[] base = { 'A', 'C', 'G', 'T' };

		Random random = new Random();

		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();

		DNA[] centroids = new DNA [K];
		for (int i = 0; i < centroids.length; i++) {
			centroids[i] = data.get(random.nextInt(N));
		}
		HashMap<String, Integer> c2i = new HashMap<String, Integer>();
		c2i.put("A", 0);
		c2i.put("C", 1);
		c2i.put("G", 2);
		c2i.put("T", 3);
		int[][][] total = new int[Len][K][4];
		int[][][] hold = new int[Len][K][4];

		while (sum_diff[0] > threshold) {
			int[] diff = new int[1];
			
			if (rank == 0) {
				// tag 0 for send centroid
				// tag 1 for send counts
				// tag 2 for send sum_diff
				for(int i=0;i<Len;i++)
					for(int j=0;j<K;j++)
						for(int m=0;m<4;m++)
							total[i][j][m]=0;
				sum_diff[0]=0;
				for (int r = 1; r < size; r++) {
					MPI.COMM_WORLD.Send(centroids, 0, K, MPI.OBJECT, r, 0);
				}
				for(int r=1;r<size;r++)
				{
					MPI.COMM_WORLD.Recv(hold, 0, K*Len*4, MPI.INT, r, 1);
					for(int i=0;i<Len;i++)
					{
						for(int j=0;j<K;j++)
							for(int m=0;m<4;m++)
								total[i][j][m]+=hold[i][j][m];
					}
					MPI.COMM_WORLD.Recv(diff, 0, 1, MPI.INT, r, 2);
					sum_diff[0]+=diff[0];
				}
				for (int r = 1; r < size; r++) {
					MPI.COMM_WORLD.Isend(sum_diff, 0, 1, MPI.INT, r, 2);
				}
				centroids = DNAs.RecentrFromCount(data.length,K,total);
				if(sum_diff[0]<=threshold)
				{
					ans= centroids;
					return;
				}

			} else {
				for(int i=0;i<Len;i++)
					for(int j=0;j<K;j++)
						for(int m=0;m<4;m++)
							hold[i][j][m]=0;
				diff[0] = 0;
				MPI.COMM_WORLD.Recv(centroids, 0, K, MPI.OBJECT, 0, 0);

				int[] range = getRange(N, size - 1, rank);
				for (int i = range[0]; i < range[1]; i++) {
					int minDis = Integer.MAX_VALUE;
					int pos = 0;

					for (int j = 0; j < K; j++) {
						int dis = DNAs.getDis(data.get(i), centroids[j]);
						if (dis < minDis) {
							minDis = dis;
							pos = j;
						}
					}
					if (data.get(i).centroid != pos) {
						diff[0]++;
						data.get(i).centroid=pos;
					}
				}
				for (int i = 0; i < Len; i++) {
					
					for (int j = range[0]; j < range[1]; j++) {
						int id = c2i.get("" + data.get(j).strand.charAt(i));
						hold[i][data.get(j).centroid][id]++;
					}
				}
				MPI.COMM_WORLD.Isend(hold, 0, Len*K*4, MPI.INT, 0, 1);
				MPI.COMM_WORLD.Isend(diff, 0, 1, MPI.INT, 0, 2);
				MPI.COMM_WORLD.Recv(sum_diff, 0, 1, MPI.INT, 0, 2);

				if(sum_diff[0]<=threshold)
				{
					System.out.println("slave "+rank+": done!");
					return;
				}

			}

		}
	}

	private int[] getRange(int pointSize, int slaveSize, int rank) {
		int between = (int) Math.ceil((double) pointSize / (double) slaveSize);
		if (rank != slaveSize) {
			int[] range = { between * (rank - 1), between * rank };
			return range;
		} else {
			int[] range = { between * (rank - 1), pointSize };
			return range;
		}
	}
	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("<cluster numbers> <DNA length> <input dataset> <output file>");
			return;
		}

		int k = 1,len=0;

		try {
			k = Integer.parseInt(args[0]);
			len = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.out.println("K and length should be integer");
			return;
		}
		String inputFile = args[2];
		String outputFile = args[3];
		DNAs data = new DNAs(len);
		try {
			BufferedReader in = new BufferedReader(new FileReader(inputFile));
			String line;
			while ((line = in.readLine()) != null) {
				DNA  hold = new DNA(new StringBuffer(line));
				data.add(hold);
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("no input file");
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("read input file error");
			System.exit(-1);
	
		}
		try {
			MPI.Init(args);
		} catch (MPIException e1) {
			e1.printStackTrace();
			return;
		}

		long startTime = System.currentTimeMillis();

		// get running time

		parDNA Kdna = new parDNA(k);
		
		try {
			Kdna.cluster(data);
		} catch (MPIException e1) {
			e1.printStackTrace();
		}

		long endTime = System.currentTimeMillis();
		long time = endTime - startTime;
		
		if(0 == MPI.COMM_WORLD.Rank())
		{
			System.out.println("Running Time is " + time);
			Kdna.writeOutput(outputFile);
		}
			try {
			MPI.Finalize();
		} catch (MPIException e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}

	}
	private void writeOutput(String outFilename) {
		// TODO Auto-generated method stub
		File outFile = new File(outFilename);
		PrintWriter fileOut = null;
		try {
			fileOut = new PrintWriter(outFile);
			for(int i=0;i<ans.length;i++)
			{
				System.out.println(ans[i].strand);
				fileOut.println(ans[i].strand);
			}
			fileOut.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("output file not found");
			System.exit(-1);

		}
	}
}

