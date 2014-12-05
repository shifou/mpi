package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class seqDNA {
	
	DNA []ans;
	int K;
	private static int threshold = 5;
	public seqDNA(int k) {
		K=k;
		ans= new DNA[k];
	}

	public static void main(String[] args) {

		if (args.length != 4) {
			System.out.println("Usage: java seqDNA <cluster numbers> <DNA length> <input dataset> <output file>");
			return;
		}

		if (args.length == 4) {
			int K = 1;
			int len=0;
			try {
				K = Integer.parseInt(args[0]);
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

			// Get running time
			long startTime = System.currentTimeMillis();

			seqDNA Kdna = new seqDNA(K);
			Kdna.cluster(data);

			long endTime = System.currentTimeMillis();

			Kdna.writeOutput(outputFile);

			System.out.println("runtime is: "
					+ (endTime - startTime));

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

	private void cluster(DNAs data) {
		// TODO Auto-generated method stub
		int diff = Integer.MAX_VALUE;
		int N = data.size();

		
		DNA[] centroids = data.getInit(K);
		
		while (diff > threshold) {
			diff = 0;
			
			for (int i = 0; i < N; i++) {
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
					diff++;
					data.get(i).centroid=pos;
				}
			}

			// recalculate
			centroids = DNAs.Recentroid(data.length,K,data);
			
		}
		ans= centroids;
	}
}
