/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package candemo;

import javax.xml.bind.DatatypeConverter;


/**
 * CAN message
 * 
 * Handle the conversion between binary and ascii/hex format CAN messages.
 * In-line coding instead of loops, used in hopes of speeding conversions.
 * 
 * @author deh
 */
public class Canmsg2 {
    
    public int seq; // Sequence number (if used)
    /**
     * 32b word with CAN id (STM32 CAN format)
     */
    public int id;  // 32b word with CAN id (STM32 CAN format)
    public int dlc; // Payload count (number of bytes in payload)
    
    
    public byte[] pb;// Binary bytes as received and converted from ascii/hex input
    public int chk; // Byte checksum
    public int val; // After a conversion: Zero = no error; not zero = error
   
    
//    private int p0;  // Assembled Integer of payload bytes [0]-[3]
//    private int p1;  // Assembled Integer of payload bytes [4]-[7]

    
    
    /* *********************************************************************
    * Constructors
    * ********************************************************************* */
    public Canmsg2(){
        seq = 0; id = 0; dlc = 0; pb = new byte[15];  val = 0;
    }
    public Canmsg2(int iseq, int iid){
        seq = iseq; id = iid; pb = new byte[15]; val = 0;
    }
    public Canmsg2(int iseq, int iid, int idlc){
        seq = iseq; id = iid; dlc = idlc; pb = new byte[15]; val = 0;
    }
    public Canmsg2(int iseq, int iid, int idlc, byte[] px){
        seq = iseq; id = iid; dlc = idlc; pb = new byte[15]; pb = px;val = 0;
    }
    /* *************************************************************************
    Compute CAN message checksum on binary array
    param   : byte[] b: Array of binary bytes in check sum computation\
    param   : int m: Number of bytes in array to use in computation
    return  : computed checksum 
    ************************************************************************ */
      private byte checksum(int m){
          /* Convert pairs of ascii/hex chars to a binary byte */
          int chktot = 0xa5a5;    // Initial value for computing checksum
          for (int i = 0; i < m; i ++){ 
              chktot += (pb[i] & 0xff);  // Build total (int) from byte array
      }
           /* Add in carries and carry from adding carries */
          chktot += (chktot >> 16); // Add carries from low half word
          chktot += (chktot >> 16); // Add carry from above addition
          chktot += (chktot >> 8);  // Add carries from low byte
          chktot += (chktot >> 8);  // Add carry from above addition  
          return (byte)chktot;
      }    
      /** *********************************************************************
    * Check message for errors and Convert incoming ascii/hex CAN msg to an array of bytes
    *   plus assemble the bytes comprising CAN ID into an int.
   
     * @param msg
     * msg = String with ascii/hex of a CAN msg
     * @return  
     *  * Return: 0 = OK;
    *        -1 = message too short (less than 14)
    *        -2 = message too long (greater than 30)
    *        -3 = number of bytes not even
    *        -4 = payload count is negative or greater than 8
    *        -5 = checksum error
    * *********************************************************************
     */
   public int convert_msgtobin (String msg){
    int m = msg.length();
    if (m < 14) return -1;  // Too short for a valid CAN msg
    if (m > 30) return -2;  // Longer than the longest CAN msg
    if ((m & 0x1) != 0) return -3; // Not even: asci1: hex must be pairs
    
    pb = DatatypeConverter.parseHexBinary(msg); // Convert ascii/hex to byte array
    
    /* Check computed checksum versus recieved checksum.  */
    byte chkx = checksum((m/2) - 1);
    if (chkx != pb[((m/2)-1)]){ 
        /* System.out is for debugging */
        System.out.println(msg);
        for (int j = 0; j < (m/2); j++){
           System.out.format("%02X ",pb[j]);
        }
        System.out.format("chkx: %02X" + " pb[((m/2) -1)]: %02X\n", chkx, pb[((m/2) -1)] );
        return -5; // Return error code
     }
    
    /* Check that the payload count is within bounds */
    if (pb[5] < 0) return -4;    // This should not be possible
    if (pb[5] > 8) return -4;    // Too large means something wrong.
       
    /* Extract some items that are of general use */
    seq = (pb[0] & 0xff);     // Sequence number
    dlc = (pb[5] & 0xff);     // Save payload ct in an easy to remember variable
    id = ((((((pb[4] << 8) | (pb[3] & 0xff)) << 8) | (pb[2] & 0xff)) << 8) | (pb[1] & 0xff));
   
    return 0;
   }
   /**
    * Combine four payload bytes to an Integer,
    * @param 
    *       offset of first byte in payload (0 - 4)
    * @return 
    *       int
    */
   public int get_1int(int offset){
       if ( pb[5] < (offset+4) ){val = -1; return 0;} // Return not enough payload
       return (((((((pb[(offset+9)]) << 8) | (pb[(offset+8)] & 0xff)) << 8) | (pb[(offset+7)] & 0xff)) << 8) | (pb[(offset+6)] & 0xff));
   }
    /**
     * Convert bytes to an int array
     * @return
     */
   public int[] get_2int(){
       int[] nt2 = {0,0};
       if (pb[5] < 8) {val = -1; return nt2;}   // Return: not enough payload bytes
        nt2[0] = (((((((pb[ 9] & 0xff) << 8) | (pb[ 8] & 0xff)) << 8) | (pb[ 7] & 0xff)) << 8) | (pb[ 6] & 0xff));
        nt2[1] = (((((((pb[13] & 0xff) << 8) | (pb[12] & 0xff)) << 8) | (pb[11] & 0xff)) << 8) | (pb[10] & 0xff));
        return nt2;
   }
   /**
    * Combine payload bytes [0]-[7] to one long 
    */
   public long get_1long(){
       if (pb[5] != 8){ val = -1; return 0;}
        int p0  = (((((((pb[ 9] & 0xff) << 8) | (pb[ 8] & 0xff)) << 8) | (pb[ 7] & 0xff)) << 8) | (pb[ 6] & 0xff));
        int p1 = (((((((pb[13] & 0xff) << 8) | (pb[12] & 0xff)) << 8) | (pb[11] & 0xff)) << 8) | (pb[10] & 0xff));
        long pl = ( ((long)p1 << 32) | (p0 & 0xffffffffL) ); // Combine to make a long
       return pl;
   }
   /**
    * 
    * @param offset range (0 - 6)
    * @return short, or zero if offset error
    */
   public short get_1short(int offset){
       if (pb[5] < (offset+2) ) return 0;
       return (short)(((pb[(offset+7)]) << 8) | (pb[(offset+6)] & 0xff));
   }
   /**
    * 
    * @return array of 4 shorts, use pb[5] (dlc] to determine valid number
    */
   public short[] get_shorts(){
       if (pb[5] < 2){ 
           short sta5[] = {0,0,0,0,0}; 
           return sta5;
       }
       short sta4[] = {0,0,0,0};
       for (int i = 0; i < pb[5]; i += 2){
           sta4[i/2] = (short)(((pb[(i+7)]) << 8) | (pb[(i+6)] & 0xff));     
       }
       return sta4;
   }
   /**
    * Prepare CAN msg: Convert the array pb[] to hex and add checksum
    * The binary array pb[] is expected to have been set up.
    *
     * @return 
     *   String with ascii/hex in ready to send
    */
    public String msg_prep(){  // Convert payload bytes from byte array
        
       /* A return of 'null' indicates an error */
       if (dlc > 8) return null;
       if (dlc < 0) return null;
       
       /* Setup Id bytes, little endian */
       pb[1] = (byte)(id);
       pb[2] = (byte)((id >>  8));
       pb[3] = (byte)((id >> 16));
       pb[4] = (byte)((id >> 24));
       
       pb[5] = (byte)dlc;    // Payload size
       
       int msglength = (dlc + 6); // Length not including checksum
  
       pb[(msglength)] = checksum(msglength); // Place checksum in array
      
       /* Convert binary array to ascii/hex */
       StringBuilder x = new StringBuilder(DatatypeConverter.printHexBinary(pb));
       x.append("\n"); // Line terminator
       
       return x.toString();
    }
   /**
    * Convert to payload byte array little endian
    */
    public void set_int0(int n){
        pb[6] = (byte)(n);
        pb[7] = (byte)((n >>  8));
        pb[8] = (byte)((n >> 16));
        pb[9] = (byte)((n >> 24));
        dlc = 4;   // set payload count (dlc)
    }
   /**
    * Convert to payload byte array little endian
    */
    public void set_int1(int n){
        pb[10] = (byte)(n);
        pb[11] = (byte)((n >>  8));
        pb[12] = (byte)((n >> 16));
        pb[13] = (byte)((n >> 24));
        dlc = 8;   // set payload count (dlc)
    }
   /**
    * Convert long to payload byte array little endian
    */
    public void set_1long(long l){
        pb[ 6] = (byte)( l       );
        pb[ 7] = (byte)((l >>  8));
        pb[ 8] = (byte)((l >> 16));
        pb[ 9] = (byte)((l >> 24));
        pb[10] = (byte)((l >> 32));
        pb[11] = (byte)((l >> 40));
        pb[12] = (byte)((l >> 48));
        pb[13] = (byte)((l >> 56));
        dlc = 8;   // set payload count (dlc)
    }
   /**
    * Convert shorts to payload byte array, little endian 
    */
    private boolean set_nshort(Short[] s){
        int x;
        x = s.length;
        if (x == 0){    // JIC
            dlc = 0; return true; // Set payload size and return
        }
        if (x < 0) return false;    // Should not be possible
        if (x > 4) return false;    // Oops!
        
        for (int i = 0; i < x; x += 1){
            pb[((2*i) + 6)] = (byte)((s[i] >> 8));
            pb[((2*i) + 7)] = (byte)(s[i] & 0xff);
        }
        dlc = (x * 2);  // Set payload size
        return true;
    }
}
