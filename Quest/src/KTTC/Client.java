package KTTC;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
 
public class Client {
    public final static String SERVER_IP = "localhost";
    public final static int SERVER_PORT = 9999;
 
    public static void main(String[] args) throws IOException, InterruptedException {
    	List<Integer> ds = new ArrayList<Integer>();
    	if(args == null)
    		System.out.printf("Danh sách rống...");
    	else
    	{
    		for(String i:args)
    		{
    			int n = Integer.parseInt(i);
    			ds.add(n);
    		}
    		 Socket socket = null;
    	        try {
    	        	List<Integer> dskq = new ArrayList<Integer>();
    	            socket = new Socket(SERVER_IP, SERVER_PORT);
    	            System.out.println("Connected: " + socket);
    	 
    	            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
    	            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
    	            oos.writeObject(ds);
    	            dskq = (List<Integer>) ois.readObject();
    	            System.out.println("Ds so nguyen: ");
    	            for(int i:dskq)
    	            {
    	            	System.out.print(i+" ");
    	            }
    	        } catch (IOException | ClassNotFoundException ie) {
    	            System.out.println("Can't connect to server");
    	        } finally {
    	            if (socket != null) {
    	                socket.close();
    	            }
    	        }
    	}
    }
}
