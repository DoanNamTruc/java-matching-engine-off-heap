/*
package exchange.core;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Random;

public final class ClientTest {

    public static void main(String[] args) throws Exception {
        try (Socket s = new Socket("localhost", 9000)) {
            DataOutputStream out =
                    new DataOutputStream(s.getOutputStream());
//            public long orderId;
//            public int symbol;
//            public long price;
//            public long qty;
//            public byte side; // 0=BUY, 1=SELL

            for (long i = 1; i <= 3; i++) {
                int side = new java.util.Random().nextInt(2);
                out.writeLong(i); // orderId
                out.writeInt(1); // symbol
                out.writeLong(side* 2000 + new Random().nextInt(4000 - 100 + 1) + 100); // price
                out.writeLong(new Random().nextInt(4000 - 100 + 1) + 100); // qty
                out.writeByte(side); // side (0=BUY)
                out.flush();

                Thread.sleep(1000);
            }


            out.flush();


        }
    }
}

*/
