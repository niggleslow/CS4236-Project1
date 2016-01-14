import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class RC4Cracker {
	public static final int N = 160;
	public static List<ArrayList<Integer>> possibleKeys = new ArrayList<ArrayList<Integer>>();
		
	public static void main(String[] args) {
		/*for(int i=0; i<60; i++) {
			String currentFile;
			if(i < 10) {
				currentFile = "A0"+i+".data";
			} else {
				currentFile = "A"+i+".data";
			}
			
			System.out.println("Trying data file: " + currentFile);
			List<Integer> key = runFMS(currentFile);
			if(key != null) {
				System.out.println(key);
			}
		}*/
		//List<Integer> key = runFMS(args[0], Integer.parseInt(args[1]));
		List<Integer> key = runFMS("A00.data", 3);
		if (key == null) {
			System.out.println("No possible keys");
		} else {
			// permutate keys using dfs
			//String outputFile = args[0].substring(0, 3).concat("_keys.txt");
			dfsPermute(possibleKeys);
		}
	}
	
	public static void dfsPermute(List<ArrayList<Integer>> list) {
		String key = new String();
		dfsPermute(key, 0, list);
	}
	
	public static void dfsPermute(String key, int index, List<ArrayList<Integer>> list) {
		ArrayList<Integer> current = list.get(index);
		// recursive case
		if(index != list.size()-1) {
			for(Integer num : current) {
				String new_key = key.concat(num.toString()).concat(" ");
				dfsPermute(new_key, index+1, list);
			}
		} else {
			for(Integer num : current) {
				System.out.println(key.concat(num.toString()));
			}
		}
	}

	/**
	 * Run FMS on data file defaulted backtracking priority to 1
	 * @param fileName data file
	 * @return the key if found, else null
	 */
	public static List<Integer> runFMS(String fileName) {
		return runFMS(fileName, 1);
	}
	
	/**
	 * Run FMS on data file
	 * @param fileName data file
	 * @param num of backtrack priority
	 * @return the key if found, else null
	 */
	public static List<Integer> runFMS(String fileName, int num) {
		int kSize, numTuples;
		ArrayList<int[]> tuples;
		ByteBuffer buffer;
		
		/*----- READ DATA -----*/
		// read data files and process bytes
		try {
			buffer = readDataFile(fileName);
			// get header values
			kSize = buffer.getInt();
			numTuples = buffer.getInt();
			// read in tuples
			tuples = readTuples(buffer, numTuples);
		} catch(IOException e) {
			System.err.println("Error reading file!");
			return null;
		}
		
		
		/*----- ALGORITHM (FMS) -----*/
		int L = kSize;
		int K[] = new int[L-3];
		List<ArrayList<Integer>> keyFreq = new ArrayList<ArrayList<Integer>>(L-3);
		
		// check through tuples for each key byte K[C]
		for(int C=3; C<L; C++) {
			ArrayList<Integer> freq = new ArrayList<Integer>(N);
			for(int i=0; i<N; i++) {
				freq.add(0);
			}
			int bufferK[] = new int[C+1];
			
			Iterator<int[]> iter = tuples.iterator();
			//  check through each tuple
			while(iter.hasNext()) {
				int tuple[] = iter.next();			// current tuple
				// assigning IVs to K[]
				for(int i=0; i<bufferK.length; i++) {
					if(i < 3) {
						bufferK[i] = tuple[i];
					} else {
						bufferK[i] = K[i-3];
					}
				}
				int R = tuple[3];
				int S[] = generateS(N);
				
				// run through first C steps of KSA
				int j = 0;
				for(int i=0; i<C; i++) {
					j = (j + S[i] + bufferK[i]) % N;
					swap(S, i, j);
				}
				
				// check condition
				int X = S[1], Y = S[S[1]];
				if(X+Y == C) {		// tuple has weak IV
					int j_new = -1;
					for(int i=0; i<S.length; i++) {
						if(S[i] == R) {
							j_new = i;
							break;
						}
					}
					
					// error handling if next j not found
					if(j_new == -1) {
						return null;
					}
					
					// calculate possible K[C]
					int possibleValue = j_new - j - S[C];
					if(possibleValue < 0) { 
						possibleValue += N;
					}

					// possible key values
					int frequency = freq.get(possibleValue);
					freq.set(possibleValue, frequency+1);
				}
			}
			
			// find most possible value and add to key
			possibleKeys.add(topNMostFrequent(freq, num));
			
			// key found (return result)
			if(C == L-1) {
				List<Integer> list = new ArrayList<Integer>(K.length);
				for(int key : K) {
					list.add(Integer.valueOf(key));
				}
				return list;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the most frequent integer in the list
	 * @param list
	 * @return the most frequent integer
	 */
	public static int mostFrequent(ArrayList<Integer> list) {
		int highestFreq = 1, mostFreq = list.get(0);
		int currentFreq = highestFreq;
		
		Collections.sort(list);
		for(int i=1; i<list.size(); i++) {
			int prev = list.get(i-1), curr = list.get(i);
			if(curr == prev) {
				currentFreq++;
			} else {
				if(currentFreq >= highestFreq) {
					highestFreq = currentFreq;
					mostFreq = prev;
				}
				currentFreq = 1;
			}
		}
		
		return mostFreq;
	}
	
	public static ArrayList<Integer> topNMostFrequent(ArrayList<Integer> freq, int n) {
		//System.out.println(freq);
		
		ArrayList<Integer> topList = new ArrayList<Integer>();
		for(int i=0; i<n; i++) {
			int highestFreq = freq.get(0);
			int highestFreqNum = 0;
			for(int j=1; j<freq.size(); j++) {
				Integer currentFreq = freq.get(j);
				
				if(currentFreq != -1 && j < 90) {
					if(currentFreq > highestFreq) {
						highestFreqNum = j;
						highestFreq = currentFreq;
					}
				}
			}
			
			System.out.println(highestFreqNum);
			topList.add(highestFreqNum);
			freq.set(highestFreqNum, -1);
		}
		
		return topList;
	}
	
	/**
	 * Swaps two elements in an integer array
	 * @param arr integer array to swap
	 * @param a first index of element to swap
	 * @param b second index of element to swap
	 */
	public static void swap(int[] arr, int a, int b) {
		int temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}
	
	/**
	 * Generates a new S array
	 * @return S array of size N
	 */
	public static int[] generateS(int size) {
		int S[] = new int[size];
		for(int i=0; i<size; i++) {
			S[i] = i;
		}
		
		return S;
	}
	
	/**
	 * Reads in data file
	 * @param fileName of data file
	 * @return a byte buffer for further processing
	 * @throws IOEXception error reading file
	 */
	public static ByteBuffer readDataFile(String fileName) throws IOException {
		ByteBuffer buffer = null;
		
		File file = new File(fileName);
		Path path = file.toPath();
		byte[] data = Files.readAllBytes(path);
		buffer = ByteBuffer.wrap(data);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		return buffer;
	}

	/**
	 * Reads in tuples (assumes no header information)
	 * @param buffer the ByteBuffer to read from
	 * @param num of tuples
	 * @return the arraylist of tuples
	 */
	public static ArrayList<int[]> readTuples(ByteBuffer buffer, int num) {
		ArrayList<int[]> array = new ArrayList<int[]>();
		
		// read in tuples
		for(int i=0; i<num; i++) {
			Short hex = buffer.getShort();
			int v1 = hex & 0xFF;
			int v2 = ((hex & 0xFF00) >> 8) & 0xFF;
			hex = buffer.getShort();
			int v3 = hex & 0xFF;
			int x = ((hex & 0xFF00) >> 8) & 0xFF;
			int[] tuple = {v1, v2, v3, x};
			array.add(tuple);
		}		
		return array;
	}

}
