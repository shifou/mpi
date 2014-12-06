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
	// store final centrid
	DNA []ans;
	// number of cluster we want to get
	int K;
	// stop thresholdprivate static int threshold = 5;
	private static int threshold = 5;
	public parDNA(int k) {
		K=k;
		ans= new DNA[k];
	}
	public void cluster(DNAs data) throws MPIException{
		//total difference
		int[] sum_diff = new int[1];
		sum_diff[0] = Integer.MAX_VALUE;
		// dna numbers
		int N = data.size();
		// every dna length
		int Len = data.length;

		char[] base = { 'A', 'C', 'G', 'T' };

		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		// get start centroid
		DNA[] centroids = data.getInit(K);
		
		HashMap<String, Integer> c2i = new HashMap<String, Integer>();
		c2i.put("A", 0);
		c2i.put("C", 1);
		c2i.put("G", 2);
		c2i.put("T", 3);
		// same as total[len][K][4] as the total count
		int[]total = new int[Len*K*4];
		int[]hold = new int[Len*K*4];

		while (sum_diff[0] > threshold) {
			int[] diff = new int[1];
			
			if (rank == 0) {
			// master running code
				// tag 0 for send centroid
				// tag 1 for send counts
				// tag 2 for send sum_diff
				for(int i=0;i<Len;i++)
					for(int j=0;j<K;j++)
						for(int m=0;m<4;m++)
							hold[i*4*K+j*4+m]=total[i*4*K+j*4+m]=0;
				sum_diff[0]=0;
				// send centroid to the slaves
				for (int r = 1; r < size; r++) {
					MPI.COMM_WORLD.Send(centroids, 0, K, MPI.OBJECT, r, 0);
				}
				for(int r=1;r<size;r++)
				{
					// receive the partial count from slave r
					MPI.COMM_WORLD.Recv(hold, 0, Len*K*4, MPI.INT, r, 1);
					for(int i=0;i<Len;i++)
					{
						for(int j=0;j<K;j++)	
						{
							
							for(int m=0;m<4;m++)
							{
								//add to total
								int temp=hold[i*4*K+j*4+m];
								total[i*4*K+j*4+m]+=temp;
							}
						}
					}
					//receive the partial difference
					MPI.COMM_WORLD.Recv(diff, 0, 1, MPI.INT, r, 2);
					//add to the total difference
					sum_diff[0]+=diff[0];
				}
				System.out.println("?????"+sum_diff[0]);
				for (int r = 1; r < size; r++) {
					//send the final total difference to slaves
					MPI.COMM_WORLD.Send(sum_diff, 0, 1, MPI.INT, r, 2);
				}
				// get new centroid
				centroids = DNAs.RecentrFromCount(data.length,K,total);
				// if smaller than threshold return
				if(sum_diff[0]<=threshold)
				{
					ans= centroids;
					return;
				}

			} else {
			// slave running code
				for(int i=0;i<Len;i++)
					for(int j=0;j<K;j++)
						for(int m=0;m<4;m++)
							hold[i*4*K+j*4+m]=0;
				diff[0] = 0;
				// receive the start centroid
				MPI.COMM_WORLD.Recv(centroids, 0, K, MPI.OBJECT, 0, 0);

				int[] range = getRange(N, size - 1, rank);
				// get the working range label each DNA its closest cluster
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
					// if not same as last time add as difference
					if (data.get(i).centroid != pos) {
						diff[0]++;
						data.get(i).centroid=pos;
					}
				}
				// count
				for (int i = 0; i < Len; i++) {
					
					for (int j = range[0]; j < range[1]; j++) {
						int id = c2i.get("" + data.get(j).strand.charAt(i));
						hold[i*4*K+data.get(j).centroid*4+id]++;
					}
				}
				//send partial count to master
				MPI.COMM_WORLD.Send(hold, 0, Len*K*4, MPI.INT, 0, 1);
				//send partial difference to master
				MPI.COMM_WORLD.Send(diff, 0, 1, MPI.INT, 0, 2);
				// receive the current total difference from master
				MPI.COMM_WORLD.Recv(sum_diff, 0, 1, MPI.INT, 0, 2);
				// if smaller threshold return
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
		//read the dataset
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
			/// begin MPI
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
			// write to the final output
			Kdna.writeOutput(outputFile);
		}
			try {
				//shutdown
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

