package piotrek.projektinzynierski;

import android.os.AsyncTask;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Transmission extends AsyncTask {

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            String mUDPAdress="192.168.0.26";
            int server_port = 9999;
            String messageStr = "Time : " + (long) params[0] + " Diameter : "
                    + (double) params[1] + " Flash  : " + (boolean) params[2];
            DatagramSocket s = new DatagramSocket();
            InetAddress local = InetAddress.getByName(mUDPAdress);
            int msg_length = messageStr.length();
            byte[] message = messageStr.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(message, msg_length, local, server_port);
            s.send(packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
