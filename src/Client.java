import java.io.*;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
//import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import RTP.RTPpacket;

public class Client {

	/**
	 * @param filename
	 * @param input
	 * @param client
	 * @param args
	 * @throws IOException
	 * @throws LineUnavailableException
	 * @throws UnsupportedAudioFileException
	 * @throws InterruptedException
	 */

	public InetAddress server;
	public int port;
	public DatagramSocket socket;
	static int filesize;
	static int blockSize = 65000;
	static int newPort;

	static SourceDataLine line = null;
	static AudioFormat af;

	private AudioFormat getFormat(DatagramSocket client) throws IOException {
		// TODO Auto-generated method stub

		// Receiving channels
		byte[] channels = new byte[4];

		DatagramPacket recv1 = new DatagramPacket(channels, channels.length);

		client.receive(recv1);

		int ch = ByteBuffer.wrap(recv1.getData()).getInt();

		// System.out.println("Received channels = " + ch);

		// Receiving Framesize
		byte[] frameSize = new byte[4];

		DatagramPacket recv2 = new DatagramPacket(frameSize, frameSize.length);

		client.receive(recv2);

		int fs = ByteBuffer.wrap(recv2.getData()).getInt();

		// System.out.println("Received framesize = " + fs);

		// Receiving Samplesizeinbits
		byte[] sampleSize = new byte[4];

		DatagramPacket recv3 = new DatagramPacket(sampleSize, sampleSize.length);

		client.receive(recv3);

		int ss = ByteBuffer.wrap(recv3.getData()).getInt();

		// System.out.println("Received SampleSizeInBits = " + ss);

		// Receiving Framerate
		byte[] frameRate = new byte[4];

		DatagramPacket recv4 = new DatagramPacket(frameRate, frameRate.length);

		client.receive(recv4);

		float fr = ByteBuffer.wrap(recv4.getData()).getFloat();

		// System.out.println("Received FrameRates = " + fr);

		// Receiving Samplerate
		byte[] sampleRate = new byte[4];

		DatagramPacket recv5 = new DatagramPacket(sampleRate, sampleRate.length);

		client.receive(recv5);

		float sr = ByteBuffer.wrap(recv5.getData()).getFloat();

		// System.out.println("Received SampleRate = " + sr);

		// Receiving Endian and send int.
		byte[] encoding = new byte[4];

		DatagramPacket recv6 = new DatagramPacket(encoding, encoding.length);

		client.receive(recv6);

		int en = ByteBuffer.wrap(recv6.getData()).getInt();

		AudioFormat.Encoding encode;

		if (en == 1) {
			encode = AudioFormat.Encoding.PCM_SIGNED;
		} else {
			encode = AudioFormat.Encoding.PCM_UNSIGNED;
		}

		// System.out.println("Received Encoding = " + encode);

		// Receiving Endian
		byte[] bool = new byte[4];

		DatagramPacket recv7 = new DatagramPacket(bool, bool.length);

		client.receive(recv7);

		int be = ByteBuffer.wrap(recv7.getData()).getInt();

		boolean bigEndian = true;

		if (be == 1) {
			bigEndian = true;
		} else {
			bigEndian = false;
		}

		// System.out.println("Received Endian = " + bigEndian);

		byte[] fileS = new byte[4];

		DatagramPacket recv8 = new DatagramPacket(fileS, fileS.length);

		client.receive(recv8);

		filesize = ByteBuffer.wrap(recv8.getData()).getInt();

		// System.out.println("Received filesize = " + filesize);

		byte[] newP = new byte[4];

		DatagramPacket recv9 = new DatagramPacket(newP, newP.length);

		client.receive(recv9);

		newPort = ByteBuffer.wrap(recv9.getData()).getInt();

		System.out.println("The new port received is: " + newPort);
		System.out.println();

		return (new AudioFormat(encode, sr, ss, ch, fs, fr, bigEndian));

	}

	public void playReceived(byte[] sound) throws UnsupportedAudioFileException, IOException, InterruptedException {
		// byte receive[] = dataRcv.getData();

		InputStream is = new ByteArrayInputStream(sound);

		AudioInputStream ais2 = new AudioInputStream(is, af, sound.length);

		AudioInputStream ais = AudioSystem.getAudioInputStream(af.getEncoding(), ais2);

		// System.out.println("sound.length = " + sound.length);

		int bytesLeft = 0;// , flag = 0;

		byte[] buffer = new byte[blockSize];

		while (bytesLeft != -1) {
			try {
				bytesLeft = ais.read(buffer, 0, buffer.length);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (bytesLeft >= 0) {
				// Thread.sleep(4);
				line.write(buffer, 0, bytesLeft);
				// System.out.println("Playing Received Bytes");
			}

		}
	}

	public static void main(String[] args)
			throws IOException, LineUnavailableException, UnsupportedAudioFileException, InterruptedException {
		// TODO Auto-generated method stub
		// Creating a UDP Socket.

		System.out.println("Client Running.");
		System.out.println();

		// Creating client object.
		Client cl = new Client();

		cl.socket = new DatagramSocket();

		// To receive data.
		byte[] rcvData = new byte[blockSize + 12];

		// To send data.
		byte[] sndData = new byte["Hello Thread!".length()];

		// Assigning send data.
		sndData = "Hello Thread!".getBytes();

		// Getting server address.
		InetAddress addr = InetAddress.getByName("localhost");

		// Server Details.
		cl.server = addr;
		cl.port = 1025;

		// Creating packet for server.
		DatagramPacket dataSnd = new DatagramPacket(sndData, sndData.length, cl.server, cl.port);

		// Communicates to server. Establishes connection.
		cl.socket.send(dataSnd);
		// System.out.println("Client: After sending request");

		// Receiving format
		af = cl.getFormat(cl.socket);

		System.out.println("Received format = " + af.toString());
		System.out.println();

		/***************************************************************/
		DatagramPacket dataSnd2 = new DatagramPacket(sndData, sndData.length, cl.server, newPort);

		Thread.sleep(1000);

		cl.socket.send(dataSnd2);
		/***************************************************************/

		// Datagram Packet to send. Inside Loop.
		DatagramPacket dataRcv = new DatagramPacket(rcvData, blockSize + 12);

		int loop = 1, flag = filesize / blockSize, i = 0;

		// System.out.println("Before Do-While. Filesize = " + flag);

		ArrayList<byte[]> b = new ArrayList<byte[]>();

		while (flag >= loop) {
			/************************************/

			// Initiating Dataline information.
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);

			// To write data to.
			line = (SourceDataLine) AudioSystem.getLine(info);

			// Opening Audio Line.
			line.open(af, blockSize);

			// Start to fetch data from inputstream(from file).
			line.start();

			/************************************/

			// Receiving from server. RTP Packet
			cl.socket.receive(dataRcv);

			// RTPpacket rtp = new
			// RTPpacket(0,1,(int)System.currentTimeMillis()/1000,dataRcv.getData(),blockSize);

			RTPpacket rtp = new RTPpacket(dataRcv.getData(), dataRcv.getData().length);

			rtp.printheaderFields();

			// System.out.println("dataRcv.getData().length = " +
			// dataRcv.getData().length);

			byte[] rtpRcv = new byte[blockSize];

			// System.out.println("Before Payload");

			rtp.getpayload(rtpRcv);

			// System.out.println("Before Payload");

			/************************************/

			b.add(rtpRcv);

			cl.playReceived(b.get(i));

			i++;

			// cl.socket.send(new DatagramPacket(new byte[4], 4, cl.server,
			// cl.port));

			/************************************/

			// if (loop == 15) {
			// (new Scanner(System.in)).nextInt();
			// }

			byte[] empty = new byte[4];
			DatagramPacket sync = new DatagramPacket(empty, empty.length, cl.server, newPort);
			cl.socket.send(sync);

			/************************************/
			// Incrementing loop
			// System.out.println("loop = " + loop);
			loop++;
		}

		// cl.socket.send(new DatagramPacket(new byte[4], 4, cl.server,
		// cl.port));

		// cl.playReceived(b.array());

		System.out.println("Completed playing");

<<<<<<< HEAD
		cl.socket.close(); 
=======
		cl.socket.close();
>>>>>>> 1ef33136a3dbab7d4d28131e681b1cc2d9fdf6ab

	}

}