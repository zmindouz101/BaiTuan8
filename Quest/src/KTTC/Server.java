package KTTC;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
 
public class Server {
 
    public final static int SERVER_PORT = 9999;
    public static int SoNT(int n)
    {
    	if(n<2) return 0;
    	else if(n==2) return 1;
    	else
    	{
    		for(int i =2;i<=Math.sqrt(n);i++)
    			if(n%i==0)
    				return 0;
    		return 1;
    	}
    }
    @SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        try {
            System.out.println("Binding to port " + SERVER_PORT + ", please wait  ...");
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started: " + serverSocket);
            System.out.println("Waiting for a client ...");
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("Client accepted: " + socket);
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    List<Integer> ds = new ArrayList<Integer>();
                    List<Integer> dskq = new ArrayList<Integer>();
                    ds = (List<Integer>) ois.readObject();
                    for(int i : ds)
                    	if(SoNT(i)==1)
                    		dskq.add(i);
                    oos.writeObject(dskq);
                    socket.close();
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println(" Connection Error: " + e);
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
 
}