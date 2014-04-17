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
 *
 * @author deh
 */
/*
 *   Decode methods for extracting data from payload extneded by 
 *   @author gsm
 *
 */
public class CanCnvt
{

    public int val; // Valid indicator (zero if valid)

    public int seq; // Sequence number (if used)
    /**
     * 32b word with CAN id (STM32 CAN format)
     */
    public int id;  // 32b word with CAN id (STM32 CAN format)
    public int dlc; // Payload count (number of bytes in payload)

    public byte[] pb;// Binary bytes as received and converted from ascii/hex input
    public int chk; // Byte checksum

    /* *********************************************************************
     * Constructors
     * ********************************************************************* */
    public CanCnvt()
    {
            pb = new byte[15];
    }

    public CanCnvt(int iseq, int iid)
    {
        seq = iseq;
        id = iid;
        pb = new byte[15];
    }

    public CanCnvt(int iseq, int iid, int idlc)
    {
        val = 0;    // Initialize as valid
        seq = iseq;
        id = iid;
        dlc = idlc;
        pb = new byte[15];

    }
    public CanCnvt(int iseq, int iid, int idlc, byte[] px)
    {
        val = 0;    // Initialize as valid
        seq = iseq;
        id = iid;
        dlc = idlc;
        pb = new byte[15]; pb = px;

    }
    /* *************************************************************************
     Compute CAN message checksum on binary array
     param   : byte[] b: Array of binary bytes in check sum computation\
     param   : int m: Number of bytes in array to use in computation
     return  : computed checksum 
     ************************************************************************ */
    private byte checksum(int m)
    {
        /* Convert pairs of ascii/hex chars to a binary byte */
        int chktot = 0xa5a5;    // Initial value for computing checksum
        for (int i = 0; i < m; i++)
        {
            chktot += (pb[i] & 0xff);  // Build total (int) from byte array
        }
        /* Add in carries and carry from adding carries */
        chktot += (chktot >> 16); // Add carries from low half word
        chktot += (chktot >> 16); // Add carry from above addition
        chktot += (chktot >> 8);  // Add carries from low byte
        chktot += (chktot >> 8);  // Add carry from above addition  
        return (byte) chktot;
    }

    /**
     * Check message for errors and Convert incoming ascii/hex CAN msg
     * to an array of bytes plus assemble the bytes comprising CAN ID 
     * into an int.
     *
     * @param msg msg = String with ascii/hex of a CAN msg
     * @return * Return: 
     *  0 = OK; 
     * -1 = message too short (less than 14) 
     * -2 = message too long (greater than 30) 
     * -3 = number of bytes not even 
     * -4 = payload count is negative or greater than 8 -5 = checksum error
     */
    public int convert_msgtobin(String msg)
    {
        int m = msg.length();
        if (m < 14)
        {
            return -1;  // Too short for a valid CAN msg
        }
        if (m > 30)
        {
            return -2;  // Longer than the longest CAN msg
        }
        if ((m & 0x1) != 0)
        {
            return -3; // Not even: asci1: hex must be pairs
        }
        pb = DatatypeConverter.parseHexBinary(msg); // Convert ascii/hex to byte 
        // array

        /* Check computed checksum versus recieved checksum.  */
        byte chkx = checksum((m / 2) - 1);
        if (chkx != pb[((m / 2) - 1)])
        {
            System.out.println(msg);    // Display for debugging
            for (int j = 0; j < (m / 2); j++)
            {
                System.out.format("%02X ", pb[j]);
            }
            System.out.format("chkx: %02X" + " pb[((m/2) -1)]: %02X\n", chkx,
                    pb[((m / 2) - 1)]);
            return -5; // Return error code
        }

        /* Check that the payload count is within bounds */
        if (pb[5] < 0)
        {
            return -4;    // This should not be possible
        }
        if (pb[5] > 8)
        {
            return -4;    // Too large means something wrong.
        }
        /* Extract some items that are of general use */
        seq = (pb[0] & 0xff);     // Sequence number byte->unsigned
        dlc = (pb[5] & 0xff);     // Save payload ct in an easy to remember variable
        id = ((((((pb[4] << 8) | (pb[3] & 0xff)) << 8)
                | (pb[2] & 0xff)) << 8) | (pb[1] & 0xff));
        return 0;
    }

    /**
     * Extract long from payload
     *
     * @return payload bytes [0]-[7] as one long
     */
    public long get_long()
    {
        if (pb[5] != 8)
        {
            val = -1;   // insufficient payload length
            return 0;
        } else
        {
            int x0 = (((((pb[9] << 8 | (pb[8] & 0xff)) << 8)
                    | (pb[7] & 0xff)) << 8) | (pb[6] & 0xff));
            int x1 = ((((((pb[13] << 8) | (pb[12] & 0xff)) << 8)
                    | (pb[11] & 0xff)) << 8) | (pb[10] & 0xff));
            // Combine to make a long
            long lng = ((long)x1 << 32) | (((long) x0) & 0x0000ffffL);
            val = 0;
            return lng;
        }
    }
    /**
     * Extract four payload bytes to an int,
     *
     * @param offset of first byte in payload (0 - 4)
     * @return int, val = -1 if insufficient payload bytes
     */
    public int get_int(int offset)
    {
        if (pb[5] < (offset + 4))
        {
            val = -1;   // insufficient payload length
            return 0;
        } else
        {
            int nt = ((((((pb[offset + 9] << 8)
                    | (pb[offset + 8] & 0xff)) << 8)
                    | (pb[offset + 7] & 0xff)) << 8)
                    | (pb[offset + 6] & 0xff));
            val = 0;
            return nt;
        }
    }
    /**
     * Convert bytes to an int array
     *
     * @param offset is offset from start of payload
     * @param numb is number of ints to convert
     * @return int[] of values, val = -1 if insufficient payload bytes
     */
    public int[] get_ints(int offset, int numb)
    {
        if (pb[5] < offset + numb * 4)
        {
            val = -1;   // insufficient payload length;
            return new int[0];
        } else
        {
            int[] nt = new int[numb];
            for (int i = 0; i < numb; i++)
            {
                int i4o = i * 4 + offset;
                nt[i] = ((((((pb[i4o + 9] << 8)
                        | (pb[i4o + 8] & 0xff)) << 8)
                        | (pb[i4o + 7] & 0xff)) << 8)
                        | (pb[i4o + 6] & 0xff));
            }
            val = 0;
            return nt;
        }
    }

    /**
     * Combine four payload bytes to an uint
     *
     * @param offset of first byte in payload (0 - 4)
     * @return long to hold full range, val = -1 if insufficient payload bytes
     */
    public long get_uint(int offset)
    {
        if (pb[5] < (offset + 4))
        {
            val = -1;   // insufficient payload length
            return 0;
        } else
        {
            long lng;
            lng = ((long) (((((pb[offset + 9] & 0xff) << 8)
                    | (pb[offset + 8] & 0xff)) << 8)
                    | (pb[offset + 7] & 0xff)) << 8)
                    | (pb[offset + 6] & 0xff);
            val = 0;
            return lng;
        }
    }

    /**
     *
     * Extract short from payload
     *
     * @param offset range (0 - 6)
     * @return short, val = -1 if insufficient payload bytes
     */
    public short get_short(int offset)
    {
        short st;
        if (pb[5] < (offset + 2))
        {
            val = -1;   // insufficient payload length
            return 0;
        } else
        {
            st = (short) ((pb[offset + 7] << 8)
                    | (pb[offset + 6] & 0xff));
        }
        val = 0;
        return st;

    }

    /**
     *
     * Extract shorts from payload
     *
     * @param offset range (0 - 6)
     * @param numb number of shorts (0-4)
     * @return short[], length 0 with val = -1 if insufficient payload bytes
     */
    public short[] get_shorts(int offset, int numb)
    {
        if (pb[5] < offset + numb * 2)
        {
            val = -1;   // insufficient payload length
            return new short[0];
        } else
        {
            short[] sh = new short[numb];
            for (int i = 0; i < numb; i++)
            {
                int i2o = i * 2 + offset;
                sh[i] = (short) ((pb[i2o + 7] << 8)
                        | (pb[i2o + 6] & 0xff));
            }
            val = 0;
            return sh;
        }
    }

    /**
     *
     * Extract ushort from payload
     *
     * @param offset range (0 - 6)
     * @return int, val = -1 if insufficient payload bytes
     */
    public int get_ushort(int offset)
    {
        if (pb[5] < (offset + 2))
        {
            val = -1;   // insufficient payload length
            return 0;
        } else
        {
            int ust = ((pb[offset + 7] & 0xff) << 8)
                    | (pb[offset + 6] & 0xff);
            return ust;
        }
    }

    /**
     *
     * Extract shorts from payload
     *
     * @param offset range (0 - 6)
     * @param numb number of shorts (0-4)
     * @return short[], length 0 with val = -1 if error
     */
    public int[] get_ushorts(int offset, int numb)
    {
        int[] ust = new int[numb];
        if (pb[5] < offset + numb * 2)
        {
            val = -1;   // insufficient payload length
            return new int[0];
        } else
        {
            
            for (int i = 0; i < numb; i++)
            {
                int i2o = i * 2 + offset;
                ust[i] = ((pb[i2o + 7] & 0xff) << 8)
                        | (pb[i2o + 6] & 0xff);
           
            }
        }
        val = 0;
        return ust;

    }

    /**
     *
     * Extract byte from payload
     *
     * @param offset range (0 - 7)
     * @return byte, 0 with val = -1 if insufficient payload
     */
    public byte get_byte(int offset)
    {
        if (pb[5] < (offset + 1))
        {
            val = -1;   // insufficient payload length
            return 0;
        } else
        {
            byte by = pb[offset + 6];
            val = 0;
            return by;
        }
    }

    /**
     *
     * Extract bytes from payload
     *
     * @param offset range (0 - 7)
     * @param numb number of shorts (0 - 4)
     * @return short[], length 0 with val = -1 if insufficient payload bytes
     */
    public byte[] get_bytes(int offset, int numb)
    {
        if (pb[5] < offset + numb)
        {
            val = -1;   // insufficient payload length
            return new byte[0];
        } else
        {
            byte[] by = new byte[numb];
            for (int i = 0; i < numb; i++)
            {
                by[i] = pb[offset + 6];
            }
            val = 0;
            return by;
        }
    }

    /**
     *
     * Extract ubytes from payload
     *
     * @param offset range (0 - 7)
     * @return short, with val = -1 if insufficient payload
     */
    public short get_ubyte(int offset)
    {
        if (pb[5] < (offset + 1))
        {
            val = -1;   // insufficient payload length
            return 0;
        } else
        {
            return (short) (pb[offset + 6] & 0xff);
        }
    }

    /**
     *
     * Extract ubytes from payload
     *
     * @param offset range (0 - 7)
     * @param numb number of shorts (0-4)
     * @return short[], length 0 with val = -1 if insufficient payload bytes
     */
    public short[] get_ubytes(int offset, int numb)
    {
        if (pb[5] < offset + numb)
        {
            val = -1;   // insufficient payload length
            return new short[0];
        } else
        {
            short[] sh = new short[numb];
            for (int i = 0; i < numb; i++)
            {
                int ipo = offset + i;
                sh[i] = (short) (pb[ipo + 6] & 0xff);
            }
            val = 0;
            return sh;
        }
    }

    /**
     * Convert bytes to an uint array
     *
     * @param offset is offset from start of payload
     * @param numb is number of ints to convert
     * @return long[] to hold full range, length 0 if error
     */
    public long[] get_uints(int offset, int numb)
    {
        if (pb[5] < offset + numb * 4)
        {
            val = -1;   // insufficient payload length
            return new long[0];
        } else
        {
            long[] lng = new long[numb];
            for (int i = 0; i < numb; i++)
            {
                int i4o = offset + i * 4;
                lng[i] = ((long) (((((pb[i4o + 9] & 0xff) << 8)
                        | (pb[i4o + 8] & 0xff)) << 8)
                        | (pb[i4o + 7] & 0xff)) << 8)
                        | (pb[i4o + 6] & 0xff);
            }
            val = 0;
            return lng;
        }
    }

    /**
     * Prepare CAN msg: Convert the array pb[] to hex and add checksum The
     * binary array pb[] is expected to have been set up.
     *
     * @return String with ascii/hex in ready to send
     */
    public String msg_prep()
    {  // Convert payload bytes from byte array

        /* A return of 'null' indicates an error */
        if (dlc > 8)
        {
            return null;
        }
        if (dlc < 0)
        {
            return null;
        }

        /* Setup Id bytes, little endian */
        pb[1] = (byte) (id        );
        pb[2] = (byte) ((id >> 8) );
        pb[3] = (byte) ((id >> 16));
        pb[4] = (byte) ((id >> 24));

        pb[5] = (byte) dlc;    // Payload size

        int msglength = (dlc + 6); // Length not including checksum

        pb[(msglength)] = checksum(msglength); // Place checksum in array

        /* Convert binary array to ascii/hex */
        StringBuilder x = new StringBuilder(DatatypeConverter.printHexBinary(pb));
        x.append("\n"); // Line terminator

        return x.toString();
    }

    /**
     * @param big endian int to be stored in byte array little endian Convert to
     * payload byte array little endian offset
     * *********************************************************************
     */
    public void set_int(int n, int offset)
    {
        if ((offset > (dlc - 4)))
        {
            val = -1;   //  offset too large
        } else
        {
            pb[6 + offset] = (byte) (n        );
            pb[7 + offset] = (byte) ((n >> 8) );
            pb[8 + offset] = (byte) ((n >> 16));
            pb[9 + offset] = (byte) ((n >> 24));
        }
    }

    /**
     * @param Big endian int. Convert to payload byte array little endian Set
     * dlc to 8
     */
    public void set_ints(int[] n)
    {
        if ((n.length != 2) | dlc != 8)
        {
            val = -1;   //  should only be used to insert two ints
        } else
        {
            pb[ 6] = (byte) ( n[0]       );
            pb[ 7] = (byte) ((n[0] >>  8));
            pb[ 8] = (byte) ((n[0] >> 16));
            pb[ 9] = (byte) ((n[0] >> 24));
            pb[10] = (byte) ( n[1]       );
            pb[11] = (byte) ((n[1] >>  8));
            pb[12] = (byte) ((n[1] >> 16));
            pb[13] = (byte) ((n[1] >> 24));
            val = 0;
        }
    }

    /**
     * @param Big endian long. Convert long to payload byte array little endian
     *
     * *********************************************************************
     */
    public void set_long(long lng)
    {
        if (dlc != 8)
        {
            val = -1;
        } else
        {
            pb[ 6] = (byte) (lng        );
            pb[ 7] = (byte) ((lng >>  8));
            pb[ 8] = (byte) ((lng >> 16));
            pb[ 9] = (byte) ((lng >> 24));
            pb[10] = (byte) ((lng >> 32));
            pb[11] = (byte) ((lng >> 40));
            pb[12] = (byte) ((lng >> 48));
            pb[13] = (byte) ((lng >> 56));
            val = 0;
        }
    }

    /**
     * @param Short[] s holds shorts to be stored in payload, little endian
     * Convert shorts to payload byte array, little endian
     * @return false: array length doesn't accommodate payload count
     */
    private void set_nshort(Short[] s)
    {
        int x;
        x = s.length;
        if (x == 0)
        {    // JIC
            dlc = 0;
            val = -1; return; // Set payload size and return
        }
        if (x < 0)
        {
            val = -2; return;    // Should not be possible
        }
        if (x > 4)
        {
            val = -3; return;    // Oops!
        }
        for (int i = 0; i < x; x += 1)
        {
            pb[((2 * i) + 6)] = (byte) ((s[i] >>  8));
            pb[((2 * i) + 7)] = (byte) ( s[i] & 0xff);
        }
        dlc = (x * 2);  // Set payload size
        val = 0; return;
    }

}