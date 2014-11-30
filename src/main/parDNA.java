package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import mpi.MPI;
import mpi.MPIException;
public class parDNA {
	
	private String outFileName = "outputDNA";
	public void cluster(DNAs data, int k) throws MPIException{
	
		
	}
	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("<cluster numbers> <DNA length> <input dataset> <output file>");
			return;
		}

		int K = 1,len=0;

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
		try {
			MPI.Init(args);
		} catch (MPIException e1) {
			e1.printStackTrace();
			return;
		}

		long startTime = System.currentTimeMillis();

		// get running time

		parDNA Kdna = new parDNA();
		
		try {
			Kdna.cluster(data,K);
		} catch (MPIException e1) {
			e1.printStackTrace();
		}

		long endTime = System.currentTimeMillis();
		long[] time = new long[1];
		time[0] = endTime - startTime;
		long[] sum_time = new long[1];

		try {
			MPI.COMM_WORLD
					.Allreduce(time, 0, sum_time, 0, 1, MPI.LONG, MPI.SUM);
		} catch (MPIException e1) {
			e1.printStackTrace();
		}

		System.out.println("Time is " + sum_time[0] / K);

		// Kdna.writeOutput(DNAs);
		try {
			MPI.Finalize();
		} catch (MPIException e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}

	}
}

