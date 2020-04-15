package edu.bo.app.asteroid_multiplayer_server;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Vector;

public class SoundMixer {

    private int chunkSize = 4096;
    volatile public boolean running = true;

    public Vector<Chatter> chatters = new Vector<>();

    public SoundMixer() {};

    // only 16-bit int depth
    private byte[] mixBuffers(byte[] bufferA, byte[] bufferB) {
        byte[] array = new byte[bufferA.length];

        for (int i = 0; i < bufferA.length; i += 2) {
            short buf1A = bufferA[i + 1];
            short buf2A = bufferA[i];
            buf1A = (short) ((buf1A & 0xff) << 8);
            buf2A = (short) (buf2A & 0xff);

            short buf1B = bufferB[i + 1];
            short buf2B = bufferB[i];
            buf1B = (short) ((buf1B & 0xff) << 8);
            buf2B = (short) (buf2B & 0xff);

            short buf1C = (short) (buf1A + buf1B);
            short buf2C = (short) (buf2A + buf2B);

            short res = (short) (buf1C + buf2C);

            array[i] = (byte) res;
            array[i + 1] = (byte) (res >> 8);
        }
        return array;
    }

    public void start() {
        Thread th = new Thread() {

            @Override
            public void run() {
                byte outputBuffer[] = new byte[chunkSize];
                byte buffer[] = new byte[chunkSize];
                Boolean first;
                while (running) {
                    try {
                        first = true;
                        for (Chatter chatter : chatters) {
                            try {
                                chatter.getInputStream()
                                       .read(buffer);
                                if (first) {
                                    outputBuffer = buffer;
                                    first = false;
                                    continue;
                                }
                                outputBuffer = mixBuffers(buffer, outputBuffer);
                            } catch (IOException e) {
                                e.printStackTrace();
                                try {
                                    chatter.closeConnection();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        for (Chatter chatter : chatters) {
                            try {
                                chatter.getOutputStream()
                                       .write(outputBuffer);
                                chatter.getOutputStream()
                                       .flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                                try {
                                    chatter.closeConnection();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    } catch (ConcurrentModificationException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        th.start();
    }
}
