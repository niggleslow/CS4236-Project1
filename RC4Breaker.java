import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class RC4Breaker {
    static int keySize;
    static int noOfTuples;
    static int[] key;
    static int[] firstBytes;
    static int[] permutation;
    static ArrayList<int[]> freqList = new ArrayList<int[]>();
    static ArrayList<int[]> tupList  = new ArrayList<int[]>();

    public static void main(String[] args) throws IOException {
        String fileName = args[0];
        populateTupleList(fileName);
        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()) {
            String input = sc.nextLine();
            permutation = new int[7];
            for(int i = 0; i < 7; i++) { //1 - 5 options
                permutation[i] = sc.nextInt();
            }
            //System.out.println("Current permutation: " + permutation[0] + " " + permutation[1] + " " + permutation[2] + " " + permutation[3] + " " + permutation[4]);
            key = new int[keySize - 3];
            findKey();
            for(int a = 0; a < key.length; a++) {
                System.out.print(key[a] + " ");
            }
            System.out.println();
        }
        //System.out.println(keySize);
        //System.out.println(noOfTuples);
        //System.out.println(tupList.size());
    }

    public static void populateTupleList(String fileName) throws IOException {
        File file = new File(fileName);
        Path path = file.toPath();
        byte[] data = Files.readAllBytes(path);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        keySize = buffer.getInt();
        noOfTuples = buffer.getInt();
        for (int i = 0; i < noOfTuples; i++) {
            int[] current = new int[4];
            for (int j = 0; j < 4; j++) {
                current[j] = buffer.get() & 0xff;
                //System.out.println(current[j]);                
            }
            tupList.add(current);
        }
    }

    public static void findKey() {
        for(int count = 0; count < key.length; count++) {
            //create IV array
            int[] IV = new int[3 + count];
            //create frequency array
            int[] frequency = new int[160];
            //for each tuple
            for(int tupleNo = 0; tupleNo < noOfTuples; tupleNo++) {
                int[] tuple = tupList.get(tupleNo);
                //set K[0], K[1], K[2] .. K[N-1] as IV to find K[N]
                for(int counter = 0; counter < IV.length; counter++) {
                    if(counter < 3) {
                        IV[counter] = tuple[counter];
                    } else {
                        IV[counter] = key[counter - 3];
                    }
                }
                //simulation of KSA begins here
                int numberOfShuffles = IV.length;
                int[] arrayJ = new int[numberOfShuffles];
                int i = 0;
                int j = 0;
                //initialize state array [0,1,2...159]
                int[] state = new int[160];
                for(int a = 0; a < state.length; a++) {
                    state[a] = a;
                }
                for(int shuffleNo = 0; shuffleNo < numberOfShuffles; shuffleNo++) {
                    //calculating and storing value of j into arrayJ
                    if(shuffleNo == 0) {
                        j = 0 + 0 + IV[shuffleNo];
                    } else {
                        j = (arrayJ[shuffleNo - 1] + state[i] + IV[shuffleNo]) % 160;
                    }
                    arrayJ[shuffleNo] = j;
                    //everybody do the shuffle!
                    int temp = state[i];
                    state[i] = state[j];
                    state[j] = temp;
                    i++;
                }
                //check if IV is weak: x + y == c?
                int C = IV.length;
                int x = state[1];
                int y = state[state[1]];
                if((x + y) == C) { //weak IV detected!
                    //finding the next j value
                    for(int b = 0; b < state.length; b++) {
                        if(state[b] == tuple[3]) {
                            j = b;
                        }
                    }
                    //System.out.println("Value of C = " + C);
                    //using equation to find value of current key byte
                    int keyValue = (j - arrayJ[C-1] - state[C]);
                    while(keyValue < 0) {
                        keyValue += 160;
                    }
                    frequency[keyValue]++; 
                }
            }
            //Frequency analysis time!
            freqList.add(frequency);
            int[] probableValues = new int[10];
            int mostFrequent = -1;
            for(int d = 0; d < frequency.length; d++) {
                if(d < 90) {
                    if(mostFrequent <= frequency[d]) {
                        mostFrequent = frequency[d];
                        probableValues[9] = probableValues[8];
                        probableValues[8] = probableValues[7];
                        probableValues[7] = probableValues[6];
                        probableValues[6] = probableValues[5];
                        probableValues[5] = probableValues[4];
                        probableValues[4] = probableValues[3];
                        probableValues[3] = probableValues[2];
                        probableValues[2] = probableValues[1];
                        probableValues[1] = probableValues[0];
                        probableValues[0] = d;
                    }    
                }
            }
            /*System.out.println("Most probable: " + probableValues[0] + " with " + frequency[probableValues[0]] + " occurences.");
            System.out.println("2nd most probable: " + probableValues[1] + " with " + frequency[probableValues[1]] + " occurences.");
            System.out.println("3rd most probable: " + probableValues[2] + " with " + frequency[probableValues[2]] + " occurences.");
            System.out.println("4th most probable: " + probableValues[3] + " with " + frequency[probableValues[3]] + " occurences.");
            System.out.println("5th most probable: " + probableValues[4] + " with " + frequency[probableValues[4]] + " occurences.");
            System.out.println("6th most probable: " + probableValues[5] + " with " + frequency[probableValues[5]] + " occurences.");
            System.out.println("7th most probable: " + probableValues[6] + " with " + frequency[probableValues[6]] + " occurences.");
            System.out.println("8th most probable: " + probableValues[7] + " with " + frequency[probableValues[7]] + " occurences.");
            System.out.println("9th most probable: " + probableValues[8] + " with " + frequency[probableValues[8]] + " occurences.");
            System.out.println("10th most probable: " + probableValues[9] + " with " + frequency[probableValues[9]] + " occurences.");
            System.out.println();*/
            //System.out.println(probableValues[permutation[count] - 1]);
            key[count] = probableValues[permutation[count] - 1];
        }
    }
}