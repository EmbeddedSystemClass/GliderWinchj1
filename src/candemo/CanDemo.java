/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package candemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 *
 * @author deh
 */
public class CanDemo {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {        
        String ip;
        ip = "127.0.0.1";   // Default ip address
        //String ip = new String("10.1.1.80");
        int port = new Integer (32123); // Default port
        
    /* Deal with the arguments on the command line */
        if (args.length > 2){
            System.out.format("Only two args allowed, we saw %d\n", args.length);
            System.exit(-1);
        } 
        if (args.length == 2){
            ip = args[0];
            port = Integer.parseInt(args[1]);
        }
   Socket socket = new Socket(ip, port);
   BufferedReader in = 
           new BufferedReader(
                   new InputStreamReader(socket.getInputStream()));
 //  OutputStream outstream = socket.getOutputStream(); 
//   PrintWriter out = new PrintWriter(outstream);
   
   OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
   

    Canmsg2 can1; int ret;
        can1 = new Canmsg2();    // Received CAN message
        
    CanCnvt cvt1;   // Received CAN message
        cvt1 = new CanCnvt();
        
        int[] nta;
            nta = new int[2];
            
    Canmsg2 can2;                // Transmit CAN message
        can2 = new Canmsg2();
        
    
     int seqsend = 0;
        
    String xmt_msg;
    
    long tim0; 
    int[] eng;
        eng = new int[4];
    int shft1_ct;
    int shft1_rpm;
    int msgctr = 0;
    int msgctr_next = 0;
    
    
    long unixTime;
    /* ===================================================================== */    
    /* Endless loop displays messages */
   while (true) {
    String msg = in.readLine();         // Get a line from socket
    ret = can1.convert_msgtobin(msg);   // Convert ascii/hex msg to binary byte array
    if (ret != 0){ // Did the conversion pass all the checks?
        System.out.format("Input conversion error: %d\n", ret); // No. Show the error code
        continue;
    }
    ret = cvt1.convert_msgtobin(msg);
    if (ret != 0){
        System.out.format("Input conversion error: %d\n", ret); // No. Show the error code
        continue;
    }

//    System.out.format("    %02X " + "%08X " + "%02X\n",can1.seq, can1.id, can1.dlc);
//    System.out.format("%2d: " + "%s\n",msg.length(), msg);
    /* At this point we have the CAN msg in byte array, and the id in converted to a word */
    
    /* Time sync message: Output date/time for 1/64th tick = 0. */
    if (can1.id == 0x00200000){ // Is this a time sync ID?
        //tim0 = can1.get_1long(); 
        tim0 = cvt1.get_long(); // Yes. Convert payload to a long
        if (tim0 > 0){        // Was the conversion good?
            if ((tim0 & 0x3f) == 0){ /* Is this 64/sec time tick an even seconds tick? */
            /* Yes.  Shift to conver 1/64th ticks to secs, and  add Epoch shift */ 
                unixTime = (tim0 >> 6) + 1318478400;
                java.util.Date time=new java.util.Date(unixTime*1000); // Convert to printble format
                System.out.println(time.toString());   // Pretty printout
            }
        } else{
                System.out.format("TIME CONVERT ERR %s\n",msg); // Argh!
        }
    }
    
    /* Select, extract, and convert the two CAN msgs that have two 4 bit ints, from the engine sensor */
    if (can1.id == 0x40800000){ /* Is this CAN ID an engine sensor message? */
//            nta = can1.get_2int();     // Convert payload to two ints
            nta = cvt1.get_ints(0, 2);  //  Convert payload to two ints
            eng[0] = nta[0];   // Save the values if we want it later
            eng[1] = nta[1]; 
int z0; int z1;
z0 = cvt1.get_int(0);
cvt1.valerr();
if (z0 != eng[0]){System.out.format("z0:%08X " + "z1: %08X\n",z0, eng[0]);}
z1 = cvt1.get_int(4);
cvt1.valerr();
if (z1 != eng[1]){System.out.format("z0:%08X " + "z1: %08X\n",z1, eng[1]);}

//            System.out.format("P0:%08x " + "P1:%08x " + "%s\n", eng[0], eng[1], msg);
    }
        if (can1.id == 0x30800000){ /* Is this CAN ID the other engine sensor message? */
//            nta = can1.get_2int();     // Convert payload to two ints
            nta = cvt1.get_ints(0, 2);  // Convert payload to two ints
            eng[2] = nta[0];   // Save the values if we want it later
            eng[3] = nta[1];
 //           System.out.format("[2]:%08x " + "[3]:%08x " + "%s\n", eng[2], eng[3], msg);
 //           System.out.format("eng[0]:%08x " + "eng[1]:%08x " + "eng[2]:%08x " + "eng[3]:%08x " + "%s\n", eng[0], eng[1], eng[2], eng[3], msg);
 //           System.out.format("%d\n", eng[1]);
            short[] sta;
                sta = new short[4];
            sta = can1.get_shorts();
    }
        
        
    /* Select, extract, and convert shaft encoder and rpm to two ints */
    if (can1.id == 0x31e00000){ /* Is this CAN ID an engine sensor message? */
//            nta = can1.get_2int();     // Convert payload to two ints
            nta = cvt1.get_ints(0, 2);  // Convert payload to two ints
            shft1_ct  = nta[0];   // Save the values if we want it later
            shft1_rpm = nta[1];
//            System.out.format("shft1_ct:%08x " + "shft1_rpm:%08x " + "%s\n", shft1_ct, shft1_rpm, msg);
//            System.out.format("shft1_ct:%5d "  + "shft1_rpm:%5d\n", shft1_ct, shft1_rpm);
            
            /* For a test periodically send a message with the same data that id 0x31e00000 sent. */
            msgctr += 1;    // Count the number of messages coming in.
            if (msgctr >= msgctr_next){ // Is it time to send one back?
                msgctr_next = msgctr + 8;   // Set the next count to send count
                /* Setup up a test msg to send */
                CanCnvt cvt2 = new CanCnvt(seqsend,0x31e00004,8,can1.pb);
//                CanCnvt cvt2 = new CanCnvt();
//               can2.id = 0x31e00004;   // Use this CAN id (29 bit)
//                can2.dlc = 8;           // Payload count
//                can2.pb = can1.pb;      // Copy the payload from the incoming can msg
                // Note: pb[0] is the seq number, and will be the one received with can1 
//                xmt_msg = can2.msg_prep();  // Convert to ascii/hex with checksum
                xmt_msg = cvt2.msg_prep();  // Convert to ascii/hex with checksum
//                System.out.format("xmt_msg: length %d: " + "%s",xmt_msg.length(),xmt_msg);// Look at msg
                out.write(xmt_msg,0,xmt_msg.length());  // Send the msg to the socket
                out.flush();  // DO NOT FORGET THIS !  Otherwise the msgs pile up until a buffer fills.
            }
    }
    
    } // End of endless while loop
    }
    
}
