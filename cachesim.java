
import java.util.*;
import java.io.*;
import java.util.Scanner;

public class cachesim {
	
	public static int size;
	public static int associativity;
	public static String WriteType;
	public static int blockSize;
	public static block[][]cache;
	public static String[]mem;
	
	public static void main(String[]args) {
		mem = new String [65536]; //index number corresponds to address number
		for(int i=0;i<mem.length;i++) { //Setting all memory vals to 00
			mem[i]="00";
		}
		
		File f = new File(args[0]);
		size = Integer.parseInt(args[1]);
		associativity= Integer.parseInt(args[2]);
		WriteType = args[3];
		blockSize = Integer.parseInt(args[4]);
		
		int rowSize=(size*1024)/(associativity*blockSize);
		System.out.println(rowSize);
		cache= new block[rowSize][associativity];
		for(int r=0;r<rowSize;r++) {
			for(int c=0;c<associativity;c++) {
				cache[r][c]= new block(0,0,0,blockSize);
			}
		}
		
		int address;
		int setNum;
		int tag;
		int blockOffset;
		int accessSize;
		String strToPrint;
		String hexAdd;
		String writeVal = "";
		
		try {
			Scanner sc = new Scanner(f);
			while(sc.hasNextLine()) {
				strToPrint="";
				String str= sc.nextLine();
				String[]lineArr = str.split(" ");
				address = Integer.parseInt(lineArr[1],16);
				hexAdd = lineArr[1];
				System.out.println("access type: "+lineArr[0]);
				System.out.println("address: "+lineArr[1]);
				System.out.println("Address in decimal: "+Integer.parseInt(lineArr[1],16));
				System.out.println("access size: "+lineArr[2]);
				
				accessSize = Integer.parseInt(lineArr[2]);
				if(lineArr.length==4) {
					System.out.println("value of store: "+lineArr[3]);
					writeVal = lineArr[3];
				}
				
				setNum= (address/blockSize)%rowSize;
				tag = address/(rowSize*blockSize);
				blockOffset = address%blockSize;
				
				if(lineArr[0].equals("load")) {
					strToPrint = load(setNum,tag,blockOffset, accessSize,address);
					strToPrint = "load "+ hexAdd + " " + strToPrint;
				}
				
				else if(lineArr[0].equals("store")) {
					strToPrint = wtWrite(setNum,tag,blockOffset, accessSize,address,writeVal);
					strToPrint = "store " +hexAdd+ " " + strToPrint;
				}
				

				System.out.println(strToPrint);
			}
			sc.close();
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
		}	
		
	}
	
	public static String wtWrite(int set, int tag, int blockOffset, int accessSize,int address, String writeVal) {
		String str= "";
		
		boolean miss= true; //initially set to true. Set to false if hit detected during loop.
		for(int c=0; c<associativity;c++) {
			block b = cache[set][c];
			if(b.isValidBit()&&(b.getTag()==tag)) {
				lruUpdate(set);
				b.resetLRU();
				
				int count=0;
				for(int i=blockOffset;i<blockOffset+accessSize;i++) {
					String currVal = writeVal.substring(count*2,count*2+2);
					b.writeBlock(currVal,i);
					count++;
				}
				writetoMem(b,address,blockOffset);
				
				str = "hit";
				miss = false;
				break;
			}
		}
		if (miss) {
			for(int i=0;i<accessSize;i++) {
				mem[address+i]=writeVal.substring(i*2,i*2+2);
			}
			str="miss";
		}
		return str;	
	}
	
	public static String wbWrite(int set, int tag, int blockOffset, int accessSize,int address) {
		String str= "";
				
		return str;	
	}
	
	public static void writetoMem(block b,int address,int offset) {
		int memStart= address-offset;
		for(int i=0;i<blockSize;i++) {
			mem[memStart+i]= b.getSingleVal(i);
		}
	}
	
	
	public static String load(int set, int tag, int blockOffset, int accessSize,int address){
		String str = "";
		boolean miss= true; //initially set to true. Set to false if hit detected during loop.
		for(int c=0; c<associativity;c++) {
			block b = cache[set][c];
			if(b.isValidBit()&&(b.getTag()==tag)) {
				lruUpdate(set);
				b.resetLRU();
				str = b.getBlockVals(blockOffset, accessSize);
				str = "hit " + str;
				miss = false;
				break;
			}
		}
		//If load miss
		if (miss) {
			str= "";
			int evictedBlocknum = findEvict(set);
			block overwriteBlock = cache[set][evictedBlocknum];
			overwriteBlock.setValid();
			for(int i=0;i<accessSize;i++) {
				overwriteBlock.writeBlock(mem[address+i],blockOffset+i);
				str= str+ mem[address+i];
			}
			str = "miss "+str;
		}

		return str;
	}
	public static void lruUpdate(int set) {
		for(int c=0; c<associativity;c++) {
			cache[set][c].updateLRU();
		}
	}
	public static int findEvict(int set) {
		for(int i=0;i<associativity;i++) {
			if(!cache[set][i].isValidBit()) {
				return i;
			}
		}
		return findLRU(set);
	}
	
	public static int findLRU(int set) {
		int maxVal=-1;
		int max=0;
		int currLRU;
		for(int i=0;i<associativity;i++) {
			currLRU= cache[set][i].getRecencyVal();
			if(currLRU>maxVal) {
				maxVal= currLRU;
				max = i;
			}
		}
		return max;
	}
	
}
