package VideoStreaming;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class Server extends JFrame implements ActionListener {
	DatagramSocket RTPsocket; 
	DatagramPacket senddp; 
	InetAddress ClientIPAddr; 
	int RTP_dest_port = 0; 
	JLabel label;
	int imagenb = 0; 
	VideoStream video; 
	static int MJPEG_TYPE = 26;
	static int FRAME_PERIOD = 50;
	static int VIDEO_LENGTH = 500; 
	Timer timer; 
	byte[] buf; 
	final static int INIT = 0;

	final static int READY = 1;

	final static int PLAYING = 2;

	final static int SETUP = 3;

	final static int PLAY = 4;

	final static int PAUSE = 5;

	final static int TEARDOWN = 6;

	static int state; 
	Socket RTSPsocket; 
	static BufferedReader RTSPBufferedReader;

	static BufferedWriter RTSPBufferedWriter;

	static String VideoFileName; 
	static int RTSP_ID = 123456; 
	int RTSPSeqNb = 0; 
	final static String CRLF = "\r\n";
	public Server() {

		// init Frame
		super("Server");

		// init Timer
		timer = new Timer(FRAME_PERIOD, this);
		timer.setInitialDelay(0);
		timer.setCoalesce(true);

		// allocate memory for the sending buffer
		buf = new byte[15000];

		// Handler to close the main window
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// stop the timer and exit
				timer.stop();
				System.exit(0);
			}
		});

		label = new JLabel("Send frame #        ", JLabel.CENTER);
		getContentPane().add(label, BorderLayout.CENTER);
	}
	public static void main(String argv[]) throws Exception {
		// create a Server object
		Server theServer = new Server();

		// show GUI:
		theServer.pack();
		theServer.setVisible(true);

		// get RTSP socket port from the command line
		int RTSPport = Integer.parseInt("9999");

		try {
			// Initiate TCP connection with the client for the RTSP session
			ServerSocket listenSocket = new ServerSocket(RTSPport);
			theServer.RTSPsocket = listenSocket.accept();
			listenSocket.close();

			// Get Client IP address
			theServer.ClientIPAddr = theServer.RTSPsocket.getInetAddress();

			// Initiate RTSPstate
			state = INIT;

			// Set input and output stream filters:
			RTSPBufferedReader = new BufferedReader(new InputStreamReader(
					theServer.RTSPsocket.getInputStream()));
			RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(
					theServer.RTSPsocket.getOutputStream()));

			// Wait for the SETUP message from the client
			int request_type;
			boolean done = false;
			while (!done) {
				request_type = theServer.parse_RTSP_request(); // blocking

				if (request_type == SETUP) {
					done = true;

					// update RTSP state
					state = READY;
					System.out.println("New RTSP state: READY");

					// Send response
					theServer.send_RTSP_response();

					// init the VideoStream object:
					theServer.video = new VideoStream(VideoFileName);

					// init RTP socket
					theServer.RTPsocket = new DatagramSocket();
				}
			}

			// loop to handle RTSP requests
			while (true) {
				// parse the request
				request_type = theServer.parse_RTSP_request(); // blocking

				if ((request_type == PLAY) && (state == READY)) {
					// send back response
					theServer.send_RTSP_response();
					// start timer
					theServer.timer.start();
					// update state
					state = PLAYING;
					System.out.println("New RTSP state: PLAYING");
				} else if ((request_type == PAUSE) && (state == PLAYING)) {
					// send back response
					theServer.send_RTSP_response();
					// stop timer
					theServer.timer.stop();
					// update state
					state = READY;
					System.out.println("New RTSP state: READY");
				} else if (request_type == TEARDOWN) {
					// send back response
					theServer.send_RTSP_response();
					// stop timer
					theServer.timer.stop();
					// close sockets
					theServer.RTSPsocket.close();
					theServer.RTPsocket.close();

					System.exit(0);
				}
			}
		} catch (BindException e) {
			System.out.println("Could not init server on port '" + argv[0] + "'");
			System.exit(0);
		}

	}
	public void actionPerformed(ActionEvent e) {
		if (imagenb < VIDEO_LENGTH) {
			imagenb++;

			try {
				int image_length = video.getnextframe(buf);
				RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb,
						imagenb * FRAME_PERIOD, buf, image_length);
				int packet_length = rtp_packet.getlength();
				byte[] packet_bits = new byte[packet_length];
				rtp_packet.getpacket(packet_bits);

				senddp = new DatagramPacket(packet_bits, packet_length,
						ClientIPAddr, RTP_dest_port);
				RTPsocket.send(senddp);
				label.setText("Send frame #" + imagenb);
			} catch (Exception ex) {
				System.out.println("Exception caught: " + ex);
				System.exit(0);
			}
		} else {
			timer.stop();
		}
	}
	private int parse_RTSP_request() {
		int request_type = -1;
		try {
			String RequestLine = RTSPBufferedReader.readLine();
			System.out.println(RequestLine);

			StringTokenizer tokens = new StringTokenizer(RequestLine);
			String request_type_string = tokens.nextToken();
			if ((new String(request_type_string)).compareTo("SETUP") == 0)
				request_type = SETUP;
			else if ((new String(request_type_string)).compareTo("PLAY") == 0)
				request_type = PLAY;
			else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
				request_type = PAUSE;
			else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
				request_type = TEARDOWN;

			if (request_type == SETUP) {
				VideoFileName = tokens.nextToken();
			}
			String SeqNumLine = RTSPBufferedReader.readLine();
			System.out.println(SeqNumLine);
			tokens = new StringTokenizer(SeqNumLine);
			tokens.nextToken();
			RTSPSeqNb = Integer.parseInt(tokens.nextToken());

			String LastLine = RTSPBufferedReader.readLine();
			System.out.println(LastLine);

			if (request_type == SETUP) {
				tokens = new StringTokenizer(LastLine);
				for (int i = 0; i < 3; i++)
					tokens.nextToken(); 
				RTP_dest_port = Integer.parseInt(tokens.nextToken());
			}
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
		return (request_type);
	}
	private void send_RTSP_response() {
		try {
			RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
			RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);
			RTSPBufferedWriter.write("Session: " + RTSP_ID + CRLF);
			RTSPBufferedWriter.flush();
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
	}
}
